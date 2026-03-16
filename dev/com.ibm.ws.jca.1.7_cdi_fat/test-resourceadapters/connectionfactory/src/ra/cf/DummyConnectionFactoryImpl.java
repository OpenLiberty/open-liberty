/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package ra.cf;

import javax.naming.NamingException;
import javax.naming.Reference;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.ConnectionSpec;
import jakarta.resource.cci.RecordFactory;
import jakarta.resource.cci.ResourceAdapterMetaData;

/**
 * Connection Factory impl that does nothing
 */
public class DummyConnectionFactoryImpl implements ConnectionFactory {

    private static final long serialVersionUID = 1L;

    @Override
    public void setReference(Reference arg0) {
        return;
    }

    @Override
    public Reference getReference() throws NamingException {
        return null;
    }

    @Override
    public Connection getConnection() throws ResourceException {
        return null;
    }

    @Override
    public Connection getConnection(ConnectionSpec arg0) throws ResourceException {
        return null;
    }

    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        return new DummyResourceAdapterMetaDataImpl();
    }

    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        return null;
    }
}
