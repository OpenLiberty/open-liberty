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
     * Configures a connection before first use. This method is invoked only
     * when a new connection to the database is created. It is not invoked when connections
     * are reused from the connection pool.
     *
     * @param conn the connection to set up.
     * @exception SQLException if connection setup cannot be completed successfully.
     */
    void doConnectionSetup(Connection conn) throws SQLException;

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