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

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;
import javax.resource.spi.ManagedConnection;

public class HelloWorldConnectionImpl implements Connection {

    private static final String CLOSED_ERROR = "Connection closed";
    private static final String TRANSACTIONS_NOT_SUPPORTED = "Local transactions not supported";
    private static final String RESULT_SETS_NOT_SUPPORTED = "Result sets not supported";
    private boolean valid;

    private ManagedConnection mc;

    /**
     * Constructor for HelloWorldConnectionImpl
     */
    public HelloWorldConnectionImpl(ManagedConnection mc) {

        super();
        this.mc = mc;
        valid = true;
    }

    void invalidate() {

        mc = null;
        valid = false;
    }

    /**
     * @see Connection#createInteraction()
     */
    @Override
    public Interaction createInteraction() throws ResourceException {

        if (valid) {
            return new HelloWorldInteractionImpl(this);
        } else {
            throw new ResourceException(CLOSED_ERROR);
        }
    }

    /**
     * @see Connection#getLocalTransaction()
     */
    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {

        throw new NotSupportedException(TRANSACTIONS_NOT_SUPPORTED);
    }

    /**
     * @see Connection#getMetaData()
     */
    @Override
    public ConnectionMetaData getMetaData() throws ResourceException {

        if (valid) {
            return new HelloWorldConnectionMetaDataImpl();
        } else {
            throw new ResourceException(CLOSED_ERROR);
        }
    }

    /**
     * @see Connection#getResultSetInfo()
     */
    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {

        throw new NotSupportedException(RESULT_SETS_NOT_SUPPORTED);
    }

    /**
     * @see Connection#close()
     */
    @Override
    public void close() throws ResourceException {

        if (valid) {
            ((HelloWorldManagedConnectionImpl) mc).close();
        }
    }

}