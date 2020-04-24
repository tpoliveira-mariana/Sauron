package pt.tecnico.sauron.silo.client;

import com.google.protobuf.util.Timestamps;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.exceptions.ErrorMessage;
import pt.tecnico.sauron.exceptions.SauronException;
import pt.tecnico.sauron.silo.grpc.ObjectType;
import pt.tecnico.sauron.silo.grpc.Observation;
import pt.tecnico.sauron.silo.grpc.TraceResponse;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class TraceIT extends BaseIT {
    private static SiloFrontend frontend;
    private static final String TEST_DATA_FILE = "./src/test/traceIT_data.txt";
    private static final String PERSON_ID_1 = "1";
    private static final String INEXISTENT_PERSON_ID = "30";
    private static final String PERSON_TYPE = "person";
    private static final String CAR_TYPE = "car";
    private static final String INEXISTENT_TYPE = "dog";
    private static final String CAR_ID = "11AB12";
    private static final String CAM_TAGUS = "Tagus";
    private static final String CAM_ALAMEDA = "Alameda";
    private static final String CAM_FCT = "FCT";
    private static final String BLANC = "";

    // one-time initialization and clean-up
    @BeforeAll
    public static void oneTimeSetUp() throws ZKNamingException {
        frontend = new SiloFrontend("localhost", "8081", "/grpc/sauron/silo/1");
        try {
            frontend.ctrlClear();
            frontend.ctrlInit(TEST_DATA_FILE);
        }
        catch (SauronException e) {
            System.out.println(e.getErrorMessageLabel());
        }

    }

    @AfterAll
    public static void oneTimeTearDown() {
        try {
            frontend.ctrlClear();
        }
        catch (SauronException e) {
            System.out.println(e.getErrorMessageLabel());
        }
    }

    // tests

    @Test
    public void traceOK() {
        try {
            List<String> cams = setVals(CAM_FCT, CAM_ALAMEDA, CAM_TAGUS);
            List<String> lats = setVals("26.0", "13.0", "12.0");
            List<String> longs = setVals("-39.567", "-36.5", "-36.0" );
            TraceResponse response = frontend.trace(PERSON_TYPE, PERSON_ID_1);
            List<String> result = response.getObservationsList().stream()
                    .map(TraceIT::printObservation)
                    .collect(Collectors.toList());
            Assertions.assertTrue(result.size() == 3);
            int i = 0;
            for (String r : result) {
                StringTokenizer st = new StringTokenizer(r, ",");
                Assertions.assertEquals(6, st.countTokens());
                Assertions.assertEquals("person", st.nextToken());
                Assertions.assertEquals(PERSON_ID_1, st.nextToken());
                st.nextToken();
                Assertions.assertEquals(cams.get(i), st.nextToken());
                Assertions.assertEquals(lats.get(i), st.nextToken());
                Assertions.assertEquals(longs.get(i), st.nextToken());
                i++;
            }
        }
        catch (SauronException e) {
            System.out.println(e.getErrorMessageLabel());
        }
    }

    @Test
    public void traceNOK_empty() {
        try {
            TraceResponse response = frontend.trace(CAR_TYPE, CAR_ID);
            List<String> result = response.getObservationsList().stream()
                    .map(TraceIT::printObservation)
                    .collect(Collectors.toList());
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.OBJECT_NOT_FOUND, e.getErrorMessage());
        }
    }

    @Test
    public void traceNOK_Id() {
        try {
            TraceResponse response = frontend.trace(PERSON_TYPE, INEXISTENT_PERSON_ID);
            List<String> result = response.getObservationsList().stream()
                    .map(TraceIT::printObservation)
                    .collect(Collectors.toList());
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.OBJECT_NOT_FOUND, e.getErrorMessage());
        }
    }

    @Test
    public void traceNOK_Type() {
        try {
            TraceResponse response = frontend.trace(INEXISTENT_TYPE, PERSON_ID_1);
            List<String> result = response.getObservationsList().stream()
                    .map(TraceIT::printObservation)
                    .collect(Collectors.toList());
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.TYPE_DOES_NOT_EXIST, e.getErrorMessage());
        }
    }

    @Test
    public void traceNOK_EmptyType() {
        try {
            TraceResponse response = frontend.trace(BLANC, PERSON_ID_1);
            List<String> result = response.getObservationsList().stream()
                    .map(TraceIT::printObservation)
                    .collect(Collectors.toList());
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.TYPE_DOES_NOT_EXIST, e.getErrorMessage());
        }
    }

    @Test
    public void traceNOK_EmptyId_Person() {
        try {
            TraceResponse response = frontend.trace(PERSON_TYPE, BLANC);
            List<String> result = response.getObservationsList().stream()
                    .map(TraceIT::printObservation)
                    .collect(Collectors.toList());
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.INVALID_PERSON_ID, e.getErrorMessage());
        }
    }

    @Test
    public void traceNOK_EmptyId_car() {
        try {
            TraceResponse response = frontend.trace(CAR_TYPE, BLANC);
            List<String> result = response.getObservationsList().stream()
                    .map(TraceIT::printObservation)
                    .collect(Collectors.toList());
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.INVALID_CAR_ID, e.getErrorMessage());
        }
    }

    private List<String> setVals(String v1, String v2, String v3) {
        List<String> c = new ArrayList<>();
        c.add(v1);
        c.add(v2);
        c.add(v3);

        return c;
    }

    private static String printObservation(Observation obs) {
        String ts = Timestamps.toString(obs.getTimestamp());
        return typeToString(obs.getObject().getType()) + ","
                + obs.getObject().getId() + ","
                + ts.substring(0, ts.lastIndexOf('.')) + ","
                + obs.getCam().getName() + ","
                + obs.getCam().getCoordinates().getLatitude() + ","
                + obs.getCam().getCoordinates().getLongitude();
    }

    private static String typeToString(ObjectType type) {
        switch (type){
            case PERSON:
                return "person";
            case CAR:
                return "car";
            default:
                return "<UNRECOGNIZED>";
        }
    }

}
