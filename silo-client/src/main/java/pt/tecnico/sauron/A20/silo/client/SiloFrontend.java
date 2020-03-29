package pt.tecnico.sauron.A20.silo.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.sauron.A20.exceptions.SauronException;
import pt.tecnico.sauron.A20.silo.grpc.*;
import java.util.List;

import static pt.tecnico.sauron.A20.exceptions.ErrorMessage.*;

public class SiloFrontend {

    public SiloFrontend() { }

    public void camJoin(String target, String name, double lat, double lon) throws SauronException {
        SauronGrpc.SauronBlockingStub stub = getStub(target);

        Coordinates coordinates = Coordinates.newBuilder().setLatitude(lat).setLongitude(lon).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setName(name).setCoordinates(coordinates).build();

        Status status = stub.camJoin(request).getStatus();
        if (status != Status.OK) {
            reactToStatus(status);
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
            reactToStatus(status);
            return null;
        }



    }

    public void report(String target, String name, List<List<String>> observations) throws SauronException{
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
        if (status != Status.OK)
            reactToStatus(status);

    }

    public void track(String target, String type, String id) throws SauronException {
        System.out.println("Track");
    }

    public void trackMatch(String target, String type, String id) throws SauronException {
        System.out.println("TrackMatch");
    }

    public void trace(String target, String type, String id) throws SauronException {
        System.out.println("Trace");
    }

    private SauronGrpc.SauronBlockingStub getStub(String target) {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        return  SauronGrpc.newBlockingStub(channel);
    }

    private void reactToStatus(Status status) throws SauronException {
        switch (status) {
            case DUPLICATE_CAMERA:
                throw new SauronException(DUPLICATE_CAMERA);

            case INVALID_NAME:
            case INEXISTANT_CAMERA:
                throw new SauronException(INVALID_CAM_NAME);

            case INVALID_COORDINATES:
                throw new SauronException(INVALID_COORDINATES);
            case INVALID_ID:
                throw new SauronException(INVALID_ID);
            case INVALID_TYPE:
                throw new SauronException(TYPE_DOES_NOT_EXIST);
            default:
                //TODO throw exception
        }
    }

    private ObjectType getObjectType(String type) throws SauronException{
        switch (type){
            case "person":
                return ObjectType.PERSON;
            case "car":
                return ObjectType.CAR;
            default:
                throw new SauronException(TYPE_DOES_NOT_EXIST);
        }
    }

}
