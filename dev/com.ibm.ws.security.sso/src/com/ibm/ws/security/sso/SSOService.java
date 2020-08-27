/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.sso;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.token.ltpa.LTPAConfiguration;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 *
 */
@Component(service = { SSOService.class },
           name = "com.ibm.ws.security.sso.SSOService",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class SSOService {
    public static final TraceComponent tc = Tr.register(SSOService.class);
    public static final String KEY_SERVICE_PID = "service.pid";
    static final String LTPA_CONFIGURATION = "ltpaConfiguration";
    public final static String KEY_FILTER = "authenticationFilter";
    protected final AtomicServiceReference<LTPAConfiguration> ltpaConfigurationRef = new AtomicServiceReference<>(LTPA_CONFIGURATION);
    protected final ConcurrentServiceReferenceMap<String, AuthenticationFilter> authFilterServiceRef = new ConcurrentServiceReferenceMap<String, AuthenticationFilter>(KEY_FILTER);

    private LTPAConfiguration ltpaConfig;

    @Reference(name = LTPA_CONFIGURATION,
               service = LTPAConfiguration.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setLTPAConfiguration(ServiceReference<LTPAConfiguration> ref) {
        ltpaConfigurationRef.setReference(ref);
    }

    protected void unsetLTPAConfiguration(ServiceReference<LTPAConfiguration> ref) {
        ltpaConfigurationRef.unsetReference(ref);
    }

    @Reference(name = KEY_FILTER,
               service = AuthenticationFilter.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
        String pid = (String) ref.getProperty(KEY_SERVICE_PID);
        authFilterServiceRef.putReference(pid, ref);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "authFilter pid: " + pid);
            Tr.debug(tc, "setAuthFilter service pid: " + getAuthFilterService(pid));
        }
    }

    protected void updatedAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updatedAuthenticationFilter service.pid:" + ref.getProperty(KEY_SERVICE_PID));
        }
        authFilterServiceRef.putReference((String) ref.getProperty(KEY_SERVICE_PID), ref);
    }

    protected void unsetAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "unsetAuthenticationFilter service.pid:" + ref.getProperty(KEY_SERVICE_PID));
        }
        authFilterServiceRef.removeReference((String) ref.getProperty(KEY_SERVICE_PID), ref);
    }

    public AuthenticationFilter getAuthFilterService(String pid) {
        return authFilterServiceRef.getService(pid);
    }

/*
 * If there no authentication filter defined, then process the request
 */
    public boolean processRequest(HttpServletRequest req) {
        AuthenticationFilter authFilter = getAuthFilter();
        if (authFilter != null) {
            if (!authFilter.isAccepted(req))
                return false;
        }
        return true;
    }

    public AuthenticationFilter getAuthFilter() {
        AuthenticationFilter authFilter = null;
        if (ltpaConfigurationRef != null) {
            ltpaConfig = ltpaConfigurationRef.getService();
            String authFilterRef = ltpaConfig.getAuthFilterRef();
            if (authFilterRef != null && authFilterRef.length() > 0) {
                authFilter = authFilterServiceRef.getService(authFilterRef);
            }
        }
        return authFilter;
    }

    @Activate
    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " SSOService active:" + props);
        }
        ltpaConfigurationRef.activate(cc);
        authFilterServiceRef.activate(cc);
    }

    @Modified
    protected synchronized void modified(Map<String, Object> props) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " SSOService modified:" + props);
        }
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext cc) {
        ltpaConfigurationRef.deactivate(cc);
        authFilterServiceRef.deactivate(cc);
    }
}
