package pt.tecnico.sauron.A20.silo.domain;


import pt.tecnico.sauron.A20.exceptions.SauronException;

public abstract class SauronObject {

    private String _id;

    public SauronObject() {}

    protected void setId(String id) {
        _id = id;
    }

    public String getId() {
        return _id;
    }

    public abstract String getType();

    protected abstract void checkId(String id) throws SauronException;

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