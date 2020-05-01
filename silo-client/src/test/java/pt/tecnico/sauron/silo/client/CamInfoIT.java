package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;
import pt.tecnico.sauron.exceptions.ErrorMessage;
import pt.tecnico.sauron.exceptions.SauronException;
import pt.tecnico.sauron.silo.grpc.CamInfoResponse;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class CamInfoIT extends BaseIT{
    private static final String HOST = "localhost";
    private static final String PORT = "2181";
    private static final String PATH = "/grpc/sauron/silo/1";
    private static SiloFrontend frontend;

    private static final String NAME_ALAMEDA = "Alameda";
    private static final String NAME_TAGUS = "Tagus";
    private static final double LAT_ALAMEDA = 38.736748;
    private static final double LON_ALAMEDA = -9.138908;
    private static final String SHORT_NAME = "as";
    private static final String LONG_NAME = "camcamcacmacmcam";
    private static final String NULL_NAME = null;
    private static final String SYMBOLIC_NAME = "cam-cam";


    // one-time initialization and clean-up
    @BeforeAll
    public static void oneTimeSetUp() throws ZKNamingException {
        try {
            frontend = new SiloFrontend(HOST, PORT, -1);
            frontend.ctrlClear();
        } catch (SauronException e) {
            System.out.println(e.getErrorMessageLabel());
        }
    }

    @AfterAll
    public static void oneTimeCleanUp() {
        try {
            frontend.ctrlClear();
        } catch (SauronException e) {
            System.out.println(e.getErrorMessageLabel());
        }
    }

    // tests

    @Test
    public void camInfoOK_noCams() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camInfo(NAME_ALAMEDA));
        Assertions.assertEquals(ErrorMessage.CAMERA_NOT_FOUND, e.getErrorMessage());
    }

    @Test
    public void camInfoOK_camExists() {
        oneTimeCleanUp();
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA));

        CamInfoResponse response = Assertions.assertDoesNotThrow(() -> frontend.camInfo(NAME_ALAMEDA));
        Assertions.assertEquals(LAT_ALAMEDA, response.getCoordinates().getLatitude());
        Assertions.assertEquals(LON_ALAMEDA, response.getCoordinates().getLongitude());

        oneTimeCleanUp();
    }

    @Test
    public void camInfoOK_camNotExists() {
        oneTimeCleanUp();
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA));

        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camInfo(NAME_TAGUS));
        Assertions.assertEquals(ErrorMessage.CAMERA_NOT_FOUND, e.getErrorMessage());

        oneTimeCleanUp();
    }

    @Test
    public void camInfoNOK_NameTooShort() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camInfo(SHORT_NAME));
        Assertions.assertEquals(ErrorMessage.INVALID_CAM_NAME, e.getErrorMessage());
    }

    @Test
    public void camInfoNOK_NameTooLong() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camInfo(LONG_NAME));
        Assertions.assertEquals(ErrorMessage.INVALID_CAM_NAME, e.getErrorMessage());
    }

    @Test
    public void camInfoNOK_NameIsNull() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camInfo(NULL_NAME));
        Assertions.assertEquals(ErrorMessage.INVALID_CAM_NAME, e.getErrorMessage());
    }

    @Test
    public void camInfoNOK_NameNotAlphanum() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camInfo(SYMBOLIC_NAME));
        Assertions.assertEquals(ErrorMessage.INVALID_CAM_NAME, e.getErrorMessage());
    }
}
