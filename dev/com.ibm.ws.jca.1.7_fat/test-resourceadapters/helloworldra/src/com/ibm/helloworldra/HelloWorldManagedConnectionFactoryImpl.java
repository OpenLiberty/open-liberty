/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.helloworldra;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

public class HelloWorldManagedConnectionFactoryImpl implements ManagedConnectionFactory {

    private PrintWriter writer;

    /**
     * Constructor for HelloWorldManagedConnectionFactoryImpl
     */
    public HelloWorldManagedConnectionFactoryImpl() {

        super();
    }

    /**
     * @see ManagedConnectionFactory#createConnectionFactory(ConnectionManager)
     */
    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {

        return new HelloWorldConnectionFactoryImpl(this, cm);
    }

    /**
     * @see ManagedConnectionFactory#createConnectionFactory()
     */
    @Override
    public Object createConnectionFactory() throws ResourceException {

        return new HelloWorldConnectionFactoryImpl(this, null);
    }

    /**
     * @see ManagedConnectionFactory#createManagedConnection(Subject, ConnectionRequestInfo)
     */
    @Override
    public ManagedConnection createManagedConnection(
                                                     Subject subject,
                                                     ConnectionRequestInfo cxRequestInfo) throws ResourceException {

        return new HelloWorldManagedConnectionImpl();
    }

    /**
     * @see ManagedConnectionFactory#matchManagedConnections(Set, Subject, ConnectionRequestInfo)
     */
    @Override
    public ManagedConnection matchManagedConnections(
                                                     Set connectionSet,
                                                     Subject subject,
                                                     ConnectionRequestInfo cxRequestInfo) throws ResourceException {

        ManagedConnection match = null;
        Iterator iterator = connectionSet.iterator();
        if (iterator.hasNext()) {
            match = (ManagedConnection) iterator.next();
        }

        return match;
    }

    /**
     * @see ManagedConnectionFactory#setLogWriter(PrintWriter)
     */
    @Override
    public void setLogWriter(PrintWriter writer) throws ResourceException {

        this.writer = writer;
    }

    /**
     * @see ManagedConnectionFactory#getLogWriter()
     */
    @Override
    public PrintWriter getLogWriter() throws ResourceException {

        return writer;
    }

    @Override
    public boolean equals(Object other) {

        if (other instanceof HelloWorldManagedConnectionFactoryImpl) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {

        return 0;
    }

}