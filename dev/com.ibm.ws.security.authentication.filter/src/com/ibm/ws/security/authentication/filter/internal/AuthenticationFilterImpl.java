/*******************************************************************************
 * Copyright (c) 2014, 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authentication.filter.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;

@Component(configurationPid = "com.ibm.ws.security.authentication.filter", service = { AuthenticationFilter.class }, configurationPolicy = ConfigurationPolicy.REQUIRE, property = { "service.vendor=IBM" })
public class AuthenticationFilterImpl implements AuthenticationFilter, IAuthenticationFilterInternal {
    public static final TraceComponent tc = Tr.register(AuthenticationFilterImpl.class);
    protected AuthFilterConfig authFilterConfig = null;
    protected CommonFilter commonFilter = null;

    @Activate
    protected void activate(Map<String, Object> properties) {
        String id = getAuthFilterConfig(properties);
        if (id != null) {
            Tr.info(tc, "AUTH_FILTER_CONFIG_PROCESSED", id);
        }
    }

    @Modified
    protected void modify(Map<String, Object> properties) {
        String id = getAuthFilterConfig(properties);
        if (id != null) {
            Tr.info(tc, "AUTH_FILTER_CONFIG_MODIFIED", id);
        }
    }

    @Deactivate
    protected void deactivate(Map<String, Object> properties) {
        authFilterConfig = null;
        commonFilter = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean init(String filter) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAccepted(HttpServletRequest request) {
        if (commonFilter != null) {
            return commonFilter.isAccepted(new RealRequestInfo(request));
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void setProcessAll(boolean all) {
        //do nothing
    }

    protected String getAuthFilterConfig(Map<String, Object> properties) {
        authFilterConfig = new AuthFilterConfig(properties);
        if (authFilterConfig.hasFilterConfig()) {
            commonFilter = new CommonFilter(authFilterConfig);
            return authFilterConfig.getId();
        }
        commonFilter = null;
        authFilterConfig = null;
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthFilterId() {
        return authFilterConfig != null ? authFilterConfig.getId() : null;
    }

    @Override
    public List<String> getRequestUrlProperties() {
        if (authFilterConfig == null || authFilterConfig.getRequestUrls() == null) {
            return Collections.emptyList();
        }

        List<String> urlPatterns = new ArrayList<>();

        for (Properties requestUrl : authFilterConfig.getRequestUrls()) {
            String urlPattern = requestUrl.getProperty(AuthFilterConfig.KEY_URL_PATTERN);

            if (urlPattern != null && !urlPattern.trim().isEmpty()) {
                urlPatterns.add(urlPattern);
            }
        }

        return urlPatterns;
    }
}
