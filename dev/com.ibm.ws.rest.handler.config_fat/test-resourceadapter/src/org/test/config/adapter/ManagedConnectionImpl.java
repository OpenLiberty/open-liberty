package org.test.config.adapter;
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

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.SecurityException;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

public class ManagedConnectionImpl implements ManagedConnection {
    private boolean destroyed;
    private final ManagedConnectionFactoryImpl mcf;

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
    public void destroy() {
        destroyed = true;
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        ConnectionSpecImpl conSpec = (ConnectionSpecImpl) cri;
        String userName = mcf.getUserName();
        String password = mcf.getPassword();
        if (subject == null) {
            userName = conSpec.getUserName() == null ? userName : conSpec.getUserName();
            password = conSpec.getPassword() == null ? password : conSpec.getPassword();
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
        if (userName == null && password == null ||
            userName != null && userName.replace("user", "pwd").equals(password))
            try {
                InvocationHandler handler = new ConnectionImpl(userName);
                return AccessController.doPrivileged((PrivilegedExceptionAction<?>) () -> {
                    return Proxy.newProxyInstance(conSpec.interfaces[0].getClassLoader(),
                                                  conSpec.interfaces,
                                                  handler);
                });
            } catch (PrivilegedActionException x) {
                Throwable cause = x.getCause();
                if (cause instanceof ResourceException)
                    throw (ResourceException) cause;
                else
                    throw new ResourceException(cause);
            }
        else
            throw new SecurityException("Unable to authenticate as " + userName, "ERR_AUTH");
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
     */
    public boolean testConnection() {
        return !destroyed;
    }
}
