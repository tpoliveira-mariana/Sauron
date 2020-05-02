package pt.tecnico.sauron.silo;


import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.grpc.Status.*;


public class SiloServerImpl extends SauronGrpc.SauronImplBase {
    /** Sauron implementation. */
    private Silo silo = new Silo();

    // partially detect invalid person partial id
    private static final Pattern INVAL_PERSON_PATT = Pattern.compile("0+.*[*].*|.*[*][*].*|.*[^0-9*].*");

    // partially detect invalid car partial id
    private static final Pattern INVAL_CAR_PATT = Pattern.compile(".*[*][*].*|.*[^A-Z0-9*].*");

    private static final int STUB_TIMEOUT = 10000;

    private List<Record> updateLog = new ArrayList<>();
    private List<Integer> replicaTS;
    private List<Integer> valueTS;
    private List<List<Integer>> tableTS;
    private Map<UUID, Record> opIds = new HashMap<>();
    private int instance;
    private ZKNaming nameServer;


    public SiloServerImpl(String zooHost, String zooPort, int replicaNum, int instance) {
        this.instance = instance;
        this.valueTS = new ArrayList<>(Collections.nCopies(replicaNum, 0));
        this.replicaTS = new ArrayList<>(Collections.nCopies(replicaNum, 0));
        this.tableTS = new ArrayList<>(Collections.nCopies(replicaNum, new ArrayList<>(Collections.nCopies(replicaNum, 0))));
        tableTS.forEach(entry -> entry = new ArrayList<>(Collections.nCopies(replicaNum, 0)));
        this.nameServer = new ZKNaming(zooHost, zooPort);
    }

    private class Record{
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        private Any _request;
        private List<Integer> _prevTS;
        private List<Integer> _updateTS;
        private int _handlerInstance;
        private boolean _applied = false;

        String _timestamp;
        UUID _opId;

        public Record(Any request, List<Integer> prevTS, List<Integer> updateTS, int inst, ZonedDateTime ts, UUID opId){
            _request = request;
            _prevTS = prevTS;
            _updateTS = updateTS;
            _handlerInstance = inst;
            _timestamp = ts.format(formatter);
            _opId = opId;
        }

        public Record(Any request, List<Integer> prevTS, List<Integer> updateTS, int inst, String ts, UUID opId){
            _request = request;
            _prevTS = prevTS;
            _updateTS = updateTS;
            _handlerInstance = inst;
            _timestamp = ts;
            _opId = opId;
        }

        public Any getRequest() {
            return _request;
        }

        public List<Integer> getPrevTS() {
            return _prevTS;
        }

        public List<Integer> getUpdateTS() {
            return _updateTS;
        }

        public int getHandlerInstance() {
            return _handlerInstance;
        }

        public boolean isApplied() {
            return _applied;
        }

        public String getTimestamp() {
            return _timestamp;
        }

        public UUID getOpId() {
            return _opId;
        }

        public void setApplied(boolean applied) {
            _applied = applied;
        }

        public void setTimestamp(String ts) {
            _timestamp = ts;
        }

        @Override
        public String toString (){
            String output = "{";
            if (_request.is(CamJoinRequest.class))
                output += "CamJoin, ";
            else
                output += "Report, ";

            return output + _prevTS + ", " + _updateTS + ", " + _applied + "}";
        }
    }

    @Override
    public synchronized void camJoin(CamJoinRequest request, StreamObserver<CamJoinResponse> responseObserver) {
        List<Integer> updateID = handleWriteRequest(Any.pack(request.getCam()), request.getVector().getTsList(), UUID.fromString(request.getOpId()));
        CamJoinResponse.Builder builder = CamJoinResponse.newBuilder();

        CamJoinResponse response = builder.setVector(VectorTS.newBuilder().addAllTs(updateID).build()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
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
        List<Integer> updateID = handleWriteRequest(Any.pack(request.getReport()), request.getVector().getTsList(), UUID.fromString(request.getOpId()));

        ReportResponse response = ReportResponse.newBuilder().setVector(VectorTS.newBuilder().addAllTs(updateID).build()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

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
        //Prioritize response and do the log related actions after
        GossipResponse response = GossipResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        int senderInstance = request.getInstance();
        List<Integer> newTS = request.getReplicaTS().getTsList();
        List<RecordMessage> requests = request.getLogRecordList();

        handleNewRequests(requests);
        this.replicaTS = mergeTS(this.replicaTS, newTS);
        tableTS.set(senderInstance-1, newTS);
        checkLog();
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
                so.onError(ALREADY_EXISTS.withDescription("DUPLICATE_CAMERA").asRuntimeException());
                break;
            case DUPLICATE_CAM_NAME:
                so.onError(ALREADY_EXISTS.withDescription("DUPLICATE_CAM_NAME").asRuntimeException());
                break;
            case INVALID_COORDINATES:
                so.onError(INVALID_ARGUMENT.withDescription("INVALID_COORDINATES").asRuntimeException());
                break;
            case INVALID_CAM_NAME:
                so.onError(INVALID_ARGUMENT.withDescription("INVALID_CAM_NAME").asRuntimeException());
                break;
            case INVALID_PERSON_ID:
                so.onError(INVALID_ARGUMENT.withDescription("INVALID_PERSON_ID").asRuntimeException());
                break;
            case INVALID_CAR_ID:
                so.onError(INVALID_ARGUMENT.withDescription("INVALID_CAR_ID").asRuntimeException());
                break;
            case OBJECT_NOT_FOUND:
                so.onError(NOT_FOUND.withDescription("OBJECT_NOT_FOUND").asRuntimeException());
                break;
            case TYPE_DOES_NOT_EXIST:
                so.onError(NOT_FOUND.withDescription("TYPE_DOES_NOT_EXIST").asRuntimeException());
                break;
            case CAMERA_NOT_FOUND:
                so.onError(NOT_FOUND.withDescription("CAMERA_NOT_FOUND").asRuntimeException());
                break;
            default:
                so.onError(UNKNOWN.withDescription("UNKNOWN").asRuntimeException());
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

    private int tsCompare(List<Integer> ts1, List<Integer> ts2) {
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

    private boolean tsAfter(List<Integer> ts1, List<Integer> ts2) {
        for (int i = 0; i < ts1.size(); i++) {
            if (ts1.get(i) < ts2.get(i))
                return false;
        }
        return true;
    }

    private List<Integer> handleWriteRequest(Any request, List<Integer> prevTS, UUID opId) {
        // check if request is duplicated
        if (opIds.containsKey(opId))
            return prevTS;

        // update replicaTS
        int i = this.instance -1;
        this.replicaTS.set(i, this.replicaTS.get(i) + 1);

        // build updateID to return
        List<Integer> updateID = new ArrayList<>(prevTS);
        updateID.set(i, this.replicaTS.get(i));

        // add request to log
        Record record = new Record(request, prevTS, updateID, this.instance, ZonedDateTime.now(), opId);
        this.updateLog.add(record);
        this.opIds.put(opId, record);
        if (tsAfter(valueTS, prevTS)) {
            applyToValue(record);
        }
        return updateID;
    }

    public synchronized void performGossip(String serverPath){
        List<ZKRecord> replicas;
        display("Replica " + instance + " initiating gossip...");

        try {
            replicas = new ArrayList<>(this.nameServer.listRecords(serverPath));
        } catch(ZKNamingException e){
            display("Cannot perform gossip round - error getting replicas.");
            return;
        }
        if (replicas.size() == 1 || replicaTS.size() == 1) { //remove second condition for testing connections
            display("No more replicas available. No gossip performed by replica " + instance);
            return;
        }
        String replicaPath;
        String currentPath = serverPath + "/" + this.instance;
        int newInstance, startInstance, replicaNum;
        Set<Integer> seen = new HashSet<>();
        do {
            startInstance = newInstance = (new Random()).nextInt(replicas.size());
            replicaPath = replicas.get(newInstance).getPath();
            replicaNum = Integer.parseInt(replicaPath.substring(replicaPath.lastIndexOf('/') + 1));
            seen.add(replicaNum);
        } while ((replicaNum == this.instance || replicaNum > this.replicaTS.size()) && seen.size() != replicas.size()); //remove = for testing connections
        if (seen.size() == replicas.size() && (replicaNum > replicaTS.size() || replicaNum == this.instance)) {
            display("No more replicas available. No gossip performed by replica " + instance);
            return;
        }
        boolean failed;
        do {
            failed = false;
            try {
                ZKRecord record = this.nameServer.lookup(replicaPath);
                String target = record.getURI();
                ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                SauronGrpc.SauronBlockingStub stub = SauronGrpc.newBlockingStub(channel);

                GossipRequest request = getGossipRequest(replicaNum);

                display("Connecting to replica " + replicaNum + " at " + target + "...");
                stub.withDeadlineAfter((long)(Math.random() + 1) * STUB_TIMEOUT, TimeUnit.MILLISECONDS).gossip(request);

                channel.shutdown();
                display("Gossip to replica " + replicaNum + " successful, exiting gossip");
            } catch (ZKNamingException | StatusRuntimeException e) {
                //Could not perform gossip to the replica(connection failed)
                display("An error occurred. Couldn't perform gossip with replica " + replicaNum + ". Changing replica...");
                do {
                    newInstance = (newInstance + 1) % replicas.size();
                    replicaPath = replicas.get(newInstance).getPath();
                    replicaNum = Integer.parseInt(replicaPath.substring(replicaPath.lastIndexOf('/') + 1));
                } while (replicaPath.equals(currentPath) || replicaNum > this.replicaTS.size());
                failed = true;
                display(newInstance + " - " + startInstance);
                if (newInstance == startInstance){
                    display("Completed round through all available replicas without any successful connection. No Gossip performed by replica " + instance);
                    failed = false;
                }
            }
        } while (failed);
    }

    private GossipRequest getGossipRequest(int newInstance){
        GossipRequest.Builder gossipReq = GossipRequest.newBuilder().setInstance(this.instance);
        List<RecordMessage> sendRecords = new ArrayList<>();
        if (updateLog.isEmpty()) {
            gossipReq.setReplicaTS(VectorTS.newBuilder().addAllTs(replicaTS).build()).addAllLogRecord(sendRecords);
        }
        for (Record record : updateLog){
            List<Integer> currentTS = record.getUpdateTS();
            int requestInst = record.getHandlerInstance();
            if (currentTS.get(requestInst-1) > tableTS.get(newInstance-1).get(requestInst-1)) {
                VectorTS updateTS = VectorTS.newBuilder().addAllTs(currentTS).build();
                VectorTS prevTS = VectorTS.newBuilder().addAllTs(record.getPrevTS()).build();
                Timestamp ts;
                try {
                    ts = Timestamps.parse(record.getTimestamp());
                } catch (ParseException e) {
                    ts = Timestamp.getDefaultInstance();
                }
                RecordMessage req = RecordMessage.newBuilder()
                        .setRequest(record.getRequest())
                        .setInstance(requestInst)
                        .setUpdateTS(updateTS).setPrevTS(prevTS)
                        .setTimestamp(ts)
                        .setOpId(record.getOpId().toString())
                        .build();
                sendRecords.add(0, req); //add to the beginning
            }
        }
        gossipReq.setReplicaTS(VectorTS.newBuilder().addAllTs(replicaTS).build()).addAllLogRecord(sendRecords);
        return gossipReq.build();
    }

    private void checkLog(){
        if (updateLog.isEmpty()) return;

        int i = 0;
        while (i < updateLog.size()) {
            Record record = updateLog.get(i);
            if (record.isApplied() && !checkRecordRemoval(record, i))
                i++;
            else if (tsCompare(valueTS, record.getPrevTS()) == -1)
                break;
            else {
                applyToValue(record);
                if (!checkRecordRemoval(record, i))
                    i++;
            }
        }
    }

    private void applyToValue(Record record) {
        Any request = record.getRequest();
        try {
            if (request.is(Cam.class))
                handleCamJoin(request.unpack(Cam.class));
            else
                handleReport(request.unpack(Report.class), record.getTimestamp());
            record.setApplied(true);
            this.valueTS = mergeTS(this.valueTS, record.getUpdateTS());
        } catch (InvalidProtocolBufferException e) { /*ignore*/ }

    }

    private boolean checkRecordRemoval(Record record, int index){
        List<Integer> updateTS = record.getUpdateTS();
        int recordInst = record.getHandlerInstance();

        for (int i = 0; i < tableTS.size(); i++) {
            if (i != instance-1 && tableTS.get(i).get(recordInst - 1) < updateTS.get(recordInst-1))
                return false;
        }
        updateLog.remove(index);
        opIds.remove(record.getOpId());
        return true;
    }

    private void handleCamJoin(Cam request){
        try {
            SauronCamera newCam = new SauronCamera(request.getName(), request.getCoordinates().getLatitude(), request.getCoordinates().getLongitude());
            silo.addCamera(newCam);
        } catch (SauronException e) { /*ignore*/ }
    }

    private void handleReport(Report request, String ts){
        SauronCamera cam;
        try {
            cam = silo.getCamByName(request.getName());
        } catch(SauronException e) {
            cam = null;
        }
        List<Object> objects = request.getObjectList();
        if (cam == null) return;
        for (Object obj : objects) {
            try {
                String type = typeToString(obj.getType());
                SauronObject sauObj = silo.getObject(type, obj.getId());
                if (sauObj == null)
                    sauObj = silo.addObject(type, obj.getId());

                SauronObservation observation = new SauronObservation(sauObj, cam, ts);
                silo.addObservation(observation);
            } catch (SauronException e) { /*ignore*/ }
        }
    }

    private void handleNewRequests(List<RecordMessage> requests) {
        display("Received " + requests.size() + " new requests. Handling them...");
        if (requests.isEmpty())
            return;

        requests.forEach(request -> {
            List<Integer> reqTS = request.getUpdateTS().getTsList();
            int requestInst = request.getInstance();
            if (reqTS.get(requestInst-1) > replicaTS.get(requestInst-1)){
                //check if replicaTS is outdated in relation to updateTS
                UUID opId = UUID.fromString(request.getOpId());
                Record record = new Record(request.getRequest(), request.getPrevTS().getTsList() ,reqTS, requestInst, Timestamps.toString(request.getTimestamp()), opId);
                if (opIds.containsKey(opId))
                    enforceReportConsistency(opIds.get(opId), record);
                else {
                    updateLog.add(record);
                    opIds.put(opId, record);
                    display("Added new request. updateTS = " + reqTS + " | replicaTS = " + replicaTS);
                }
            }
        });
        updateLog.sort((record1, record2) -> tsCompare(record1.getPrevTS(), record2.getPrevTS()));
    }

    private void enforceReportConsistency(Record current, Record duplicate) {
        if (!current.getRequest().is(Report.class) || !duplicate.getRequest().is(Report.class))
            return;
        if (ZonedDateTime.parse(duplicate.getTimestamp()).isAfter(ZonedDateTime.parse(current.getTimestamp())))
            return;
        try {
            Report request = current.getRequest().unpack(Report.class);
            if (current.isApplied()) {
                for (Object obj : request.getObjectList()) {
                    SauronCamera sauCam = silo.getCamByName(request.getName());
                    SauronObject sauObj = silo.getObject(typeToString(obj.getType()), obj.getId());
                    SauronObservation sauObs = silo.findObservation(sauObj, sauCam, current.getTimestamp());
                    sauObs.setTimeStamp(duplicate.getTimestamp());
                }
            } else {
                current.setTimestamp(duplicate.getTimestamp());
            }
        } catch (InvalidProtocolBufferException | SauronException e) { /*ignore*/ }
    }

    private List<Integer> mergeTS(List<Integer> ts1, List<Integer> ts2) {
        List<Integer> merged = new ArrayList<>(Collections.nCopies(ts1.size(), 0));
        for (int i = 0; i < ts1.size(); i++) {
            merged.set(i, Math.max(ts1.get(i), ts2.get(i)));
        }
        return merged;
    }


    private static void display(String msg) {
        System.out.println(msg);
    }

}
