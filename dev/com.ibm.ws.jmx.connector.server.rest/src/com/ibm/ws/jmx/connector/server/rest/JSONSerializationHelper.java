/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.converter.SerializationHelper;
import com.ibm.ws.jmx.connector.datatypes.ConversionException;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * This Serialization Helper leverages the com.ibm.ws.serialization.SerializationService
 * to deserialize Liberty types . We need this service since the rest connector client
 * has NO visibility to the myriad types that are passed as parameters to mbeans
 * 
 */
public class JSONSerializationHelper implements SerializationHelper {

    private final AtomicServiceReference<SerializationService> serializationServiceRef =
                    new AtomicServiceReference<SerializationService>("serializationService");

    ComponentContext _context = null;

    public void activate(ComponentContext context) {
        serializationServiceRef.activate(context);
        JSONConverter.setSerializationHelper(this);
        _context = context;

    }

    public void deactivate(ComponentContext context) {
        serializationServiceRef.deactivate(context);
        JSONConverter.setSerializationHelper(null);
        _context = null;
    }

    protected void setSerializationService(ServiceReference<SerializationService> ref) {
        serializationServiceRef.setReference(ref);
    }

    protected void unsetSerializationService(ServiceReference<SerializationService> ref) {
        serializationServiceRef.unsetReference(ref);
    }

    @Override
    public Object readObject(Object in, int blen, byte[] binary) throws ClassNotFoundException, ConversionException {

        SerializationService ss = serializationServiceRef.getService();
        if (null != ss && null != _context) {
            try {
                ClassLoader bundleCL = _context.getBundleContext().getClass().getClassLoader();
                ObjectInputStream objectInputStream = ss.createObjectInputStream(new ByteArrayInputStream(binary, 0, blen), bundleCL);
                return objectInputStream.readObject();
            } catch (IOException ioe) {
                if (ioe.getCause() instanceof ClassNotFoundException) {
                    throw (ClassNotFoundException) ioe.getCause();
                } else {
                    JSONConverter.throwConversionException(ioe, in);
                }
            }
        }
        return null;
    }
}
