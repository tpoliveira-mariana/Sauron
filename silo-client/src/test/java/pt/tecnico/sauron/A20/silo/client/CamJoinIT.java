package pt.tecnico.sauron.A20.silo.client;

import org.junit.jupiter.api.*;
import pt.tecnico.sauron.A20.exceptions.ErrorMessage;
import pt.tecnico.sauron.A20.exceptions.SauronException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CamJoinIT extends BaseIT{
    private static final String HOST = "localhost";
    private static final String PORT = "8080";
    private static final String TEST_DATA_FILE = "./src/test/camJoinIT_data.txt";
    private static SiloFrontend frontend;

    private static final String NAME_TAGUS = "Tagus";
    private static final double LAT_TAGUS = 38.737613;
    private static final double LON_TAGUS = -9.303164;
    private static final String NAME_ALAMEDA = "Alameda";
    private static final double LAT_ALAMEDA = 38.736748;
    private static final double LON_ALAMEDA = -9.138908;
    private static final String SHORT_NAME = "as";
    private static final String LONG_NAME = "camcamcacmacmcam";
    private static final String NULL_NAME = null;
    private static final String SYMBOLIC_NAME = "cam-cam";
    private static final double INVALID_LAT = 91.0;
    private static final double INVALID_LON = 181.0;



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

    @BeforeEach
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
    public void camJoinOK_OneCam() {
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA));
    }

    @Test
    public void camJoinOK_DiffNameDiffCoords() {
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA));
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_TAGUS, LAT_TAGUS, LON_TAGUS));
    }

    @Test
    public void camJoinOK_DiffNameSameCoords() {
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA));
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_TAGUS, LAT_ALAMEDA, LON_ALAMEDA));
    }

    @Test
    public void camJoinNOK_NameTooShort() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camJoin(SHORT_NAME, LAT_ALAMEDA, LON_ALAMEDA));
        Assertions.assertEquals(ErrorMessage.INVALID_CAM_NAME, e.getErrorMessage());
    }

    @Test
    public void camJoinNOK_NameTooLong() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camJoin(LONG_NAME, LAT_ALAMEDA, LON_ALAMEDA));
        Assertions.assertEquals(ErrorMessage.INVALID_CAM_NAME, e.getErrorMessage());
    }

    @Test
    public void camJoinNOK_NameIsNull() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camJoin(NULL_NAME, LAT_ALAMEDA, LON_ALAMEDA));
        Assertions.assertEquals(ErrorMessage.INVALID_CAM_NAME, e.getErrorMessage());
    }

    @Test
    public void camJoinNOK_NameNotAlphanum() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camJoin(SYMBOLIC_NAME, LAT_ALAMEDA, LON_ALAMEDA));
        Assertions.assertEquals(ErrorMessage.INVALID_CAM_NAME, e.getErrorMessage());
    }

    @Test
    public void camJoinNOK_invalidCoords() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camJoin(NAME_ALAMEDA, INVALID_LAT, INVALID_LON));
        Assertions.assertEquals(ErrorMessage.INVALID_COORDINATES, e.getErrorMessage());
    }

    @Test
    public void camJoinNOK_duplicateCam() {
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA));
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camJoin(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA));
        Assertions.assertEquals(ErrorMessage.DUPLICATE_CAM_NAME, e.getErrorMessage());
    }


}
