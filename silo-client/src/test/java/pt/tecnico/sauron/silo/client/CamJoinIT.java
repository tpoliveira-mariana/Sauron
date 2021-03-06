package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;
import pt.tecnico.sauron.exceptions.ErrorMessage;
import pt.tecnico.sauron.exceptions.SauronException;
import pt.tecnico.sauron.silo.grpc.CamInfoResponse;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class CamJoinIT extends BaseIT{
    private static final String ZOOHOST = "localhost";
    private static final String ZOOPORT = "2181";
    private static final String PATH = "/grpc/sauron/silo/1";
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
    private static final double INVALID_LAT_SUP = 91.0;
    private static final double INVALID_LON_SUP = 181.0;
    private static final double INVALID_LAT_INF = -91.0;
    private static final double INVALID_LON_INF = -181.0;


    @BeforeAll
    public static void oneTimeSetUp() throws ZKNamingException {
        try {
            frontend = new SiloFrontend(ZOOHOST, ZOOPORT, 1, 1);
        } catch(SauronException e){
            System.out.println(e.getErrorMessageLabel());
        }
    }

    @BeforeEach
    public void cleanBeforeEach() {
        try {
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

    private void assertCamSaved(String name, double lat, double lon, boolean saved) {
        if (saved) {
            CamInfoResponse response = Assertions.assertDoesNotThrow(() -> frontend.camInfo(name));
            Assertions.assertEquals(lat, response.getCoordinates().getLatitude());
            Assertions.assertEquals(lon, response.getCoordinates().getLongitude());
        } else {
            try {
                CamInfoResponse response = frontend.camInfo(name);
                Assertions.assertNotEquals(lat, response.getCoordinates().getLatitude());
                Assertions.assertNotEquals(lon, response.getCoordinates().getLongitude());
            } catch (SauronException e) {
                Assertions.assertTrue(e.getErrorMessage() == ErrorMessage.CAMERA_NOT_FOUND
                        || e.getErrorMessage() == ErrorMessage.INVALID_CAM_NAME);
            }
        }
    }


    // tests
    @Test
    public void camJoinOK_OneCam() {
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA));
        assertCamSaved(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA, true);
    }

    @Test
    public void camJoinOK_DiffNameDiffCoords() {
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA));
        assertCamSaved(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA, true);
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_TAGUS, LAT_TAGUS, LON_TAGUS));
        assertCamSaved(NAME_TAGUS, LAT_TAGUS, LON_TAGUS, true);
    }

    @Test
    public void camJoinOK_DiffNameSameCoords() {
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA));
        assertCamSaved(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA, true);
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_TAGUS, LAT_ALAMEDA, LON_ALAMEDA));
        assertCamSaved(NAME_TAGUS, LAT_ALAMEDA, LON_ALAMEDA, true);
    }

    @Test
    public void camJoinOK_duplicateCam() {
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA));
        assertCamSaved(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA, true);

        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA));
        assertCamSaved(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA, true);
    }

    @Test
    public void camJoinOK_duplicateCamName() {
        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA));
        assertCamSaved(NAME_ALAMEDA, LAT_ALAMEDA, LON_ALAMEDA, true);

        Assertions.assertDoesNotThrow(() -> frontend.camJoin(NAME_ALAMEDA, LAT_TAGUS, LON_TAGUS));
        assertCamSaved(NAME_ALAMEDA, LAT_TAGUS, LON_TAGUS, false);
    }

    @Test
    public void camJoinNOK_NameTooShort() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camJoin(SHORT_NAME, LAT_ALAMEDA, LON_ALAMEDA));
        Assertions.assertEquals(ErrorMessage.INVALID_CAM_NAME, e.getErrorMessage());
        assertCamSaved(SHORT_NAME, LAT_ALAMEDA, LON_ALAMEDA, false);
    }

    @Test
    public void camJoinNOK_NameTooLong() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camJoin(LONG_NAME, LAT_ALAMEDA, LON_ALAMEDA));
        Assertions.assertEquals(ErrorMessage.INVALID_CAM_NAME, e.getErrorMessage());
        assertCamSaved(LONG_NAME, LAT_ALAMEDA, LON_ALAMEDA, false);
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
        assertCamSaved(SYMBOLIC_NAME, LAT_ALAMEDA, LON_ALAMEDA, false);
    }

    @Test
    public void camJoinNOK_invalidSupCoords() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camJoin(NAME_ALAMEDA, INVALID_LAT_SUP, INVALID_LON_SUP));
        Assertions.assertEquals(ErrorMessage.INVALID_COORDINATES, e.getErrorMessage());
        assertCamSaved(NAME_ALAMEDA, INVALID_LAT_SUP, INVALID_LON_SUP, false);
    }

    @Test
    public void camJoinNOK_invalidInfCoords() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camJoin(NAME_ALAMEDA, INVALID_LAT_INF, INVALID_LON_INF));
        Assertions.assertEquals(ErrorMessage.INVALID_COORDINATES, e.getErrorMessage());
        assertCamSaved(NAME_ALAMEDA, INVALID_LAT_INF, INVALID_LON_INF, false);
    }

    @Test
    public void camJoinNOK_invalidMixCoords() {
        SauronException e = Assertions.assertThrows(SauronException.class, () -> frontend.camJoin(NAME_ALAMEDA, INVALID_LAT_INF, INVALID_LON_SUP));
        Assertions.assertEquals(ErrorMessage.INVALID_COORDINATES, e.getErrorMessage());
        assertCamSaved(NAME_ALAMEDA, INVALID_LAT_INF, INVALID_LON_SUP, false);

        e = Assertions.assertThrows(SauronException.class, () -> frontend.camJoin(NAME_ALAMEDA, INVALID_LAT_SUP, INVALID_LON_INF));
        Assertions.assertEquals(ErrorMessage.INVALID_COORDINATES, e.getErrorMessage());
        assertCamSaved(NAME_ALAMEDA, INVALID_LAT_SUP, INVALID_LON_INF, false);
    }
}
