/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal;

import java.util.Map;
import java.util.regex.Pattern;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.config.CommonConfigUtils;
import com.ibm.ws.security.common.web.CommonWebConstants;
import com.ibm.ws.security.social.SocialLoginWebappConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.SocialUtil;
import com.ibm.wsspi.wab.configure.WABConfiguration;

/*
 * This class allows the context root to be specified in server.xml
 */

@Component(configurationPid = "com.ibm.ws.security.social.webapp", configurationPolicy = ConfigurationPolicy.REQUIRE, service = { SocialLoginWebappConfig.class, WABConfiguration.class }, immediate = true, property = { "service.vendor=IBM" })
public class SocialLoginWebappConfigImpl implements SocialLoginWebappConfig {

    private static final TraceComponent tc = Tr.register(SocialLoginWebappConfigImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String KEY_SOCIAL_MEDIA_SELECTION_PAGE_URL = "socialMediaSelectionPageUrl";
    protected String socialMediaSelectionPageUrl = null;

    public static final String KEY_ENABLE_LOCAL_AUTHENTICATION = "enableLocalAuthentication";
    protected boolean enableLocalAuthentication = false;

    private final CommonConfigUtils configUtils = new CommonConfigUtils();

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> config) {
        initProps(config);
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        initProps(config);
    }

    @Deactivate
    protected void deactivate() {

    }

    public void initProps(Map<String, Object> props) {
        String contextPath = configUtils.getConfigAttribute(props, WABConfiguration.CONTEXT_PATH);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Context Path=" + contextPath);
        }
        validateAndSetContextPath(contextPath);

        validateAndSetSelectionPageUrl(configUtils.getConfigAttribute(props, KEY_SOCIAL_MEDIA_SELECTION_PAGE_URL));

        enableLocalAuthentication = configUtils.getBooleanConfigAttribute(props, KEY_ENABLE_LOCAL_AUTHENTICATION, this.enableLocalAuthentication);
    }

    @Override
    public String getSocialMediaSelectionPageUrl() {
        return socialMediaSelectionPageUrl;
    }

    @Override
    public boolean isLocalAuthenticationEnabled() {
        return enableLocalAuthentication;
    }

    void validateAndSetContextPath(String contextPath) {
        if (isValidUriPath(contextPath)) {
            Oauth2LoginConfigImpl.setContextRoot(contextPath);
        } else {
            Tr.error(tc, "INVALID_CONTEXT_PATH_CHARS", new Object[] { contextPath });
        }
    }

    void validateAndSetSelectionPageUrl(String selectionPageUrl) {
        if (isSelectionPageUrlNullOrEmpty(selectionPageUrl)) {
            socialMediaSelectionPageUrl = null;
        } else {
            validateAndSetNonEmptySelectionPageUrl(selectionPageUrl);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, KEY_SOCIAL_MEDIA_SELECTION_PAGE_URL + "=" + socialMediaSelectionPageUrl);
        }
    }

    boolean isSelectionPageUrlNullOrEmpty(String selectionPageUrl) {
        if (selectionPageUrl == null || selectionPageUrl.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, KEY_SOCIAL_MEDIA_SELECTION_PAGE_URL + " is null or empty");
            }
            return true;
        }
        return false;
    }

    @FFDCIgnore(SocialLoginException.class)
    void validateAndSetNonEmptySelectionPageUrl(String selectionPageUrl) {
        if (!isHttpOrRelativeUrl(selectionPageUrl)) {
            Tr.error(tc, "SELECTION_PAGE_URL_NOT_HTTP", new Object[] { selectionPageUrl });
            socialMediaSelectionPageUrl = null;
            return;
        }
        try {
            SocialUtil.validateEndpointFormat(selectionPageUrl, false);
            socialMediaSelectionPageUrl = selectionPageUrl;
        } catch (SocialLoginException e) {
            Tr.error(tc, "SELECTION_PAGE_URL_NOT_VALID", new Object[] { selectionPageUrl, e.getMessage() });
            socialMediaSelectionPageUrl = null;
        }
    }

    boolean isHttpOrRelativeUrl(String selectionPageUrl) {
        if (selectionPageUrl == null) {
            return false;
        }
        String loweredUrl = selectionPageUrl.toLowerCase();
        if (loweredUrl.startsWith("http://") || loweredUrl.startsWith("https://")) {
            return true;
        }
        if (selectionPageUrl.contains("://")) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Provided URL does not start with http or https, but appears to contain a protocol");
            }
            return false;
        }
        return true;
    }

    boolean isValidUriPath(String input) {
        if (input == null) {
            return false;
        }
        return Pattern.matches(CommonWebConstants.VALID_URI_PATH_CHARS + "*", input);
    }

}
