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

public class TrackIT extends BaseIT{
    private static final String HOST = "localhost";
    private static final String PORT = "8080";
    private static final String TEST_DATA_FILE = "./src/test/trackIT_data.txt";
    private static SiloFrontend frontend;
    private static final String PERSON_ID = "1";
    private static final String INEXISTENT_PERSON_ID = "30";
    private static final String INVALID_PERSON_ID = "abc";
    private static final String PERSON_TYPE = "person";
    private static final String CAR_TYPE = "car";
    private static final String INEXISTENT_CAR_ID = "ZZZZ99";
    private static final String INVALID_CAR_ID = "ZZZZZZ";
    private static final String INEXISTENT_TYPE = "dog";
    private static final String CAM_TAGUS = "Tagus";


    // one-time initialization and clean-up
    @BeforeAll
    public static void oneTimeSetUp() {
        frontend = new SiloFrontend(HOST, PORT);
        try {
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
    public void trackOK() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime previousTime = LocalDateTime.now();

            String result = frontend.track(PERSON_TYPE, PERSON_ID);
            String[] results = result.split(",");

            LocalDateTime resultTime = LocalDateTime.parse(results[2], formatter);
            LocalDateTime postTime = LocalDateTime.now();

            Assertions.assertEquals(6, results.length);
            Assertions.assertEquals("person", results[0]);
            Assertions.assertEquals(PERSON_ID, results[1]);
            Assertions.assertTrue(resultTime.isAfter(previousTime) && resultTime.isBefore(postTime));
            Assertions.assertEquals(CAM_TAGUS, results[3]);
            Assertions.assertEquals("12.0", results[4]);
            Assertions.assertEquals("-36.0", results[5]);
        }
        catch (SauronException e) {
            System.out.println(e.getErrorMessageLabel());
        }
    }

    @Test
    public void traceNOK_invalidCarId() {
        try {
            String result = frontend.track(CAR_TYPE, INVALID_CAR_ID);
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.INVALID_CAR_ID, e.getErrorMessage());
        }
    }

    @Test
    public void traceNOK_invalidPersonId() {
        try {
            String result = frontend.track(PERSON_TYPE, INVALID_PERSON_ID);
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.INVALID_PERSON_IDENTIFIER, e.getErrorMessage());
        }
    }

    @Test
    public void traceNOK_PersonId() {
        try {
            String result = frontend.track(PERSON_TYPE, INEXISTENT_PERSON_ID);
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.OBJECT_NOT_FOUND, e.getErrorMessage());
        }
    }

    @Test
    public void traceNOK_CarId() {
        try {
            String result = frontend.track(CAR_TYPE, INEXISTENT_CAR_ID);
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.OBJECT_NOT_FOUND, e.getErrorMessage());
        }
    }

    @Test
    public void traceNOK_Type() {
        try {
            String result = frontend.track(INEXISTENT_TYPE, PERSON_ID);
            Assertions.assertTrue(result.isEmpty());
        }
        catch (SauronException e) {
            Assertions.assertEquals(ErrorMessage.TYPE_DOES_NOT_EXIST, e.getErrorMessage());
        }
    }

}
