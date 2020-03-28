package pt.tecnico.sauron.silo.domain;

import java.util.HashSet;
import java.util.Set;

public class Silo {

    private Set<SauronCamera> _cams = new HashSet<>();
    private Set<SauronObject> _objs = new HashSet<>();


    public Silo() { }

    public Set<SauronCamera> getCameras() {
        return _cams;
    }

    public Set<SauronObject> getObjects() {
        return _objs;
    }

    public void addCamera(SauronCamera cam) {
        if(!_cams.add(cam))
            //TODO - return exception
            return;
    }

    public void addObject(SauronObject obj) {
        if (!_objs.add(obj))
            //TODO - return exception
            return;
    }
}
