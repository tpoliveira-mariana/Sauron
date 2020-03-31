package pt.tecnico.sauron.A20.silo.domain;

import pt.tecnico.sauron.A20.exceptions.SauronException;

import static pt.tecnico.sauron.A20.exceptions.ErrorMessage.INVALID_CAM_NAME;
import static pt.tecnico.sauron.A20.exceptions.ErrorMessage.INVALID_COORDINATES;

public class SauronCamera {

    private String _name;
    private double _lat;
    private double _lon;


    public SauronCamera(String name, double lat, double lon) throws SauronException {
        checkAttributes(name, lat, lon);
        setName(name);
        setLatitude(lat);
        setLongitude(lon);

    }

    public static void checkAttributes(String name, double lat, double lon) throws SauronException {
        if (lat > 90 || lat < -90 || lon > 180 || lon < -180)
            throw new SauronException(INVALID_COORDINATES);

        else if (!name.matches("[A-Za-z0-9]+") || name.length() < 3 || name.length() > 15)
            throw new SauronException(INVALID_CAM_NAME);
    }

    public String getName() {
        return _name;
    }

    public double getLatitude() {
        return _lat;
    }

    public double getLongitude() {
        return _lon;
    }

    private void setName(String name) {
        _name = name;
    }

    private void setLatitude(double lat) {
        _lat = lat;
    }

    private void setLongitude(double lon) {
        _lon = lon;
    }

}
