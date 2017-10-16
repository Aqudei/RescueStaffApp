package cortez.archie.dev.staffapp.models;

/**
 * Created by Administrator on 10/16/2017.
 */

public class MemberInfo {

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private int id;
    public String getFirstName() {
        return FirstName;
    }

    public void setFirstName(String firstName) {
        FirstName = firstName;
    }

    public String getMiddleName() {
        return MiddleName;
    }

    public void setMiddleName(String middleName) {
        MiddleName = middleName;
    }

    public String getLastName() {
        return LastName;
    }

    public void setLastName(String lasrName) {
        LastName = lasrName;
    }

    private String FirstName;
    private String MiddleName;
    private String LastName;

    @Override
    public String toString() {
        return getFullName();
    }

    public String getFullName() {
        return String.format("%s, %s %s", getLastName(), getFirstName(), getMiddleName());
    }
}
