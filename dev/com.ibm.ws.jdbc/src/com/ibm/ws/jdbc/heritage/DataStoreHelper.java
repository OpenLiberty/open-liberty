/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.heritage;

import java.sql.Connection;
import java.sql.SQLException;

import javax.resource.ResourceException;

/**
 * Extension point for compatibility with data store helpers.
 */
public interface DataStoreHelper {
    /**
     * Invoked after the last active connection handle is closed.
     * This provides an opportunity to undo connection setup that was previously performed by
     * <code>doConnectionSetupPerGetConnection</code>.
     *
     * @param conn the connection to clean up.
     * @param isCMP always false.
     * @param unused always null.
     * @return boolean false indicates no connection cleanup is performed by this method, true otherwise. Default is false as its a no-op.
     * @throws SQLException if it fails.
     */
    boolean doConnectionCleanupPerCloseConnection(Connection conn, boolean isCMP, Object unused) throws SQLException;

    /**
     * Configures a connection before first use. This method is invoked only
     * when a new connection to the database is created. It is not invoked when connections
     * are reused from the connection pool.
     *
     * @param conn the connection to set up.
     * @exception SQLException if connection setup cannot be completed successfully.
     */
    void doConnectionSetup(Connection conn) throws SQLException;

    /**
     * Invoked per getConnection request when the connection handle count is 1,
     * meaning that the second, third, and so forth sharable connection handles are skipped over.
     *
     * @param conn connection to set up.
     * @param isCMP always false.
     * @param props java.util.Map with String key of "SUBJECT" and its value being the
     *        <code>javax.security.auth.Subject</code> if a Subject is available.
     * @return true if any connection setup is performed by this method, otherwise false.
     * @throws SQLException if it fails.
     */
    boolean doConnectionSetupPerGetConnection(Connection conn, boolean isCMP, Object props) throws SQLException;

    /**
     * Returns the default to use for transaction isolation level when not specified another way.
     *
     * @param unused always null. This is only here for compatibility.
     * @return transaction isolation level constant from java.sql.Connection
     * @throws ResourceException never. This is only here for compatibility.
     */
    int getIsolationLevel(AccessIntent unused) throws ResourceException;

    /**
     * Returns metadata for the data store helper.
     *
     * @return metadata.
     */
    DataStoreHelperMetaData getMetaData();
}