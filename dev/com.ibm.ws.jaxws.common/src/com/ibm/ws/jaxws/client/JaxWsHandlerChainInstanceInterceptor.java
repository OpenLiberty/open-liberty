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
package com.ibm.ws.jaxws.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.handler.Handler;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.ResourceInjector;
import org.apache.cxf.jaxws.handler.InitParamResourceResolver;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;

import com.ibm.ws.jaxws.metadata.HandlerInfo;
import com.ibm.ws.jaxws.metadata.ParamValueInfo;
import com.ibm.ws.jaxws.support.JaxWsInstanceManager.InstanceInterceptor;
import com.ibm.ws.jaxws.support.JaxWsInstanceManager.InterceptException;
import com.ibm.ws.jaxws.support.JaxWsInstanceManager.InterceptorContext;

/**
 *
 */
public class JaxWsHandlerChainInstanceInterceptor implements InstanceInterceptor {

    private final Bus bus;
    private final HandlerInfo handlerInfo;

    public JaxWsHandlerChainInstanceInterceptor(Bus bus, HandlerInfo handlerInfo) {
        this.bus = bus;
        this.handlerInfo = handlerInfo;
    }

    @Override
    public void postNewInstance(InterceptorContext ctx) throws InterceptException {
        configureHandler((Handler<?>) ctx.getInstance());
    }

    @Override
    public void postInjectInstance(InterceptorContext ctx) {

    }

    @Override
    public void preDestroyInstance(InterceptorContext ctx) throws InterceptException {

    }

    private void configureHandler(Handler<?> handler) {

        if (handlerInfo.getInitParam().size() == 0) {
            return;
        }

        Map<String, String> params = new HashMap<String, String>();

        for (ParamValueInfo param : handlerInfo.getInitParam()) {
            params.put(trimString(param.getParamName() == null ? null : param.getParamName()),
                       trimString(param.getParamValue() == null ? null : param.getParamValue()));
        }

        initializeViaInjection(handler, params);
    }

    private void initializeViaInjection(Handler<?> handler, final Map<String, String> params) {
        if (bus != null) {
            ResourceManager resMgr = bus.getExtension(ResourceManager.class);
            List<ResourceResolver> resolvers = new ArrayList<ResourceResolver>(resMgr.getResourceResolvers());
            resolvers.add(new InitParamResourceResolver(params));
            ResourceInjector resInj = new ResourceInjector(resMgr, resolvers);
            resInj.inject(handler);
        }
    }

    private String trimString(String str) {
        return str != null ? str.trim() : null;
    }
}