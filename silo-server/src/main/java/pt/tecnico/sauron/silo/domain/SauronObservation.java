package pt.tecnico.sauron.silo.domain;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SauronObservation {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private SauronObject _obj;
    private SauronCamera _cam;
    private String _ts;


    public SauronObservation(SauronObject obj, SauronCamera cam, ZonedDateTime ts) {
        setObject(obj);
        setCamera(cam);
        setTimeStamp(ts.format(formatter));
    }

    public SauronObject getObject() {
        return _obj;
    }

    public String getObjectId() {
        return _obj.getId();
    }

    public String getObjectType() {
        return _obj.getType();
    }

    public SauronCamera getCamera() {
        return _cam;
    }

    public String getTimeStamp() {
        return _ts;
    }

    private void setObject(SauronObject obj) {
        _obj = obj;
    }

    private void setCamera(SauronCamera cam) {
        _cam = cam;
    }

    private void setTimeStamp(String ts) {
        _ts = ts;
    }
}