// IBM Confidential
//
// OCO Source Materials
//
// Copyright IBM Corp. 2013
//
// The source code for this program is not published or otherwise divested 
// of its trade secrets, irrespective of what has been deposited with the 
// U.S. Copyright Office.
//
// Change Log:
//  Date        pgmr        reason      Description
//  --------    -------     ------      ---------------------------------
//  05/18/04    swai        LIDB2110-67 create AdminObject for M8_ComplexMessage
//                                      Message Processing test cases.
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.activationSpec;

import java.io.Serializable;

import javax.jms.Destination;

/**
 * <p>This class implements the Destination interface. This Destination implementation
 * class has a few attributes, which are verifyString, .</p>
 */
public class FVTCompMsgDestAOImpl implements Destination, Serializable {

    /** configured property - verifyString */
    private String verifyString;

    /**
     * Returns the verifyString.
     * 
     * @return String
     */
    public String getVerifyString() {
        return verifyString;
    }

    /**
     * Sets the name.
     * 
     * @param name The name to set
     */
    public void setVerifyString(String verifyString) {
        this.verifyString = verifyString;
    }

    public String introspectSelf() {
        return "ComplexMsgDest - verifyString: " + verifyString;
    }
}