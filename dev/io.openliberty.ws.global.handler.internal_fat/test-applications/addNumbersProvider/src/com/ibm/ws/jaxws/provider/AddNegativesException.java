//
// @(#) 1.1 autoFVT/src/faultbean/wsfvt/server/AddNegativesException.java, WAS.websvcs.fvt, WAS85.FVT, cf011231.01 6/18/10 12:10:41 [8/8/12 06:58:49]
//
// IBM Confidential OCO Source Material
// (C) COPYRIGHT International Business Machines Corp. 2009
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Change History:
// Date       UserId      Defect          Description
// ----------------------------------------------------------------------------
// 06/18/2010 jtnguyen    657385          New File

package com.ibm.ws.jaxws.provider;

import javax.xml.ws.WebFault;

@WebFault(name = "LocalName")
public class AddNegativesException extends Exception {
    /**  */
    private static final long serialVersionUID = 1L;
    private String message = null;

    public AddNegativesException() {

    }

    public AddNegativesException(String message) {
        this.message = message;
    }

    public String getInfo() {
        return message;
    }
}
