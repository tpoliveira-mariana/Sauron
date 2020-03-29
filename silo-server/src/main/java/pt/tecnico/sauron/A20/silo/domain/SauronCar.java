package pt.tecnico.sauron.A20.silo.domain;



public class SauronCar extends SauronObject {

    public SauronCar(String id) {
        checkId(id);
        setId(id);
    }

    @Override
    protected void checkId(String id) {
        int numFields = 0;
        if (id.length() != 6)
            return;
        for (int i = 0; i <3; i++){
            char firstChar = id.charAt(2 * i);
            char secChar = id.charAt(2 * i + 1);
            if (Character.isDigit(firstChar) && Character.isDigit(secChar))
                numFields++;
            else if (!(Character.isUpperCase(firstChar) && Character.isUpperCase(secChar)))
                // TODO - exceptions
                return;
        }
        if (numFields == 3 || numFields == 0){
            // TODO - exceptions
            return;
        }

        // TODO - check and exceptions
        return;
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
