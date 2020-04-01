package pt.tecnico.sauron.A20.silo.domain;

import pt.tecnico.sauron.A20.exceptions.SauronException;
import static pt.tecnico.sauron.A20.exceptions.ErrorMessage.INVALID_PERSON_ID;

public class SauronPerson extends SauronObject {

    public SauronPerson(String id) throws SauronException {
        checkId(id);
        setId(id);
    }

    public static void checkId(String id) throws SauronException {
        try {
            if (Long.parseLong(id) <= 0 || (id.length()>0 && id.charAt(0)=='0'))
                throw new SauronException(INVALID_PERSON_ID);

        } catch(NumberFormatException e) {
            throw new SauronException(INVALID_PERSON_ID);
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
