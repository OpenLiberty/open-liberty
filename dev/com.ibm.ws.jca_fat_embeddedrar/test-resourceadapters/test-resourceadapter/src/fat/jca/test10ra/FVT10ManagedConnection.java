/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.jca.test10ra;

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

public class FVT10ManagedConnection implements ManagedConnection {

    String userName;
    String password;
    final FVT10ConnectionRequestInfo cri;
    private final ConcurrentLinkedQueue<FVT10Connection> handles = new ConcurrentLinkedQueue<FVT10Connection>();
    private final ConcurrentLinkedQueue<ConnectionEventListener> listeners = new ConcurrentLinkedQueue<ConnectionEventListener>();
    final FVT10ManagedConnectionFactory mcf;
    final Subject subject;

    FVT10ManagedConnection(final FVT10ManagedConnectionFactory mcf, FVT10ConnectionRequestInfo cri, Subject subj) throws ResourceException {
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
        if (userPwd != null && userPwd.length == 2) {
            userName = userPwd[0];
            password = userPwd[1];
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void associateConnection(Object connectionHandle) throws ResourceException {
        FVT10Connection handle = ((FVT10Connection) connectionHandle);
        handle.mc.handles.remove(handle);
        handle.mc = this;
        handles.add(handle);
    }

    @Override
    public void cleanup() throws ResourceException {
        for (FVT10Connection handle : handles)
            handle.close();
    }

    @Override
    public void destroy() throws ResourceException {
        cleanup();
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        FVT10Connection handle = new FVT10Connection(this);
        handles.add(handle);
        return handle;
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Local Transaction Not supported");
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return mcf.getLogWriter();
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new FVT10ManagedConnectionMetadata(userName);
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException("XA Transaction Not supported");
    }

    void notify(int eventType, FVT10Connection conHandle, Exception failure) {
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
                default:
                    throw new IllegalArgumentException(Integer.toString(eventType));
            }
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void setLogWriter(PrintWriter writer) throws ResourceException {
        throw new UnsupportedOperationException();
    }
}
