package pt.tecnico.sauron.A20.silo.client;

import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.A20.silo.grpc.*;

import java.util.List;

public class SiloFrontend {

    public SiloFrontend() { }

    public void camJoin(String name, double lat, double lon) {
        /*CurrentBoardResponse response = CurrentBoardResponse.newBuilder().setBoard(ttt.toString()).build();

        // Send a single response through the stream.
        responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();*/
    }

    public void camInfo(String name) {

    }

    public void report(String name, List<List<String>> observations) {

    }

    public void track(String type, String id) {

    }

    public void trackMatch(String type, String id) {

    }

    public void trace(String type, String id) {

    }





}
