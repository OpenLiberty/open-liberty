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

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;

public class ConnectionImpl implements Connection, ConnectionMetaData {
    private final String userName;

    ConnectionImpl(String userName) {
        this.userName = userName;
    }

    @Override
    public void close() throws ResourceException {}

    @Override
    public Interaction createInteraction() throws ResourceException {
        return new InteractionImpl(this);
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public ConnectionMetaData getMetaData() throws ResourceException {
        return this;
    }

    @Override
    public String getEISProductName() throws ResourceException {
        return "TestValidationEIS";
    }

    @Override
    public String getEISProductVersion() throws ResourceException {
        return "33.56.65";
    }

    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public String getUserName() throws ResourceException {
        return userName;
    }
}
