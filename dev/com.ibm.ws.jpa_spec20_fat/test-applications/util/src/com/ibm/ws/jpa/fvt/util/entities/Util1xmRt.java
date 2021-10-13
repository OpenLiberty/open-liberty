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
// Module  :  Util1xmRt
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
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
public class Util1xmRt {

    private int id;

    private int version;

    private String lastName;

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

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String toString() {
        return "Util1xmRt[id=" + id + ",ver=" + version + "]";
    }
}
