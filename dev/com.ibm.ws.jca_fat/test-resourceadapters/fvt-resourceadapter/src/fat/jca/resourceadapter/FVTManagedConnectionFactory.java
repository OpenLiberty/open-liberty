/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.jca.resourceadapter;

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;
import javax.sql.XAConnection;

import com.ibm.websphere.j2c.ConnectionEvent;

public class FVTManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation {
    private static final long serialVersionUID = 7834485368743035738L;

    transient FVTResourceAdapter adapter;
    private transient String clientID; // demonstrates a config-property with a default value
    private transient String password; // confidential config-property
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
        return new FVTConnectionFactory(cm, this);
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
        createDefaultTable();
        return new FVTManagedConnection(this, (FVTConnectionRequestInfo) cri, subject);
    }

    public String getClientID() {
        return clientID;
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

    String getPassword() {
        return password;
    }

    /** {@inheritDoc} */
    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
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
        System.out.println("Enter matchManagedConnections");
        for (Object o : set)
            if (o instanceof FVTManagedConnection) {
                FVTManagedConnection m = (FVTManagedConnection) o;
                if (m.isInvalid()) {
                    System.out.println("m is invalid in matchManagedConnections");
                    FVTConnection conHandle = null;
                    Exception failure = null;
                    m.notify(ConnectionEvent.CONNECTION_ERROR_OCCURRED, conHandle, failure);
                }
                if (match(m.cri, cri) && matchSubjects(m.subject, subject)) {
                    System.out.println("Returning m from matchManagedConnections");
                    return m;
                }
            }
        System.out.println("Exit matchManagedConnections");
        return null;
    }

    private static final boolean matchSubjects(final Subject s1, final Subject s2) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return match(s1, s2);
            }
        });
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
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

    public void setPassword(String password) {
        this.password = password;
    }

    /** {@inheritDoc} */
    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (FVTResourceAdapter) adapter;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
