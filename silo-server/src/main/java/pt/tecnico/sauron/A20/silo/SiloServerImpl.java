package pt.tecnico.sauron.A20.silo;


import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.A20.silo.domain.SauronCamera;
import pt.tecnico.sauron.A20.silo.domain.Silo;
import pt.tecnico.sauron.A20.silo.grpc.*;



public class SiloServerImpl extends SauronGrpc.SauronImplBase{
    /** Sauron implementation. */
    private Silo silo = new Silo();

    @Override
    public void camJoin(CamJoinRequest request, StreamObserver<CamJoinResponse> responseObserver) {
        CamJoinResponse.Builder builder = CamJoinResponse.newBuilder();
        try {
            SauronCamera newCam = new SauronCamera(request.getName(), request.getCoordinates().getLatitude(), request.getCoordinates().getLongitude());
            silo.addCamera(newCam);
            builder.setStatus(Status.OK);
        }
        catch(Exception e) {
            //TODO handle exceptions
        }

        CamJoinResponse response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void camInfo(CamInfoRequest request, StreamObserver<CamInfoResponse> responseObserver) {
        CamInfoResponse.Builder builder = CamInfoResponse.newBuilder();
        try {
            SauronCamera cam = silo.getCamByName(request.getName());
            builder.setStatus(Status.OK);
            builder.setCoordinates(Coordinates.newBuilder().setLatitude(cam.getLatitude()).setLongitude(cam.getLongitude()).build());
        }
        catch (NullPointerException e) {
            builder.setStatus(Status.INEXISTANT_CAMERA);
        }

        CamInfoResponse response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void report(ReportRequest request, StreamObserver<ReportResponse> responseObserver) {

    }

    @Override
    public void track(TrackRequest request, StreamObserver<TrackResponse> responseObserver) {

    }

    @Override
    public void trackMatch(TrackMatchRequest request, StreamObserver<TrackMatchResponse> responseObserver) {

    }

    @Override
    public void trace(TraceRequest request, StreamObserver<TraceResponse> responseObserver) {

    }

}
