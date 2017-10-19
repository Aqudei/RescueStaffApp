package cortez.archie.dev.staffapp.models;

/**
 * Created by _develop on 20/10/2017.
 */

public class CheckIn {
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public CheckIn() {
        setId(-1);
    }

    int id;
    String scope;
    String status;
}
