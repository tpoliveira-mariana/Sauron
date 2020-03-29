package pt.tecnico.sauron.A20.silo.domain;

import pt.tecnico.sauron.A20.silo.grpc.ObjectType;
import pt.tecnico.sauron.A20.silo.grpc.Observation;

import java.util.*;

public class Silo {

    private Map<String, SauronCamera> _cams = new HashMap<>();
    private Map<SauronObject, List<SauronObservation>> _objs = new HashMap<>();


    public Silo() {

    }

    public Map<String, SauronCamera> getCameras() {
        return _cams;
    }

    public Map<SauronObject, List<SauronObservation>> getObjects() {
        return _objs;
    }

    public void addCamera(SauronCamera cam) {
        if(_cams.putIfAbsent(cam.getName(), cam) != null)
            //TODO - return exception
            return;
    }

    public void addObservation(SauronObservation obs) {
        List<SauronObservation> lstObs = _objs.get(obs.getObject());
        if (lstObs == null) {
            lstObs = new ArrayList<>();
            lstObs.add(obs);
            _objs.put(obs.getObject(), lstObs);
        } else {
            lstObs.add(obs);
        }
    }

    public SauronCamera getCamByName(String name) {
        return _cams.get(name);
    }

}
