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

package com.ibm.adapter.jdbc;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.sql.DataSource;

import com.ibm.adapter.AdapterUtil;
import com.ibm.adapter.spi.ConnectionRequestInfoImpl;
import com.ibm.adapter.spi.ManagedConnectionFactoryImpl;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 *         This datasource will be the datasource object used by the application
 *         developers.
 */
public class JdbcDataSource implements DataSource, Serializable {

    private ManagedConnectionFactoryImpl mcf;
    private ConnectionManager cm;

    private final int defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED;

    private static final TraceComponent tc = Tr.register(JdbcDataSource.class);

    /**
     * List of commonly used CRI instances, which include only the isolation
     * level. In the JDBC layer, isolation level is the only value that will
     * ever be used when the getConnection (with no parameters) is specified.
     * Because CRI objects cannot be changed once created, these same instances
     * may be used for concurrent getConnection requests [d139351.15]
     */
    private static final ConnectionRequestInfo[] defaultCRIs;

    static {
        if (tc.isDebugEnabled())
            Tr.debug(tc,
                     "Creating default, reusable ConnectionRequestInfo instances.");

        defaultCRIs = new ConnectionRequestInfoImpl[9];
        defaultCRIs[0] = new ConnectionRequestInfoImpl(0);
        defaultCRIs[1] = new ConnectionRequestInfoImpl(1);
        defaultCRIs[2] = new ConnectionRequestInfoImpl(2);
        defaultCRIs[4] = new ConnectionRequestInfoImpl(4);
        defaultCRIs[8] = new ConnectionRequestInfoImpl(8);
    }

    // start 313344.1
    public JdbcDataSource() {
        if (tc.isEventEnabled())
            Tr.event(tc, "JdbcDataSource default constructor", this);
    }

    // end 313344.1

    public JdbcDataSource(ManagedConnectionFactoryImpl mcf, ConnectionManager cm) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>", new Object[] { mcf, cm });

        this.mcf = mcf;
        this.cm = cm;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);
    }

    /**
     * @see javax.sql.DataSource#getConnection()
     */
    @Override
    public Connection getConnection() throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getConnection");

        int isolationLevelForCRI = defaultIsolationLevel;

        return getConnection(defaultCRIs[isolationLevelForCRI]).initialize(cm);
    }

    /**
     * @see javax.sql.DataSource#getConnection(String, String)
     */
    @Override
    public Connection getConnection(String user, String pwd) throws SQLException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getConnection", new Object[] { user,
                                                         pwd == null ? pwd : "******" });

        ConnectionRequestInfo connInfo = new ConnectionRequestInfoImpl(user, pwd, defaultIsolationLevel);

        return getConnection(connInfo).initialize(cm);
    }

    /**
     * This is the common getConnection implementation used by the other
     * getConnection methods. This method handles the connection request to the
     * CM and related exceptions, including the ConnectionWaitTimeoutException.
     * Exceptions thrown by the CM are converted to SQLExceptions.
     *
     * @param connInfo
     *            useful information for requesting a Connection.
     *
     * @return the Connection
     *
     * @throws SQLException
     *             if an error occurs while obtaining a Connection.
     */
    private JdbcConnection getConnection(ConnectionRequestInfo connInfo) throws SQLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getConnection",
                     new Object[] { this, AdapterUtil.toString(connInfo) });

        JdbcConnection connWrapper;

        try {
            connWrapper = (JdbcConnection) cm.allocateConnection(mcf, connInfo);
        } catch (ResourceException resX) {

            if (tc.isEntryEnabled())
                Tr.exit(tc, "getConnection", "Exception");
            throw AdapterUtil.toSQLException(resX);
        } catch (Exception timeoutX) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "Received ConnectionWaitTimeoutException from ConnectionManager. "
                             + "Converting to SQLException subclass. Original exception follows.",
                         timeoutX);

            SQLException sqlX = new SQLException(AdapterUtil.toErrorMessage(timeoutX));

            if (tc.isEntryEnabled())
                Tr.exit(tc, "getConnection", sqlX);
            throw sqlX;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getConnection", connWrapper);
        return connWrapper;
    }

    /**
     * @see javax.sql.DataSource#getLogWriter()
     */
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        try {
            return mcf.getLogWriter();
        } catch (ResourceException ex) {
            throw AdapterUtil.toSQLException(ex);
        }
    }

    /**
     * @see javax.sql.DataSource#getLoginTimeout()
     */
    @Override
    public int getLoginTimeout() throws SQLException {
        try {
            return mcf.getLoginTimeout();
        } catch (ResourceException ex) {
            throw AdapterUtil.toSQLException(ex);
        }
    }

    /**
     * @see javax.sql.DataSource#setLogWriter(PrintWriter)
     */
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        try {
            mcf.setLogWriter(out);
        } catch (ResourceException ex) {
            throw AdapterUtil.toSQLException(ex);
        }
    }

    /**
     * @see javax.sql.DataSource#setLoginTimeout(int)
     */
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        try {
            mcf.setLoginTimeout(seconds);
        } catch (ResourceException ex) {
            throw AdapterUtil.toSQLException(ex);
        }
    }

    public boolean is2Phase() {
        return mcf.is2Phase();
    }

    /**
     * Returns the mcf.
     *
     * @return ManagedConnectionFactoryImpl
     */
    public ManagedConnectionFactoryImpl getMcf() {
        return mcf;
    }

    /**
     * @return boolean Whether the MCF supports Lazy Associatable optimization.
     */
    public boolean isLazyAssociatable() {
        // return mcf.isLazyAssociatable();
        return mcf.getLazyAssociatable().equals(Boolean.TRUE);
    }

    /**
     * @return boolean Whether the MCF supports Lazy Enlistable optimization.
     */
    public boolean isLazyEnlistable() {
        // return mcf.isLazyEnlistable();
        return mcf.getLazyEnlistable().equals(Boolean.TRUE);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    // @Override //Java7
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new UnsupportedOperationException();
    }
}
