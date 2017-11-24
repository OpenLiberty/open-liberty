/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsonb.cdi;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

@Component(service = { WebSphereCDIExtension.class },
           property = { "api.classes=javax.json.bind.Jsonb;com.ibm.ws.jsonb.cdi.JsonbBean" },
           immediate = true)
public class JsonbExtension implements Extension, WebSphereCDIExtension {

    @Reference
    protected JsonbProvider jsonbProvider;

    @Reference
    protected JsonProvider jsonpProvider;

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscoveryEvent, BeanManager beanManager) {
        // Need to get these refs out of the svc registry because the object instance that invokes
        // this method will not be the same object instance that has DS refs injected into it
        BundleContext bctx = FrameworkUtil.getBundle(JsonbExtension.class).getBundleContext();
        JsonbProvider jsonbProvider = bctx.getService(bctx.getServiceReference(JsonbProvider.class));
        JsonProvider jsonpProvider = bctx.getService(bctx.getServiceReference(JsonProvider.class));

        afterBeanDiscoveryEvent.addBean(new JsonbBean(jsonbProvider, jsonpProvider));
    }
}
