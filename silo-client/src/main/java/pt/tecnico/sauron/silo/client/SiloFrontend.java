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
import java.util.regex.Pattern;

public class SiloFrontend {

    private SauronGrpc.SauronBlockingStub _stub;

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

    private Map<String, Any> responses = new HashMap<>();

    public SiloFrontend(String zooHost, String zooPort, int instance) throws ZKNamingException {
        nameServer = new ZKNaming(zooHost,zooPort);
        List<ZKRecord> replicas = new ArrayList<>(nameServer.listRecords(SERVER_PATH));
        replicaNum = replicas.size();
        prevTS = new ArrayList<>(Collections.nCopies(replicaNum, 0));
        connect(instance);
    }

    public void connect(int instance) throws ZKNamingException{
        String path = SERVER_PATH + "/" + instance;
        if (instance == -1) {
            List<ZKRecord> replicas = new ArrayList<>(nameServer.listRecords(SERVER_PATH));
            System.out.println(path);
            path = replicas.get((new Random()).nextInt(replicas.size())).getPath();
        }
        ZKRecord record = nameServer.lookup(path);
        String target = record.getURI();
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        _stub = SauronGrpc.newBlockingStub(channel);
    }

    public void camJoin(String name, double lat, double lon) throws SauronException {
        checkCamera(name, lat, lon);

        try {
            Coordinates coordinates = Coordinates.newBuilder().setLatitude(lat).setLongitude(lon).build();
            CamJoinRequest request = CamJoinRequest.newBuilder().setName(name)
                                                                .setCoordinates(coordinates)
                                                                .setVector(VectorTS.newBuilder().addAllTs(this.prevTS).build())
                                                                .build();

            CamJoinResponse response = _stub.camJoin(request);
            List<Integer> valueTS = response.getVector().getTsList();
            getConsistentResponse(response, valueTS, "camJoin"+name+lat+lon, CamJoinResponse.class);
        }
        catch (StatusRuntimeException e) {
            getConsistentError(e, "camJoin"+name+lat+lon, CamJoinResponse.class);
        }
    }

    public double[] camInfo(String name) throws SauronException {
        checkCameraName(name);

        try {
            CamInfoRequest request = CamInfoRequest.newBuilder().setName(name).build();

            CamInfoResponse response = _stub.camInfo(request);
            List<Integer> valueTS = response.getVector().getTsList();
            response = getConsistentResponse(response, valueTS, "camInfo"+name, CamInfoResponse.class);
            /*if (this.tsAfter(valueTS, this.prevTS)) {
                this.prevTS = valueTS;
                responses.put("camInfo"+name, Any.pack(response));
            } else {
                response = responses.get("camInfo"+name).unpack(CamInfoResponse.class);
            }*/

            return new double[]{response.getCoordinates().getLatitude(), response.getCoordinates().getLongitude()};
        }
        catch (StatusRuntimeException e) {
            CamInfoResponse response = getConsistentError(e, "camInfo"+name, CamInfoResponse.class);
            return new double[]{response.getCoordinates().getLatitude(), response.getCoordinates().getLongitude()};
        }/* catch (InvalidProtocolBufferException e) {
            throw new SauronException(ErrorMessage.UNKNOWN);
        }*/
    }

    public void report(String name, List<List<String>> observations) throws SauronException{
        checkCameraName(name);
        boolean error = false;
        ErrorMessage errorMessage = ErrorMessage.UNKNOWN;
        String query = "";
        for (List<String> observation : observations)
            query += observation.get(0) + observation.get(1);

        try {
            ReportRequest.Builder builder = ReportRequest.newBuilder().setName(name);
            for (List<String> observation : observations) {
                try {
                    checkObjectArguments(observation.get(0), observation.get(1), false);
                    ObjectType type = stringToType(observation.get(0));
                    Object builderObj = Object.newBuilder().setType(type)
                                                           .setId(observation.get(1)).build();
                    builder.addObject(builderObj);
                } catch (SauronException e){
                    if (!error)
                        errorMessage = e.getErrorMessage();
                    error = true;
                }
            }

            ReportRequest request = builder.setVector(VectorTS.newBuilder().addAllTs(this.prevTS).build()).build();

            ReportResponse response = _stub.report(request);
            List<Integer> valueTS = response.getVector().getTsList();
            getConsistentResponse(response, valueTS, query, ReportResponse.class);
        }
        catch (StatusRuntimeException e) {
            getConsistentError(e, query, ReportResponse.class);
        }
        if (error)
            throw new SauronException(errorMessage);
    }

    public TrackResponse track(String type, String id) throws SauronException {
        try {
            checkObjectArguments(type, id, false);
            TrackRequest request = TrackRequest.newBuilder()
                    .setType(stringToType(type))
                    .setId(id)
                    .build();

            TrackResponse response = _stub.track(request);
            List<Integer> valueTS = response.getVector().getTsList();
            return getConsistentResponse(response, valueTS, "track"+type+id, TrackResponse.class);
            /*if (this.tsAfter(valueTS, this.prevTS)) {
                this.prevTS = valueTS;
                responses.put("track"+type+id, Any.pack(response));
                return response;
            } else {
                return responses.get("track"+type+id).unpack(TrackResponse.class);
            }*/
        } catch (StatusRuntimeException e) {
            return getConsistentError(e, "track"+type+id, TrackResponse.class);
        }/* catch (InvalidProtocolBufferException e) {
            throw new SauronException(ErrorMessage.UNKNOWN);
        }*/

    }

    public TrackMatchResponse trackMatch(String type, String id) throws SauronException {
        try {
            checkObjectArguments(type, id, true);
            TrackMatchRequest request = TrackMatchRequest.newBuilder()
                    .setType(stringToType(type))
                    .setId(id)
                    .build();

            TrackMatchResponse response = _stub.trackMatch(request);
            List<Integer> valueTS = response.getVector().getTsList();
            return getConsistentResponse(response, valueTS, "trackMatch"+type+id, TrackMatchResponse.class);
            /*if (this.tsAfter(valueTS, this.prevTS)) {
                this.prevTS = valueTS;
                responses.put("trackMatch"+type+id, Any.pack(response));
                return response;
            } else {
                return responses.get("trackMatch"+type+id).unpack(TrackMatchResponse.class);
            }*/
        } catch (StatusRuntimeException e) {
            return getConsistentError(e, "trackMatch"+type+id, TrackMatchResponse.class);
        }/* catch (InvalidProtocolBufferException e) {
            throw new SauronException(ErrorMessage.UNKNOWN);
        }*/
    }

    public TraceResponse trace(String type, String id) throws SauronException {
        try {
            checkObjectArguments(type, id, false);
            TraceRequest request = TraceRequest.newBuilder()
                    .setType(stringToType(type))
                    .setId(id)
                    .build();

            TraceResponse response = _stub.trace(request);
            List<Integer> valueTS = response.getVector().getTsList();
            return getConsistentResponse(response, valueTS, "trace"+type+id, TraceResponse.class);
            /*if (this.tsAfter(valueTS, this.prevTS)) {
                this.prevTS = valueTS;
                responses.put("trace"+type+id, Any.pack(response));
                return response;
            } else {
                return responses.get("trace"+type+id).unpack(TraceResponse.class);
            }*/

        } catch (StatusRuntimeException e) {
            return getConsistentError(e, "trace"+type+id, TraceResponse.class);
        }/* catch (InvalidProtocolBufferException e) {
            throw new SauronException(ErrorMessage.UNKNOWN);
        }*/
    }

    public String ctrlPing(String input) throws SauronException {
        try {
            PingRequest request = PingRequest.newBuilder().setInput(input).build();

            PingResponse response = _stub.ctrlPing(request);
            return response.getOutput();
        }
        catch (StatusRuntimeException e) {
            throw properException(e);
        }
    }

    public void ctrlClear() throws SauronException {
        try {
            ClearRequest request = ClearRequest.getDefaultInstance();
            _stub.ctrlClear(request);
        }
        catch (StatusRuntimeException e) {
            throw properException(e);
        }
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

    private SauronException properException(StatusRuntimeException e) {
        if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
            return new SauronException(ErrorMessage.REFUSED);
        }
        return reactToStatus(e.getStatus().getDescription());
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
                char firstChar = id.charAt(2 * i);
                char secChar = id.charAt(2 * i + 1);
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

}