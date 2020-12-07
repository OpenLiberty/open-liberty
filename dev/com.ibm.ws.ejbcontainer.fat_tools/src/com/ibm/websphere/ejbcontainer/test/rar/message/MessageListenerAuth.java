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
//  Date       pgmr    reason			Description
//  --------   ------  ------     	---------------------------------
//  04/06/04   cjn	   LIDB2110-69  Create class
//
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.message;

import javax.jms.MessageListener;

/**
 * <p>This interface defines one interface which TRA supports. FVT team can add more
 * methods if they want to.</p>
 */
public interface MessageListenerAuth extends MessageListener {
    /**
     * <p>Passes a String message to the listener.
     * 
     * @param a String message
     */
    void onStringMessage(String message);

    /**
     * <p>Passes an Integermessage to the listener.
     * 
     * @param an Integer message
     */
    void onIntegerMessage(Integer message);

    /**
     * <p>onCreateDBEntryNikki from tra.MessageListner interface
     * This method will be used by J2C FVT to create a db entry
     * in cmtest table in jtest1 database.
     * 
     * @param a String message
     */
    void onCreateDBEntryNikki(String message);

    /**
     * <p>onCreateDBEntryZiyad from tra.MessageListner interface
     * This method will be used by J2C FVT to create a db entry
     * in cmtest table in jtest1 database.
     * 
     * @param a String message
     */
    void onCreateDBEntryZiyad(String message);

    /**
     * <p>onWait from tra.MessageListner interface
     * This method will be used by J2C FVT to wait for x seconds specified
     * by the message when message is received. This is used
     * when we want to do prepare call before the submitted Work is completed.
     * 
     * @param a String message, represents the wait time in seconds.
     */
    void onWait(String msg);

    /**
     * <p>onGetTimestamp from tra.MessageListner interface
     * This method will be used by J2C FVT to get a system time when message is
     * received. This is used for validing when each message is
     * received to make sure the message is sent periodically according to the
     * period specified.
     * 
     * Details to be determined later.
     * 
     * @param a String message
     */
    void onGetTimestamp(String msg);

    // d174256
    /**
     * onThrowEJBException from tra.MessageListner interface
     * This method will throw EJB exceptions. This is used for validing if there are
     * any problem in stopping the server after MDB throws this exception.
     */
    public void onThrowEJBException(String msg);
}