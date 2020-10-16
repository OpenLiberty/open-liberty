/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter;

import javax.resource.ResourceException;

/**
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
