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
//  Date       pgmr       reason       Description
//  --------   -------    ------       ---------------------------------
//  07/29/03   jitang     LIDB2110.31  create - Provide J2C 1.5 resource adapter
//
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.message;

/**
 * <p>An exception indicating that the sendMessageWait method has been timed out.</p>
 */
public class MessageWaitTimeoutException extends RuntimeException {
    /**
     * Constructor for MessageWaitTimeoutException.
     */
    public MessageWaitTimeoutException() {
        super();
    }

    /**
     * Constructor for MessageWaitTimeoutException.
     * 
     * @param arg0
     */
    public MessageWaitTimeoutException(String arg0) {
        super(arg0);
    }

    /**
     * Constructor for MessageWaitTimeoutException.
     * 
     * @param arg0
     * @param arg1
     */
    public MessageWaitTimeoutException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    /**
     * Constructor for MessageWaitTimeoutException.
     * 
     * @param arg0
     */
    public MessageWaitTimeoutException(Throwable arg0) {
        super(arg0);
    }
}