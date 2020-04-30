package pt.tecnico.sauron.silo.domain;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import pt.tecnico.sauron.exceptions.ErrorMessage;
import pt.tecnico.sauron.exceptions.SauronException;


public class Silo {

    private Map<String, SauronCamera> _cams = new HashMap<>();
    private Map<String, SauronObject> _objs = new HashMap<>();
    private Map<SauronObject, List<SauronObservation>> _obs = new HashMap<>();


    public Silo() { }

    public Map<String, SauronCamera> getCameras() {
        return _cams;
    }

    public Map<SauronObject, List<SauronObservation>> getObjects() {
        return _obs;
    }

    public void clear() {
        _cams.clear();
        _objs.clear();
        _obs.clear();
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

    public SauronObject addObject(String type, String id) throws SauronException {
        SauronObject sauObj = createNewObject(type, id);
        String key = type + ":" + id;
        _objs.put(key, sauObj);
        return sauObj;
    }

    public SauronObject getObject(String type, String id) {
        String key = type + ":" + id;
        return _objs.get(key);
    }

    public void addObservation(SauronObservation obs) {
        List<SauronObservation> lstObs = _obs.get(obs.getObject());
        if (lstObs == null) {
            lstObs = new ArrayList<>();
            lstObs.add(obs);
            _obs.put(obs.getObject(), lstObs);
        } else {
            lstObs.add(obs);
        }
    }

    public SauronObservation findObservation(SauronObject sauObj, SauronCamera sauCam, String ts) {
        for (SauronObservation sauObs : _obs.get(sauObj)) {
            if (sauObs.getCamera().equals(sauCam) && sauObs.getTimeStamp().equals(ts))
                return sauObs;
        }
        return null;
    }

    public SauronCamera getCamByName(String name) throws SauronException {
        SauronCamera.checkAttributes(name, 0, 0);
        SauronCamera cam = _cams.get(name);
        if (cam == null)
            throw new SauronException(ErrorMessage.CAMERA_NOT_FOUND);
        return cam;
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

    SauronObservation findMostRecent(List<SauronObservation> obs) {
        SauronObservation max = obs.get(0);

        for (int i = 1; i < obs.size(); i++) {
            if (max.compareTo(obs.get(i)) < 0) {
                max = obs.get(i);
            }
        }
         return max;
    }

    public SauronObservation track(String type, String id) throws SauronException {
        SauronObject object = getObject(type, id);
        if (object == null)
            throw new SauronException(ErrorMessage.OBJECT_NOT_FOUND);

        List<SauronObservation> sauObs = _obs.get(object);
        if (sauObs.isEmpty())
            throw new SauronException(ErrorMessage.OBJECT_NOT_FOUND);

        return findMostRecent(sauObs);
    }

    public List<SauronObservation> trackMatch(String type, String partId) throws SauronException {
        Pattern pattern = buildRegex(partId);
        return _obs.keySet()
                .stream()
                .filter(obj -> pattern.matcher(obj.getId()).matches() && obj.getType().equals(type))
                .map(obj -> findMostRecent(_obs.get(obj)))
                .collect(Collectors.toList());
    }

    public List<SauronObservation> trace(String type, String id) throws SauronException {
        SauronObject object = getObject(type, id);
        if (object == null)
            throw new SauronException(ErrorMessage.OBJECT_NOT_FOUND);

        List<SauronObservation> sauObs = _obs.get(object);
        if (sauObs == null)
            throw new SauronException(ErrorMessage.OBJECT_NOT_FOUND);

        sauObs = new ArrayList<>(sauObs);
        Collections.sort(sauObs);
        Collections.reverse(sauObs);
        return sauObs;
    }

    private Pattern buildRegex(String partId) throws SauronException {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i < partId.length(); i++) {
            char c = partId.charAt(i);
            if (c == '*')
                builder.append(".*");
            else if (Character.isLetterOrDigit(c))
                builder.append(c);
            else {
                throw new SauronException(ErrorMessage.INVALID_ID);
            }
        }
        return Pattern.compile(builder.toString());
    }
}
