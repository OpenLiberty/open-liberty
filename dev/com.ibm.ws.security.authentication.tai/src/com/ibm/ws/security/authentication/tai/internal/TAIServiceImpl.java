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
 * 1) One or more shared library interceptors with TAI configured in server.xml
 * 2) One or more user feature interceptors with TAI configured in server.xml
 * 3) One or more shared library interceptors and one or more user feature interceptors with TAI configured in server.xml.
 *
 * The TAIServiceImpl service will not be available if there is no trustAssociation configuration in server.xml file
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

    private final Set<String> orderOfInterceptorIds = new TreeSet<String>();

    protected synchronized void setInterceptorService(ServiceReference<TrustAssociationInterceptor> ref) {
        String id = getComponentId(ref);
        orderOfInterceptorIds.add(id);
        interceptorServiceRef.putReference(id, ref);
        initTAI(id);
    }

    protected synchronized void unsetInterceptorService(ServiceReference<TrustAssociationInterceptor> ref) {
        String id = getComponentId(ref);
        orderOfInterceptorIds.remove(id);
        interceptorServiceRef.removeReference(id, ref);
        clearTaiMaps(id);
    }

    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        interceptorServiceRef.activate(cc);
        taiConfig = new TAIConfigImpl(props);
    }

    protected synchronized void modified(Map<String, Object> props) {
        taiConfig = new TAIConfigImpl(props);
    }

    protected synchronized void deactivate(ComponentContext cc) {
        interceptorServiceRef.deactivate(cc);
        orderOfInterceptorIds.clear();
        clearTaiMaps();
    }

    void initAllTAIs(String newTaiId) {
        if (newTaiId != null) {
            orderOfInterceptorIds.add(newTaiId);
        }
        clearTaiMaps();
        for (String id : orderOfInterceptorIds) {
            initTAI(id);
        }
    }

    /**
     * @param id
     */
    void initTAI(String id) {
        Object interceptor = interceptorServiceRef.getService(id);
        if (interceptor != null) {
            if (interceptor instanceof InterceptorConfigImpl) {
                processSharedLibrary(id, (InterceptorConfigImpl) interceptor);
            } else if (interceptor instanceof TrustAssociationInterceptor) {
                processUserFeature(id, (TrustAssociationInterceptor) interceptor);
            }
            printTaiMap();
        } else {
            clearTaiMaps(id);
        }
    }

    private void printTaiMap() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "invokeBeforeSSOTais " + invokeBeforeSSOTais.toString());
            Tr.debug(tc, "invokeAfterSSOTais  " + invokeAfterSSOTais.toString());
            Tr.debug(tc, "disableLtpaCookieTais " + disableLtpaCookieTais.toString());
        }
    }

    /**
     * @param id
     * @param itcConf
     */
    private void processSharedLibrary(String id, InterceptorConfigImpl itcConf) {
        TrustAssociationInterceptor tai = itcConf.getInterceptorInstance(this);
        updateTaiMaps(id, tai, itcConf.isInvokeBeforeSSO(), itcConf.isInvokeAfterSSO(), null);
    }

    /**
     * @param id
     * @param iterceptor
     */
    private void processUserFeature(String id, TrustAssociationInterceptor tai) {
        TAIUtil taiUtil = new TAIUtil(interceptorServiceRef, id);
        updateTaiMaps(id, tai, taiUtil.isInvokeBeforeSSO(), taiUtil.isInvokeAfterSSO(), taiUtil.isDisableLtpaCookie());
    }

    /**
     * @param id
     * @param tai
     * @param beforeSso
     * @param afterSso
     * @param disableLtpaCookie
     */
    private void updateTaiMaps(String id, TrustAssociationInterceptor tai, boolean beforeSso, boolean afterSso, Object disableLtpaCookie) {
        if (beforeSso) {
            invokeBeforeSSOTais.put(id, tai);
        }
        if (afterSso) {
            invokeAfterSSOTais.put(id, tai);
        }
        if (disableLtpaCookie != null)
            disableLtpaCookieTais.put(id, ((Boolean) disableLtpaCookie).booleanValue());
    }

    @Override
    public Map<String, TrustAssociationInterceptor> getTais(boolean beforeSso) {
        if (beforeSso) {
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
        invokeBeforeSSOTais.remove(entry);
        invokeAfterSSOTais.remove(entry);
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
