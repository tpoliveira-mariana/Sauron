package pt.tecnico.sauron.A20.silo.domain;



public class SauronPerson extends SauronObject {

    public SauronPerson(String id) {
        checkId(id);
        setId(id);
    }

    @Override
    protected void checkId(String id) {
        try{
            if (Long.parseLong(id) <= 0)
                //TODO - throw exception
                return;

        }
        catch(NumberFormatException e){
            //TODO - throw exception
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
