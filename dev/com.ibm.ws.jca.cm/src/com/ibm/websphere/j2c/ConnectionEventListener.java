/*******************************************************************************
 * Copyright (c) 1997, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.j2c;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;

/*
 * Interface name   : ConnectionEventListener
 *
 * Scope            : EJB server, WEB server
 *
 * Object model     : 1 per ManagedConnection
 *
 * This ConnectionEventListener interface introduces the interactionPending() method,
 * which is to be used by the relational resource adapter (at least for the
 * initial implementation).  This new signal should be used in support of Deferred
 * Enlistment.  If a Resource Adapter supports Deferred Enlistment, this event
 * should be signaled whenever a ManagedConnection is about to be "dirtied" and
 * is not currently enlisted with a transaction.
 *
 * Also, a new constant is being defined for the id of this new event.  To access
 * this new id, the code will need to use ConnectionEventListener.INTERACTION_PENDING,
 * which should be okay since they will need this interface anyway.
 *
 */

/**
 * ConnectionEventListener extends the javax.resource.spi.ConnectionEventListener to add support for
 * deferred enlistment. Deferred enlistment is enabled by adding a new event to the
 * ConnectionEventListener (interactionPending) which is used to indicate that a connection
 * should be enlisted in a transaction.
 * <p>
 * This interface also extends the <code>InteractionMetrics</code> interface. The reason
 * this extends the InteractionMetrics interface is so that any ResouceAdapter can report
 * usage statics on a per connection basis and participate in WebSphere's Request Metrics tracking
 * and in advanced diagnostic information support.
 * 
 * @see <A HREF="http://www7b.boulder.ibm.com/wsdd/techjournal/0302_kelle/kelle.html#smarthandles" > JCAPaper </A>
 * 
 * @deprecated As of WAS 6.0, the functionality of this interface is replaced by J2EE Connector Architecture 1.5.
 *             Please reference {@link javax.resource.spi.LazyEnlistableConnectionManager javax.resource.spi.LazyEnlistableConnectionManager}.
 * 
 * @ibm-spi
 */
@Deprecated
public interface ConnectionEventListener extends javax.resource.spi.ConnectionEventListener, InteractionMetrics {

    /**
     * Method interactionPending is used to signal that a ManagedConnection needs to be enlisted
     * in a transaction - this should happen just prior to when work is performed on the back end.
     * 
     * @param connectionevent This should be an interactionPending event with the identifier defined
     *            in this class. The ManagedConnection to be enlisted should be
     *            included in the event.
     * 
     * @throws ResourceException if an error occurs when enlisting the ManagedConnection.
     */
    void interactionPending(ConnectionEvent connectionevent) throws ResourceException;

    /**
     * Identifier for the interactionPending event.
     */
    int INTERACTION_PENDING = 900;

}
