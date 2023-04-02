/*******************************************************************************
 * Copyright (c) 2001, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.rsadapter;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

// This is provided as legacy function. Do not use.
/**
* This interface enables an application to provide additional parameters when requesting a
* Connection.  These parameters are provided in a ConnectionSpec object.
*/
public interface WSDataSource extends DataSource {
    /**
     * Error code for SQLException that indicates that the connection pool is paused.
     */
    public static final int ERROR_CONNECTION_POOL_IS_PAUSED = 2147117569;

    /**
     *  With the NORMAL_PURGE option, the purged pool will behave as follows after the purge call:
     * <OL>
     * <LI> Existing in-flight transactions will be allowed to continue work </LI>
     * <LI> Shared connection requests will be honored</LI>
     * <LI> Free connections are cleanup and destroyed</LI>
     * <LI> In use connection (i.e. connections in transactions) are cleanup and destroyed when returned to the 
     * connection pool</LI>
     * <LI> </LI>
     * <LI> <CODE> close() </CODE> calls issued on any connections obtained prior to the <code>purgePool</code> call will 
     * be done synchronously (i.e. wait for the JDBC driver to come back before proceeding) </LI>
     * <LI> Requests for new connections (not handles to existing old connections) will be honored.</LI>
     * </OL>
     */
    public static final String NORMAL_PURGE = "normal";
    
    /**
     *  With the IMMEDIATE_PURGE option, the purged pool will behave as follows after the purge call:
     * <OL>
     * <LI> No new transactions will be allowed to start on any connections obtained prior to the <code>purgePoolContents()</code> call.  Instead,
     *      a StaleConnectionException is thrown </LI>
     * <LI> No new handles are allowed to be handed out on any connections obtained prior to the <code>purgePoolContents()</code> call.  Instead,
     *      a StaleConnectionException is thrown</LI>
     * <LI> Existing in-flight transactions will be allowed to continue work, any new activities on the purgedConnection will cause a StaleConnectionException
     * or an XAER_FAIL exception </LI>
     * <LI> <CODE> close() </CODE> calls issued on any connections obtained prior to the <code>purgePoolContents()</code> call will be
     *      done asynchronously (i.e. no wait time) </LI>
     * <LI> Requests for new connections (i.e. not handles to existing old connections) will be honored.</LI>
     * <LI> Number of connections will be decremented immediately.  This may cause the total number of connections in Liberty
     * to be, temporarily, out of sync with the database total number of connections</LI>
     * </OL>
     */
    public static final String IMMEDIATE_PURGE = "immediate";

    /**
     * Requests a connection that matches the information provided in the JDBCConnectionSpec.
     * The application can specify the catalog, cursor holdability, isReadOnly,
     * network timeout, schema, transaction isolation level, and typeMap attributes,
     * allowing for the underlying Connection to be shared based on the above criteria.
     *
     * @param connSpec information used to establish the Connection, such as user name,
     *        password, and type map.  This value should never be null.
     * @return the Connection
     * @throws SQLException if an error occurs while obtaining a Connection.
     */
    Connection getConnection(JDBCConnectionSpec connSpec) throws SQLException;

    /**
     * Indicates whether or not the underlying data source is an XADataSource, capable of
     * two phase commit.
     *
     * @return true if the underlying data source is an XADataSource, capable of two
     *         phase commit, otherwise false.
     */
    boolean isXADataSource();
}
