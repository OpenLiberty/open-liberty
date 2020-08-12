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
import com.ibm.ws.security.authentication.filter.internal.AuthFilterConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 *
 */
@SuppressWarnings("restriction")
@Component(service = { SSOService.class },
           name = "com.ibm.ws.security.sso.SSOService",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM",
           })
public class SSOService {
    public static final TraceComponent tc = Tr.register(SSOService.class);
    static final String LTPA_CONFIGURATION = "ltpaConfiguration";
    public final static String KEY_FILTER = "authenticationFilter";
    protected final AtomicServiceReference<LTPAConfiguration> ltpaConfigurationRef = new AtomicServiceReference<>(LTPA_CONFIGURATION);
    protected final ConcurrentServiceReferenceMap<String, AuthenticationFilter> authFilterServiceRef = new ConcurrentServiceReferenceMap<String, AuthenticationFilter>(KEY_FILTER);

    private LTPAConfiguration ltpaConfig;
    AuthenticationFilter authFilter = null;

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
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setAuthenticationFilter id:" + ref.getProperty(AuthFilterConfig.KEY_ID));
        }
        authFilterServiceRef.putReference((String) ref.getProperty(AuthFilterConfig.KEY_ID), ref);
    }

    protected void updatedAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updatedAuthenticationFilter id:" + ref.getProperty(AuthFilterConfig.KEY_ID));
        }
        authFilterServiceRef.putReference((String) ref.getProperty(AuthFilterConfig.KEY_ID), ref);
    }

    protected void unsetAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "unsetAuthenticationFilter id:" + ref.getProperty(AuthFilterConfig.KEY_ID));
        }
        authFilterServiceRef.removeReference((String) ref.getProperty(AuthFilterConfig.KEY_ID), ref);
    }

//    protected void setAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "authFilter id: " + ref.getProperty(AuthFilterConfig.KEY_ID) + " authFilterRef: " + ref);
//        }
//        authFilterServiceRef.setReference(ref);
//    }
//
//    protected void updatedAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
//        authFilterServiceRef.setReference(ref);
//    }
//
//    protected void unsetAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "authFilter id: " + ref.getProperty(AuthFilterConfig.KEY_ID) + " authFilterRef: " + ref);
//        }
//        authFilterServiceRef.unsetReference(ref);
//    }
/*
 * If there no authentication filter defined, then process the request
 */
    public boolean isTargetInterceptor(HttpServletRequest req) {
//        if (ltpaConfigurationRef != null) {
//            // handle filter if any
//            ltpaConfig = ltpaConfigurationRef.getService();
//            String authFilterId = ltpaConfig.getAuthFilterId();
//            if (authFilterId != null && authFilterId.length() > 0) {
//                AuthenticationFilter authFilter = authFilterServiceRef.getService(authFilterId);
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    Tr.debug(tc, "authFilter id:" + authFilterId + " authFilter:" + authFilter);
//                }
//                if (authFilter != null) {
//                    if (!authFilter.isAccepted(req))
//                        return false;
//                }
//            }
//        }
        AuthenticationFilter authFilter = getAuthenticationFilter();
        if (authFilter != null) {
            if (!authFilter.isAccepted(req))
                return false;
        }
        return true;
    }

    public AuthenticationFilter getAuthenticationFilter() {
        if (ltpaConfigurationRef != null) {
            // handle filter if any
            ltpaConfig = ltpaConfigurationRef.getService();
            String authFilterId = ltpaConfig.getAuthFilterId();
            if (authFilterId != null && authFilterId.length() > 0) {
                authFilter = authFilterServiceRef.getService(authFilterId);
            }
        }
        return authFilter;
    }

    @Activate
    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        ltpaConfigurationRef.activate(cc);
        authFilterServiceRef.activate(cc);
    }

    @Modified
    protected synchronized void modified(Map<String, Object> props) {
        Tr.info(tc, "debug");
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext cc) {
        ltpaConfigurationRef.deactivate(cc);
        authFilterServiceRef.deactivate(cc);
    }
}
