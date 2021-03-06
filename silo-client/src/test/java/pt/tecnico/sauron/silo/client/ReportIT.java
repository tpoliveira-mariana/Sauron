package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;
import pt.tecnico.sauron.exceptions.ErrorMessage;
import pt.tecnico.sauron.exceptions.SauronException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.util.ArrayList;
import java.util.List;

public class ReportIT extends BaseIT {

    private static final String TEST_DATA_FILE = "./src/test/reportIT_data.txt";
    private static SiloFrontend frontend;
    private static final String ZOOHOST = "localhost";
    private static final String ZOOPORT = "2181";
    private static final String PATH = "/grpc/sauron/silo/1";
    private static final String CAM_NAME_1 = "Tagus";
    private static final String INEXISTENT_CAM_NAME = "Test";
    private static final String PERSON_ID_1 = "1";
    private static final String INVALID_PERSON_ID_1 = "-5";
    private static final String INVALID_PERSON_ID_2 = "100000000000000000000000000";
    private static final String INVALID_PERSON_ID_3 = "12AB";
    private static final String CAR_ID_1 = "1111AA";
    private static final String INVALID_CAR_ID_1 = "555";
    private static final String INVALID_CAR_ID_2 = "112233";
    private static final String INVALID_CAR_ID_3 = "AA12cc";
    private static final String INVALID_CAR_ID_4 = "AAAAAA";

    // one-time initialization and clean-up
    @BeforeEach
    public void SetUp() throws ZKNamingException {
        try {
            frontend = new SiloFrontend(ZOOHOST, ZOOPORT, 1, 1);
            frontend.ctrlInit(TEST_DATA_FILE);
        }
        catch (SauronException e) {
            System.out.println(e.getErrorMessageLabel());
        }

    }

    @AfterEach
    public void TearDown() {
        try {
            frontend.ctrlClear();
        }
            catch (SauronException e) {
            System.out.println(e.getErrorMessageLabel());
        }
    }

    // tests

    @Test
    public void reportOK() {
        List<List<String>> obs = new ArrayList<>();
        addObservation("person", PERSON_ID_1, obs);
        addObservation("car", CAR_ID_1, obs);
        Assertions.assertDoesNotThrow(() -> frontend.report(CAM_NAME_1, obs));
    }

    @Test
    void reportNOK_Cam() {
        List<List<String>> obs = new ArrayList<>();
        addObservation("person", PERSON_ID_1, obs);
        Assertions.assertDoesNotThrow(() -> frontend.report(INEXISTENT_CAM_NAME, obs));
    }

    @Test
    void reportNOK_Type() {
        List<List<String>> obs = new ArrayList<>();
        addObservation("teacher", PERSON_ID_1, obs);
        Assertions.assertEquals(
                ErrorMessage.TYPE_DOES_NOT_EXIST,
                Assertions.assertThrows(
                        SauronException.class,
                        () -> frontend.report(CAM_NAME_1, obs)
                ).getErrorMessage()
        );
    }

    @Test
    void reportNOK_NoObs() {
        List<List<String>> obs = null;
        Assertions.assertThrows(
                NullPointerException.class,
                () -> frontend.report(CAM_NAME_1, obs)
        );
    }

    @Test
    void reportNOK_PersonId_1() {
        List<List<String>> obs = new ArrayList<>();
        addObservation("person", INVALID_PERSON_ID_1, obs);
        Assertions.assertEquals(
                ErrorMessage.INVALID_PERSON_ID,
                Assertions.assertThrows(
                        SauronException.class,
                        () -> frontend.report(CAM_NAME_1, obs)
                ).getErrorMessage()
        );
    }

    @Test
    void reportNOK_PersonId_2() {
        List<List<String>> obs = new ArrayList<>();
        addObservation("person", INVALID_PERSON_ID_2, obs);
        Assertions.assertEquals(
                ErrorMessage.INVALID_PERSON_ID,
                Assertions.assertThrows(
                        SauronException.class,
                        () -> frontend.report(CAM_NAME_1, obs)
                ).getErrorMessage()
        );
    }

    @Test
    void reportNOK_PersonId_3() {
        List<List<String>> obs = new ArrayList<>();
        addObservation("person", INVALID_PERSON_ID_3, obs);
        Assertions.assertEquals(
                ErrorMessage.INVALID_PERSON_ID,
                Assertions.assertThrows(
                        SauronException.class,
                        () -> frontend.report(CAM_NAME_1, obs)
                ).getErrorMessage()
        );
    }

    @Test
    void reportNOK_CarId_1() {
        List<List<String>> obs = new ArrayList<>();
        addObservation("car", INVALID_CAR_ID_1, obs);
        Assertions.assertEquals(
                ErrorMessage.INVALID_CAR_ID,
                Assertions.assertThrows(
                        SauronException.class,
                        () -> frontend.report(CAM_NAME_1, obs)
                ).getErrorMessage()
        );
    }

    @Test
    void reportNOK_CarId_2() {
        List<List<String>> obs = new ArrayList<>();
        addObservation("car", INVALID_CAR_ID_2, obs);
        Assertions.assertEquals(
                ErrorMessage.INVALID_CAR_ID,
                Assertions.assertThrows(
                        SauronException.class,
                        () -> frontend.report(CAM_NAME_1, obs)
                ).getErrorMessage()
        );
    }

    @Test
    void reportNOK_CarId_3() {
        List<List<String>> obs = new ArrayList<>();
        addObservation("car", INVALID_CAR_ID_3, obs);
        Assertions.assertEquals(
                ErrorMessage.INVALID_CAR_ID,
                Assertions.assertThrows(
                        SauronException.class,
                        () -> frontend.report(CAM_NAME_1, obs)
                ).getErrorMessage()
        );
    }

    @Test
    void reportNOK_CarId_4() {
        List<List<String>> obs = new ArrayList<>();
        addObservation("car", INVALID_CAR_ID_4, obs);
        Assertions.assertEquals(
                ErrorMessage.INVALID_CAR_ID,
                Assertions.assertThrows(
                        SauronException.class,
                        () -> frontend.report(CAM_NAME_1, obs)
                ).getErrorMessage()
        );
    }


    private void addObservation(String type, String id, List<List<String>> obs) {
        List<String> obs1= new ArrayList<>();
        obs1.add(type);
        obs1.add(id);
        obs.add(obs1);
    }
}
