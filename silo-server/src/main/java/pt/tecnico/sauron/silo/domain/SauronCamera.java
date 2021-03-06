package pt.tecnico.sauron.silo.domain;

import pt.tecnico.sauron.exceptions.SauronException;
import pt.tecnico.sauron.exceptions.ErrorMessage;

import java.util.regex.Pattern;

public class SauronCamera {

    private String _name;
    private double _lat;
    private double _lon;

    // fully detect valid cam name
    private static final Pattern NAME_PATT = Pattern.compile("[A-Za-z0-9]{3,15}+");

    public SauronCamera(String name, double lat, double lon) throws SauronException {
        checkAttributes(name, lat, lon);
        setName(name);
        setLatitude(lat);
        setLongitude(lon);

    }

    public static void checkAttributes(String name, double lat, double lon) throws SauronException {
        if (lat > 90 || lat < -90 || lon > 180 || lon < -180)
            throw new SauronException(ErrorMessage.INVALID_COORDINATES);

        if (!NAME_PATT.matcher(name).matches())
            throw new SauronException(ErrorMessage.INVALID_CAM_NAME);
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
