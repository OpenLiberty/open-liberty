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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.InteractionSpec;
import jakarta.resource.cci.MessageListener;
import jakarta.resource.cci.Record;
import jakarta.resource.cci.ResourceWarning;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

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
        ConcurrentLinkedQueue<Map<String, String>> table = ManagedConnectionFactoryImpl.tables.get(tableName);

        @SuppressWarnings("unchecked")
        Map<String, String> inputMap = (Map<String, String>) input;
        @SuppressWarnings("unchecked")
        List<String> outputMap = (List<String>) output;

        String function = ((InteractionSpecImpl) ispec).getFunctionName();
        if ("ADD".equalsIgnoreCase(function)) {
            if (readOnly)
                throw new NotSupportedException("functionName=ADD for read only connection");
            table.add(new TreeMap<String, String>(inputMap));
            for (String key : inputMap.keySet()) {
                outputMap.add(key + "=" + inputMap.get(key));
            }
            onMessage(function, output);
            return true;
        } else if ("FIND".equalsIgnoreCase(function)) {
            for (Map<String, String> map : table) {
                boolean match = true;
                for (Map.Entry<?, ?> entry : inputMap.entrySet())
                    match &= entry.getValue().equals(map.get(entry.getKey()));
                if (match) {
                    for (String key : map.keySet()) {
                        outputMap.add(key + "=" + map.get(key));
                    }
                    return true;
                }
            }
            return false;
        } else if ("REMOVE".equalsIgnoreCase(function)) {
            if (readOnly)
                throw new NotSupportedException("functionName=REMOVE for read only connection");
            for (Iterator<Map<String, String>> it = table.iterator(); it.hasNext();) {
                Map<String, String> map = it.next();
                boolean match = true;
                for (Map.Entry<?, ?> entry : inputMap.entrySet())
                    match &= entry.getValue().equals(map.get(entry.getKey()));
                if (match) {
                    it.remove();
                    for (String key : map.keySet()) {
                        outputMap.add(key + "=" + map.get(key));
                    }
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
