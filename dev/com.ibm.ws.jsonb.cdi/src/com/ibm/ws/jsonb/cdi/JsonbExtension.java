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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.json.bind.Jsonb;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

@Component(property = { "api.classes=javax.json.bind.Jsonb;com.ibm.ws.jsonb.cdi.JsonbBean" }, immediate = true)
public class JsonbExtension implements Extension, WebSphereCDIExtension {

    private static final TraceComponent tc = Tr.register(JsonbExtension.class);

    @Reference
    protected JsonbProvider jsonbProvider;

    @Reference
    protected JsonProvider jsonpProvider;

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscoveryEvent, BeanManager beanManager) {
        afterBeanDiscoveryEvent.addBean(new JsonbBean());
    }

    public static Jsonb create(CreationalContext<Jsonb> ctx) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Creating a JSON-B instance");
        BundleContext bctx = FrameworkUtil.getBundle(JsonbExtension.class).getBundleContext();
        JsonbProvider jsonbProvider = bctx.getService(bctx.getServiceReference(JsonbProvider.class));
        JsonProvider jsonpProvider = bctx.getService(bctx.getServiceReference(JsonProvider.class));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Using providers: ", jsonbProvider.getClass(), jsonpProvider.getClass());
        return jsonbProvider.create().withProvider(jsonpProvider).build();
    }
}
