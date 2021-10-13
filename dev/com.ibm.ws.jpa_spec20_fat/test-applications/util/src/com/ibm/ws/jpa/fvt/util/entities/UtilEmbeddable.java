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
// Module  :  UtilEmbeddable
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

import javax.persistence.Basic;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;

@Embeddable
public class UtilEmbeddable {

    private String embName;
    private String embNotLoaded;

    public String getEmbName() {
        return embName;
    }

    public void setEmbName(String name) {
        this.embName = name;
    }

    @Basic(fetch = FetchType.LAZY)
    public String getEmbNotLoaded() {
        return embNotLoaded;
    }

    public void setEmbNotLoaded(String notLoaded) {
        this.embNotLoaded = notLoaded;
    }
}
