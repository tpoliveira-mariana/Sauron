package pt.tecnico.sauron.A20.silo.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.A20.exceptions.ErrorMessage;
import pt.tecnico.sauron.A20.exceptions.SauronException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TrackMatch extends BaseIT{
    private static final String HOST = "localhost";
    private static final String PORT = "8080";
    private static final String TEST_DATA_FILE = "./src/test/trackIT_data.txt";
    private static SiloFrontend frontend;
    private static final String PERSON_ID = "11";
    private static final String INEXISTENT_PERSON_ID = "30";
    private static final String INVALID_PERSON_ID = "abc";
    private static final String PERSON_TYPE = "person";
    private static final String CAR_TYPE = "car";
    private static final String CAR_ID = "AAAA11";
    private static final String INEXISTENT_CAR_ID = "ZZZZ99";
    private static final String INVALID_CAR_ID = "ZZZZZZ";
    private static final String INEXISTENT_TYPE = "dog";
    private static final String CAM_TAGUS = "Tagus";
    private static final String CAM_ALAMEDA = "Alameda";


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
        try {
            frontend.ctrlClear();
        }
        catch (SauronException e) {
            System.out.println(e.getErrorMessageLabel());
        }
    }

    // tests

    @Test
    public void trackOK() throws SauronException{
        /*List<String> result = frontend.trackMatch(CAR_TYPE, CAR_ID);
        String[] results = result.split(",");

        Assertions.assertEquals(6, results.length);
        Assertions.assertEquals("car", results[0]);
        Assertions.assertEquals(CAR_ID, results[1]);
        Assertions.assertEquals(CAM_TAGUS, results[3]);
        Assertions.assertEquals("12.0", results[4]);
        Assertions.assertEquals("-36.0", results[5]);*/
    }

    @Test
    public void trackLastObservationOK() throws SauronException, InterruptedException{
        /*DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        List<String> result = frontend.trackMatch(PERSON_TYPE, PERSON_ID);
        String[] results = result.split(",");
        LocalDateTime timeBefore = LocalDateTime.parse(results[2], formatter);

        List<String> observation = new ArrayList<>();
        List<List<String>> observations = new ArrayList<>();
        observation.add(PERSON_TYPE);
        observation.add(PERSON_ID);
        observations.add(observation);

        Thread.sleep(1000); // test needs to sleep in order to compare the times
        frontend.report(CAM_ALAMEDA, observations);

        result = frontend.trackMatch(PERSON_TYPE, PERSON_ID);
        String[] resultsAfter = result.split(",");

        LocalDateTime timeAfter = LocalDateTime.parse(resultsAfter[2], formatter);

        Assertions.assertEquals(6, resultsAfter.length);
        Assertions.assertEquals("person", resultsAfter[0]);
        Assertions.assertEquals(PERSON_ID, resultsAfter[1]);
        Assertions.assertTrue(timeAfter.isAfter(timeBefore));
        Assertions.assertEquals(CAM_ALAMEDA, resultsAfter[3]);
        Assertions.assertEquals("13.0", resultsAfter[4]);
        Assertions.assertEquals("-36.5", resultsAfter[5]);*/

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
            List<String> result = frontend.trackMatch(INEXISTENT_TYPE, PERSON_ID);
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.TYPE_DOES_NOT_EXIST, e.getErrorMessage());
        }
    }
}
