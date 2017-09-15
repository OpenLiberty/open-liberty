/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal;

import javax.naming.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.factory.EJBLinkReferenceFactory;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class EJBLinkReferenceFactoryImpl implements EJBLinkReferenceFactory {
    private static final TraceComponent tc = Tr.register(EJBLinkReferenceFactoryImpl.class);

    private final AtomicServiceReference<EJBLinkReferenceFactory> ejbLinkReferenceFactorySRRef;

    EJBLinkReferenceFactoryImpl(AtomicServiceReference<EJBLinkReferenceFactory> ejbLinkReferenceFactory) {
        ejbLinkReferenceFactorySRRef = ejbLinkReferenceFactory;
    }

    @Override
    public Reference createEJBLinkReference(String refName, String application, String module, String component, String beanName, String beanInterface, String homeInterface,
                                            boolean localRef, boolean remoteRef) throws InjectionConfigurationException {

        EJBLinkReferenceFactory factory = ejbLinkReferenceFactorySRRef.getService();

        if (factory != null) {
            return factory.createEJBLinkReference(refName, application, module, component, beanName, beanInterface, homeInterface, localRef, remoteRef);
        }

        String componentString = component != null ? component : module;
        String message = Tr.formatMessage(tc, "EJB_REF_NOT_SUPPORTED_CWNEN1007E",
                                          refName, componentString, module, application);
        throw new InjectionConfigurationException(message);
    }
}
