package pt.tecnico.sauron.silo.domain;

import java.util.List;

public abstract class SauronObject {

    private String _id;
    private List<SauronObservation> _obs;

    public SauronObject() { }

    public void addObservation(SauronObservation obs) {
        _obs.add(obs);
    }

    protected void setId(String id) {
        _id = id;
    }

    public String getId() {
        return _id;
    }

    public abstract String getType();

    protected abstract void checkId(String id);

    @Override
    public int hashCode() {
        return _id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SauronObject)
            return getId() == ((SauronObject)obj).getId();
        return false;
    }
}
