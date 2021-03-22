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

/**
 * Extension point for compatibility with data store helpers.
 */
public interface DataStoreHelperMetaData {
    /**
     * Indicates if statements retain the transaction isolation level that was present on
     * the connection at the time the statement was created. This means that if the transaction
     * isolation level of the connection is changed at a later point, it does not impact the
     * statement. This impacts how matching is performed when caching and reusing statements.
     *
     * @return true if statements retain their isolation level once created, otherwise
     *         false, which means that statements use whatever isolation level is currently
     *         present on the connection when they run.
     */
    boolean doesStatementCacheIsoLevel();

    /**
     * Indicates whether the JDBC driver supports <code>java.sql.Connection.getCatalog</code>.
     *
     * @return true if the operation is supported, otherwise false.
     */
    boolean supportsGetCatalog();

    /**
     * Indicates whether the JDBC driver supports <code>java.sql.Connection.getNetworkTimeout</code>.
     *
     * @return true if the operation is supported, otherwise false.
     */
    boolean supportsGetNetworkTimeout();

    /**
     * Indicates whether the JDBC driver supports <code>java.sql.Connection.getSchema</code>.
     *
     * @return true if the operation is supported, otherwise false.
     */
    boolean supportsGetSchema();

    /**
     * Indicates whether the JDBC driver supports <code>java.sql.Connection.getTypeMap</code>.
     *
     * @return true if the operation is supported, otherwise false.
     */
    boolean supportsGetTypeMap();

    /**
     * Indicates whether the JDBC driver supports <code>java.sql.Connection.isReadOnly</code>.
     *
     * @return true if the operation is supported, otherwise false.
     */
    boolean supportsIsReadOnly();

    /**
     * Indicates whether the JDBC driver supports the vendor-specific unit-of-work detection API,
     * <code>com.ibm.db2.jcc.DB2Connection.isInDB2UnitOfWork</code>.
     * Because support can only be determined from a valid connection, do not invoke this method until
     * <code>gatherAndDisplayMetaDataInfo</code> has processed the first connection established.
     *
     * @return if supported, otherwise false.
     */
    boolean supportsUOWDetection();
}