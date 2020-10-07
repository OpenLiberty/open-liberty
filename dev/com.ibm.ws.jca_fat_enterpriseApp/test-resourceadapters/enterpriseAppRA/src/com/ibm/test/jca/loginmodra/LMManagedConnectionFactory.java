/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.jca.loginmodra;

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;

@ConnectionDefinition(connectionFactory = ConnectionFactory.class,
                      connectionFactoryImpl = LMConnectionFactory.class,
                      connection = Connection.class,
                      connectionImpl = LMConnection.class)
public class LMManagedConnectionFactory implements ManagedConnectionFactory {
    private static final long serialVersionUID = 1;

    @Override
    public Object createConnectionFactory() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new LMConnectionFactory(cm, this);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        String userPwd = getUserAndPassword(subject);
        return new LMManagedConnection(userPwd);
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return null;
    }

    static String getUserAndPassword(final Subject subject) {
        if (subject == null)
            return "NoSubject";

        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                for (Object credential : subject.getPrivateCredentials())
                    if (credential instanceof PasswordCredential) {
                        PasswordCredential pwdcred = (PasswordCredential) credential;
                        ManagedConnectionFactory mcf = pwdcred.getManagedConnectionFactory();
                        if (mcf != null && mcf instanceof LMManagedConnectionFactory)
                            return pwdcred.getUserName() + '/' + String.valueOf(pwdcred.getPassword());
                    }
                return "NotFoundInSubject";
            }
        });
    }

    @Override
    public ManagedConnection matchManagedConnections(@SuppressWarnings("rawtypes") Set connections, Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        String u = getUserAndPassword(subject);
        if (u == null)
            return null;

        for (Object mc : connections)
            if (mc instanceof LMManagedConnection && u.equals(((LMManagedConnection) mc).userPwd))
                return (ManagedConnection) mc;
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) throws ResourceException {
    }
}
