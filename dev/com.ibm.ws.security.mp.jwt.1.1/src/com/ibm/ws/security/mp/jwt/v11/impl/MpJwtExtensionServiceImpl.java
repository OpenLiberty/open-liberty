/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.v11.impl;

import java.util.Map;
import java.util.NoSuchElementException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.mp.jwt.MpJwtExtensionService;
import com.ibm.ws.security.mp.jwt.v11.MpConfigProxyService;
import com.ibm.ws.security.mp.jwt.v11.TraceConstants;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = MpJwtExtensionService.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = {"service.vendor=IBM", "version=1.1"}, name = "mpJwtExtensionService")
public class MpJwtExtensionServiceImpl implements MpJwtExtensionService {

    public static final TraceComponent tc = Tr.register(MpJwtExtensionServiceImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    public static final String KEY_MP_CONFIG_PROXY_SERVICE = "mpConfigProxyService";

    static private String MP_VERSION = "1.1";
    private final String uniqueId = "MpConfigProxyService";
    static final AtomicServiceReference<MpConfigProxyService> mpConfigProxyServiceRef = new AtomicServiceReference<MpConfigProxyService>(KEY_MP_CONFIG_PROXY_SERVICE);
    static private boolean isMpConfigWarningLogged = false;

    @Reference(service = MpConfigProxyService.class, name = KEY_MP_CONFIG_PROXY_SERVICE, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setMpConfigProxyService(ServiceReference<MpConfigProxyService> reference) {
        mpConfigProxyServiceRef.setReference(reference);
    }

    protected void unsetMpConfigProxyService(ServiceReference<MpConfigProxyService> reference) {
        mpConfigProxyServiceRef.unsetReference(reference);
    }


    @Activate
    protected void activate(ComponentContext cc) {
        Tr.info(tc, "MPJWT_11_CONFIG_PROCESSED", uniqueId);
        mpConfigProxyServiceRef.activate(cc);
    }

    @Modified
    protected void modified(ComponentContext cc) {
        Tr.info(tc, "MPJWT_11_CONFIG_MODIFIED", uniqueId);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        Tr.info(tc, "MPJWT_11_CONFIG_DEACTIVATED", uniqueId);
        mpConfigProxyServiceRef.deactivate(cc);
    }

    /**
     * @return
     */
    public String getVersion(){
        return MP_VERSION;
    }

    /**
     * @return
     */
    public boolean isMpConfigAvailable() {
        if (mpConfigProxyServiceRef.getService() != null) {
            return true;
        } else {
            if (!isMpConfigWarningLogged) {
                Tr.warning(tc, "MPJWT_11_NO_MP_CONFIG");
                isMpConfigWarningLogged = true;
            }
            return false;
        }
    }

    /**
     * @return
     */
    public <T> T getConfigValue(ClassLoader cl, String propertyName, Class<T> propertyType) throws IllegalArgumentException, NoSuchElementException {
        MpConfigProxyService ps = mpConfigProxyServiceRef.getService();
        if (ps != null) {
            return ps.getConfigValue(cl, propertyName, propertyType);
        } else {
            if (!isMpConfigWarningLogged) {
                Tr.warning(tc, "MPJWT_11_NO_MP_CONFIG");
                isMpConfigWarningLogged = true;
            }
            throw new IllegalStateException("mpConfigProxyService is not available.");
        }
    }
}
