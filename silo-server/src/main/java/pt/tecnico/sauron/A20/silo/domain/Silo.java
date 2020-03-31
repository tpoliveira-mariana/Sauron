package pt.tecnico.sauron.A20.silo.domain;

import java.util.*;
import java.util.stream.Collectors;

import pt.tecnico.sauron.A20.exceptions.*;


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

    public void clear() {
        _cams.clear();
        _objs.clear();
    }

    public void addCamera(SauronCamera cam) throws SauronException {
        SauronCamera test = _cams.get(cam.getName());

        if (test != null && test.getLatitude() == cam.getLatitude() && test.getLongitude() == cam.getLongitude())  {
            throw new SauronException(ErrorMessage.DUPLICATE_CAMERA);
        }
        else if (test != null) {
            throw new  SauronException(ErrorMessage.DUPLICATE_CAM_NAME);
        }
        _cams.put(cam.getName(), cam);

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
        SauronCamera.checkAttributes(name, 0, 0);
        SauronCamera cam = _cams.get(name);
        if (cam == null)
            throw new SauronException(ErrorMessage.CAMERA_NOT_FOUND);
        return cam;
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
                throw new SauronException(ErrorMessage.TYPE_DOES_NOT_EXIST);
        }
    }

    public SauronObservation track(String type, String id) throws SauronException {
        SauronObject object = createNewObject(type, id);
        List<SauronObservation> sauObs = _objs.get(object);
        if (sauObs == null)
            throw new SauronException(ErrorMessage.OBJECT_NOT_FOUND);

        return sauObs.get(sauObs.size()-1);
    }

    public List<SauronObservation> trackMatch(String type, String parcId) throws SauronException {
        String regex = buildRegex(parcId);
        return _objs.keySet()
                .stream()
                .filter(obj -> obj.getId().matches(regex) && obj.getType().equals(type))
                .map(obj -> _objs.get(obj).get(_objs.get(obj).size()-1))
                .collect(Collectors.toList());
    }

    public List<SauronObservation> trace(String type, String id) throws SauronException {
        SauronObject object = createNewObject(type, id);
        List<SauronObservation> sauObs = _objs.get(object);
        if (sauObs == null)
            throw new SauronException(ErrorMessage.OBJECT_NOT_FOUND);

        sauObs = new ArrayList<>(sauObs);
        Collections.reverse(sauObs);
        return sauObs;
    }

    private String buildRegex(String parcId) throws SauronException {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i < parcId.length(); i++) {
            char c = parcId.charAt(i);
            if (c == '*')
                builder.append(".*");
            else if (Character.isLetterOrDigit(c))
                builder.append(c);
            else {
                throw new SauronException(ErrorMessage.INVALID_ID);
            }
        }
        return builder.toString();
    }
}
