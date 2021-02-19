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

}