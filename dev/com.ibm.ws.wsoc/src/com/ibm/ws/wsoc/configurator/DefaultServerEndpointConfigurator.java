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
package com.ibm.ws.wsoc.configurator;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.wsoc.ServiceManager;
import com.ibm.ws.wsoc.WebSocketContainerManager;
import com.ibm.ws.wsoc.injection.InjectionProvider;
import com.ibm.ws.wsoc.injection.InjectionProvider12;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.ReferenceContext;

/**
 *
 */
public class DefaultServerEndpointConfigurator extends ServerEndpointConfig.Configurator {

    private static final TraceComponent tc = Tr.register(DefaultServerEndpointConfigurator.class);

    public DefaultServerEndpointConfigurator() {

    }

    @Override
    public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {

        // We don't trim anything in here... since we know the code that calls us trims everyting up for us.

        String retValue = "";
        if ((requested != null) && (requested.size() > 0) && (supported != null) && (supported.size() > 0)) {
            for (String client : requested) {
                for (String local : supported) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "looking at local sub-protocol of: " + local + "  and client sub-protocol of: " + client);
                    }
                    if (client.equalsIgnoreCase(local)) {
                        retValue = local;
                        break;
                    }
                }
                if (!retValue.equals("")) {
                    break;
                }
            }
        }

        return retValue;
    }

    @Override
    public List<Extension> getNegotiatedExtensions(List<Extension> installed, List<Extension> requested) {

        List<Extension> acceptedExtensions = new ArrayList<Extension>(10);
        if ((requested != null) && (requested.size() > 0) && (installed != null) && (installed.size() > 0)) {
            for (Extension client : requested) {
                for (Extension local : installed) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "looking at local extension of: " + local.getName() + "  and client extension of: " + client.getName());
                    }

                    if (client.getName() != null && local.getName() != null) {
                        if (client.getName().equals(local.getName())) {
                            acceptedExtensions.add(client);
                        }
                    }
                }

            }
        }
        return acceptedExtensions;
    }

    @Override
    public boolean checkOrigin(String originHeaderValue) {
        return true;
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        // no-op
    }

    @FFDCIgnore({ InjectionException.class })
    private void attemptNonCDIInjection(final Object ep) {

        // TODO: can referenceContext be cached in a map?
        // ReferenceContext referenceContext = referenceContextMap.get(klass);

        // Injection pre-CDI injections if endpointed is coded for them
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd != null) {
            final ModuleMetaData mmd = cmd.getModuleMetaData();
            if (mmd != null) {
                final ReferenceContext referenceContext = ServiceManager.getInjectionServiceReferenceContext(mmd);
                if (referenceContext != null) {
                    try {
                        J2EEName j2eeName = mmd.getJ2EEName();

                        final ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(null, j2eeName);

                        AccessController.doPrivileged(
                                        new PrivilegedExceptionAction<Void>() {
                                            @Override
                                            public Void run() throws Exception {
                                                compNSConfig.setClassLoader(Thread.currentThread().getContextClassLoader());
                                                compNSConfig.setModuleMetaData(mmd);
                                                compNSConfig.setMetaDataComplete(false);
                                                compNSConfig.setInjectionClasses(Collections.<Class<?>> singletonList(ep.getClass()));
                                                referenceContext.processDynamic(compNSConfig);
                                                return null;
                                            }
                                        }
                                        );

                        InjectionTarget[] injectionTargets = referenceContext.getInjectionTargets(ep.getClass());
                        if (injectionTargets != null && injectionTargets.length != 0) {
                            for (InjectionTarget injectionTarget : injectionTargets) {
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "found injection target of:" + injectionTarget + "  for class: " + ep.getClass());
                                }
                                injectionTarget.inject(ep, null);
                            }
                        }
                    } catch (PrivilegedActionException pae) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "for class: " + ep.getClass() + " Non-CDI injection caught a possible InjectionException of: " + pae.getCause());
                        }
                    } catch (InjectionException e) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "for class: " + ep.getClass() + " Non-CDI injection caught an InjectionException of: " + e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        try {
            InjectionProvider12 ip12 = ServiceManager.getInjectionProvider12();
            if (ip12 != null) {

                ConcurrentHashMap map = WebSocketContainerManager.getRef().getEndpointMap();
                T ep = ip12.getManagedEndpointInstance(endpointClass, map);
                if (ep != null) {
                    return ep;
                }
            } else {
                InjectionProvider ip = ServiceManager.getInjectionProvider();
                if (ip != null) {

                    ConcurrentHashMap map = WebSocketContainerManager.getRef().getEndpointMap();
                    T ep = ip.getManagedEndpointInstance(endpointClass, map);
                    if (ep != null) {
                        return ep;
                    }
                }
            }

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not create the bean using the CDI service.  Will create the instance without CDI.");
            }

            T ep = endpointClass.newInstance();

            attemptNonCDIInjection(ep);

            return ep;

        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public void releaseCC(Object key) {

        InjectionProvider12 ip12 = ServiceManager.getInjectionProvider12();
        if (ip12 != null) {
            ConcurrentHashMap map = WebSocketContainerManager.getRef().getEndpointMap();
            ip12.releaseCC(key, map);

        } else {

            InjectionProvider ip = ServiceManager.getInjectionProvider();
            if (ip != null) {
                ConcurrentHashMap map = WebSocketContainerManager.getRef().getEndpointMap();
                ip.releaseCC(key, map);
            }
        }
    }

}
