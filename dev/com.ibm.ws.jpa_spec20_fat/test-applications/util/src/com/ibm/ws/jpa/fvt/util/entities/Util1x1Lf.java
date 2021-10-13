// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// %I% %W% %G% %U%
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88 (C) COPYRIGHT International Business Machines Corp. 2009
//
// The source code for this program is not published or otherwise divested
//  of its trade secrets, irrespective of what has been deposited with the
//  U.S. Copyright Office.
//
// Module  :  Util1x1Lf
//
// Source File Description:
//
// Change Activity:
//
// Reason          Version   Date     Userid    Change Description
// --------------- --------- -------- --------- -----------------------------------------
// S9091.13261     WASX      11132009 leealber  Initial release
// --------------- --------- -------- --------- -----------------------------------------

package com.ibm.ws.jpa.fvt.util.entities;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Version;

@Entity
public class Util1x1Lf {

    private int id;

    private int version;

    private String firstName;

    public Util1x1Rt uniRight;

    public Util1x1Rt uniRightLzy;

    @Id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Version
    public int getVersion() {
        return version;
    }

    // Setter method needed by EclipseLink
    public void setVersion(int version) {
        this.version = version;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @OneToOne
    public Util1x1Rt getUniRight() {
        return uniRight;
    }

    public void setUniRight(Util1x1Rt uniRight) {
        this.uniRight = uniRight;
    }

    @OneToOne(fetch = FetchType.LAZY)
    public Util1x1Rt getUniRightLzy() {
        return uniRightLzy;
    }

    public void setUniRightLzy(Util1x1Rt uniRightLzy) {
        this.uniRightLzy = uniRightLzy;
    }

    public String toString() {
        return "Util1x1Lf[id=" + id + ",ver=" + version + "]";
    }
}
