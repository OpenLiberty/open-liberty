/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.services;

import java.lang.annotation.Annotation;

import javax.annotation.Resource;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.wsspi.injectionengine.ObjectFactoryInfo;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.uow.UOWManager;

@Component(service = { ObjectFactoryInfo.class })
public class UOWManagerObjectFactoryInfo extends ObjectFactoryInfo {

    private boolean isClient;

    @Activate
    protected void activate(BundleContext context) {
        isClient = WsLocationConstants.LOC_PROCESS_TYPE_CLIENT.equals(context.getProperty(WsLocationConstants.LOC_PROCESS_TYPE));
    }

    @Deactivate
    protected void deactivate(BundleContext context) {
        isClient = false;
    }

    @Override
    public Class<? extends Annotation> getAnnotationClass() {
        return Resource.class;
    }

    @Override
    public Class<?> getType() {
        return UOWManager.class;
    }

    @Override
    public boolean isOverrideAllowed() {
        return false;
    }

    @Override
    public Class<? extends ObjectFactory> getObjectFactoryClass() {
        if (isClient) {
            return ClientUOWManagerObjectFactory.class;
        } else {
            return UOWManagerObjectFactory.class;
        }
    }

}
