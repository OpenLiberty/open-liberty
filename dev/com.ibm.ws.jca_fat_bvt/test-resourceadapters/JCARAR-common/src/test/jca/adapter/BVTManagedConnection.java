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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LazyEnlistableManagedConnection;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;
import jakarta.resource.spi.ResourceAllocationException;
import jakarta.resource.spi.security.PasswordCredential;

import javax.security.auth.Subject;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

public class BVTManagedConnection implements LazyEnlistableManagedConnection, LocalTransaction, ManagedConnection {

    Connection con;
    final BVTConnectionRequestInfo cri;
    private final ConcurrentLinkedQueue<BVTConnection> handles = new ConcurrentLinkedQueue<BVTConnection>();
    private final ConcurrentLinkedQueue<ConnectionEventListener> listeners = new ConcurrentLinkedQueue<ConnectionEventListener>();
    private final BVTManagedConnectionFactory mcf;
    final Subject subject;
    private final XAConnection xacon;

    BVTManagedConnection(final BVTManagedConnectionFactory mcf, BVTConnectionRequestInfo cri, Subject subj) throws ResourceException {
        this.mcf = mcf;
        this.cri = cri;
        this.subject = subj;

        String[] userPwd;
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

        try {
            this.xacon = userPwd == null
                            ? mcf.adapter.xaDataSource.getXAConnection()
                            : mcf.adapter.xaDataSource.getXAConnection(userPwd[0], userPwd[1]);
        } catch (SQLException x) {
            throw new ResourceAllocationException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void associateConnection(Object connectionHandle) throws ResourceException {
        BVTConnection handle = ((BVTConnection) connectionHandle);
        handle.mc.handles.remove(handle);
        handle.mc = this;
        handles.add(handle);
    }

    /** {@inheritDoc} */
    @Override
    public void begin() throws ResourceException {
        try {
            con.setAutoCommit(false);
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void cleanup() throws ResourceException {
        for (BVTConnection handle : handles)
            handle.close();

        try {
            if (con != null)
                con.close();
            con = null;
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void commit() throws ResourceException {
        try {
            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() throws ResourceException {
        try {
            cleanup();
        } finally {
            try {
                xacon.close();
            } catch (SQLException x) {
                throw new ResourceException(x);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        if (con == null)
            try {
                con = xacon.getConnection();
            } catch (SQLException x) {
                throw new ResourceException(x);
            }
        BVTConnection handle = new BVTConnection(this);
        handles.add(handle);
        return handle;
    }

    /** {@inheritDoc} */
    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        return this;
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
        try {
            return new BVTXAResource(xacon.getXAResource(), mcf);
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    void notify(int eventType, BVTConnection conHandle, Exception failure) {
        ConnectionEvent event = new ConnectionEvent(this, eventType, failure);
        event.setConnectionHandle(conHandle);
        for (ConnectionEventListener listener : listeners)
            switch (eventType) {
                case ConnectionEvent.CONNECTION_CLOSED:
                    handles.remove(conHandle);
                    listener.connectionClosed(event);
                    break;
                case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
                    listener.connectionErrorOccurred(event);
                    break;
                case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
                    listener.localTransactionCommitted(event);
                    break;
                case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
                    listener.localTransactionRolledback(event);
                    break;
                case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
                    listener.localTransactionStarted(event);
                    break;
                default:
                    throw new IllegalArgumentException(Integer.toString(eventType));
            }
    }

    /** {@inheritDoc} */
    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void rollback() throws ResourceException {
        try {
            con.rollback();
            con.setAutoCommit(true);
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setLogWriter(PrintWriter writer) throws ResourceException {
        throw new UnsupportedOperationException();
    }
}
