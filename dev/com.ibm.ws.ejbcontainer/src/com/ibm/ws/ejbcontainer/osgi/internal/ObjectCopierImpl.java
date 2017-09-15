/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessController;

import com.ibm.ws.ejbcontainer.util.ObjectCopier;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class ObjectCopierImpl extends ObjectCopier {
    private static final ThreadContextAccessor threadContextAccessor = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());

    private final AtomicServiceReference<SerializationService> serializationServiceRef;

    ObjectCopierImpl(AtomicServiceReference<SerializationService> serializationServiceRef) {
        this.serializationServiceRef = serializationServiceRef;
    }

    @Override
    public boolean isNoLocalCopies() {
        return false;
    }

    @Override
    protected Serializable copySerializable(Serializable obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();

            InputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ClassLoader classLoader = threadContextAccessor.getContextClassLoaderForUnprivileged(Thread.currentThread());
            SerializationService serializationService = serializationServiceRef.getServiceWithException();
            ObjectInputStream ois = serializationService.createObjectInputStream(bais, classLoader);

            return (Serializable) ois.readObject();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}
