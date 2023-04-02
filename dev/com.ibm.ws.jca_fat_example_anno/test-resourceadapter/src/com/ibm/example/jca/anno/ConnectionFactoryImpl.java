/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.example.jca.anno;

import javax.naming.NamingException;
import javax.naming.Reference;

import com.ibm.example.jca.anno.ConnectionSpecImpl.ConnectionRequestInfoImpl;

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

/**
 * Example connection factory.
 */
public class ConnectionFactoryImpl implements ConnectionFactory, RecordFactory {
    private static final long serialVersionUID = 847022212144243370L;

    private final ConnectionManager cm;
    final ManagedConnectionFactoryImpl mcf;
    private Reference ref;

    ConnectionFactoryImpl(ConnectionManager cm, ManagedConnectionFactoryImpl mcf) {
        this.cm = cm;
        this.mcf = mcf;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IndexedRecord<String> createIndexedRecord(String name) throws ResourceException {
        IndexedRecord<String> record = new IndexedRecordImpl<String>();
        record.setRecordName(name);
        return record;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MappedRecord<String, String> createMappedRecord(String name) throws ResourceException {
        MappedRecord<String, String> record = new MappedRecordImpl<String, String>();
        record.setRecordName(name);
        return record;
    }

    @Override
    public Connection getConnection() throws ResourceException {
        return getConnection(new ConnectionSpecImpl());
    }

    @Override
    public Connection getConnection(ConnectionSpec conSpec) throws ResourceException {
        ConnectionRequestInfoImpl cri = ((ConnectionSpecImpl) conSpec).createConnectionRequestInfo();
        cri.put("tableName", mcf.getTableName());

        ConnectionImpl con = cm == null ? new ConnectionImpl(null, cri) : (ConnectionImpl) cm.allocateConnection(mcf, cri);
        con.cf = this;
        return con;
    }

    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        return this;
    }

    @Override
    public Reference getReference() throws NamingException {
        return ref;
    }

    @Override
    public void setReference(Reference ref) {
        this.ref = ref;
    }
}
