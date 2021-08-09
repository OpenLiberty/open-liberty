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
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

/*
 * Interface name   : ConnectionManager
 *
 * Scope            : EJB server, WEB server
 *
 * Object model     : 1 per each set of res-ref attributes for a deployed resource adapter
 *
 * This ConnectionManager interface introduces the following methods, which are only to be
 * used in support of InactiveConnectionSupport (Smart Handles):
 *   associateConnection()
 *   inactiveConnectionClosed()
 *
 */

/**
 * This ConnectionManager interface provides support for inactive connection handles ("Smart Handles").
 * SmartHandles reduce the overhead incurred when the Application Server manages the Connection Handles,
 * by allowing the ResourceAdapter to determine when a ConnectionHandle should be associated with a
 * ManagedConnection. The ResourceAdapter will have to manage the state of the Connection (active or inactive).
 * 
 * @see <A HREF="http://www7b.boulder.ibm.com/wsdd/techjournal/0302_kelle/kelle.html#smarthandles" > JCAPaper </A>
 * 
 * @deprecated As of WAS 6.0, the functionality of this interface is replaced by J2EE Connector Architecture 1.5.
 *             Please reference {@link javax.resource.spi.LazyAssociatableConnectionManager javax.resource.spi.LazyAssociatableConnectionManager}.
 * 
 * @ibm-spi
 */
@Deprecated
public interface ConnectionManager extends javax.resource.spi.ConnectionManager {

    /**
     * AssociateConnection should be called by the ResourceAdapter before any work is done on an inactive
     * Connection.
     * 
     * @param mcf The ManagedConnectionFactory that may be used to create a new ManagedConnection (usually one
     *            will be found in the pool).
     * @param subject The Subject for this connection.
     * @param cri The connection request specific info (this may include UserName & Password).
     * @param connection The Connection handle that should be associated with a valid ManagedConnection.
     * @throws ResourceException
     */
    public void associateConnection(ManagedConnectionFactory mcf, Subject subject, ConnectionRequestInfo cri, Object connection) throws ResourceException;

    /**
     * Method inactiveConnectionClosed is called by the ResourceAdapter when an inactiveConnection is closed.
     * This is necessary because no ManagedConnection instance is associated with an inactive handle, so you cannot
     * call the ConnectionClosed event on the associated ConnectionEventListener.
     * 
     * @param connection The Connection handle that was closed.
     */
    public void inactiveConnectionClosed(Object connection);

}
