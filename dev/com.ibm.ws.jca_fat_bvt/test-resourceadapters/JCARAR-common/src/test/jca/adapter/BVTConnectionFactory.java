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

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.ConnectionSpec;
import jakarta.resource.cci.IndexedRecord;
import jakarta.resource.cci.MappedRecord;
import jakarta.resource.cci.RecordFactory;
import jakarta.resource.cci.ResourceAdapterMetaData;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;

import javax.naming.NamingException;
import javax.naming.Reference;

public class BVTConnectionFactory implements ConnectionFactory, RecordFactory {
    private static final long serialVersionUID = -5156439268170318958L;

    private final ConnectionManager cm;
    private final BVTManagedConnectionFactory mcf;

    public BVTConnectionFactory(ConnectionManager cm, BVTManagedConnectionFactory mcf) {
        this.cm = cm;
        this.mcf = mcf;
    }

    /** {@inheritDoc} */
    @Override
    public IndexedRecord createIndexedRecord(String name) {
        IndexedRecord r = new BVTRecord();
        r.setRecordName(name);
        return r;
    }

    /** {@inheritDoc} */
    @Override
    public MappedRecord createMappedRecord(String name) throws ResourceException {
        throw new NotSupportedException();
    }

    /** {@inheritDoc} */
    @Override
    public Connection getConnection() throws ResourceException {
        ConnectionRequestInfo cri = new BVTConnectionRequestInfo(mcf.getUserName(), mcf.getPassword());
        return ((BVTConnection) cm.allocateConnection(mcf, cri)).init(cm);
    }

    /** {@inheritDoc} */
    @Override
    public Connection getConnection(ConnectionSpec conSpec) throws ResourceException {
        throw new NotSupportedException();
    }

    /** {@inheritDoc} */
    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        throw new NotSupportedException();
    }

    /** {@inheritDoc} */
    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Reference getReference() throws NamingException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void setReference(Reference arg0) {
        throw new UnsupportedOperationException();
    }
}
