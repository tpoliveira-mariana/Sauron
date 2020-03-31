package pt.tecnico.sauron.A20.silo.client;

import org.junit.jupiter.api.*;
import pt.tecnico.sauron.A20.exceptions.ErrorMessage;
import pt.tecnico.sauron.A20.exceptions.SauronException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class TrackMatchIT extends BaseIT{
    private static final String HOST = "localhost";
    private static final String PORT = "8080";
    private static final String TEST_DATA_FILE = "./src/test/trackMatchIT_data.txt";
    private static SiloFrontend frontend;

    private static final String PERSON_TYPE = "person";
    private static final String PERSON_ID_1 = "151";
    private static final String PERSON_ID_2 = "31";
    private static final String PERSON_ID_3 = "15";
    private static final String PERSON_ID_4 = "1331";
    private static final String PERSON_ID_5 = "555";
    private static final String INEXISTENT_PERSON_ID = "30";
    private static final String INVALID_PERSON_ID = "abc";
    private static final String CAR_TYPE = "car";
    private static final String CAR_ID_1 = "ABAA99";
    private static final String CAR_ID_2 = "ZZ33AB";
    private static final String CAR_ID_3 = "ABCD99";
    private static final String CAR_ID_4 = "CD55DB";
    private static final String CAR_ID_5 = "AE32EB";
    private static final String INEXISTENT_CAR_ID = "ZZZZ99";
    private static final String INVALID_CAR_ID = "ZZZZZZ";
    private static final String INEXISTENT_TYPE = "dog";
    private static final String CAM_TAGUS = "Tagus";
    private static final String CAM_ALAMEDA = "Alameda";
    private static final String CAM_FCT = "FCT";


    // one-time initialization and clean-up
    @BeforeAll
    public static void oneTimeSetUp() {
        frontend = new SiloFrontend(HOST, PORT);
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
        /*try {
            frontend.ctrlClear();
        }
        catch (SauronException e) {
            System.out.println(e.getErrorMessageLabel());
        }*/
    }

    // tests

    @Test
    public void trackOKCarRegexBegin() throws SauronException{
        List<String> cams = setVals(CAM_TAGUS, CAM_ALAMEDA, CAM_FCT);
        List<String> lats = setVals("12.0", "13.0", "26.0");
        List<String> longs = setVals("-36.0", "-36.5", "-39.567");
        List<String> ids = setVals(CAR_ID_1, CAR_ID_3, CAR_ID_5);

        List<String> result = frontend.trackMatch(CAR_TYPE, "A*");

        checkResults(result, cams, lats, longs, "car", ids);
    }

    @Test
    public void trackOKCarRegexMiddle() throws SauronException{
        List<String> cams = setVals(CAM_TAGUS, CAM_ALAMEDA, "");
        List<String> lats = setVals("12.0", "13.0", "");
        List<String> longs = setVals("-36.0", "-36.5", "");
        List<String> ids = setVals(CAR_ID_1, CAR_ID_3, "");

        List<String> result = frontend.trackMatch(CAR_TYPE, "AB*99");

        checkResults(result, cams, lats, longs, "car", ids);
    }

    @Test
    public void trackOKCarRegexEnd() throws SauronException{
        List<String> cams = setVals(CAM_FCT, CAM_ALAMEDA, CAM_TAGUS);
        List<String> lats = setVals("26.0", "13.0", "12.0");
        List<String> longs = setVals("-39.567", "-36.5", "-36.0" );
        List<String> ids = setVals(CAR_ID_5, CAR_ID_4, CAR_ID_2);

        List<String> result = frontend.trackMatch(CAR_TYPE, "*B");

        checkResults(result, cams, lats, longs, "car", ids);
    }

    @Test
    public void trackOKPersonRegexBegin() throws SauronException{
        List<String> cams = setVals(CAM_ALAMEDA, CAM_TAGUS, CAM_FCT);
        List<String> lats = setVals("13.0", "12.0", "26.0");
        List<String> longs = setVals("-36.5", "-36.0", "-39.567");
        List<String> ids = setVals(PERSON_ID_3, PERSON_ID_1, PERSON_ID_4);

        List<String> result = frontend.trackMatch(PERSON_TYPE, "1*");

        checkResults(result, cams, lats, longs, "person", ids);
    }

    @Test
    public void trackOKPersonRegexMiddle() throws SauronException{
        List<String> cams = setVals(CAM_TAGUS, CAM_FCT, "");
        List<String> lats = setVals("12.0", "26.0", "");
        List<String> longs = setVals("-36.0", "-39.567", "");
        List<String> ids = setVals(PERSON_ID_1, PERSON_ID_4, "");

        List<String> result = frontend.trackMatch(PERSON_TYPE, "1*1");

        checkResults(result, cams, lats, longs, "person", ids);
    }

    @Test
    public void trackOKPersonRegexEnd() throws SauronException{
        List<String> cams = setVals(CAM_ALAMEDA, CAM_TAGUS, CAM_FCT);
        List<String> lats = setVals("13.0", "12.0", "26.0");
        List<String> longs = setVals("-36.5", "-36.0", "-39.567");
        List<String> ids = setVals(PERSON_ID_2, PERSON_ID_1, PERSON_ID_4);

        List<String> result = frontend.trackMatch(PERSON_TYPE, "*1");

        checkResults(result, cams, lats, longs, "person", ids);
    }

    @Test
    public void trackLastObservationOK() throws SauronException, InterruptedException{
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        List<String> result = frontend.trackMatch(PERSON_TYPE, PERSON_ID_5);
        String[] results = result.get(0).split(",");
        LocalDateTime timeBefore = LocalDateTime.parse(results[2], formatter);

        List<String> observation = new ArrayList<>();
        List<List<String>> observations = new ArrayList<>();
        observation.add(PERSON_TYPE);
        observation.add(PERSON_ID_5);
        observations.add(observation);

        Thread.sleep(5000); // test needs to sleep in order to compare the times
        frontend.report(CAM_ALAMEDA, observations);

        result = frontend.trackMatch(PERSON_TYPE, PERSON_ID_5);
        String[] resultsAfter = result.get(0).split(",");

        LocalDateTime timeAfter = LocalDateTime.parse(resultsAfter[2], formatter);

        Assertions.assertEquals(6, resultsAfter.length);
        Assertions.assertEquals("person", resultsAfter[0]);
        Assertions.assertEquals(PERSON_ID_5, resultsAfter[1]);
        Assertions.assertTrue(timeAfter.isAfter(timeBefore));
        Assertions.assertEquals(CAM_ALAMEDA, resultsAfter[3]);
        Assertions.assertEquals("13.0", resultsAfter[4]);
        Assertions.assertEquals("-36.5", resultsAfter[5]);

    }

    @Test
    public void trackMatchNOK_invalidCarId() {
        try {
            List<String> result = frontend.trackMatch(CAR_TYPE, INVALID_CAR_ID);
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.INVALID_ID, e.getErrorMessage());
        }
    }

    @Test
    public void trackMatchNOK_invalidPersonId() {
        try {
            List<String> result = frontend.trackMatch(PERSON_TYPE, INVALID_PERSON_ID);
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.INVALID_ID, e.getErrorMessage());
        }
    }

    @Test
    public void trackMatchNOK_PersonId() {
        try {
            List<String> result = frontend.trackMatch(PERSON_TYPE, INEXISTENT_PERSON_ID);
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.OBJECT_NOT_FOUND, e.getErrorMessage());
        }
    }

    @Test
    public void trackMatchNOK_CarId() {
        try {
            List<String> result = frontend.trackMatch(CAR_TYPE, INEXISTENT_CAR_ID);
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.OBJECT_NOT_FOUND, e.getErrorMessage());
        }
    }

    @Test
    public void trackMatchNOK_Type() {
        try {
            List<String> result = frontend.trackMatch(INEXISTENT_TYPE, PERSON_ID_1);
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.TYPE_DOES_NOT_EXIST, e.getErrorMessage());
        }
    }

    private void checkResults(List<String> output, List<String> cams, List<String> lats, List<String> longs, String type, List<String> ids) throws SauronException {
        int i = 0;
        for (String r : output) {
            System.out.println(r);
            String[] results = r.split(",");
            Assertions.assertEquals(6, results.length);
            Assertions.assertEquals(type, results[0]);
            Assertions.assertEquals(ids.get(i), results[1]);
            Assertions.assertEquals(cams.get(i), results[3]);
            Assertions.assertEquals(lats.get(i), results[4]);
            Assertions.assertEquals(longs.get(i), results[5]);
            i++;
        }
    }

    private List<String> setVals(String v1, String v2, String v3) {
        List<String> c = new ArrayList<>();
        c.add(v1);
        c.add(v2);
        c.add(v3);

        return c;
    }
}
