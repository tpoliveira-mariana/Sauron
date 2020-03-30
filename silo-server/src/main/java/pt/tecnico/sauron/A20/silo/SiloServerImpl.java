package pt.tecnico.sauron.A20.silo;


import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.A20.exceptions.ErrorMessage;
import pt.tecnico.sauron.A20.silo.domain.*;
import pt.tecnico.sauron.A20.exceptions.SauronException;
import pt.tecnico.sauron.A20.exceptions.ErrorMessage.*;
import pt.tecnico.sauron.A20.silo.domain.SauronCamera;
import pt.tecnico.sauron.A20.silo.domain.Silo;
import pt.tecnico.sauron.A20.silo.grpc.*;
import pt.tecnico.sauron.A20.silo.grpc.Object;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static pt.tecnico.sauron.A20.exceptions.ErrorMessage.TYPE_DOES_NOT_EXIST;


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
        catch(SauronException e) {
            builder.setStatus(reactToException(e));
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
        catch (SauronException e) {
            builder.setStatus(reactToException(e));
        }

        CamInfoResponse response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void report(ReportRequest request, StreamObserver<ReportResponse> responseObserver) {
        ReportResponse.Builder builder = ReportResponse.newBuilder();
        SauronCamera cam;
        try {
            cam = silo.getCamByName(request.getName());
        } catch(SauronException e) {
            cam = null;
        }
        List<Object> objects = request.getObjectList();
        builder.setStatus(cam == null ? Status.INEXISTENT_CAMERA : Status.OK);
        for(Object obj: objects) {
            if (cam == null) break;

            try {
                SauronObject sauObj = silo.getObjectByTypeAndId(getObjectType(obj.getType()), obj.getId());
                SauronObservation observation = new SauronObservation(sauObj, cam, LocalDateTime.now());
                silo.addObservation(observation);
            }
            catch(SauronException e) {
                builder.setStatus(reactToException(e));
            }
        }

        ReportResponse response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void track(TrackRequest request, StreamObserver<TrackResponse> responseObserver) {
        TrackResponse.Builder builder = TrackResponse.newBuilder();
        try {
            SauronObservation sauObs = silo.track(getObjectType(request.getType()), request.getId());

            builder.setObservation(buildObs(sauObs)).setStatus(Status.OK);

        } catch (SauronException e) {
            builder.setStatus(reactToException(e));
        }

        TrackResponse response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void trackMatch(TrackMatchRequest request, StreamObserver<TrackMatchResponse> responseObserver) {
        TrackMatchResponse.Builder builder = TrackMatchResponse.newBuilder();

        try {
            List<SauronObservation> sauObs = silo.trackMatch(getObjectType(request.getType()), request.getId());

            if (sauObs.isEmpty())
                builder.setStatus(Status.OBJECT_NOT_FOUND);
            else
                builder.addAllObservations(sauObs.stream().map(this::buildObs).collect(Collectors.toList()))
                        .setStatus(Status.OK);
        } catch (SauronException e) {
            builder.setStatus(reactToException(e));
        }

        TrackMatchResponse response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void trace(TraceRequest request, StreamObserver<TraceResponse> responseObserver) {
        TraceResponse.Builder builder = TraceResponse.newBuilder();

        try {
            List<SauronObservation> sauObs = silo.trace(getObjectType(request.getType()), request.getId());

            builder.addAllObservations(sauObs.stream().map(this::buildObs).collect(Collectors.toList()))
                    .setStatus(Status.OK);

        } catch (SauronException e) {
            builder.setStatus(reactToException(e));
        }

        TraceResponse response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void ctrlPing(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        PingResponse.Builder builder = PingResponse.newBuilder();
        String input = request.getInput();
        if (input == null || input.isBlank()) {
            builder.setStatus(Status.INVALID_ARGUMENT);
        } else {
            String output = "Hello " + request.getInput() + "!";
            builder.setOutput(output).setStatus(Status.OK);
        }

        PingResponse response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void ctrlClear(ClearRequest request, StreamObserver<ClearResponse> responseObserver) {
        silo.clear();

        ClearResponse response = ClearResponse.newBuilder().setStatus(Status.OK).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private Observation buildObs(SauronObservation sauObs) {
        ObjectType type = sauObs.getObjectType().equals("car") ? ObjectType.CAR : ObjectType.PERSON;
        Timestamp ts;
        try {
            ts = Timestamps.parse(sauObs.getTimeStamp());
        } catch (ParseException e) {
            ts = Timestamp.getDefaultInstance();
        }

        Object object = Object.newBuilder().setType(type).setId(sauObs.getObjectId()).build();
        Coordinates coords = Coordinates.newBuilder()
                                .setLongitude(sauObs.getCamera().getLongitude())
                                .setLatitude(sauObs.getCamera().getLatitude()).build();
        Cam cam = Cam.newBuilder().setName(sauObs.getCamera().getName()).setCoordinates(coords).build();

        return Observation.newBuilder().setObject(object).setCam(cam).setTimestamp(ts).build();
    }

    private String getObjectType(ObjectType type) throws SauronException{
        switch (type){
            case PERSON:
                return "person";
            case CAR:
                return "car";
            default:
                throw new SauronException(TYPE_DOES_NOT_EXIST);
        }
    }

    private Status reactToException(SauronException e) {
        switch(e.getErrorMessage()) {
            case DUPLICATE_CAMERA:
                return Status.DUPLICATE_CAMERA;
            case DUPLICATE_CAM_NAME:
                return Status.DUPLICATE_CAM_NAME;
            case INVALID_COORDINATES:
                return Status.INVALID_COORDINATES;
            case INVALID_CAM_NAME:
                return Status.INVALID_NAME;
            case OBJECT_NOT_FOUND:
                return Status.OBJECT_NOT_FOUND;
            case INVALID_PERSON_IDENTIFIER:
            case INVALID_CAR_ID:
            case INVALID_ID:
                return Status.INVALID_ID;
            case TYPE_DOES_NOT_EXIST:
                return Status.INVALID_TYPE;
            default:
                return Status.UNRECOGNIZED;
        }
    }

}
