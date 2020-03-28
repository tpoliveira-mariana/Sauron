package pt.tecnico.sauron.silo.domain;

public class SauronPerson extends SauronObject {

    public SauronPerson(String id) {
        checkId(id);
        setId(id);
    }

    public SauronPerson(String id, SauronObservation obs) {
        checkId(id);
        setId(id);
        addObservation(obs);
    }

    @Override
    protected void checkId(String id) {
        // TODO - check and exceptions
        return;
    }

    @Override
    public String getType(){
        return "person";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SauronPerson)
            return getId() == ((SauronPerson)obj).getId();
        return false;
    }


}
