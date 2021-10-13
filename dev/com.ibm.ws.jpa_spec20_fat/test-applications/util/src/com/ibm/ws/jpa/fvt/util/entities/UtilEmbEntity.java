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
// Module  :  UtilEntity
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

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
public class UtilEmbEntity {

    private int id;

    private int version;

    private String name;

    private UtilEmbeddable emb;

    private UtilEmbeddable emb1;

    private UtilEmbeddable2 initNullEmb;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Embedded
    public UtilEmbeddable getEmb() {
        return emb;
    }

    public void setEmb(UtilEmbeddable emb) {
        this.emb = emb;
    }

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name="embName",      column=@Column(name="emb1Name")),
        @AttributeOverride(name="embNotLoaded", column=@Column(name="emb1NotLoaded"))
        })
    public UtilEmbeddable getEmb1() {
        return emb1;
    }

    public void setEmb1(UtilEmbeddable emb1) {
        this.emb1 = emb1;
    }

    @Embedded
    public UtilEmbeddable2 getInitNullEmb() {
        return initNullEmb;
    }

    public void setInitNullEmb(UtilEmbeddable2 initNullEmb) {
        this.initNullEmb = initNullEmb;
    }

    public String toString() {
        return "UtilEmbEntity[id=" + id + ",ver=" + version + "]";
    }
}
