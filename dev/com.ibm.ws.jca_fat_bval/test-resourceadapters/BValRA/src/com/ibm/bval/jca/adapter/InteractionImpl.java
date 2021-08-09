/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.bval.jca.adapter;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.MessageListener;
import javax.resource.cci.Record;
import javax.resource.cci.ResourceWarning;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;

/**
 * Example interaction.
 */
public class InteractionImpl implements Interaction {
    private ConnectionImpl con;

    InteractionImpl(ConnectionImpl con) {
        this.con = con;
    }

    @Override
    public void clearWarnings() throws ResourceException {
    }

    @Override
    public void close() throws ResourceException {
        if (con == null)
            throw new ResourceException("already closed");
        con = null;
    }

    @Override
    public Record execute(InteractionSpec ispec, Record input) throws ResourceException {
        Record output = con.cf.createMappedRecord("output");
        execute(ispec, input, output);
        return output;
    }

    @Override
    public boolean execute(InteractionSpec ispec, Record input, Record output) throws ResourceException {
        if (con == null)
            throw new ResourceException("connection is closed");

        Boolean readOnly = (Boolean) con.cri.get("readOnly");
        String tableName = (String) con.cri.get("tableName");
        ConcurrentLinkedQueue<Map<?, ?>> table = ManagedConnectionFactoryImpl.tables.get(tableName);

        @SuppressWarnings("unchecked")
        Map<Object, Object> inputMap = (Map<Object, Object>) input;
        @SuppressWarnings("unchecked")
        Map<Object, Object> outputMap = (Map<Object, Object>) output;

        String function = ((InteractionSpecImpl) ispec).getFunctionName();
        if ("ADD".equalsIgnoreCase(function)) {
            if (readOnly)
                throw new NotSupportedException("functionName=ADD for read only connection");
            table.add(new TreeMap<Object, Object>(inputMap));
            outputMap.putAll(inputMap);
            onMessage(function, output);
            return true;
        } else if ("FIND".equalsIgnoreCase(function)) {
            for (Map<?, ?> map : table) {
                boolean match = true;
                for (Map.Entry<?, ?> entry : inputMap.entrySet())
                    match &= entry.getValue().equals(map.get(entry.getKey()));
                if (match) {
                    outputMap.putAll(map);
                    return true;
                }
            }
            return false;
        } else if ("REMOVE".equalsIgnoreCase(function)) {
            if (readOnly)
                throw new NotSupportedException("functionName=REMOVE for read only connection");
            for (Iterator<Map<?, ?>> it = table.iterator(); it.hasNext();) {
                Map<?, ?> map = it.next();
                boolean match = true;
                for (Map.Entry<?, ?> entry : inputMap.entrySet())
                    match &= entry.getValue().equals(map.get(entry.getKey()));
                if (match) {
                    it.remove();
                    outputMap.putAll(map);
                    onMessage(function, output);
                    return true;
                }
            }
            return false;
        } else
            throw new NotSupportedException("InteractionSpec: functionName = " + function);
    }

    @Override
    public Connection getConnection() {
        return con;
    }

    @Override
    public ResourceWarning getWarnings() throws ResourceException {
        return null;
    }

    private void onMessage(String functionName, Record record) throws ResourceException {
        for (Entry<ActivationSpecImpl, MessageEndpointFactory> entry : con.cf.mcf.adapter.endpointFactories.entrySet())
            if (functionName.equalsIgnoreCase(entry.getKey().getFunctionName())) {
                MessageEndpoint endpoint = entry.getValue().createEndpoint(null);
                try {
                    ((MessageListener) endpoint).onMessage(record);
                } finally {
                    endpoint.release();
                }
            }
    }
}
