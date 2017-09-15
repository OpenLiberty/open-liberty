/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.passivator;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.StatefulBeanO;
import com.ibm.ejs.container.passivator.StatefulPassivator;
import com.ibm.websphere.csi.SessionBeanStore;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.serialization.DeserializationContext;
import com.ibm.ws.serialization.SerializationContext;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
public class StatefulPassivatorImpl extends StatefulPassivator {

    private final AtomicServiceReference<SerializationService> serializationServiceRef;

    /**
     * @param beanStore
     * @param container
     * @param failoverCache
     * @param serializationService
     */
    public StatefulPassivatorImpl(SessionBeanStore beanStore,
                                  EJSContainer container,
                                  AtomicServiceReference<SerializationService> serializationServiceRef) {
        super(beanStore, container, null);
        this.serializationServiceRef = serializationServiceRef;
    }

    @Override
    public ObjectOutputStream createPassivationOutputStream(OutputStream os) throws IOException {

        SerializationContext context = serializationServiceRef.getServiceWithException().createSerializationContext();
        StatefulPassivatorSerializationHandler handler = new StatefulPassivatorSerializationHandler();
        context.addObjectReplacer(handler);

        return context.createObjectOutputStream(os);
    }

    @Override
    public ObjectInputStream createActivationInputStream(InputStream is,
                                                         StatefulBeanO beanO,
                                                         ClassLoader classLoader) throws IOException {
        DeserializationContext context = serializationServiceRef.getServiceWithException().createDeserializationContext();
        StatefulPassivatorSerializationHandler handler = new StatefulPassivatorSerializationHandler(beanO);
        context.addObjectResolver(handler);

        return context.createObjectInputStream(is, classLoader);
    }

    @Override
    protected void writeManagedObjectContext(ObjectOutputStream oos, ManagedObjectContext context) throws IOException {
        oos.writeObject(context);
    }

    @Override
    protected ManagedObjectContext readManagedObjectContext(ObjectInputStream ois, BeanMetaData bmd, Object instance) throws IOException, ClassNotFoundException {
        return (ManagedObjectContext) ois.readObject();
    }
}
