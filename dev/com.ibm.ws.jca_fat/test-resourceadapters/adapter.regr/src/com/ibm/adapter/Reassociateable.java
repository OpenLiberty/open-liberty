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

import java.sql.Connection;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;

/**
 * <p>Enables a Connection handle to support handle reassociation.</p>
 *
 * <p>The process of reassociation involves the following: The handle may be dissociated with
 * its underlying database connection and placed in an inactive state. It may later be
 * reassociated, which returns the handle to an active state. If implicit reactivation
 * is enabled, any operation on an inactive handle will trigger a dynamic request for
 * reassociation with the underlying connection. Otherwise, any operations to the database
 * on an inactive handle are considered an error. Operations which do not involve the database
 * may be allowed on inactive handles even if they do not support implicit reactivation.</p>
 *
 * <p>Inactive handles may be closed, as usual, after which they are no longer useable.</p>
 *
 * <p>An <code>isClosed</code> request on an inactive handle should return false, as the
 * handle may still be used (once it is reassociated).</p>
 *
 * <p>Because this interface is meant to be implemented by the handle, which is directly
 * accessible by the application, a special <code>key</code> parameter is added to certain
 * methods which it might be dangerous to allow the application to invoke directly, such as
 * <code>reassociate</code>. The handle implementation may choose to restrict access to these
 * methods using the <code>key</code> since the key is not available to application code. The
 * handle implementation is not required to restrict access in this manner, in which case
 * the key parameter may simply be ignored.</p>
 */
public interface Reassociateable extends HandleStates {
    /**
     * <p>Transitions the Connection handle to the INACTIVE state. Retrieves and stores all
     * information needed for reassociation. This method may close all child objects of the
     * connection instead of saving their states. A reserved handle may be dissociated, in
     * which case the handle must lose its reserved status.</p>
     *
     * @throws ResourceException if the Connection handle is closed or a fatal error occurs on
     *             dissociation.
     */
    void dissociate() throws ResourceException;

    /**
     * Retrieve the ManagedConnection this Connection handle is currently associated with.
     *
     * @param key a special key that must be provided to invoke this method.
     *
     * @return the ManagedConnection, or null if not associated.
     *
     * @throws ResourceException if an incorrect key is supplied.
     */
    ManagedConnection getManagedConnection() throws ResourceException;

    /**
     * @return the current handle state, as defined in WSJdbcObject.
     */
    int getState();

    /**
     * @return true if the handle is reserved for reassociation with its current
     *         ManagedConnection, otherwise false.
     */
    boolean isReserved();

    /**
     * <p>Reassociates this Connection handle with a new ManagedConnection and underlying
     * connection and reestablishes all saved states. It is an error to reassociate a handle
     * which is not in the inactive state.</p>
     *
     * @param mc the new ManagedConnection to associate this handle with.
     * @param connImplObject the new underlying JDBC Connection object to associate this handle
     *            with.
     * @param key a special key that must be provided to invoke this method.
     *
     * @throws ResourceException if an incorrect key is supplied, if the handle is not ready
     *             for reassociation, or if an error occurs during the reassociation.
     */
    void reassociate(ManagedConnection mc, Connection connImplObject) throws ResourceException;

    /**
     * <p>Reserve this Connection handle for reassociation only with its current
     * ManagedConnection. This optimization allows child objects of the handle also
     * associated with the ManagedConnection (or associated with underlying objects of the
     * ManagedConnection) to be left open across reassociations. This method should only be
     * used when the guarantee can be made that the handle will always be reassociated back to
     * its original ManagedConnection.</p>
     *
     * <p>Reserved handles should always be placed in the INACTIVE state. The handle loses
     * its reserved status when either of the following occur,</p>
     * <ul>
     * <li>The handle is explicitly dissociated.
     * <li>A reassociation request is made for the handle to reassociate with its current
     * ManagedConnection.
     * </ul>
     * = *
     *
     * @throws ResourceException if an incorrect key is supplied or if the handle may not be
     *             reserved from its current state.
     */
    void reserve() throws ResourceException;

    /**
     * <p>Indicates whether the handle supports implicit reactivation. Implicit reactivation
     * means that an inactive connection handle will implicitly request reassociation when
     * used. For example, if the handle state is inactive and a <code>createStatement</code>
     * operation is requested, the handle will implicitly reassociate with a new underlying
     * connection and continue the operation.</p>
     *
     * @returns true if the handle supports implicit reactivation, otherwise false.
     */
    boolean supportsImplicitReactivation();
}
