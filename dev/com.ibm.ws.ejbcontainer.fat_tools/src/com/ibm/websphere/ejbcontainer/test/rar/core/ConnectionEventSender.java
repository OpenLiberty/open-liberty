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
//  Date       pgmr       reason   Description
//  --------   -------    ------   ---------------------------------
//  02/05/03   jitang	  d157688  create
//  03/10/03   jitang     d159967  Fix some java doc problem
//  ----------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.core;

import javax.resource.ResourceException;

/**
 * @author jitang
 * 
 *         This interface is used to send connection events to event listeners. <p>
 * 
 *         A ConnectionEventSender object can send the following events to its
 *         event listeners:
 *         <UL>
 *         <LI>CONNECTION_ERROR_OCCURRED </LI>
 *         <LI>CONNECTION_CLOSED</LI>
 *         <LI>LOCAL_TRANSACTION_STARTED</LI>
 *         <LI>LOCAL_TRANSACTION_COMMITTED</LI>
 *         <LI>LOCAL_TRANSACTION_ROLLEDBACK</LI>
 *         <LI>INTERACTION_PENDING</LI>
 *         </UL>
 */
public interface ConnectionEventSender {

    /**
     * Send a CONNECTION_ERROR_OCCURRED event to listeners.<p>
     * 
     * @param Object connection handle to which exception has occurred
     * @param Exception specific exception which has occurred
     */
    void sendConnectionErrorOccurredEvent(Object handle, Exception ex) throws ResourceException;

    /**
     * Send a CONNECTION_CLOSED event to listeners.<p>
     * 
     * @param Object connection handle which has been closed.
     */
    void sendConnectionClosedEvent(Object handle) throws ResourceException;

    /**
     * Send a LOCAL_TRANSACTION_STARTED event to listeners.<p>
     * 
     * @param Object connection handle of which local transaction has been started.
     */
    void sendLocalTransactionStartedEvent(Object handle) throws ResourceException;

    /**
     * Send a LOCAL_TRANSACTION_COMMITTED event to listeners.<p>
     * 
     * @param Object connection handle of which local transaction has been committed.
     */
    void sendLocalTransactionCommittedEvent(Object handle) throws ResourceException;

    /**
     * Send a LOCAL_TRANSACTION_ROLLEDBACK event to listeners.<p>
     * 
     * @param Object connection handle of which local transaction has been rollbacked.
     */
    void sendLocalTransactionRolledbackEvent(Object handle) throws ResourceException;

    /**
     * Send a INTERACTION_PENDING event to listeners.<p>
     * 
     * @param Object connection handle of which global transaction has been started.
     */
    void sendInteractionPendingEvent(Object handle) throws ResourceException;
}