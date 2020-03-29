package pt.tecnico.sauron.A20.silo.domain;

import java.util.*;
import pt.tecnico.sauron.A20.exceptions.*;
import pt.tecnico.sauron.A20.silo.grpc.Observation;
import pt.tecnico.sauron.A20.silo.grpc.Status;

import static pt.tecnico.sauron.A20.exceptions.ErrorMessage.*;


public class Silo {

    private Map<String, SauronCamera> _cams = new HashMap<>();
    private Map<SauronObject, List<SauronObservation>> _objs = new HashMap<>();


    public Silo() { }

    public Map<String, SauronCamera> getCameras() {
        return _cams;
    }

    public Map<SauronObject, List<SauronObservation>> getObjects() {
        return _objs;
    }

    public void addCamera(SauronCamera cam) throws SauronException {
        if(_cams.putIfAbsent(cam.getName(), cam) != null)
            throw new SauronException(DUPLICATE_CAMERA);
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

    public SauronCamera getCamByName(String name) throws SauronException {
        try {
            return _cams.get(name);
        }
        catch (NullPointerException e) {
            throw new SauronException(CAMERA_NOT_FOUND);
        }
    }

    public SauronObject getObjectByTypeAndId(String type, String id) throws SauronException {
        Optional<SauronObject> obj = _objs.keySet().stream().filter(object -> object.getType().equals(type) && object.getId().equals(id)).findFirst();
        if (obj.isEmpty()) {
            return createNewObject(type, id);
        }
        return obj.get();
    }

    public SauronObject createNewObject(String type, String id) throws SauronException {
        switch (type){
            case "person":
                return new SauronPerson(id);
            case "car":
                return new SauronCar(id);
            default:
                throw new SauronException(TYPE_DOES_NOT_EXIST);
        }
    }
}
