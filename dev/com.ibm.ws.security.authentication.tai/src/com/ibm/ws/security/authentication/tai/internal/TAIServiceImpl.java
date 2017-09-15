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
package com.ibm.ws.security.authentication.tai.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.ws.security.authentication.tai.TAIUtil;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

/**
 * TAI service handle the following scenarios:
 * 1)One or more custom interceptors are using shared library
 * 2)One or more custom interceptors using shared library and one or more custom interceptor user features
 * 3)One or more interceptor user features.
 */
public class TAIServiceImpl implements TAIService {
    private static final TraceComponent tc = Tr.register(TAIServiceImpl.class);

    private static final String KEY_INTERCEPTOR_SERVICE = "interceptorService";
    private static final String KEY_ID = "id";
    private final ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef = new ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor>(
                    KEY_INTERCEPTOR_SERVICE);
    private TAIConfigImpl taiConfig = null;

    //Map of TrustAssociationInterceptor - by TAI id 
    //Order matters here: use a LinkedHashMap to preserve order across platforms
    private final Map<String, TrustAssociationInterceptor> invokeBeforeSSOTais = new LinkedHashMap<String, TrustAssociationInterceptor>();
    private final Map<String, TrustAssociationInterceptor> invokeAfterSSOTais = new LinkedHashMap<String, TrustAssociationInterceptor>();

    private final Set<String> orderedInterceptorIds = new TreeSet<String>();

    protected synchronized void setInterceptorService(ServiceReference<TrustAssociationInterceptor> ref) {
        String id = getComponentId(ref);
        orderedInterceptorIds.add(id);
        interceptorServiceRef.putReference(id, ref);
        initialize();
    }

    protected synchronized void unsetInterceptorService(ServiceReference<TrustAssociationInterceptor> ref) {
        String id = getComponentId(ref);
        orderedInterceptorIds.remove(id);
        interceptorServiceRef.removeReference(id, ref);
        initialize();
    }

    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        interceptorServiceRef.activate(cc);
        modified(props);
    }

    protected synchronized void modified(Map<String, Object> props) {
        taiConfig = new TAIConfigImpl(props);
        initialize();
    }

    protected synchronized void deactivate(ComponentContext cc) {
        interceptorServiceRef.deactivate(cc);
        clearInvokeBeforeAndAfterSSO();
    }

    void initialize() {
        clearInvokeBeforeAndAfterSSO();

        if (interceptorServiceRef.isEmpty() && interceptorServiceRef.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There is no TAI config or enabled");
            }
            return;
        }
        TrustAssociationInterceptor tai = null;
        TAIUtil taiUtil = new TAIUtil();
        for (String interceptorId : orderedInterceptorIds) {
            Object itc = interceptorServiceRef.getService(interceptorId);
            if (itc instanceof InterceptorConfigImpl) {
                InterceptorConfigImpl interceptor = (InterceptorConfigImpl) itc;
                tai = interceptor.getInterceptorInstance(this);
                if (interceptor.isInvokeBeforeSSO()) {
                    invokeBeforeSSOTais.put(interceptorId, tai);
                }
                if (interceptor.isInvokeAfterSSO()) {
                    invokeAfterSSOTais.put(interceptor.getId(), tai);
                }
            } else if (itc instanceof TrustAssociationInterceptor) {
                tai = (TrustAssociationInterceptor) itc;
                taiUtil.processTAIUserFeatureProps(interceptorServiceRef, interceptorId);
                if (taiUtil.isInvokeBeforeSSO()) {
                    invokeBeforeSSOTais.put(interceptorId, tai);
                }

                if (taiUtil.isInvokeAfterSSO()) {
                    invokeAfterSSOTais.put(interceptorId, tai);
                }
            }
        }
    }

    @Override
    public Map<String, TrustAssociationInterceptor> getTais(boolean invokeBeforeSSO) {
        if (invokeBeforeSSO) {
            return invokeBeforeSSOTais;
        } else {
            return invokeAfterSSOTais;
        }
    }

    @Override
    public boolean isInvokeForUnprotectedURI() {
        return taiConfig.isInvokeForUnprotectedURI();
    }

    @Override
    public boolean isInvokeForFormLogin() {
        return taiConfig.isInvokeForFormLogin();
    }

    @Override
    public boolean isFailOverToAppAuthType() {
        return taiConfig.isFailOverToAppAuthType();
    }

    private void clearInvokeBeforeAndAfterSSO() {
        invokeBeforeSSOTais.clear();
        invokeAfterSSOTais.clear();
    }

    private String getComponentId(ServiceReference<TrustAssociationInterceptor> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        if (id == null) {
            id = (String) ref.getProperty("component.name");
            if (id == null) {
                id = (String) ref.getProperty("component.id");
            }
        }
        return id;
    }
}
