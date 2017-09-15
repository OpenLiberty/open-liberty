/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.sib.ra;

import com.ibm.wsspi.sib.core.AbstractConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SITransaction;

/**
 * Interface implemented by message-driven beans wishing to receive
 * asynchronously delivered messages from the core SPI resource adapter.
 */
public interface SibRaMessageListener {

    /**
     * Passes a message to the listener.
     * 
     * @param message
     *            the message passed to the listener
     * @param session
     *            the session that the messages was received on. From this the
     *            MDB may obtain the parent connection and use this to, for
     *            example, determine further information about the destination
     *            from which the message was received or create a producer
     *            session to send a response. If a
     *            <code>MessageDeletionMode</code> of <code>Application</code>
     *            has been specified then the session may be used to delete or
     *            unlock the given message or, alternatively, to persist the
     *            lock beyond the scope of this method for later deletion using
     *            a bifurcated consumer session. In order to protect the
     *            resource adapter's processing, the <code>close</code> method
     *            on the session will throw a
     *            <code>SibRaNotSupportedException</code>. In addition, the
     *            <code>getConnection</code> will return a clone of the
     *            original connection. When the <code>onMessage</code> method
     *            returns, all other methods on the session will throw
     *            <code>SIObjectClosedException</code> and any connection
     *            created will also be closed.
     * @param transaction
     *            the transaction the message was received under if the
     *            message-driven bean is deployed with a transaction attribute
     *            of <code>Required</code> for this method otherwise
     *            <code>null</code>. This transaction may be used with the
     *            parent connection from the given session in order to send and
     *            receive messages under the same transaction.
     */
    void onMessage(SIBusMessage message, AbstractConsumerSession session,
            SITransaction transaction);

}
