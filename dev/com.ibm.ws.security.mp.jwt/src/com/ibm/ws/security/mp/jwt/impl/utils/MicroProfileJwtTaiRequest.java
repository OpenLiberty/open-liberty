/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.mp.jwt.impl.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.mp.jwt.MicroProfileJwtConfig;
import com.ibm.ws.security.mp.jwt.TraceConstants;
import com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException;
import com.ibm.ws.security.mp.jwt.tai.TAIRequestHelper;

/*
 * Store the data for a httpServletRequest session
 *
 * Initialize when a session starts and
 * discard after it ends
 */
public class MicroProfileJwtTaiRequest {
    private static TraceComponent tc = Tr.register(MicroProfileJwtTaiRequest.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    protected String providerName; // providerId

    protected HttpServletRequest request;

    protected List<MicroProfileJwtConfig> filteredConfigs = null;
    protected List<MicroProfileJwtConfig> genericConfigs = null;
    MicroProfileJwtConfig microProfileJwtConfig = null;
    MicroProfileJwtConfig jwtssoConfig = null;
    MpJwtProcessingException taiException = null;
    Map<String, String> mpConfigProps = new HashMap<String, String>();

    TAIRequestHelper taiRequestHelper = new TAIRequestHelper();

    /**
     * Called by TAI for now
     *
     * @param request2
     */
    public MicroProfileJwtTaiRequest(HttpServletRequest request) {
        this.request = request;
    }

    public void setMpConfigProps(Map<String, String> mpConfigProps) {
        if (mpConfigProps != null) {
            this.mpConfigProps = new HashMap<String, String>(mpConfigProps);
        }
    }

    public Map<String, String> getMpConfigProps() {
        return mpConfigProps;
    }

    /**
     * @param service
     */
    public void addFilteredConfig(MicroProfileJwtConfig mpJwtConfig) {
        if (mpJwtConfig != null) {
            if (this.filteredConfigs == null) {
                this.filteredConfigs = new ArrayList<MicroProfileJwtConfig>();
            }
            if (!this.filteredConfigs.contains(mpJwtConfig)) {
                this.filteredConfigs.add(mpJwtConfig);
            }
        }
    }

    /**
     * This is supposed to be called once in a request only.
     *
     * @param service
     */
    public void setSpecifiedConfig(MicroProfileJwtConfig mpJwtConfig) {
        this.microProfileJwtConfig = mpJwtConfig;
    }

    /**
     * @param service
     */
    public void addGenericConfig(MicroProfileJwtConfig mpJwtConfig) {
        String methodName = "addGenericConfig";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, mpJwtConfig);
        }
        if (mpJwtConfig != null) {
            if (this.genericConfigs == null) {
                this.genericConfigs = new ArrayList<MicroProfileJwtConfig>();
            }
            if (!this.genericConfigs.contains(mpJwtConfig)) {
                this.genericConfigs.add(mpJwtConfig);
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    public String getProviderName() {
        String methodName = "getProviderName";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        if (this.providerName == null) {
            if (this.microProfileJwtConfig != null) {
                this.providerName = this.microProfileJwtConfig.getUniqueId();
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, this.providerName);
        }
        return this.providerName;
    }

    public HttpServletRequest getRequest() {
        return this.request;
    }

    public List<MicroProfileJwtConfig> getFilteredConfigs() {
        return this.filteredConfigs;
    }

    public List<MicroProfileJwtConfig> getGenericConfigs() {
        return this.genericConfigs;
    }

    public Set<MicroProfileJwtConfig> getAllMatchingConfigs() {
        String methodName = "getAllMatchingConfigs";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        Set<MicroProfileJwtConfig> allConfigs = new HashSet<MicroProfileJwtConfig>();
        if (microProfileJwtConfig != null) {
            allConfigs.add(microProfileJwtConfig);
        }
        if (filteredConfigs != null) {
            allConfigs.addAll(filteredConfigs);
        }
        if (genericConfigs != null) {
            allConfigs.addAll(genericConfigs);
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, allConfigs);
        }
        return allConfigs;
    }

    public Set<String> getAllMatchingConfigIds() {
        String methodName = "getAllMatchingConfigIds";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        Set<String> allConfigIds = new HashSet<String>();
        Set<MicroProfileJwtConfig> allConfigs = getAllMatchingConfigs();
        for (MicroProfileJwtConfig config : allConfigs) {
            allConfigIds.add(config.getUniqueId());
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, allConfigIds);
        }
        return allConfigIds;
    }

    /**
     * @return
     */
    public MicroProfileJwtConfig getOnlyMatchingConfig() throws MpJwtProcessingException {
        String methodName = "getOnlyMatchingConfig";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        throwExceptionIfPresent();
        if (microProfileJwtConfig == null) {
            microProfileJwtConfig = findAppropriateGenericConfig();
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, microProfileJwtConfig);
        }
        return microProfileJwtConfig;
    }

    void throwExceptionIfPresent() throws MpJwtProcessingException {
        if (taiException != null) {
            MpJwtProcessingException exception = this.taiException;
            this.taiException = null;
            throw exception;
        }
    }

    MicroProfileJwtConfig findAppropriateGenericConfig() throws MpJwtProcessingException {
        String methodName = "findAppropriateGenericConfig";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        if (this.filteredConfigs != null) {
            if (this.filteredConfigs.size() == 1) {
                this.microProfileJwtConfig = this.filteredConfigs.get(0);
            } else {
                // error handling -- multiple mpJwtConfig qualified and we do not know how to select
                String configIds = getConfigIds(filteredConfigs);
                String msg = Tr.formatMessage(tc, "TOO_MANY_MP_JWT_PROVIDERS", new Object[] { configIds });
                Tr.error(tc, msg);
                throw new MpJwtProcessingException(msg);
            }
        } else if (this.genericConfigs != null) {
            if (this.genericConfigs.size() < 2) {
                this.microProfileJwtConfig = this.genericConfigs.get(0);
            } else if (this.genericConfigs.size() <= 3) {
                if (!handleMultipleConfigurations()) {
                    // if we have two jwtsso or two mpjwt configurations, then we cannot process
                    handleTooManyConfigurations();
                }

            } else {
                handleTooManyConfigurations();
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, this.microProfileJwtConfig);
        }
        return this.microProfileJwtConfig;
    }

    /**
     *
     */
    private boolean handleTwoConfigurations() {

        Iterator it = this.genericConfigs.iterator();
        boolean jwtsso = false;
        boolean mpjwt = false;
        boolean defaultmpjwt = true;

        while (it.hasNext()) {
            MicroProfileJwtConfig mpJwtConfig = (MicroProfileJwtConfig) it.next();
            if (taiRequestHelper.isJwtSsoFeatureActive(mpJwtConfig)) {
                jwtsso = true;
                this.microProfileJwtConfig = mpJwtConfig;
            } else {
                mpjwt = true;
            }
        }
        return jwtsso && mpjwt;
    }

    private boolean handleMultipleConfigurations() {

        Iterator it = this.genericConfigs.iterator();
        int jwtsso = 0;
        int mpjwt = 0;

        MicroProfileJwtConfig mpjwtConfiguration = null;

        while (it.hasNext()) {
            MicroProfileJwtConfig mpJwtConfig = (MicroProfileJwtConfig) it.next();
            if (taiRequestHelper.isJwtSsoFeatureActive(mpJwtConfig)) {
                jwtsso++;
                this.microProfileJwtConfig = mpJwtConfig;
                this.jwtssoConfig = mpJwtConfig;
            } else if (!taiRequestHelper.isMpJwtDefaultConfig(mpJwtConfig)) {
                mpjwt++;
                mpjwtConfiguration = mpJwtConfig;
            }
        }
        if (jwtsso <= 1 && mpjwt == 1) {
            this.microProfileJwtConfig = mpjwtConfiguration;
        }
        return jwtsso <= 1 && mpjwt <= 1;
    }

    public MicroProfileJwtConfig getJwtSsoConfig() {
        return this.jwtssoConfig;
    }

    void handleTooManyConfigurations() throws MpJwtProcessingException {
        // error handling -- multiple mpJwtConfig qualified and we do not know how to select
        String configIds = getConfigIds(genericConfigs);
        String msg = Tr.formatMessage(tc, "TOO_MANY_MP_JWT_PROVIDERS", new Object[] { configIds });
        Tr.error(tc, msg);
        throw new MpJwtProcessingException(msg);
    }

    String getConfigIds(List<MicroProfileJwtConfig> multiConfigs) {
        String result = "";
        if (multiConfigs == null) {
            return result;
        }
        boolean bInit = true;
        for (MicroProfileJwtConfig mpJwtConfig : multiConfigs) {
            if (!bInit) {
                result = result.concat(", ");
            } else {
                bInit = false;
            }
            result = result.concat(mpJwtConfig.getUniqueId());
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Micro Profile Jwt TaiRequest [provider:").append(getProviderName()).append(" request:").append(this.request).append("]");
        return sb.toString();
    }

    /**
     * @return
     */
    public boolean hasServices() {
        String methodName = "hasServices";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        boolean hasServices = (this.genericConfigs != null || this.microProfileJwtConfig != null);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, hasServices);
        }
        return hasServices;
    }

    public void setTaiException(MpJwtProcessingException taiException) {
        this.taiException = taiException;
    }

}
