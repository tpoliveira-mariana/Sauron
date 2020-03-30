package pt.tecnico.sauron.A20.silo.client;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.sauron.A20.exceptions.SauronException;
import pt.tecnico.sauron.A20.silo.grpc.*;
import pt.tecnico.sauron.A20.silo.grpc.Object;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static pt.tecnico.sauron.A20.exceptions.ErrorMessage.*;

public class SiloFrontend {

    public SiloFrontend() {}

    public void camJoin(String target, String name, double lat, double lon) throws SauronException {
        SauronGrpc.SauronBlockingStub stub = getStub(target);

        Coordinates coordinates = Coordinates.newBuilder().setLatitude(lat).setLongitude(lon).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setName(name).setCoordinates(coordinates).build();

        Status status = stub.camJoin(request).getStatus();
        if (status != Status.OK) {
            throw reactToStatus(status);
        }
    }

    public double[] camInfo(String target, String name) throws SauronException {
        SauronGrpc.SauronBlockingStub stub = getStub(target);

        CamInfoRequest request = CamInfoRequest.newBuilder().setName(name).build();
        CamInfoResponse response = stub.camInfo(request);

        Status status = response.getStatus();
        if (status == Status.OK) {
            return new double[]{response.getCoordinates().getLatitude(), response.getCoordinates().getLongitude()};
        }
        else {
            throw reactToStatus(status);
        }
    }

    public void report(String target, String name, List<List<String>> observations) throws SauronException{
        SauronGrpc.SauronBlockingStub stub = getStub(target);

        ReportRequest.Builder builder = ReportRequest.newBuilder().setName(name);

        for (List<String> observation : observations){
            ObjectType type = stringToType(observation.get(0));
            Object builderObj = Object.newBuilder().setType(type).setId(observation.get(1)).build();
            builder.addObject(builderObj);
        }

        ReportRequest request = builder.build();
        ReportResponse response = stub.report(request);

        Status status = response.getStatus();
        if (status != Status.OK)
            throw reactToStatus(status);
    }

    public String track(String target, String type, String id) throws SauronException {
        SauronGrpc.SauronBlockingStub stub = getStub(target);

        TrackRequest request = TrackRequest.newBuilder()
                .setType(stringToType(type))
                .setId(id)
                .build();

        TrackResponse response = stub.track(request);
        if (response.getStatus() == Status.OK) {
            return printObservation(response.getObservation());
        } else {
            throw reactToStatus(response.getStatus());
        }
    }

    public List<String> trackMatch(String target, String type, String id) throws SauronException {
        SauronGrpc.SauronBlockingStub stub = getStub(target);

        TrackMatchRequest request = TrackMatchRequest.newBuilder()
                .setType(stringToType(type))
                .setId(id)
                .build();

        TrackMatchResponse response = stub.trackMatch(request);
        if (response.getStatus() == Status.OK) {
            return response.getObservationsList()
                    .stream()
                    .sorted(Comparator.comparing(obs -> obs.getObject().getId()))
                    .map(this::printObservation)
                    .collect(Collectors.toList());
        } else {
            throw reactToStatus(response.getStatus());
        }
    }

    public List<String> trace(String target, String type, String id) throws SauronException {
        SauronGrpc.SauronBlockingStub stub = getStub(target);

        TrackMatchRequest request = TrackMatchRequest.newBuilder()
                .setType(stringToType(type))
                .setId(id)
                .build();

        TrackMatchResponse response = stub.trackMatch(request);
        if (response.getStatus() == Status.OK) {
            return response.getObservationsList().stream()
                    .map(this::printObservation)
                    .collect(Collectors.toList());
        } else {
            throw reactToStatus(response.getStatus());
        }
    }

    public String ctrlPing(String target, String input) throws SauronException {
        SauronGrpc.SauronBlockingStub stub = getStub(target);

        PingRequest request = PingRequest.newBuilder().setInput(input).build();

        PingResponse response = stub.ctrlPing(request);
        if (response.getStatus() == Status.OK) {
            return response.getOutput();
        } else {
            throw reactToStatus(response.getStatus());
        }
    }

    public void ctrlClear(String target) throws SauronException {
        SauronGrpc.SauronBlockingStub stub = getStub(target);

        ClearRequest request = ClearRequest.getDefaultInstance();

        ClearResponse response = stub.ctrlClear(request);
        if (response.getStatus() != Status.OK)
            throw reactToStatus(response.getStatus());
    }

    public void ctrl_init(String target, String fileName) throws SauronException {
        try {
            File file = new File(fileName);
            Scanner scanner = new Scanner(file);
            String camName = null;
            List<List<String>> data = new ArrayList<>();

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                StringTokenizer st = new StringTokenizer(line, ",");
                String type = st.nextToken();
                if (type.equals("done") && !data.isEmpty() && camName != null) {
                    report(target, camName, data);
                }
                if (type.equals("cam")) {
                    if (!data.isEmpty()) {
                        report(target, camName, data);
                        data.clear();
                    }
                    camName = st.nextToken();
                    camJoin(target, camName, Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()));
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
        return typeToString(obs.getObject().getType()) + ", "
                + obs.getObject().getId() + ", "
                + ts.substring(0, ts.lastIndexOf('.')) + ", "
                + obs.getCam().getName() + ", "
                + obs.getCam().getCoordinates().getLatitude() + ", "
                + obs.getCam().getCoordinates().getLongitude();
    }

    private SauronGrpc.SauronBlockingStub getStub(String target) {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        return  SauronGrpc.newBlockingStub(channel);
    }

    private SauronException reactToStatus(Status status) {
        switch (status) {
            case DUPLICATE_CAMERA:
                return new SauronException(DUPLICATE_CAMERA);
            case DUPLICATE_CAM_NAME:
                return new SauronException(DUPLICATE_CAM_NAME);

            case INVALID_NAME:
            case INEXISTENT_CAMERA:
                return new SauronException(INVALID_CAM_NAME);

            case INVALID_COORDINATES:
                return new SauronException(INVALID_COORDINATES);
            case INVALID_ID:
                return new SauronException(INVALID_ID);
            case INVALID_TYPE:
                return new SauronException(TYPE_DOES_NOT_EXIST);
            case OBJECT_NOT_FOUND:
                return new SauronException(OBJECT_NOT_FOUND);
            case INVALID_ARGUMENT:
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
