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

import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.PortInfo;

import com.ibm.ws.jaxws.Destroyable;
import com.ibm.ws.jaxws.metadata.HandlerChainInfo;
import com.ibm.ws.jaxws.metadata.HandlerChainsInfo;
import com.ibm.ws.jaxws.metadata.HandlerInfo;
import com.ibm.ws.jaxws.metadata.JaxWsClientMetaData;
import com.ibm.ws.jaxws.metadata.WebServiceRefInfo;
import com.ibm.ws.jaxws.metadata.builder.HandlerChainInfoBuilder;
import com.ibm.ws.jaxws.support.JaxWsInstanceManager.InterceptException;
import com.ibm.ws.jaxws.utils.JaxWsUtils;

/**
 * The implement of HandlerResolver. Used in serviceRefObjectFactory when create service proxy for client.
 */
public class JaxWsClientHandlerResolver implements HandlerResolver, Destroyable {
    @SuppressWarnings({ "rawtypes" })
    private final Map<PortInfo, List<Handler>> handlersMap = new HashMap<PortInfo, List<Handler>>();

    private final WebServiceRefInfo wsrInfo;

    private final ClassLoader classLoader;

    private final HandlerChainInfoBuilder handlerChainBuilder;

    private final JaxWsClientMetaData clientMetaData;

    /**
     * @param wsrInfo
     * @param bus
     */
    public JaxWsClientHandlerResolver(WebServiceRefInfo wsrInfo, JaxWsClientMetaData clientMetaData) {
        super();
        this.wsrInfo = wsrInfo;
        this.clientMetaData = clientMetaData;
        this.classLoader = clientMetaData.getModuleMetaData().getAppContextClassLoader();
        this.handlerChainBuilder = new HandlerChainInfoBuilder(classLoader);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.handler.HandlerResolver#getHandlerChain(javax.xml.ws.handler.PortInfo)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public List<Handler> getHandlerChain(PortInfo paramPortInfo) {
        List<Handler> handlers = handlersMap.get(paramPortInfo);

        if (handlers == null) {
            handlers = createHandlerFromHandlerInfo(paramPortInfo);
            handlersMap.put(paramPortInfo, handlers);
        }
//        return Collections.unmodifiableList(handlers);
        return handlers;
    }

    @SuppressWarnings("rawtypes")
    protected List<Handler> createHandlerFromHandlerInfo(PortInfo paramPortInfo) {
        /*
         * According to JSR109 6.2.2.3 if XML descriptor exists,
         * should ignore the @HandlerChain's configuration
         */
        List<Handler> handlers = new ArrayList<Handler>();
        if (wsrInfo.getHandlersFromXML()) {
            // Get from HandlerChains
            for (HandlerChainInfo hcInfo : wsrInfo.getHandlerChains()) {
                if (!JaxWsUtils.matchesQName(hcInfo.getServiceNamePattern(), paramPortInfo.getServiceName())
                    || !JaxWsUtils.matchesQName(hcInfo.getPortNamePattern(), paramPortInfo.getPortName())) {
                    continue;
                }

                boolean include = false;
                for (String binding : hcInfo.getProtocolBindings()) {
                    if (JaxWsUtils.singleProtocolMatches(binding, paramPortInfo.getBindingID())) {
                        include = true;
                        break;
                    }
                }
                if (!include && !hcInfo.getProtocolBindings().isEmpty()) {
                    continue;
                }
                // Process handlerChain Info
                for (HandlerInfo hInfo : hcInfo.getHandlerInfos()) {
                    Handler handler = constructInstance(hInfo);
                    if (handler != null) {
                        handlers.add(handler);
                    }
                }
            }
        } else {
            if (wsrInfo.getHandlerChainAnnotation() != null) {

                HandlerChainsInfo hChainsInfo = handlerChainBuilder.buildHandlerChainsInfoFromAnnotation(wsrInfo.getHandlerChainDeclaringClassName(),
                                                                                                         wsrInfo.getHandlerChainAnnotation(), paramPortInfo.getPortName(),
                                                                                                         paramPortInfo.getServiceName(), paramPortInfo.getBindingID());
                List<HandlerInfo> hInfos = hChainsInfo.getAllHandlerInfos();
                for (HandlerInfo hInfo : hInfos) {
                    Handler handler = constructInstance(hInfo);
                    if (handler != null) {
                        handlers.add(handler);
                    }
                }
            }
        }

        return HandlerChainInfoBuilder.sortHandlers(handlers);
    }

    @SuppressWarnings("rawtypes")
    private Handler constructInstance(HandlerInfo handlerInfo) {
        try {
            return (Handler) clientMetaData.getModuleMetaData()
                            .getJaxWsInstanceManager().createInstance(handlerInfo.getHandlerClass(),
                                                                      new JaxWsHandlerChainInstanceInterceptor(clientMetaData.getClientBus(), handlerInfo));
        } catch (InstantiationException e) {
            throw new WebServiceException(e);
        } catch (IllegalAccessException e) {
            throw new WebServiceException(e);
        } catch (ClassNotFoundException e) {
            throw new WebServiceException(e);
        } catch (InterceptException e) {
            throw new WebServiceException(e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jaxws.support.Destroyable#destroy()
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void destroy() {
        for (List<Handler> handlers : this.handlersMap.values()) {
            for (Handler handler : handlers) {
                try {
                    clientMetaData.getModuleMetaData().getJaxWsInstanceManager().destroyInstance(handler);
                } catch (InterceptException e) {
                    throw new WebServiceException(e);
                }
            }
        }
    }
}
