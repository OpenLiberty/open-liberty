/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.internal;

import java.util.Collection;
import java.util.Collections;

import javax.naming.NameClassPair;
import javax.naming.NamingException;

import org.omg.CORBA.ORB;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.container.service.naming.JavaColonNamingHelper;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.ws.transport.iiop.spi.ClientORBRef;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Provides support for lookup and injection of java:comp/ORB.
 *
 * Prioritized ahead of InjectionJavaColonHelper so that java:comp/ORB
 * lookups will work even if no ComponentMetaData is available on the thread.
 * This is required for interoperability, so that it is available when
 * demarshalling remote handles.
 */
@Component(property = { "service.ranking:Integer=100" })
public class ORBNamingHelper implements JavaColonNamingHelper {
    private static final String COMP_NAME = "ORB";

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
    public Object getObjectInstance(JavaColonNamespace namespace, String name) throws NamingException {
        if (JavaColonNamespace.COMP == namespace && COMP_NAME.equals(name)) {
            return orbRef.getServiceWithException().getORB();
        }
        return null;
    }

    @Override
    public boolean hasObjectWithPrefix(JavaColonNamespace namespace, String name) throws NamingException {
        return namespace == JavaColonNamespace.COMP && name.isEmpty();
    }

    @Override
    public Collection<? extends NameClassPair> listInstances(JavaColonNamespace namespace, String nameInContext) throws NamingException {
        if (namespace == JavaColonNamespace.COMP && nameInContext.isEmpty()) {
            return Collections.singletonList(new NameClassPair(COMP_NAME, ORB.class.getName()));
        }
        return Collections.emptyList();
    }
}
