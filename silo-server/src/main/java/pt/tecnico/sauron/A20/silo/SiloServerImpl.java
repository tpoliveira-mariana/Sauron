package pt.tecnico.sauron.A20.silo;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.A20.silo.domain.SauronObservation;
import pt.tecnico.sauron.A20.silo.domain.Silo;
import pt.tecnico.sauron.A20.silo.grpc.*;

import java.io.FileDescriptor;


public class SiloServerImpl extends SauronGrpc.SauronImplBase{
    /** Sauron implementation. */
    private Silo silo = new Silo();

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
