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
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LazyEnlistableManagedConnection;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.ResourceAllocationException;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

public class FVTManagedConnection implements LazyEnlistableManagedConnection, LocalTransaction, ManagedConnection {

    Connection con;
    final FVTConnectionRequestInfo cri;
    private final ConcurrentLinkedQueue<FVTConnection> handles = new ConcurrentLinkedQueue<FVTConnection>();
    private final ConcurrentLinkedQueue<ConnectionEventListener> listeners = new ConcurrentLinkedQueue<ConnectionEventListener>();
    final FVTManagedConnectionFactory mcf;
    final Subject subject;
    final String user;
    private final XAConnection xacon;

    public boolean invalid = false; // Testing only, remove before pushing to GH

    FVTManagedConnection(final FVTManagedConnectionFactory mcf, FVTConnectionRequestInfo cri, Subject subj) throws ResourceException {
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
            this.xacon = userPwd == null ? mcf.adapter.xaDataSource.getXAConnection() : mcf.adapter.xaDataSource.getXAConnection(userPwd[0], userPwd[1]);
        } catch (SQLException x) {
            throw new ResourceAllocationException(x);
        }

        this.user = userPwd == null ? null : userPwd[0];
    }

    /** {@inheritDoc} */
    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void associateConnection(Object connectionHandle) throws ResourceException {
        FVTConnection handle = ((FVTConnection) connectionHandle);
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
        for (FVTConnection handle : handles)
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
        FVTConnection handle = new FVTConnection(this);
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
            return xacon.getXAResource();
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    void notify(int eventType, FVTConnection conHandle, Exception failure) {
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

    /**
     * GJW Internal test verification only
     * Remove before pushing to GH
     *
     * @return
     */
    public boolean isInvalid() {
        return invalid;
    }
}
