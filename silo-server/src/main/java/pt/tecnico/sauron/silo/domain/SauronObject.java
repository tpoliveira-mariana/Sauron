package pt.tecnico.sauron.silo.domain;


import pt.tecnico.sauron.exceptions.ErrorMessage;
import pt.tecnico.sauron.exceptions.SauronException;

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

    public static void checkId(String id) throws SauronException {
        throw new SauronException(ErrorMessage.INVALID_ID);
    }

}
