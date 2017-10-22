package cortez.archie.dev.staffapp.models;

/**
 * Created by Administrator on 10/17/2017.
 */

public class CheckIn {

    private int Id;
    private String scope;
    private String status;

    public int getUserId() {
        return Id;
    }

    public void setUserId(int id) {
        Id = id;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (obj instanceof CheckIn) {
            return getUserId() == ((CheckIn) obj).getUserId();
        } else {
            return false;
        }
    }
}
