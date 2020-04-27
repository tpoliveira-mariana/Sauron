package pt.tecnico.sauron.silo;


import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.exceptions.ErrorMessage;
import pt.tecnico.sauron.exceptions.SauronException;
import pt.tecnico.sauron.silo.domain.*;
import pt.tecnico.sauron.silo.grpc.Object;
import pt.tecnico.sauron.silo.domain.Silo;
import pt.tecnico.sauron.silo.grpc.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.grpc.Status.*;
import static org.apache.commons.lang.math.RandomUtils.nextInt;


public class SiloServerImpl extends SauronGrpc.SauronImplBase {
    /** Sauron implementation. */
    private Silo silo = new Silo();

    // partially detect invalid person partial id
    private static final Pattern INVAL_PERSON_PATT = Pattern.compile("0+.*[*].*|.*[*][*].*|.*[^0-9*].*");

    // partially detect invalid car partial id
    private static final Pattern INVAL_CAR_PATT = Pattern.compile(".*[*][*].*|.*[^A-Z0-9*].*");

    private List<Any> updateLog = new ArrayList<>();
    private List<Integer> replicaTS;
    private List<Integer> valueTS;
    private List<List<Integer>> tableTS;
    private int instance;
    private ZKNaming nameServer;


    public SiloServerImpl(String zooHost, String zooPort, int replicaNum, int instance) {
        this.instance = instance;
        this.valueTS = new ArrayList<>(Collections.nCopies(replicaNum, 0));
        this.replicaTS = new ArrayList<>(Collections.nCopies(replicaNum, 0));
        this.tableTS = new ArrayList<>(replicaNum);
        tableTS.forEach(entry -> entry = new ArrayList<>(Collections.nCopies(replicaNum, 0)));
        this.nameServer = new ZKNaming(zooHost, zooPort);
        System.out.println("valueTS: " + this.valueTS);
    }

    @Override
    public synchronized void camJoin(CamJoinRequest request, StreamObserver<CamJoinResponse> responseObserver) {
        List<Integer> updateID = handleWriteRequest(Any.pack(request), request.getVector().getTsList());

        CamJoinResponse.Builder builder = CamJoinResponse.newBuilder();
        try {
            SauronCamera newCam = new SauronCamera(request.getName(), request.getCoordinates().getLatitude(), request.getCoordinates().getLongitude());
            silo.addCamera(newCam);

            CamJoinResponse response = builder.setVector(VectorTS.newBuilder().addAllTs(updateID).build()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch(SauronException e) {
            reactToException(e, responseObserver);
        }
    }

    @Override
    public synchronized void camInfo(CamInfoRequest request, StreamObserver<CamInfoResponse> responseObserver) {
        CamInfoResponse.Builder builder = CamInfoResponse.newBuilder();
        try {
            SauronCamera cam = silo.getCamByName(request.getName());
            builder.setCoordinates(Coordinates.newBuilder().setLatitude(cam.getLatitude()).setLongitude(cam.getLongitude()).build());

            CamInfoResponse response = builder.setVector(VectorTS.newBuilder().addAllTs(this.valueTS).build()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (SauronException e) {
            reactToException(e, responseObserver);
        }
    }

    @Override
    public synchronized void report(ReportRequest request, StreamObserver<ReportResponse> responseObserver) {
        List<Integer> updateID = handleWriteRequest(Any.pack(request), request.getVector().getTsList());

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
                    .withDescription("CAMERA_NOT_FOUND").asRuntimeException());
        }
        else {
            boolean error = false;
            for(Object obj: objects) {
                try {
                    String type = typeToString(obj.getType());
                    SauronObject sauObj = silo.getObject(type, obj.getId());
                    if (sauObj == null)
                        sauObj = silo.addObject(type, obj.getId());

                    SauronObservation observation = new SauronObservation(sauObj, cam, ZonedDateTime.now());
                    silo.addObservation(observation);
                } catch (SauronException e) {
                    if (!error)
                        reactToException(e, responseObserver);
                    error = true;
                }
            }
            if (!error) {
                ReportResponse response = builder.setVector(VectorTS.newBuilder().addAllTs(updateID).build()).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
    }

    @Override
    public synchronized void track(TrackRequest request, StreamObserver<TrackResponse> responseObserver) {
        TrackResponse.Builder builder = TrackResponse.newBuilder();
        try {
            checkObjectArguments(typeToString(request.getType()), request.getId(), false);
            SauronObservation sauObs = silo.track(typeToString(request.getType()), request.getId());
            builder.setObservation(buildObs(sauObs));

            TrackResponse response = builder.setVector(VectorTS.newBuilder().addAllTs(this.valueTS).build()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (SauronException e) {
            reactToException(e, responseObserver);
        }
    }

    @Override
    public synchronized void trackMatch(TrackMatchRequest request, StreamObserver<TrackMatchResponse> responseObserver) {
        TrackMatchResponse.Builder builder = TrackMatchResponse.newBuilder();

        try {
            checkObjectArguments(typeToString(request.getType()), request.getId(), true);
            List<SauronObservation> sauObs = silo.trackMatch(typeToString(request.getType()), request.getId());

            if (sauObs.isEmpty())
                responseObserver.onError(NOT_FOUND
                        .withDescription("OBJECT_NOT_FOUND").asRuntimeException());
            else {
                builder.addAllObservations(sauObs.stream().map(this::buildObs).collect(Collectors.toList()));

                TrackMatchResponse response = builder.setVector(VectorTS.newBuilder().addAllTs(this.valueTS).build()).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (SauronException e) {
            reactToException(e, responseObserver);
        }
    }

    @Override
    public synchronized void trace(TraceRequest request, StreamObserver<TraceResponse> responseObserver) {
        TraceResponse.Builder builder = TraceResponse.newBuilder();

        try {
            checkObjectArguments(typeToString(request.getType()), request.getId(), false);
            List<SauronObservation> sauObs = silo.trace(typeToString(request.getType()), request.getId());

            builder.addAllObservations(sauObs.stream().map(this::buildObs).collect(Collectors.toList()));

            TraceResponse response = builder.setVector(VectorTS.newBuilder().addAllTs(this.valueTS).build()).build();
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
    public synchronized void ctrlClear(ClearRequest request, StreamObserver<ClearResponse> responseObserver) {
        silo.clear();

        ClearResponse response = ClearResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver) {

        //Prioritize response and do the actions after
        GossipResponse response = GossipResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        //debug-System.out.println("Received message from instance - " + request.getInstance());

        /*int instance = request.getInstance();
        List<Integer> newTS = request.getReplicaTS().getTsList();
        List<Request> requests = request.getLogList();

        handleNewRequests(requests);
        updateTS(newTS);
        tableTS.set(instance, newTS);
        checkLog();*/
    }

    private Observation buildObs(SauronObservation sauObs) {
        ObjectType type;
        try {
            type = stringToType(sauObs.getObjectType());
        } catch (SauronException e) {
            type = ObjectType.UNRECOGNIZED;
        }

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

    private String typeToString(ObjectType type) throws SauronException{
        switch (type){
            case PERSON:
                return "person";
            case CAR:
                return "car";
            default:
                throw new SauronException(ErrorMessage.TYPE_DOES_NOT_EXIST);
        }
    }

    private ObjectType stringToType(String type) throws SauronException{
        switch (type){
            case "person":
                return ObjectType.PERSON;
            case "car":
                return ObjectType.CAR;
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
            case INVALID_PERSON_ID:
                so.onError(INVALID_ARGUMENT
                        .withDescription("INVALID_PERSON_ID").asRuntimeException());
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

    private static void checkObjectArguments(String type, String id, boolean partial) throws SauronException {
        switch (type) {
            case "car":
                checkCarId(id, partial);
                break;
            case "person":
                checkPersonId(id, partial);
                break;
            default:
                throw new SauronException(ErrorMessage.TYPE_DOES_NOT_EXIST);
        }
    }

    private static void checkCarId(String id, boolean partial) throws SauronException {
        if (!partial || !id.contains("*")) {
            SauronCar.checkId(id);
        } else if (id.length() > 6 || INVAL_CAR_PATT.matcher(id).matches()) {
            throw new SauronException(ErrorMessage.INVALID_CAR_ID);
        }
    }

    private static void checkPersonId(String id, boolean partial) throws SauronException {
        if (!partial || !id.contains("*")) {
            SauronPerson.checkId(id);
        } else if (INVAL_PERSON_PATT.matcher(id).matches()) {
            throw new SauronException(ErrorMessage.INVALID_PERSON_ID);
        }
    }

    private int tsAfter(List<Integer> ts1, List<Integer> ts2) {
        boolean before = false;
        boolean after = false;
        for (int i = 0; i < ts1.size(); i++) {
            if (ts1.get(i) > ts2.get(i))
                after = true;
            if (ts1.get(i) < ts2.get(i))
                before = true;
        }
        if (after && before || !after && !before) {
            //Ts's are both equal or are concurrent(some entries greater than others, other entries lower)
            return 0;
        } else if (after){
            //all entries of ts1 are greater than ts2
            return 1;
        } else{
            // all entries of ts1 are lower than ts2
            return -1;
        }
    }

    private List<Integer> handleWriteRequest(Any request, List<Integer> prevTS) {

        // update replicaTS
        int i = this.instance -1;
        this.replicaTS.set(i, this.replicaTS.get(i) + 1);

        // build updateID to return
        List<Integer> updateID = new ArrayList<>();
        updateID.addAll(prevTS);
        updateID.set(i, this.replicaTS.get(i));

        // add request to log
        this.updateLog.add(request);

        return updateID;
    }

    public void performGossip(String serverPath){
        List<ZKRecord> replicas;
        //debug-System.out.println("Gossip round!");

        try {
            replicas = new ArrayList<>(this.nameServer.listRecords(serverPath));
        } catch(ZKNamingException e){
            System.out.println("Cannot perform gossip round - error getting replicas.");
            return;
        }
        if (replicas.size() == 1)
            // only replica available is the one calling the method
            return;
        try {
            String currentPath = serverPath + "/" + this.instance;
            String replicaPath = replicas.get((new Random()).nextInt(replicas.size())).getPath();
            while (replicaPath.equals(currentPath)){
                replicaPath = replicas.get((new Random()).nextInt(replicas.size())).getPath();
            }

            ZKRecord record = this.nameServer.lookup(replicaPath);
            String target = record.getURI();
            ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
            SauronGrpc.SauronBlockingStub stub = SauronGrpc.newBlockingStub(channel);
            GossipRequest request = GossipRequest.newBuilder().setInstance(this.instance).build();

            //TODO-Send Log and replicaTS taking into account tableTS
            GossipResponse response = stub.gossip(request);
            //debug-System.out.println("Sent message to instance - " + replicaPath);
            channel.shutdown();
        } catch (ZKNamingException | StatusRuntimeException e){
            //Could not perform gossip round
        }
    }

    private void checkLog(){
        updateLog.forEach(request -> {
            try {
                List<Integer> reqTS;
                CamJoinRequest cam = request.is(CamJoinRequest.class) ? request.unpack(CamJoinRequest.class) : null;
                ReportRequest report = request.is(ReportRequest.class) ? request.unpack(ReportRequest.class) : null;
                if (cam != null)
                    reqTS = cam.getVector().getTsList();
                else
                    reqTS = report.getVector().getTsList();

                if (tsAfter(valueTS, reqTS) == 1){
                    //TODO-perform update and remove the request from the log
                }
            } catch(InvalidProtocolBufferException e){
                //TODO-Maybe remove the request
            }
        });
    }

    private void handleNewRequests(List<Request> requests){
        requests.forEach(request -> {
            List<Integer> reqTS = request.getUpdateTS().getTsList();
            if (tsAfter(reqTS, valueTS) == 1){
                updateLog.add(request.getCamjoin() != null ? Any.pack(request.getCamjoin()) : Any.pack(request.getReport()));
            }
            //TODO-missing check to see if the request is already in the list
        });
    }

    private void updateTS(List<Integer> newTS){
        for (int i = 0; i < this.replicaTS.size(); i++){
            int prevValue = this.replicaTS.get(i);
            int newValue = newTS.get(i);
            this.replicaTS.set(i, Math.max(prevValue, newValue));
        }
    }



}
