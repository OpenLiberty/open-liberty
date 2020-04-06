/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.ws.handler.Handler;

import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.endpoint.Server;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.client.JaxWsHandlerChainInstanceInterceptor;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.HandlerInfo;
import com.ibm.ws.jaxws.metadata.builder.HandlerChainInfoBuilder;
import com.ibm.ws.jaxws.support.JaxWsInstanceManager.InterceptException;

/**
 * Extend the org.apache.cxf.jaxws.JaxWsServerFactoryBean;
 * so that we can create handlers by ourselves.
 */
public class LibertyJaxWsServerFactoryBean extends JaxWsServerFactoryBean {

    private static final TraceComponent tc = Tr.register(LibertyJaxWsServerFactoryBean.class);

    /**
     * Need to set doInit false to disable buildHandlerChain and inject resources
     */
    public LibertyJaxWsServerFactoryBean(JaxWsServiceFactoryBean serviceFactoryBean) {
        super(serviceFactoryBean);
        this.doInit = false;
    }

    @SuppressWarnings("rawtypes")
    public List<Handler> createHandlers(EndpointInfo edpInfo, JaxWsInstanceManager instanceMgr) {
        List<HandlerInfo> handlerInfos = edpInfo.getHandlerChainsInfo().getAllHandlerInfos();

        Iterator<HandlerInfo> handlerIter = handlerInfos.iterator();
        List<Handler> handlers = new ArrayList<Handler>(handlerInfos.size());

        while (handlerIter.hasNext()) {
            final HandlerInfo hInfo = handlerIter.next();

            Handler handler = null;
            try {
                handler = (Handler) instanceMgr.createInstance(hInfo.getHandlerClass(), new JaxWsHandlerChainInstanceInterceptor(this.bus, hInfo));
            } catch (InstantiationException e) {
                Tr.warning(tc, "warn.could.not.create.handler", e.getMessage());
            } catch (IllegalAccessException e) {
                Tr.warning(tc, "warn.could.not.create.handler", e.getMessage());
            } catch (ClassNotFoundException e) {
                Tr.warning(tc, "warn.could.not.create.handler", e.getMessage());
            } catch (InterceptException e) {
                Tr.warning(tc, "warn.could.not.create.handler", e.getMessage());
            }

            if (handler != null) {
                handlers.add(handler);
            }
        }
        return HandlerChainInfoBuilder.sortHandlers(handlers);
    }

}
