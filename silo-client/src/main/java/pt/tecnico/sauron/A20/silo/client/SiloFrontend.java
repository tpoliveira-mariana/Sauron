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

    }

    public void track(String type, String id) {

    }

    public void trackMatch(String type, String id) {

    }

    public void trace(String type, String id) {

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
                throw new SauronException(INVALID_CAM_NAME);

            case INVALID_COORDINATES:
                throw new SauronException(INVALID_COORDINATES);
            default:
                //TODO throw exception
        }
    }





}
