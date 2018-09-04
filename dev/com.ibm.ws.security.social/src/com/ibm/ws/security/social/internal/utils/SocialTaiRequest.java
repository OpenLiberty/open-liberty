/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;

/*
 * Store the data for a httpServletRequest session
 *
 * Initialize when a session starts and
 * discard after it ends
 */
public class SocialTaiRequest {
    private static TraceComponent tc = Tr.register(SocialTaiRequest.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    protected String providerName; // providerId

    protected HttpServletRequest request;

    protected List<SocialLoginConfig> filteredConfigs = null;
    protected List<SocialLoginConfig> genericConfigs = null;
    SocialLoginConfig socialLoginConfig = null;
    SocialLoginException taiException = null;

    /**
     * Called by TAI for now
     *
     * @param request2
     */
    public SocialTaiRequest(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * @param service
     */
    public void addFilteredConfig(SocialLoginConfig socialLoginConfig) {
        if (socialLoginConfig != null) {
            if (this.filteredConfigs == null) {
                this.filteredConfigs = new ArrayList<SocialLoginConfig>();
            }
            if (!this.filteredConfigs.contains(socialLoginConfig)) {
                this.filteredConfigs.add(socialLoginConfig);
            }
        }
    }

    /**
     * This is supposed to be called once in a request only.
     *
     * @param service
     */
    public void setSpecifiedConfig(SocialLoginConfig socialLoginConfig) {
        this.socialLoginConfig = socialLoginConfig;
    }

    /**
     * @param service
     */
    public void addGenericConfig(SocialLoginConfig socialLoginConfig) {
        if (socialLoginConfig != null) {
            if (this.genericConfigs == null) {
                this.genericConfigs = new ArrayList<SocialLoginConfig>();
            }
            if (!this.genericConfigs.contains(socialLoginConfig)) {
                this.genericConfigs.add(socialLoginConfig);
            }
        }
    }

    public String getProviderName() {
        if (this.providerName == null) {
            if (this.socialLoginConfig != null) {
                this.providerName = this.socialLoginConfig.getUniqueId();
            }
        }
        return this.providerName;
    }

    public HttpServletRequest getRequest() {
        return this.request;
    }

    public List<SocialLoginConfig> getFilteredConfigs() {
        return this.filteredConfigs;
    }

    public List<SocialLoginConfig> getGenericConfigs() {
        return this.genericConfigs;
    }

    public Set<SocialLoginConfig> getAllMatchingConfigs() {
        Set<SocialLoginConfig> allConfigs = new HashSet<SocialLoginConfig>();
        if (socialLoginConfig != null) {
            allConfigs.add(socialLoginConfig);
        }
        if (filteredConfigs != null) {
            allConfigs.addAll(filteredConfigs);
        }
        if (genericConfigs != null) {
            allConfigs.addAll(genericConfigs);
        }
        return allConfigs;
    }

    public Set<String> getAllMatchingConfigIds() {
        Set<String> allConfigIds = new HashSet<String>();
        Set<SocialLoginConfig> allConfigs = getAllMatchingConfigs();
        for (SocialLoginConfig config : allConfigs) {
            allConfigIds.add(config.getUniqueId());
        }
        return allConfigIds;
    }

    /**
     * @return
     */
    public SocialLoginConfig getTheOnlySocialLoginConfig() throws SocialLoginException {
        if (taiException != null) {
            SocialLoginException exception = this.taiException;
            this.taiException = null;
            throw exception;
        }
        if (this.socialLoginConfig == null) {
            if (this.filteredConfigs != null) {
                if (this.filteredConfigs.size() == 1) {
                    this.socialLoginConfig = this.filteredConfigs.get(0);
                } else {
                    // error handling -- multiple socialLoginConfig qualified and we do not know how to select
                    String configIds = getConfigIds(filteredConfigs);
                    throw new SocialLoginException("SOCIAL_LOGIN_MANY_PROVIDERS", null, new Object[] { configIds });
                }
            } else if (this.genericConfigs != null) {
                if (this.genericConfigs.size() == 1) {
                    this.socialLoginConfig = this.genericConfigs.get(0);
                } else {
                    // error handling -- multiple socialLoginConfig qualified and we do not know how to select
                    String configIds = getConfigIds(genericConfigs);
                    throw new SocialLoginException("SOCIAL_LOGIN_MANY_PROVIDERS", null, new Object[] { configIds });
                }
            }
        }
        // socialLoginConfig should not be null, since we checked hasServices() before we are here
        // It either get one qualified SocialLoginConfig.
        // Or threw an exception due to multiple qualified configs.
        return this.socialLoginConfig;
    }

    /**
     * @return
     */
    String getConfigIds(List<SocialLoginConfig> multiConfigs) {
        String result = "";
        if (multiConfigs == null) {
            return result;
        }
        boolean bInit = true;
        for (SocialLoginConfig socialLoginConfig : multiConfigs) {
            if (!bInit) {
                result = result.concat(", ");
            } else {
                bInit = false;
            }
            result = result.concat(socialLoginConfig.getUniqueId());
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SocialTaiRequest [provider:").append(getProviderName()).append(" request:").append(this.request).append("]");
        return sb.toString();
    }

    /**
     * @return
     */
    public boolean hasServices() {
        return this.filteredConfigs != null ||
                this.genericConfigs != null ||
                this.socialLoginConfig != null;
        // The socialLoginConfig should be null when hasServices() is called in current code
        // But better coding it here in case for future
    }

    public void setTaiException(SocialLoginException taiException) {
        this.taiException = taiException;
    }

}
