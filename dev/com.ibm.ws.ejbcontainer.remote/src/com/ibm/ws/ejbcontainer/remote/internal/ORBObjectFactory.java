/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.internal;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.transport.iiop.spi.ClientORBRef;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = { ObjectFactory.class, ORBObjectFactory.class })
public class ORBObjectFactory implements ObjectFactory {
    private static final String REFERENCE_ORB = "orb";

    private static final AtomicServiceReference<ClientORBRef> orbRef = new AtomicServiceReference<ClientORBRef>(REFERENCE_ORB);

    @Activate
    protected void activate(ComponentContext cc) {
        orbRef.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        orbRef.deactivate(cc);
    }

    @Reference(name = REFERENCE_ORB, service = ClientORBRef.class, target = "(id=defaultOrb)")
    protected void addORBRef(ServiceReference<ClientORBRef> ref) {
        orbRef.setReference(ref);
    }

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        if (!(obj instanceof javax.naming.Reference)) {
            return null;
        }

        return orbRef.getServiceWithException().getORB();
    }
}
