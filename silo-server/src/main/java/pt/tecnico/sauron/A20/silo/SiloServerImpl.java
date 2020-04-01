package pt.tecnico.sauron.A20.silo;


import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.A20.exceptions.ErrorMessage;
import pt.tecnico.sauron.A20.silo.domain.*;
import pt.tecnico.sauron.A20.exceptions.SauronException;
import pt.tecnico.sauron.A20.silo.domain.SauronCamera;
import pt.tecnico.sauron.A20.silo.domain.Silo;
import pt.tecnico.sauron.A20.silo.grpc.*;
import pt.tecnico.sauron.A20.silo.grpc.Object;
import io.grpc.Status.*;

import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static io.grpc.Status.*;


public class SiloServerImpl extends SauronGrpc.SauronImplBase{
    /** Sauron implementation. */
    private Silo silo = new Silo();

    @Override
    public void camJoin(CamJoinRequest request, StreamObserver<CamJoinResponse> responseObserver) {
        CamJoinResponse.Builder builder = CamJoinResponse.newBuilder();
        try {
            SauronCamera newCam = new SauronCamera(request.getName(), request.getCoordinates().getLatitude(), request.getCoordinates().getLongitude());
            silo.addCamera(newCam);

            CamJoinResponse response = builder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch(SauronException e) {
            reactToException(e, responseObserver);
        }
    }

    @Override
    public void camInfo(CamInfoRequest request, StreamObserver<CamInfoResponse> responseObserver) {
        CamInfoResponse.Builder builder = CamInfoResponse.newBuilder();
        try {
            SauronCamera cam = silo.getCamByName(request.getName());
            builder.setCoordinates(Coordinates.newBuilder().setLatitude(cam.getLatitude()).setLongitude(cam.getLongitude()).build());

            CamInfoResponse response = builder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (SauronException e) {
            reactToException(e, responseObserver);
        }
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
        if (cam == null) {
            responseObserver.onError(NOT_FOUND
                    .withDescription("OBJECT_NOT_FOUND").asRuntimeException());
        }
        else {
            boolean error = false;
            for(Object obj: objects) {
                try {
                    SauronObject sauObj = silo.getObjectByTypeAndId(getObjectType(obj.getType()), obj.getId());
                    SauronObservation observation = new SauronObservation(sauObj, cam, ZonedDateTime.now());
                    silo.addObservation(observation);
                } catch (SauronException e) {
                    if (!error)
                        reactToException(e, responseObserver);
                    error = true;
                }
            }
            if (!error) {
                ReportResponse response = builder.build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
    }

    @Override
    public void track(TrackRequest request, StreamObserver<TrackResponse> responseObserver) {
        TrackResponse.Builder builder = TrackResponse.newBuilder();
        try {
            checkObjectArguments(getObjectType(request.getType()), request.getId(), false);
            SauronObservation sauObs = silo.track(getObjectType(request.getType()), request.getId());

            builder.setObservation(buildObs(sauObs));

            TrackResponse response = builder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (SauronException e) {
            reactToException(e, responseObserver);
        }
    }

    @Override
    public void trackMatch(TrackMatchRequest request, StreamObserver<TrackMatchResponse> responseObserver) {
        TrackMatchResponse.Builder builder = TrackMatchResponse.newBuilder();

        try {
            checkObjectArguments(getObjectType(request.getType()), request.getId(), true);
            List<SauronObservation> sauObs = silo.trackMatch(getObjectType(request.getType()), request.getId());

            if (sauObs.isEmpty())
                responseObserver.onError(NOT_FOUND
                        .withDescription("OBJECT_NOT_FOUND").asRuntimeException());
            else {
                builder.addAllObservations(sauObs.stream().map(this::buildObs).collect(Collectors.toList()));

                TrackMatchResponse response = builder.build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (SauronException e) {
            reactToException(e, responseObserver);
        }
    }

    @Override
    public void trace(TraceRequest request, StreamObserver<TraceResponse> responseObserver) {
        TraceResponse.Builder builder = TraceResponse.newBuilder();

        try {
            checkObjectArguments(getObjectType(request.getType()), request.getId(), false);
            List<SauronObservation> sauObs = silo.trace(getObjectType(request.getType()), request.getId());

            builder.addAllObservations(sauObs.stream().map(this::buildObs).collect(Collectors.toList()));

            TraceResponse response = builder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (SauronException e) {
            reactToException(e, responseObserver);
        }
    }

    @Override
    public void ctrlPing(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        PingResponse.Builder builder = PingResponse.newBuilder();
        String input = request.getInput();
        if (input == null || input.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription("INVALID_ARGUMENT").asRuntimeException());
        } else {
            String output = "Hello " + request.getInput() + "!";
            builder.setOutput(output);

            PingResponse response = builder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void ctrlClear(ClearRequest request, StreamObserver<ClearResponse> responseObserver) {
        silo.clear();

        ClearResponse response = ClearResponse.newBuilder().build();
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
                throw new SauronException(ErrorMessage.TYPE_DOES_NOT_EXIST);
        }
    }

    private <T> void reactToException(SauronException e, StreamObserver<T> so) {
        switch(e.getErrorMessage()) {
            case DUPLICATE_CAMERA:
                so.onError(ALREADY_EXISTS
                        .withDescription("DUPLICATE_CAMERA").asRuntimeException());
                break;
            case DUPLICATE_CAM_NAME:
                so.onError(ALREADY_EXISTS
                        .withDescription("DUPLICATE_CAM_NAME").asRuntimeException());
                break;
            case INVALID_COORDINATES:
                so.onError(INVALID_ARGUMENT
                        .withDescription("INVALID_COORDINATES").asRuntimeException());
                break;
            case INVALID_CAM_NAME:
                so.onError(INVALID_ARGUMENT
                        .withDescription("INVALID_CAM_NAME").asRuntimeException());
                break;
            case INVALID_PERSON_IDENTIFIER:
                so.onError(INVALID_ARGUMENT
                        .withDescription("INVALID_PERSON_IDENTIFIER").asRuntimeException());
                break;
            case INVALID_CAR_ID:
                so.onError(INVALID_ARGUMENT
                        .withDescription("INVALID_CAR_ID").asRuntimeException());
                break;
            case OBJECT_NOT_FOUND:
                so.onError(NOT_FOUND
                        .withDescription("OBJECT_NOT_FOUND").asRuntimeException());
                break;
            case TYPE_DOES_NOT_EXIST:
                so.onError(NOT_FOUND
                        .withDescription("TYPE_DOES_NOT_EXIST").asRuntimeException());
                break;
            case CAMERA_NOT_FOUND:
                so.onError(NOT_FOUND
                        .withDescription("CAMERA_NOT_FOUND").asRuntimeException());
                break;
            default:
                so.onError(UNKNOWN
                        .withDescription("UNKNOWN").asRuntimeException());
        }
    }

    private static boolean checkObjectArguments(String type, String id, boolean regex) throws SauronException{
        String PERSON = "person";
        String CAR = "car";
        if (!type.equals(CAR)  && !type.equals(PERSON)) {
            throw new SauronException(ErrorMessage.TYPE_DOES_NOT_EXIST);
        }
        else return (!type.equals(CAR) || checkCarId(id, regex)) && (!type.equals(PERSON) || checkPersonId(id, regex));

    }

    private static boolean checkCarId(String id, boolean regex) throws SauronException{
        int numFields = 0;
        if (!id.contains("*")) { //check if string contains *
            if (id.length() != 6)
                throw new SauronException(ErrorMessage.INVALID_CAR_ID);

            for (int i = 0; i < 3; i++) {
                char firstChar = id.charAt(2 * i);
                char secChar = id.charAt(2 * i + 1);

                if (Character.isDigit(firstChar) && Character.isDigit(secChar))
                    numFields++;

                else if (!(Character.isUpperCase(firstChar) && Character.isUpperCase(secChar)))
                    throw new SauronException(ErrorMessage.INVALID_CAR_ID);
            }
            if(numFields == 3 || numFields == 0)
                throw new SauronException(ErrorMessage.INVALID_CAR_ID);
        }
        else{
            if(regex && id.length() > 6 || id.matches(".*[*][*]+.*"))
                throw new SauronException(ErrorMessage.INVALID_CAR_ID);
        }
        return true;

    }

    private static boolean checkPersonId(String id, boolean regex) throws SauronException{
        try{
            if (!id.matches("[0-9*]+") || id.matches(".*[*][*]+.*") || (id.contains("*") && !regex)) {
                throw new SauronException(ErrorMessage.INVALID_PERSON_IDENTIFIER); //check if id doesn't match a number or if it doesn't have sequenced * or if it has * and regex is not possible
            }
            if (!id.contains("*")) { //check if string doesn't contain *
                Long.parseLong(id);
                if(Long.parseLong(id) < 0)
                    throw new SauronException(ErrorMessage.INVALID_PERSON_IDENTIFIER);
            }
        }
        catch(NumberFormatException e){
            throw new SauronException(ErrorMessage.INVALID_PERSON_IDENTIFIER);
        }
        return true;
    }

}
