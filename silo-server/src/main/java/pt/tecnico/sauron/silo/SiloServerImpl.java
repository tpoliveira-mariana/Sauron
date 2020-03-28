package pt.tecnico.sauron.silo;

import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.grpc.*;

public class SiloServerImpl extends SauronGrpc.SauronImplBase{
    /** Sauron implementation. */
    private pt.tecnico.sauron.silo.domain.Silo silo = new pt.tecnico.sauron.silo.domain.Silo();

    @Override
    public void camJoin(CamJoinRequest request, StreamObserver<CamJoinResponse> responseObserver) {
        /*CurrentBoardResponse response = CurrentBoardResponse.newBuilder().setBoard(ttt.toString()).build();

        // Send a single response through the stream.
        responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();*/
    }

    @Override
    public void camInfo(CamInfoRequest request, StreamObserver<CamInfoResponse> responseObserver) {

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
