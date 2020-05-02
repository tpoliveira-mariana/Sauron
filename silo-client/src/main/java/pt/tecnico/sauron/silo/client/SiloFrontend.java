package pt.tecnico.sauron.silo.client;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.exceptions.ErrorMessage;
import pt.tecnico.sauron.exceptions.SauronException;
import pt.tecnico.sauron.silo.grpc.Object;
import pt.tecnico.sauron.silo.grpc.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

public class SiloFrontend {

    private SauronGrpc.SauronBlockingStub _stub;
    private ManagedChannel _channel;

    // fully detect valid cam name
    private static final Pattern CAM_PATT = Pattern.compile("[A-Za-z0-9]{3,15}+");

    // partially detect invalid person partial id
    private static final Pattern INVAL_PERSON_PATT = Pattern.compile("0+.*[*].*|.*[*][*].*|.*[^0-9*].*");

    // partially detect invalid car partial id
    private static final Pattern INVAL_CAR_PATT = Pattern.compile(".*[*][*].*|.*[^A-Z0-9*].*");

    private final ZKNaming nameServer;
    private static final String SERVER_PATH = "/grpc/sauron/silo";
    private List<Integer> prevTS;
    private final int replicaNum;
    private int currentInstance;

    private static final int STUB_TIMEOUT = 5000;
    private static final int STUB_TIMEOUT_DEMO1 = 10;
    private boolean demo1 = false;
    private int timeout = STUB_TIMEOUT;

    private Map<String, Any> responses = new HashMap<>();

    public SiloFrontend(String zooHost, String zooPort, int instance) throws ZKNamingException, SauronException {
        nameServer = new ZKNaming(zooHost,zooPort);
        List<ZKRecord> replicas = new ArrayList<>(nameServer.listRecords(SERVER_PATH));
        replicaNum = replicas.size();
        prevTS = new ArrayList<>(Collections.nCopies(replicaNum, 0));
        connect(instance);
    }

    public void connect(int instance) throws ZKNamingException, SauronException {
        String path = SERVER_PATH + "/" + instance;
        ZKRecord record = null;
        if (instance != -1) {
            try {
                display("Trying connection to instance: " + instance);
                record = nameServer.lookup(path);
                currentInstance = instance;
            } catch (ZKNamingException e) {
                display("Replica with path " + path + " not found in registry");
                instance = -1;
            }
        }
        if (instance == -1) {
            display("Choosing random replica...");
            List<ZKRecord> replicas = new ArrayList<>(nameServer.listRecords(SERVER_PATH));
            if (replicas.isEmpty()) {
                display("There are no available replicas. Failing...");
                throw new SauronException(ErrorMessage.REFUSED);
            }
            record = replicas.get((new Random()).nextInt(replicas.size()));
            path = record.getPath();
            currentInstance = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
        }
        try {
            String target = record == null ? "" : record.getURI();
            _channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
            _stub = SauronGrpc.newBlockingStub(_channel);
            display("Connected to: " + path);
        } catch (IllegalArgumentException e) {
            display("Couldn't build channel. Failing...");
            throw new SauronException(ErrorMessage.UNKNOWN);
        }
    }

    private SauronGrpc.SauronBlockingStub timedStub() {
        return _stub.withDeadlineAfter(timeout, TimeUnit.MILLISECONDS);
    }

    public void camJoin(String name, double lat, double lon) throws SauronException {
        checkCamera(name, lat, lon);
        Coordinates coordinates = Coordinates.newBuilder().setLatitude(lat).setLongitude(lon).build();
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCam(Cam.newBuilder().setName(name).setCoordinates(coordinates).build())
                .setVector(VectorTS.newBuilder().addAllTs(this.prevTS).build())
                .setOpId(UUID.randomUUID().toString())
                .build();

        doUpdate(req -> timedStub().camJoin(req), request, res -> res.getVector().getTsList());
    }

    public CamInfoResponse camInfo(String name) throws SauronException {
        checkCameraName(name);
        CamInfoRequest request = CamInfoRequest.newBuilder().setName(name).build();

        return doQuery(req -> timedStub().camInfo(req), request, "camInfo"+name, CamInfoResponse.class, res -> res.getVector().getTsList());
    }

    public void report(String name, List<List<String>> observations) throws SauronException {
        checkCameraName(name);
        ErrorMessage errorMessage = null;

        Report.Builder builder = Report.newBuilder().setName(name);
        for (List<String> observation : observations) {
            try {
                checkObjectArguments(observation.get(0), observation.get(1), false);
                builder.addObject(Object.newBuilder().setType(stringToType(observation.get(0))).setId(observation.get(1)).build());
            } catch (SauronException e) {
                if (errorMessage == null) errorMessage = e.getErrorMessage();
            }
        }
        ReportRequest request = ReportRequest.newBuilder()
                .setReport(builder.build())
                .setVector(VectorTS.newBuilder().addAllTs(this.prevTS).build())
                .setOpId(UUID.randomUUID().toString()).build();

        doUpdate(req -> timedStub().report(req), request, res -> res.getVector().getTsList());
        if (errorMessage != null) throw new SauronException(errorMessage);
    }

    public TrackResponse track(String type, String id) throws SauronException {
        checkObjectArguments(type, id, false);
        TrackRequest request = TrackRequest.newBuilder().setType(stringToType(type)).setId(id).build();

        return doQuery(req -> timedStub().track(req), request, "track"+type+id, TrackResponse.class, res -> res.getVector().getTsList());
    }

    public TrackMatchResponse trackMatch(String type, String id) throws SauronException {
        checkObjectArguments(type, id, true);
        TrackMatchRequest request = TrackMatchRequest.newBuilder().setType(stringToType(type)).setId(id).build();

        return doQuery(req -> timedStub().trackMatch(req), request, "trackMatch"+type+id, TrackMatchResponse.class, res -> res.getVector().getTsList());
    }

    public TraceResponse trace(String type, String id) throws SauronException {
        checkObjectArguments(type, id, false);
        TraceRequest request = TraceRequest.newBuilder().setType(stringToType(type)).setId(id).build();

        return doQuery(req -> timedStub().trace(req), request, "trace"+type+id, TraceResponse.class, res -> res.getVector().getTsList());
    }

    public String ctrlPing(String input) throws SauronException {
        PingResponse response = null;
        int instance = currentInstance;
        PingRequest request = PingRequest.newBuilder().setInput(input).build();
        boolean failed;
        do {
            failed = false;
            try {
                response = _stub.ctrlPing(request);
            } catch (StatusRuntimeException e) {
                if (reconnectOnFail(e, instance)) {
                    failed = true;
                    instance = -1;
                } else throw properException(e);
            }
        } while (failed);
        return response.getOutput();
    }

    public void ctrlClear() throws SauronException {
        boolean failed;
        int instance = currentInstance;
        ClearRequest request = ClearRequest.getDefaultInstance();
        do {
            failed = false;
            try {
                _stub.ctrlClear(request);
            }
            catch (StatusRuntimeException e) {
                if (reconnectOnFail(e, instance)) {
                    failed = true;
                    instance = -1;
                } else throw properException(e);
            }
        } while (failed);
    }

    public void ctrlInit(String fileName) throws SauronException {
        try {
            File file = new File(fileName);
            Scanner scanner = new Scanner(file);
            String camName = null;
            List<List<String>> data = new ArrayList<>();

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                StringTokenizer st = new StringTokenizer(line, ",");
                String type = st.nextToken();

                if (type.equals("done")) {
                    if (!data.isEmpty() && camName != null) {
                        report(camName, data);
                    }
                    break;
                }
                else if (type.equals("cam")) {
                    if (!data.isEmpty()) {
                        report(camName, data);
                        data.clear();
                    }
                    camName = st.nextToken();
                    camJoin(camName, Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()));
                }
                else {
                    List<String> obs = new ArrayList<>();
                    obs.add(type);
                    obs.add(st.nextToken());
                    data.add(obs);
                }
            }
            scanner.close();

        } catch (FileNotFoundException e) {
            throw new SauronException(ErrorMessage.ERROR_PROCESSING_FILE);
        }
    }

    private <Q extends Message, S extends Message> S doQuery(Function<Q, S> command, Q request, String query, Class<S> responseClass, Function<S, List<Integer>> getter) throws SauronException {
        boolean failed;
        int instance = currentInstance;
        S response = null;

        do {
            failed = false;
            try {
                display("Sending request...");
                response = command.apply(request);
                List<Integer> valueTS = getter.apply(response);
                display("Received response with TS: " + valueTS);
                response = getConsistentResponse(response, valueTS, query, responseClass);
            } catch (StatusRuntimeException e) {
                if (reconnectOnFail(e, instance)) {
                    failed = true;
                    instance = -1;
                } else response = getConsistentError(e, query, responseClass);
            }
        } while (failed);
        return response;
    }

    private <Q extends Message, S extends Message> void doUpdate(Function<Q, S> command, Q request, Function<S, List<Integer>> getter) throws SauronException {
        boolean failed;
        int instance = currentInstance;

        do {
            failed = false;
            try {
                display("Sending request...");
                S response = command.apply(request);
                List<Integer> valueTS = getter.apply(response);
                display("Received response with TS: " + valueTS);
                this.prevTS = mergeTS(this.prevTS, valueTS);
                display("Updated prevTS to " + this.prevTS);
            } catch (StatusRuntimeException e) {
                if (demo1) {
                    try {
                        Thread.sleep(2000);
                    } catch (java.lang.InterruptedException error) {return;}
                    timeout = STUB_TIMEOUT;
                    instance = 2;
                }
                if (reconnectOnFail(e, instance)) {
                    failed = true;
                    instance = -1;
                } else throw properException(e);
            }
        } while (failed);
        if (demo1)
            timeout = STUB_TIMEOUT_DEMO1;
    }

    private SauronException properException(StatusRuntimeException e) {
        return reactToStatus(e.getStatus().getDescription());
    }

    private boolean reconnectOnFail(StatusRuntimeException exception, int instance) throws SauronException {
        Status.Code code = exception.getStatus().getCode();
        try {
            if (code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED) {
                display("Failed to connect to replica: " + code.toString());
                if (replicaNum > 1) {
                    _channel.shutdown();
                    if (instance == -1) display("Choosing another replica to connect to...");
                    else display("Retrying connection to instance " + instance + "...");
                    connect(instance);
                }
                return true;
            }
            return false;
        } catch (ZKNamingException e) {
            display("Couldn't connect to any replica. Failing...");
            throw new SauronException(ErrorMessage.REFUSED);
        }
    }


    private boolean tsAfter(List<Integer> ts1, List<Integer> ts2) {
        for (int i = 0; i < ts1.size(); i++) {
            if (ts1.get(i) < ts2.get(i))
                return false;
        }
        return true;
    }

    private <T extends Message> T getConsistentError(StatusRuntimeException exception, String query, Class<T> responseClass) throws SauronException {
        try {
            Any response = responses.get(query);
            if (response == null) {
                throw properException(exception);
            }
            return response.unpack(responseClass);
        } catch (InvalidProtocolBufferException e) {
            throw new SauronException(ErrorMessage.UNKNOWN);
        }
    }

    private <T extends Message> T getConsistentResponse(T response, List<Integer> valueTS, String query, Class<T> responseClass) throws SauronException {
        try {
            if (this.tsAfter(valueTS, this.prevTS)) {
                this.prevTS = valueTS;
                responses.put(query, Any.pack(response));
                return response;
            } else {
                if (responses.get(query) != null) {
                    return responses.get(query).unpack(responseClass);
                }
                throw new SauronException(ErrorMessage.UNKNOWN);
            }
        } catch (InvalidProtocolBufferException e) {
            throw new SauronException(ErrorMessage.UNKNOWN);
        }
    }

    private List<Integer> mergeTS(List<Integer> ts1, List<Integer> ts2) {
        List<Integer> merged = new ArrayList<>(Collections.nCopies(ts1.size(), 0));
        for (int i = 0; i < ts1.size(); i++) {
            merged.set(i, Math.max(ts1.get(i), ts2.get(i)));
        }
        return merged;
    }


    private void checkCamera(String name, double lat, double lon) throws SauronException{
        checkCameraName(name);
        checkCameraCoordinates(lat,lon);
    }

    private void checkCameraName(String name) throws SauronException{
        if (name == null || !CAM_PATT.matcher(name).matches()) {
            throw new SauronException(ErrorMessage.INVALID_CAM_NAME);
        }
    }

    private void checkCameraCoordinates(double lat, double lon) throws SauronException{
		if (lat > 90 || lat < -90 || lon > 180 || lon < -180) {
            throw new SauronException(ErrorMessage.INVALID_COORDINATES);
        }
    }


    private void checkObjectArguments(String type, String id, boolean partial) throws SauronException {
        switch (type) {
            case "car":
                checkCarId(id, partial);
                break;
            case "person":
                checkPersonId(id, partial);
                break;
            default:
                throw new SauronException(ErrorMessage.TYPE_DOES_NOT_EXIST);
        }
    }

    private void checkCarId(String id, boolean partial) throws SauronException {
        if (!partial || !id.contains("*")) {
            int numFields = 0;
            if (id.length() != 6)
                throw new SauronException(ErrorMessage.INVALID_CAR_ID);
            for (int i = 0; i <3; i++) {
                char firstChar = id.charAt(2*i);
                char secChar = id.charAt(2*i + 1);
                if (Character.isDigit(firstChar) && Character.isDigit(secChar))
                    numFields++;
                else if (!(Character.isUpperCase(firstChar) && Character.isUpperCase(secChar)))
                    throw new SauronException(ErrorMessage.INVALID_CAR_ID);
            }
            if (numFields == 3 || numFields == 0) {
                throw new SauronException(ErrorMessage.INVALID_CAR_ID);
            }
        } else if (id.length() > 6 || INVAL_CAR_PATT.matcher(id).matches()) {
            throw new SauronException(ErrorMessage.INVALID_CAR_ID);
        }
    }

    private void checkPersonId(String id, boolean partial) throws SauronException {
        if (!partial || !id.contains("*")) {
            try {
                if (Long.parseLong(id) <= 0 || (id.length()>0 && id.charAt(0)=='0'))
                    throw new SauronException(ErrorMessage.INVALID_PERSON_ID);
            } catch(NumberFormatException e) {
                throw new SauronException(ErrorMessage.INVALID_PERSON_ID);
            }
        } else if (INVAL_PERSON_PATT.matcher(id).matches()) {
            throw new SauronException(ErrorMessage.INVALID_PERSON_ID);
        }
    }

    private SauronException reactToStatus(String msg) {
        if (msg == null)
            return new SauronException(ErrorMessage.UNKNOWN);
        switch (msg) {
            case "DUPLICATE_CAMERA":
                return new SauronException(ErrorMessage.DUPLICATE_CAMERA);
            case "DUPLICATE_CAM_NAME":
                return new SauronException(ErrorMessage.DUPLICATE_CAM_NAME);
            case "INVALID_CAM_NAME":
                return new SauronException(ErrorMessage.INVALID_CAM_NAME);
            case "CAMERA_NOT_FOUND":
                return new SauronException(ErrorMessage.CAMERA_NOT_FOUND);
            case "INVALID_COORDINATES":
                return new SauronException(ErrorMessage.INVALID_COORDINATES);
            case "INVALID_PERSON_ID":
                return new SauronException(ErrorMessage.INVALID_PERSON_ID);
            case "INVALID_CAR_ID":
                return new SauronException(ErrorMessage.INVALID_CAR_ID);
            case "INVALID_ID":
                return new SauronException(ErrorMessage.INVALID_ID);
            case "TYPE_DOES_NOT_EXIST":
                return new SauronException(ErrorMessage.TYPE_DOES_NOT_EXIST);
            case "OBJECT_NOT_FOUND":
                return new SauronException(ErrorMessage.OBJECT_NOT_FOUND);
            case "INVALID_ARGUMENT":
                return new SauronException(ErrorMessage.INVALID_ARGUMENT);
            default:
                return new SauronException(ErrorMessage.UNKNOWN);
        }
    }

    private ObjectType stringToType(String type) throws SauronException {
        switch (type){
            case "person":
                return ObjectType.PERSON;
            case "car":
                return ObjectType.CAR;
            default:
                throw new SauronException(ErrorMessage.TYPE_DOES_NOT_EXIST);
        }
    }

    private static void display(String msg) {
        System.out.println(msg);
    }

    public void setDemo1(boolean state){
        this.demo1 = state;
        this.timeout = state ? STUB_TIMEOUT_DEMO1 : STUB_TIMEOUT;
    }
}