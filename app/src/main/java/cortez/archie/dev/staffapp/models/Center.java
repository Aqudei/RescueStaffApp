package cortez.archie.dev.staffapp.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 10/16/2017.
 */

public class Center {

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private  int id;
    public Center() {
        members = new ArrayList<>();
    }

    public String getCenterName() {
        return CenterName;
    }

    public void setCenterName(String centerName) {
        CenterName = centerName;
    }

    public String getAddress() {
        return Address;
    }

    public void setAddress(String address) {
        Address = address;
    }

    private String CenterName;
    private String Address;

    public List<MemberInfo> getMembers() {
        return members;
    }

    public void setMembers(List<MemberInfo> members) {
        this.members = members;
    }

    private List<MemberInfo> members;

    public String getInCharge() {
        return InCharge;
    }

    public void setInCharge(String inCharge) {
        InCharge = inCharge;
    }

    public String getInChargeCellphone() {
        return InChargeCellphone;
    }

    public void setInChargeCellphone(String inChargeCellphone) {
        InChargeCellphone = inChargeCellphone;
    }

    private String InCharge;
    private String InChargeCellphone;
}
