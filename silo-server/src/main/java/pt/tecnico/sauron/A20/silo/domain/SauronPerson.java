package pt.tecnico.sauron.A20.silo.domain;

import pt.tecnico.sauron.A20.exceptions.SauronException;
import static pt.tecnico.sauron.A20.exceptions.ErrorMessage.INVALID_PERSON_IDENTIFIER;

public class SauronPerson extends SauronObject {

    public SauronPerson(String id) throws SauronException {
        checkId(id);
        setId(id);
    }

    @Override
    protected void checkId(String id) throws SauronException {
        try{
            if (Long.parseLong(id) <= 0)
                throw new SauronException(INVALID_PERSON_IDENTIFIER);

        }
        catch(NumberFormatException e) {
            throw new SauronException(INVALID_PERSON_IDENTIFIER);
        }
    }

    @Override
    public String getType(){
        return "person";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SauronPerson) {
            SauronPerson person = (SauronPerson) obj;
            return getId().equals(person.getId());
        }
        return false;
    }


}
