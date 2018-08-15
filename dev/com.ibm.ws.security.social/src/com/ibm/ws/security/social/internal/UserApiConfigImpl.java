/*
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.social.internal;

import java.io.IOException;
import java.util.Dictionary;

import org.osgi.service.cm.Configuration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.config.CommonConfigUtils;
import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.SocialLoginService;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.UserApiConfig;
import com.ibm.ws.security.social.error.SocialLoginException;

public class UserApiConfigImpl implements UserApiConfig {
    public static final TraceComponent tc = Tr.register(UserApiConfigImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String KEY_api = "api";
    String api = null;
    public static final String KEY_method = "method";
    String method = null;
    public static final String KEY_parameter = "parameter";
    String parameter = null;

    /**
     * @param configAdmin
     * @param string
     * @throws SocialLoginException
     */
    public UserApiConfigImpl(String userApi) {
        this.api = userApi;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "api = " + this.api);
        }
        this.method = Constants.client_secret_basic;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "method = " + this.method);
        }
    }

    /**
     * @param parentSocialLoginService
     * @param userinfoRef
     * @throws SocialLoginException
     */
    public UserApiConfigImpl(SocialLoginService parentSocialLoginService, String userinfoRef) throws SocialLoginException {
        Configuration config = null;
        try {
            if (parentSocialLoginService == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "No service provided, so nothing to do");
                }
                return;
            }
            config = parentSocialLoginService.getConfigAdmin().getConfiguration(userinfoRef, parentSocialLoginService.getBundleLocation()); // yes, the config belongs to our component
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid userinfo configuration", userinfoRef);
            }
            throw new SocialLoginException(e); // this should not happen, but in case
        }

        if (config == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "A configuration that matches [" + userinfoRef + "] could not be found");
            }
            return;
        }

        // let's get the properties/dictionary
        Dictionary<String, Object> props = config.getProperties(); // userinfo specific props
        if (props == null) {
            // Somehow at some dynamic updating situations, this could be null
            // No further handling needed
            return;
        }
        this.api = CommonConfigUtils.trim((String) props.get(KEY_api));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "api = " + this.api);
        }
        this.method = CommonConfigUtils.trim((String) props.get(KEY_method));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "method = " + this.method);
        }
        this.parameter = CommonConfigUtils.trim((String) props.get(KEY_parameter));
    }

    /** {@inheritDoc} */
    @Override
    public String getApi() {
        return this.api;
    }

    /** {@inheritDoc} */
    @Override
    public String getMethod() {
        return this.method;
    }

    /** {@inheritDoc} */
    @Override
    public String getParameter() {
        return this.parameter;
    }

}
