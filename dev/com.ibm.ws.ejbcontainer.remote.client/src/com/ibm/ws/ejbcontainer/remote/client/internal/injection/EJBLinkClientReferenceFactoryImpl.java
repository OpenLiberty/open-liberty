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
package com.ibm.ws.ejbcontainer.remote.client.internal.injection;

import javax.naming.Reference;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.wsspi.injectionengine.factory.EJBLinkReferenceFactory;

@Component(service = EJBLinkReferenceFactory.class)
public class EJBLinkClientReferenceFactoryImpl implements EJBLinkReferenceFactory {

    private static final String FACTORY_CLASS_NAME = EJBLinkClientObjectFactoryImpl.class.getName();

    @org.osgi.service.component.annotations.Reference(service = LibertyProcess.class, target = "(wlp.process.type=client)")
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    @Override
    public Reference createEJBLinkReference(String refName,
                                            String application, String module, String component,
                                            String beanName,
                                            String beanInterface, String homeInterface,
                                            boolean localRef, boolean remoteRef) {
        EJBLinkClientInfo info = new EJBLinkClientInfo(
                        refName,
                        application, module, component,
                        beanName,
                        beanInterface, homeInterface,
                        localRef, remoteRef);

        EJBLinkClientInfoRefAddr refAddr = new EJBLinkClientInfoRefAddr(info);
        Reference ref = new Reference(beanInterface, refAddr, FACTORY_CLASS_NAME, null);
        return ref;
    }
}