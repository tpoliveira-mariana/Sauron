package pt.tecnico.sauron.A20.silo.client;

import com.google.protobuf.util.Timestamps;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.A20.exceptions.SauronException;
import pt.tecnico.sauron.A20.silo.grpc.*;
import pt.tecnico.sauron.A20.silo.grpc.Object;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

import static pt.tecnico.sauron.A20.exceptions.ErrorMessage.*;

public class SiloFrontend {

    private SauronGrpc.SauronBlockingStub _stub;

    public SiloFrontend(String host, String port) {
        String target = host + ":" + port;
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        _stub = SauronGrpc.newBlockingStub(channel);
    }

    public void camJoin(String name, double lat, double lon) throws SauronException {
        if (name == null)
            throw new SauronException(INVALID_CAM_NAME);

        try {
            Coordinates coordinates = Coordinates.newBuilder().setLatitude(lat).setLongitude(lon).build();
            CamJoinRequest request = CamJoinRequest.newBuilder().setName(name).setCoordinates(coordinates).build();

            _stub.camJoin(request);
        }
        catch (StatusRuntimeException e) {
            throw properException(e);
        }
    }

    public double[] camInfo(String name) throws SauronException {
        if (name == null)
            throw new SauronException(INVALID_CAM_NAME);

        try {
            CamInfoRequest request = CamInfoRequest.newBuilder().setName(name).build();

            CamInfoResponse response = _stub.camInfo(request);

            return new double[]{response.getCoordinates().getLatitude(), response.getCoordinates().getLongitude()};
        }
        catch (StatusRuntimeException e) {
            throw properException(e);
        }
    }

    public void report(String name, List<List<String>> observations) throws SauronException{
        try {
            ReportRequest.Builder builder = ReportRequest.newBuilder().setName(name);

            for (List<String> observation : observations) {
                ObjectType type = stringToType(observation.get(0));
                Object builderObj = Object.newBuilder().setType(type).setId(observation.get(1)).build();
                builder.addObject(builderObj);
            }

            ReportRequest request = builder.build();
            _stub.report(request);
        }
        catch (StatusRuntimeException e) {
            throw properException(e);
        }
    }

    public String track(String type, String id) throws SauronException {
        try {
            TrackRequest request = TrackRequest.newBuilder()
                    .setType(stringToType(type))
                    .setId(id)
                    .build();

            TrackResponse response = _stub.track(request);
            return printObservation(response.getObservation());
        }
        catch (StatusRuntimeException e) {
            throw properException(e);
        }

    }

    public List<String> trackMatch(String type, String id) throws SauronException {
        try {
            TrackMatchRequest request = TrackMatchRequest.newBuilder()
                    .setType(stringToType(type))
                    .setId(id)
                    .build();

            TrackMatchResponse response = _stub.trackMatch(request);
            return response.getObservationsList()
                    .stream()
                    .sorted(getComparator(response))
                    .map(this::printObservation)
                    .collect(Collectors.toList());
        }
        catch (StatusRuntimeException e) {
            throw properException(e);
        }
    }

    public List<String> trace(String type, String id) throws SauronException {
        try {
            TraceRequest request = TraceRequest.newBuilder()
                    .setType(stringToType(type))
                    .setId(id)
                    .build();

            TraceResponse response = _stub.trace(request);
            return response.getObservationsList().stream()
                    .map(this::printObservation)
                    .collect(Collectors.toList());
        }
        catch (StatusRuntimeException e) {
            throw properException(e);
        }
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

        } catch (SauronException e) {
            throw e;
        } catch (FileNotFoundException e) {
            throw new SauronException(ERROR_PROCESSING_FILE);
        }
    }

    private String printObservation(Observation obs) {
        String ts = Timestamps.toString(obs.getTimestamp());
        return typeToString(obs.getObject().getType()) + ","
                + obs.getObject().getId() + ","
                + ts.substring(0, ts.lastIndexOf('.')) + ","
                + obs.getCam().getName() + ","
                + obs.getCam().getCoordinates().getLatitude() + ","
                + obs.getCam().getCoordinates().getLongitude();
    }

    private Comparator<Observation> getComparator(TrackMatchResponse response) {
        ObjectType type = response.getObservationsCount() == 0 ?
                ObjectType.CAR : response.getObservations(0).getObject().getType();
        switch (type) {
            case PERSON:
                return Comparator.comparingLong(obs -> Long.parseLong(obs.getObject().getId()));
            case CAR:
                return Comparator.comparing(obs -> obs.getObject().getId());
            default:
                return Comparator.comparing(Observation::toString);
        }
    }

    private SauronException properException(StatusRuntimeException e) {
        if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
            return new SauronException(REFUSED);
        }
        return reactToStatus(e.getStatus().getDescription());
    }

    private SauronException reactToStatus(String msg) {
        if (msg == null)
            return new SauronException(UNKNOWN);
        switch (msg) {
            case "DUPLICATE_CAMERA":
                return new SauronException(DUPLICATE_CAMERA);
            case "DUPLICATE_CAM_NAME":
                return new SauronException(DUPLICATE_CAM_NAME);
            case "INVALID_CAM_NAME":
                return new SauronException(INVALID_CAM_NAME);
            case "CAMERA_NOT_FOUND":
                return new SauronException(CAMERA_NOT_FOUND);
            case "INVALID_COORDINATES":
                return new SauronException(INVALID_COORDINATES);
            case "INVALID_PERSON_IDENTIFIER":
                return new SauronException(INVALID_PERSON_IDENTIFIER);
            case "INVALID_CAR_ID":
                return new SauronException(INVALID_CAR_ID);
            case "INVALID_ID":
                return new SauronException(INVALID_ID);
            case "TYPE_DOES_NOT_EXIST":
                return new SauronException(TYPE_DOES_NOT_EXIST);
            case "OBJECT_NOT_FOUND":
                return new SauronException(OBJECT_NOT_FOUND);
            case "INVALID_ARGUMENT":
                return new SauronException(INVALID_ARGUMENT);
            default:
                return new SauronException(UNKNOWN);
        }
    }

    private ObjectType stringToType(String type) throws SauronException {
        switch (type){
            case "person":
                return ObjectType.PERSON;
            case "car":
                return ObjectType.CAR;
            default:
                throw new SauronException(TYPE_DOES_NOT_EXIST);
        }
    }

    private String typeToString(ObjectType type) {
        switch (type){
            case PERSON:
                return "person";
            case CAR:
                return "car";
            default:
                return null;
        }
    }


}