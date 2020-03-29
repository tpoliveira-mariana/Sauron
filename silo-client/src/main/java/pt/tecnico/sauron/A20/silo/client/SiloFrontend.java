package pt.tecnico.sauron.A20.silo.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.A20.silo.grpc.*;

import java.util.List;
import java.util.Optional;

public class SiloFrontend {
    private final double INEXISTANT_COORD = 200;


    public SiloFrontend() { }

    public void camJoin(String target, String name, double lat, double lon) {
        SauronGrpc.SauronBlockingStub stub = getStub(target);

        Coordinates coordinates = Coordinates.newBuilder().setLatitude(lat).setLongitude(lon).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setName(name).setCoordinates(coordinates).build();

        Status status = stub.camJoin(request).getStatus();
        if (status == Status.OK) {
            return;
        }
        else {
            reactToStatus(status);
        }
    }

    public double[] camInfo(String target, String name) {
        SauronGrpc.SauronBlockingStub stub = getStub(target);

        CamInfoRequest request = CamInfoRequest.newBuilder().setName(name).build();
        CamInfoResponse response = stub.camInfo(request);

        Status status = response.getStatus();
        if (status == Status.OK) {
            return new double[]{response.getCoordinates().getLatitude(), response.getCoordinates().getLongitude()};
        }
        else {
            reactToStatus(status);
            return new double[]{INEXISTANT_COORD, INEXISTANT_COORD};
        }



    }

    public void report(String target, String name, List<List<String>> observations) {
        SauronGrpc.SauronBlockingStub stub = getStub(target);

        ReportRequest.Builder builder = ReportRequest.newBuilder().setName(name);

        for (List<String> observation : observations){
            ObjectType type = getObjectType(observation.get(0));
            Observation builderObs = Observation.newBuilder().setType(type).setId(observation.get(1)).build();
            builder.addObservations(builderObs);
        }

        ReportRequest request = builder.build();
        ReportResponse response = stub.report(request);

        Status status = response.getStatus();
        //TODO- create appropriate status types
        if (status != Status.OK)
            reactToStatus(status);

    }

    public void track(String target, String type, String id) {
        System.out.println("Track");
    }

    public void trackMatch(String target, String type, String id) {
        System.out.println("TrackMatch");
    }

    public void trace(String target, String type, String id) {
        System.out.println("Trace");
    }

    private SauronGrpc.SauronBlockingStub getStub(String target) {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        return  SauronGrpc.newBlockingStub(channel);
    }

    private void reactToStatus(Status status) {
        switch (status) {
            case INVALID_NAME:
                //TODO throw exception
                System.out.println("Invalid name");
                break;
            case INVALID_COORDINATES:
                //TODO throw exception
                System.out.println("Invalid coordinates");
                break;
            default:
                //TODO throw exception
        }
    }

    private ObjectType getObjectType(String type) {
        switch (type){
            case "person":
                return ObjectType.PERSON;
            case "car":
                return ObjectType.CAR;
            default:
                //TODO- throw exception
                return ObjectType.UNRECOGNIZED;
        }
    }

}
