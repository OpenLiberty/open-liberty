/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.validator.adapter;

import java.io.IOError;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.CommException;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.ResourceAllocationException;
import javax.resource.spi.SecurityException;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.test.validator.adapter.ConnectionSpecImpl.ConnectionRequestInfoImpl;

public class ManagedConnectionImpl implements ManagedConnection {
    private final ManagedConnectionFactoryImpl mcf;
    private String mostRecentUser;

    ManagedConnectionImpl(ManagedConnectionFactoryImpl mcf) {
        this.mcf = mcf;
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
    }

    @Override
    public void associateConnection(Object handle) throws ResourceException {
    }

    @Override
    public void cleanup() throws ResourceException {
    }

    @Override
    public void destroy() throws ResourceException {
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        String conClass = (String) ((ConnectionRequestInfoImpl) cri).get("ConnectionClass");
        String userName = mcf.getUserName();
        String password = mcf.getPassword();
        if (subject == null) {
            userName = (String) ((ConnectionRequestInfoImpl) cri).getOrDefault("UserName", userName);
            password = (String) ((ConnectionRequestInfoImpl) cri).getOrDefault("Password", password);
        } else { // oversimplified handling of Subject
            PasswordCredential cred;
            try {
                cred = AccessController.doPrivileged((PrivilegedExceptionAction<PasswordCredential>) () -> {
                    for (Object c : subject.getPrivateCredentials())
                        if (c instanceof PasswordCredential && ((PasswordCredential) c).getManagedConnectionFactory() == mcf)
                            return (PasswordCredential) c;
                    return null;
                });
            } catch (PrivilegedActionException x) {
                throw new SecurityException(x.getCause());
            }
            if (cred != null) {
                userName = cred.getUserName();
                password = new String(cred.getPassword());
            }
        }

        // Accept some user/password combinations and reject others
        if ("DefaultUserName".equals(userName) && "DefaultPassword".equals(password) || // resource adapter defaults
            "dbuser".equals(userName) && "dbpass".contentEquals(password) && conClass.contains("jmsadapter") || // custom login module defaults
            userName != null && password != null && userName.charAt(userName.length() - 1) == password.charAt(0))
            if (JDBCConnectionImpl.class.getName().equals(conClass))
                try {
                    InvocationHandler handler = new JDBCConnectionImpl(mostRecentUser = userName);
                    return AccessController.doPrivileged((PrivilegedExceptionAction<?>) () -> {
                        return Proxy.newProxyInstance(java.sql.Connection.class.getClassLoader(),
                                                      new Class<?>[] { java.sql.Connection.class, DatabaseMetaData.class },
                                                      handler);
                    });
                } catch (PrivilegedActionException x) {
                    Throwable cause = x.getCause();
                    if (cause instanceof ResourceException)
                        throw (ResourceException) cause;
                    else
                        throw new ResourceException(cause);
                }
            else if (ConnectionImpl.class.getName().equals(conClass))
                return new ConnectionImpl(mostRecentUser = userName);
            else if ("org.test.validator.jmsadapter.JMSConnectionImpl".equals(conClass))
                try {
                    final String user = userName;
                    return AccessController.doPrivileged((PrivilegedExceptionAction<?>) () -> {
                        Class<?> JMSConnectionImpl = getClass().getClassLoader().loadClass("org.test.validator.jmsadapter.JMSConnectionImpl");
                        Object con = JMSConnectionImpl.getConstructor(String.class).newInstance(user);
                        mostRecentUser = user;
                        return con;
                    });
                } catch (PrivilegedActionException x) {
                    Throwable cause = x.getCause();
                    if (cause instanceof ResourceException)
                        throw (ResourceException) cause;
                    else
                        throw new ResourceException(cause);
                }
            else
                throw new ResourceAllocationException(conClass);
        else
            throw new SecurityException("Unable to authenticate with " + userName, "ERR_AUTH");
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return null;
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) throws ResourceException {
    }

    /**
     * Simulate the pattern used by CICS/IMS resource adapters to allow for a managed connection to be tested.
     *
     * @return true if validation is successful. False indicates validation is unsuccessful.
     * @throws ResourceException indicates the connection is not valid.
     * @throws SQLException      indicates the connection is not valid.
     */
    public boolean testConnection() throws ResourceException, SQLException {
        // The user name parameter is a convenient way to trigger rejection of test requests in variety of ways,
        if (mostRecentUser.startsWith("testerruser"))
            throw new IOError(new SQLNonTransientConnectionException("Database appears to be down.", "08006", -13579));
        else if (mostRecentUser.startsWith("testfailuser"))
            return false;
        else if (mostRecentUser.startsWith("testresxuser"))
            throw (ResourceAdapterInternalException) new ResourceAdapterInternalException("Something bad has happened. See cause.")
                            .initCause(new CommException("Lost connection to host.", "ERR_CONNECT"));
        else if (mostRecentUser.startsWith("testrunxuser"))
            throw new IllegalStateException("Connection was dropped.");
        else
            return true;
    }
}
