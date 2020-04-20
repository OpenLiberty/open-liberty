/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.tai.internal;

import java.util.HashMap;
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
 * 1) One or more shared library interceptors.
 * 2) One or more user feature interceptors.
 * 3) One or more shared library interceptors and one or more user feature interceptors.
 */
public class TAIServiceImpl implements TAIService {
    private static final TraceComponent tc = Tr.register(TAIServiceImpl.class);

    private static final String KEY_INTERCEPTOR_SERVICE = "interceptorService";
    private static final String KEY_ID = "id";
    private final ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef = new ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor>(KEY_INTERCEPTOR_SERVICE);
    private TAIConfigImpl taiConfig = null;

    //Map of TrustAssociationInterceptor - by TAI id
    //Order matters here: use a LinkedHashMap to preserve order across platforms
    private final Map<String, TrustAssociationInterceptor> invokeBeforeSSOTais = new LinkedHashMap<String, TrustAssociationInterceptor>();
    private final Map<String, TrustAssociationInterceptor> invokeAfterSSOTais = new LinkedHashMap<String, TrustAssociationInterceptor>();
    private final Map<String, Boolean> disableLtpaCookieTais = new HashMap<String, Boolean>();

    private final Set<String> orderedOfInterceptorIds = new TreeSet<String>();

    protected synchronized void setInterceptorService(ServiceReference<TrustAssociationInterceptor> ref) {
        String id = getComponentId(ref);
        orderedOfInterceptorIds.add(id);
        interceptorServiceRef.putReference(id, ref);
        initTAIs(id);
    }

    protected synchronized void unsetInterceptorService(ServiceReference<TrustAssociationInterceptor> ref) {
        String id = getComponentId(ref);
        orderedOfInterceptorIds.remove(id);
        interceptorServiceRef.removeReference(id, ref);
        clearTaiMaps(id);
    }

    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        interceptorServiceRef.activate(cc);
        taiConfig = new TAIConfigImpl(props);
    }

    protected synchronized void modified(Map<String, Object> props) {
        taiConfig = new TAIConfigImpl(props);
        clearTaiMaps();
        initTAIs();
    }

    protected synchronized void deactivate(ComponentContext cc) {
        interceptorServiceRef.deactivate(cc);
        clearTaiMaps();
    }

    void initTAIs() {
        for (String id : orderedOfInterceptorIds) {
            initTAIs(id);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "invokeBeforeSSOTais " + invokeBeforeSSOTais.toString());
            Tr.debug(tc, "invokeAfterSSOTais " + invokeAfterSSOTais.toString());
            Tr.debug(tc, "disableLtpaCookieTais " + disableLtpaCookieTais.toString());
        }
    }

    /**
     * @param id
     */
    private void initTAIs(String id) {
        Object interceptor = interceptorServiceRef.getService(id);
        if (interceptor instanceof InterceptorConfigImpl) {
            processSharedLibTAI(id, interceptor);

        } else if (interceptor instanceof TrustAssociationInterceptor) {
            processUserFeatureTAI(id, interceptor);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "invokeBeforeSSOTais " + invokeBeforeSSOTais.toString());
            Tr.debug(tc, "invokeAfterSSOTais " + invokeAfterSSOTais.toString());
            Tr.debug(tc, "disableLtpaCookieTais " + disableLtpaCookieTais.toString());
        }
    }

    /**
     * @param id
     * @param interceptor
     */
    private void processSharedLibTAI(String id, Object interceptor) {
        InterceptorConfigImpl itcConf = (InterceptorConfigImpl) interceptor;
        TrustAssociationInterceptor tai = itcConf.getInterceptorInstance(this);

        if (itcConf.isInvokeBeforeSSO()) {
            invokeBeforeSSOTais.put(id, tai);
        }
        if (itcConf.isInvokeAfterSSO()) {
            invokeAfterSSOTais.put(id, tai);
        }
        if (itcConf.isDisableLtpaCookie()) {
            disableLtpaCookieTais.put(id, true);
        }
    }

    /**
     * @param interceptorId
     * @param itc
     */
    private void processUserFeatureTAI(String interceptorId, Object interceptor) {
        TAIUtil taiUtil = new TAIUtil(interceptorServiceRef, interceptorId);
        //TrustAssociationInterceptor tai = (TrustAssociationInterceptor) interceptor;
        //taiUtil.processProps(interceptorServiceRef, interceptorId);

        if (taiUtil.isInvokeBeforeSSO()) {
            invokeBeforeSSOTais.put(interceptorId, (TrustAssociationInterceptor) interceptor);
        }
        if (taiUtil.isInvokeAfterSSO()) {
            invokeAfterSSOTais.put(interceptorId, (TrustAssociationInterceptor) interceptor);
        }
        if (taiUtil.isDisableLtpaCookie()) {
            disableLtpaCookieTais.put(interceptorId, taiUtil.isDisableLtpaCookie());
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

    /*
     * The disableLtpaCookie attribute can be specified in trustAssociation and interceptors element or user feature TAI properties
     * The disableLtpaCookie in the interceptors element will overwrite the one in the trustAssociation element.
     */
    @Override
    public boolean isDisableLtpaCookie(String taiId) {
        if (disableLtpaCookieTais.get(taiId) != null) {
            return disableLtpaCookieTais.get(taiId);
        }

        return taiConfig.isDisableLtpaCookie();
    }

    /*
     * Clear all TAI maps for invokeBeforeSSO, invokeAfterSSO and disableLtapCookie
     */
    private void clearTaiMaps() {
        invokeBeforeSSOTais.clear();
        invokeAfterSSOTais.clear();
        disableLtpaCookieTais.clear();
    }

    /*
     * Clear a TAI entry map for invokeBeforeSSO, invokeAfterSSO and disableLtapCookie
     */
    private void clearTaiMaps(String entry) {
        if (invokeBeforeSSOTais.get(entry) != null)
            invokeBeforeSSOTais.remove(entry);
        if (invokeAfterSSOTais.get(entry) != null)
            invokeAfterSSOTais.remove(entry);
        if (disableLtpaCookieTais.get(entry) != null)
            disableLtpaCookieTais.remove(entry);
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
