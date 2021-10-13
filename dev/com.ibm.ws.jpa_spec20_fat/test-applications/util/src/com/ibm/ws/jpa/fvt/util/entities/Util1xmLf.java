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
// Module  :  Util1xmLf
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

import java.util.Collection;
import java.util.HashSet;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Version;

@Entity
public class Util1xmLf {

    private int id;

    private int version;

    private String firstName;
    
    public Collection<Util1xmRt> uniRight = new HashSet<Util1xmRt>();

    public Collection<Util1xmRt> uniRightEgr = new HashSet<Util1xmRt>();

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
    
    @OneToMany
    public Collection<Util1xmRt> getUniRight() {
        return uniRight;
    }

    public void setUniRight(Collection<Util1xmRt> uniRight) {
        this.uniRight = uniRight;
    }

    public void addUniRight(Util1xmRt uniRight) {
        this.uniRight.add(uniRight);
    }
    
    @OneToMany(fetch=FetchType.EAGER)
    public Collection<Util1xmRt> getUniRightEgr() {
        return uniRightEgr;
    }

    public void setUniRightEgr(Collection<Util1xmRt> uniRightEgr) {
        this.uniRightEgr = uniRightEgr;
    }

    public void addUniRightEgr(Util1xmRt uniRightEgr) {
        this.uniRightEgr.add(uniRightEgr);
    }
    
    public String toString() {
        return "Util1xmLf[id=" + id + ",ver=" + version + "]";
    }
}
