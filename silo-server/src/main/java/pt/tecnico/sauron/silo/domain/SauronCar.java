package pt.tecnico.sauron.silo.domain;


import pt.tecnico.sauron.exceptions.ErrorMessage;
import pt.tecnico.sauron.exceptions.SauronException;

public class SauronCar extends SauronObject {

    public SauronCar(String id) throws SauronException {
        checkId(id);
        setId(id);
    }

    public static void checkId(String id) throws SauronException {
        int numFields = 0;
        if (id.length() != 6)
            throw new SauronException(ErrorMessage.INVALID_CAR_ID);
        for (int i = 0; i <3; i++) {
            char firstChar = id.charAt(2 * i);
            char secChar = id.charAt(2 * i + 1);
            if (Character.isDigit(firstChar) && Character.isDigit(secChar))
                numFields++;
            else if (!(Character.isUpperCase(firstChar) && Character.isUpperCase(secChar)))
                throw new SauronException(ErrorMessage.INVALID_CAR_ID);
        }
        if (numFields == 3 || numFields == 0) {
            throw new SauronException(ErrorMessage.INVALID_CAR_ID);
        }
    }

    @Override
    public String getType(){
        return "car";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SauronCar) {
            SauronCar car = (SauronCar) obj;
            return getId().equals(car.getId());
        }
        return false;
    }
}
