/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.plugins.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.exception.OAuthDataException;
import com.ibm.ws.security.oauth20.plugins.db.DetectDatabaseType.DBType;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * This class was imported from tWAS to make only those changes necessary to
 * run OAuth on Liberty. The mission was not to refactor, restructure, or
 * generally cleanup the code.
 */
public abstract class OAuthJDBCImpl {
    private static final TraceComponent tc = Tr.register(OAuthJDBCImpl.class, "OAuth20Provider", null);

    public final static String CONFIG_JDBC_PROVIDER = "oauthjdbc.JDBCProvider";
    public final static String CONFIG_CLIENT_TABLE = "oauthjdbc.client.table";
    public final static String CONFIG_TOKEN_TABLE = "oauthjdbc.token.table";
    public final static String CONFIG_PROVIDER_NAME = "oauth20.provider.id";

    final static String CLASS = OAuthJDBCImpl.class.getName();
    Logger _log = Logger.getLogger(CLASS);

    private DataSource dataSource;
    private String providerName;
    private Object[] credentials;

    private DBType databaseType;

    public OAuthJDBCImpl() {
    }

    public OAuthJDBCImpl(DataSource dataSource, @Sensitive Object[] credentials) {
        this.dataSource = dataSource;
        this.credentials = credentials != null ? credentials.clone() : null;
    }

    protected void init(OAuthComponentConfiguration config) {
        String methodName = "init";
        _log.entering(CLASS, methodName);
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        try {
            providerName = config.getConfigPropertyValue(CONFIG_PROVIDER_NAME);
            if (providerName != null) {
                credentials = ConfigUtils.getProviderJdbcCredentialsMap().get(providerName);
                if (finestLoggable) {
                    _log.logp(Level.FINEST, CLASS, methodName,
                            "setting credentials for provider " + providerName, credentials);
                }
            }
            if (finestLoggable) {
                _log.logp(Level.FINEST, CLASS, methodName,
                        "Using JDBC provider: " + dataSource);
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    /**
     * Get a connection object to the database using a data source
     *
     * @throws Exception
     */
    @FFDCIgnore(java.sql.SQLException.class)
    protected Connection getDBConnection() throws OAuthDataException {
        String methodName = "getDBConnection";
        _log.entering(CLASS, methodName);
        Connection conn = null;

        try {
            if (_log.isLoggable(Level.FINEST)) {
                _log.logp(Level.FINEST, CLASS, methodName, "Credentials: ", credentials);
                _log.logp(Level.FINEST, CLASS, methodName, "this: ", this);
            }
            String userid = null;
            if (credentials != null && credentials[0] != null) {
                userid = (String) credentials[0];
            }
            String password = null;
            if (credentials != null && credentials[1] != null) {
                password = new String(((SerializableProtectedString) credentials[1]).getChars());
            }
            if (userid != null || password != null) {
                try {
                    conn = dataSource.getConnection(userid, password);
                } catch (SQLException e2) { // 247533 back off 200msec and retry
                    getDBClogAndSleep(200);
                    conn = dataSource.getConnection(userid, password);
                }
            } else {
                try {
                    conn = dataSource.getConnection();
                } catch (SQLException e2) {
                    getDBClogAndSleep(200);
                    conn = dataSource.getConnection();
                }
            }
        } catch (SQLException e) {
            throw new OAuthDataException(e);
        } finally {
            _log.exiting(CLASS, methodName);
        }

        return conn;
    }

    @Trivial
    private void getDBClogAndSleep(int msec) {
        String methodName = "getDBConnection";
        if (_log.isLoggable(Level.FINEST)) {
            _log.logp(Level.FINEST, CLASS, methodName,
                    "caught exception getting db connection, will retry in " + msec + " msec");
        }
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e1) {
        }
    }

    protected void closeConnection(Connection conn, boolean error) {
        String methodName = "closeConnection";
        _log.entering(CLASS, methodName, new Object[] { "" + error });
        if (conn != null) {

            try {
                /*
                 * if this connection is not set for auto-commit, either commit
                 * or rollback based on the error flag, then close the
                 * connection
                 */
                try {
                    if (!conn.getAutoCommit()) {
                        if (!error) {
                            conn.commit();
                        } else {
                            conn.rollback();
                        }
                    }
                } catch (SQLException e1) {
                    // just log it
                    _log.logp(Level.FINEST, CLASS, methodName,
                            "Unable to commit or rollback.", e1);
                }

                conn.close();
            } catch (SQLException e) {
                // just log the exception since no point on failing here.
                _log.logp(Level.FINEST, CLASS, methodName,
                        "Unable to close connection.", e);
            }
        }
        _log.exiting(CLASS, methodName);
    }

    protected void closeResultSet(ResultSet results) {
        String method = "closeResultSet";
        if (results != null) {
            try {
                results.close();
            } catch (SQLException e) {
                // just log the exception since no point on failing here.
                _log.logp(Level.FINEST, CLASS, method,
                        "Unable to close results.", e);
            }
        }
    }

    protected void closeStatement(Statement statement, String methodName) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
            }
        }
    }

    protected DBType getDBType() {
        if (databaseType == null) {
            try {
                databaseType = DetectDatabaseType.DetectionUtils.detectDbType(getDBConnection());
            } catch (OAuthDataException e) {
                Tr.error(tc, "Internal error getting DB connection: " + e.getMessage(), e);
            }
        }
        return databaseType;
    }
}
