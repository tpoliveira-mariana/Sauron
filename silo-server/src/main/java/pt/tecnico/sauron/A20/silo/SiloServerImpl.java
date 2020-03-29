package pt.tecnico.sauron.A20.silo;


import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.A20.silo.domain.*;
import pt.tecnico.sauron.A20.exceptions.SauronException;
import pt.tecnico.sauron.A20.exceptions.ErrorMessage.*;
import pt.tecnico.sauron.A20.silo.domain.SauronCamera;
import pt.tecnico.sauron.A20.silo.domain.Silo;
import pt.tecnico.sauron.A20.silo.grpc.*;

import java.time.LocalDateTime;
import java.util.List;

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
            switch(e.getErrorMessage()) {
                case DUPLICATE_CAMERA:
                    builder.setStatus(Status.DUPLICATE_CAMERA);
                    break;
                case INVALID_COORDINATES:
                    builder.setStatus(Status.INVALID_COORDINATES);
                    break;
                case INVALID_CAM_NAME:
                    builder.setStatus(Status.INVALID_NAME);
                    break;
                default:
                    builder.setStatus(Status.UNRECOGNIZED);
            }

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
            builder.setStatus(Status.INEXISTANT_CAMERA);
        }

        CamInfoResponse response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void report(ReportRequest request, StreamObserver<ReportResponse> responseObserver) {
        ReportResponse.Builder builder = ReportResponse.newBuilder();
        SauronCamera cam = null;
        boolean camerror = false;
        try {
            cam = silo.getCamByName(request.getName());
        } catch(SauronException e) {
            builder.setStatus(Status.INEXISTANT_CAMERA);
            camerror = true;
        }
        List<Observation> observations = request.getObservationsList();
        boolean error = false;
        for(Observation obs: observations) {
            if (camerror) break;

            try {
                SauronObject obj = silo.getObjectByTypeAndId(getObjectType(obs.getType()), obs.getId());
                SauronObservation observation = new SauronObservation(obj, cam, LocalDateTime.now());
                silo.addObservation(observation);
                builder.setStatus(Status.OK);
            }
            catch(SauronException e) {
                error = true;
                switch(e.getErrorMessage()) {
                    case INVALID_PERSON_IDENTIFIER:
                    case INVALID_CAR_ID:
                        builder.setStatus(Status.INVALID_ID);
                        break;
                    case TYPE_DOES_NOT_EXIST:
                        builder.setStatus(Status.INVALID_TYPE);
                        break;
                    default:
                        builder.setStatus(Status.UNRECOGNIZED);
                }
            }
        }

        if (!error)
            builder.setStatus(Status.OK);
        ReportResponse response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
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

}
