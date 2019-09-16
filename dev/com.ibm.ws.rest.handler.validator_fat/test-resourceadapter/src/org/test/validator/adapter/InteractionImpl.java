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
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.ResourceWarning;

public class InteractionImpl implements Interaction {
    private final ConnectionImpl con;

    InteractionImpl(ConnectionImpl con) {
        this.con = con;
    }

    @Override
    public void clearWarnings() throws ResourceException {}

    @Override
    public void close() throws ResourceException {}

    @Override
    public Record execute(InteractionSpec ispec, Record input) throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public boolean execute(InteractionSpec ispec, Record input, Record output) throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public Connection getConnection() {
        return con;
    }

    @Override
    public ResourceWarning getWarnings() throws ResourceException {
        return null;
    }
}
