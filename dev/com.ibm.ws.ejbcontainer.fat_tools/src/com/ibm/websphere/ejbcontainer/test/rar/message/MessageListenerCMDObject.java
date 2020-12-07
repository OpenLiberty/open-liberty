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
//  Date       pgmr    reason       Description
//  --------   ------  ------       ---------------------------------
//  05/27/03   swai    LIDB2110.67  Create for complex msg destination testing.
//                                  This message listner associate with
//                                  ActivationSpecCompMsgDestObjectImpl
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.message;

import javax.jms.MessageListener;

/**
 * This class is for the listener for ActivationSpecCompMsgDestObjectImpl
 */
public interface MessageListenerCMDObject extends MessageListener {
    /**
     * <p>Passes a String message to the listener.
     * 
     * @param a String message
     */
    void onStringMessage(String message);
}