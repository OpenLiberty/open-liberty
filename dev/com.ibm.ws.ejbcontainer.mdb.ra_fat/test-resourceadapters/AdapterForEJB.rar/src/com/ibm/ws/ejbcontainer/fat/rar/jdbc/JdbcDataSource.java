/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.jdbc;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ResourceAllocationException;
import javax.sql.DataSource;

import com.ibm.ejs.j2c.CMConfigData;
import com.ibm.ws.ejbcontainer.fat.rar.core.AdapterUtil;
import com.ibm.ws.ejbcontainer.fat.rar.spi.ConnectionRequestInfoImpl;
import com.ibm.ws.ejbcontainer.fat.rar.spi.ManagedConnectionFactoryImpl;

/**
 * This datasource will be the datasource object used by the application
 * developers.
 */
public class JdbcDataSource implements DataSource, Serializable {
    private final static String CLASSNAME = JdbcDataSource.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private ManagedConnectionFactoryImpl mcf;
    private ConnectionManager cm;
    private CMConfigData cmConfigData;

    private final int defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED;

    /**
     * List of commonly used CRI instances, which include only the isolation level. In the
     * JDBC layer, isolation level is the only value that will ever be used when the
     * getConnection (with no parameters) is specified. Because CRI objects cannot be changed
     * once created, these same instances may be used for concurrent getConnection requests
     * [d139351.15]
     */
    private static final ConnectionRequestInfo[] defaultCRIs;

    static {
        svLogger.info("Creating default, reusable ConnectionRequestInfo instances.");

        defaultCRIs = new ConnectionRequestInfoImpl[9];
        defaultCRIs[0] = new ConnectionRequestInfoImpl(0);
        defaultCRIs[1] = new ConnectionRequestInfoImpl(1);
        defaultCRIs[2] = new ConnectionRequestInfoImpl(2);
        defaultCRIs[4] = new ConnectionRequestInfoImpl(4);
        defaultCRIs[8] = new ConnectionRequestInfoImpl(8);
    }

    // start 313344.1
    public JdbcDataSource() {
        svLogger.info("JdbcDataSource default constructor");
    }

    // end 313344.1

    public JdbcDataSource(ManagedConnectionFactoryImpl mcf, ConnectionManager cm) {
        svLogger.entering(CLASSNAME, "<init>", new Object[] { mcf, cm });

        this.mcf = mcf;
        this.cm = cm;

        /*
         * if (cm instanceof com.ibm.ws.j2c.ConnectionManager) {
         * svLogger.info("cm is com.ibm.ws.j2c.ConnectionManager"); // 313344.1
         * cmConfigData = ((com.ibm.ws.j2c.ConnectionManager) cm).getCMConfigData();
         * svLogger.info("isolevel: " + cmConfigData.getIsolationLevel()); // 313344.1
         * if (cmConfigData.getIsolationLevel() != Connection.TRANSACTION_NONE)
         * defaultIsolationLevel = cmConfigData.getIsolationLevel();
         * }
         */

        svLogger.exiting(CLASSNAME, "<init>", this);
    }

    /**
     * @see javax.sql.DataSource#getConnection()
     */
    @Override
    public Connection getConnection() throws SQLException {
        svLogger.info("getConnection");
        int isolationLevelForCRI = defaultIsolationLevel;
        return getConnection(defaultCRIs[isolationLevelForCRI]).initialize(cm);
    }

    /**
     * @see javax.sql.DataSource#getConnection(String, String)
     */
    @Override
    public Connection getConnection(String user, String pwd) throws SQLException {
        svLogger.entering(CLASSNAME, "getConnection", new Object[] { user, pwd == null ? pwd : "******" });
        ConnectionRequestInfo connInfo = new ConnectionRequestInfoImpl(user, pwd, defaultIsolationLevel);
        return getConnection(connInfo).initialize(cm);
    }

    /**
     * This is the common getConnection implementation used by the other getConnection
     * methods. This method handles the connection request to the CM and related exceptions,
     * including the ConnectionWaitTimeoutException. Exceptions thrown by the CM are converted
     * to SQLExceptions.
     *
     * @param connInfo useful information for requesting a Connection.
     *
     * @return the Connection
     *
     * @throws SQLException if an error occurs while obtaining a Connection.
     */
    private JdbcConnection getConnection(ConnectionRequestInfo connInfo) throws SQLException {
        svLogger.entering(CLASSNAME, "getConnection", new Object[] { this, AdapterUtil.toString(connInfo) });
        JdbcConnection connWrapper;

        try {
            connWrapper = (JdbcConnection) cm.allocateConnection(mcf, connInfo);
        } catch (ResourceAllocationException timeoutX) {
            svLogger.info("Received ConnectionWaitTimeoutException from ConnectionManager. Converting to SQLException subclass. Original exception follows: " + timeoutX);
            SQLException sqlX = new SQLException(AdapterUtil.toErrorMessage(timeoutX));
            svLogger.exiting(CLASSNAME, "getConnection", sqlX);
            throw sqlX;
        } catch (ResourceException resX) {
            svLogger.exiting(CLASSNAME, "getConnection", "ResourceException");
            throw AdapterUtil.toSQLException(resX);
        }

        svLogger.exiting(CLASSNAME, "getConnection", connWrapper);
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
        return mcf.getLazyAssociatable().equals(Boolean.TRUE);
    }

    /**
     * @return boolean Whether the MCF supports Lazy Enlistable optimization.
     */
    public boolean isLazyEnlistable() {
        return mcf.getLazyEnlistable().equals(Boolean.TRUE);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    // @Override
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        // Stub for JDBC 4.1
        return null;
    }
}