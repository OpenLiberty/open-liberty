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
package com.ibm.test.dynamicconfigadapter;

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.sql.ConnectionEventListener;
import javax.transaction.xa.XAResource;

public class DynaCfgManagedConnection implements ManagedConnection {

    final ConcurrentLinkedQueue<DynaCfgConnection> handles = new ConcurrentLinkedQueue<DynaCfgConnection>();
    final ConcurrentLinkedQueue<ConnectionEventListener> listeners = new ConcurrentLinkedQueue<ConnectionEventListener>();
    final DynaCfgManagedConnectionFactory mcf;
    final String[] userPwd;

    DynaCfgManagedConnection(final DynaCfgManagedConnectionFactory mcf, DynaCfgConnectionRequestInfo cri, final Subject subject) throws ResourceException {
        this.mcf = mcf;

        if (subject == null)
            userPwd = cri == null ? null : new String[] { cri.userName, cri.password };
        else
            userPwd = AccessController.doPrivileged(new PrivilegedAction<String[]>() {
                @Override
                public String[] run() {
                    for (Object credential : subject.getPrivateCredentials())
                        if (credential instanceof PasswordCredential) {
                            PasswordCredential pwdcred = (PasswordCredential) credential;
                            if (mcf.equals(pwdcred.getManagedConnectionFactory()))
                                return new String[] { pwdcred.getUserName(), String.valueOf(pwdcred.getPassword()) };
                        }
                    return null;
                }
            });
    }

    /** {@inheritDoc} */
    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void associateConnection(Object connectionHandle) throws ResourceException {
        throw new NotSupportedException();
    }

    /** {@inheritDoc} */
    @Override
    public void cleanup() throws ResourceException {
        for (DynaCfgConnection handle : handles)
            handle.close();
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() throws ResourceException {
        cleanup();
    }

    /** {@inheritDoc} */
    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        DynaCfgConnection handle = new DynaCfgConnection(this);
        handles.add(handle);
        return handle;
    }

    /** {@inheritDoc} */
    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException();
    }

    /** {@inheritDoc} */
    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return mcf.getLogWriter();
    }

    /** {@inheritDoc} */
    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException();
    }

    /** {@inheritDoc} */
    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void setLogWriter(PrintWriter writer) throws ResourceException {
        throw new NotSupportedException();
    }
}
