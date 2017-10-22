package cortez.archie.dev.staffapp.models;

import android.os.Parcelable;

import java.io.Serializable;

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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CheckIn) {
            CheckIn chk = (CheckIn) obj;
            return getId() == chk.getId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new Integer(getId()).hashCode();
    }
}
