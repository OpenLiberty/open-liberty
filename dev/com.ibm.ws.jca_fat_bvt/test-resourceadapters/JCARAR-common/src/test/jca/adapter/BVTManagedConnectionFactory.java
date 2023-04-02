/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
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
package test.jca.adapter;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterAssociation;

import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.Subject;
import javax.sql.XAConnection;

public class BVTManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation {
    private static final long serialVersionUID = 7834485368743035738L;

    transient BVTResourceAdapter adapter;
    private transient boolean autoCreateTable; // demonstrates a String config property that can be switched to Boolean in the metatype
    private transient int numRetries; // demonstrates a String config property that can be switched to Short in the metatype 
    private transient String password; // confidential config-property
    private transient long retryInterval; // demonstrates a numeric config property that needs to be set as String
    private SSLSocketFactory sslSocketFactory;
    private transient String tableName; // demonstrates a config-property
    private transient boolean tableCreated;
    private transient String userName; // config-property

    /** {@inheritDoc} */
    @Override
    public Object createConnectionFactory() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new BVTConnectionFactory(cm, this);
    }

    private synchronized void createDefaultTable() throws ResourceException {
        if (tableCreated)
            return;
        XAConnection xacon = null;
        Connection con = null;
        try {
            xacon = adapter.xaDataSource.getXAConnection(userName, password);
            con = xacon.getConnection();
            Statement stmt = con.createStatement();
            try {
                stmt.executeUpdate("drop table " + tableName);
            } catch (SQLException x) {
            }
            stmt.executeUpdate("create table " + tableName + " (col1 int not null primary key, col2 varchar(50))");
            tableCreated = true;
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        if (sslSocketFactory == null)
            throw new IllegalArgumentException("required sslSocketFactory is missing");
        if (autoCreateTable)
            createDefaultTable();
        return new BVTManagedConnection(this, (BVTConnectionRequestInfo) cri, subject);
    }

    public String getAutoCreateTable() {
        return Boolean.toString(autoCreateTable);
    }

    /** {@inheritDoc} */
    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        try {
            return adapter.xaDataSource.getLogWriter();
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    public String getNumRetries() {
        return Integer.toString(numRetries);
    }

    public long getRetryInterval() {
        return retryInterval;
    }

    String getPassword() {
        return password;
    }

    /** {@inheritDoc} */
    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public String getTableName() {
        return tableName;
    }

    String getUserName() {
        return userName;
    }

    private static final boolean match(Object o1, Object o2) {
        return o1 == o2 || o1 != null && o1.equals(o2);
    }

    /** {@inheritDoc} */
    @Override
    public ManagedConnection matchManagedConnections(@SuppressWarnings("rawtypes") Set set, Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        for (Object o : set)
            if (o instanceof BVTManagedConnection) {
                BVTManagedConnection m = (BVTManagedConnection) o;
                if (match(m.cri, cri) && match(m.subject, subject))
                    return m;
            }
        return null;
    }

    public void setAutoCreateTable(String autoCreateTable) {
        this.autoCreateTable = Boolean.parseBoolean(autoCreateTable);
    }

    /** {@inheritDoc} */
    @Override
    public void setLogWriter(PrintWriter logwriter) throws ResourceException {
        try {
            adapter.xaDataSource.setLogWriter(logwriter);
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    public void setNumRetries(String numRetries) {
        if (!"0".equals(numRetries))
            throw new UnsupportedOperationException("JCA BVT resource adapter doesn't support retries: " + numRetries);
        this.numRetries = Integer.parseInt(numRetries);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /** {@inheritDoc} */
    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (BVTResourceAdapter) adapter;
    }

    public void setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
    }

    public void setRetryInterval(String retryInterval) {
        this.retryInterval = "INFINITE".equalsIgnoreCase(retryInterval) ? Long.MAX_VALUE :
                        "NONE".equalsIgnoreCase(retryInterval) ? 0 :
                                        Long.parseLong(retryInterval);
    }

    public void setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
