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
//  06/19/03   jitang     LIDB2110.31  create - Provide J2C 1.5 resource adapter
//
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.message;

/**
 * <p>A MessageException object indicates that users try to add a message which doesn't have
 * the right format.</p>
 */
public class MessageException extends RuntimeException {
    /**
     * Constructor for MessageException.
     */
    public MessageException() {
        super();
    }

    /**
     * Constructor for MessageException.
     * 
     * @param arg0
     */
    public MessageException(String arg0) {
        super(arg0);
    }

    /**
     * Constructor for MessageException.
     * 
     * @param arg0
     * @param arg1
     */
    public MessageException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    /**
     * Constructor for MessageException.
     * 
     * @param arg0
     */
    public MessageException(Throwable arg0) {
        super(arg0);
    }
}