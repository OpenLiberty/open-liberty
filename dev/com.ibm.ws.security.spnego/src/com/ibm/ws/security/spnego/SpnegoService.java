/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import com.ibm.ws.security.kerberos.auth.KerberosService;
import com.ibm.ws.security.spnego.internal.SpnegoConfigImpl;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = { WebAuthenticator.class },
           name = "com.ibm.ws.security.spnego",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = { "service.vendor=IBM",
                        "com.ibm.ws.security.webAuthenticator.type=SPNEGO" })
public class SpnegoService implements WebAuthenticator {
    public static final TraceComponent tc = Tr.register(SpnegoService.class);
    static final String CONFIGURATION_ADMIN = "configurationAdmin";
    public final static String KEY_FILTER = "authenticationFilter";
    private final String KEY_LOCATION_ADMIN = "locationAdmin";

    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);
    protected final AtomicServiceReference<AuthenticationFilter> authFilterServiceRef = new AtomicServiceReference<AuthenticationFilter>(KEY_FILTER);
    protected final AtomicServiceReference<KerberosService> kerberosServiceRef = new AtomicServiceReference<>("kerberosService");

    private final AuthenticationResult CONTINUE = new AuthenticationResult(AuthResult.CONTINUE, "SPNEGO service said continue...");
    private SpnegoAuthenticator spnegoAuthenticator = null;
    private SpnegoConfig spnegoConfig = null;

    @Reference(name = KEY_FILTER,
               service = AuthenticationFilter.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "authFilter id: " + ref.getProperty(AuthFilterConfig.KEY_ID) + " authFilterRef: " + ref);
        }
        authFilterServiceRef.setReference(ref);
    }

    protected void updatedAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
        authFilterServiceRef.setReference(ref);
    }

    protected void unsetAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "authFilter id: " + ref.getProperty(AuthFilterConfig.KEY_ID) + " authFilterRef: " + ref);
        }
        authFilterServiceRef.unsetReference(ref);
    }

    @Reference(service = WsLocationAdmin.class, name = KEY_LOCATION_ADMIN,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.setReference(ref);
    }

    protected void unsetLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.unsetReference(ref);
    }

    @Reference(service = KerberosService.class)
    protected void setKerberosService(ServiceReference<KerberosService> ref) {
        kerberosServiceRef.setReference(ref);
    }

    protected void unsetKerberosService(ServiceReference<KerberosService> ref) {
        kerberosServiceRef.unsetReference(ref);
    }

    @Activate
    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        kerberosServiceRef.activate(cc);
        locationAdminRef.activate(cc);
        authFilterServiceRef.activate(cc);
        spnegoConfig = new SpnegoConfigImpl(locationAdminRef.getServiceWithException(), kerberosServiceRef.getServiceWithException(), props);
        spnegoAuthenticator = new SpnegoAuthenticator();
        Tr.info(tc, "SPNEGO_CONFIG_PROCESSED", spnegoConfig.getId());
    }

    @Modified
    protected synchronized void modified(Map<String, Object> props) {
        spnegoConfig = new SpnegoConfigImpl(locationAdminRef.getServiceWithException(), kerberosServiceRef.getServiceWithException(), props);
        Tr.info(tc, "SPNEGO_CONFIG_MODIFIED", spnegoConfig.getId());
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext cc) {
        locationAdminRef.deactivate(cc);
        authFilterServiceRef.deactivate(cc);
        kerberosServiceRef.deactivate(cc);
        spnegoConfig = null;
        spnegoAuthenticator = null;
    }

    //This method for unit test
    public void setSpnegoConfig(SpnegoConfig spnegoConfig) {
        this.spnegoConfig = spnegoConfig;
    }

    //This method for unit test
    public void setSpnegoAuthenticator(SpnegoAuthenticator spnegoAuthenticator) {
        this.spnegoAuthenticator = spnegoAuthenticator;
    }

    @Override
    public AuthenticationResult authenticate(WebRequest webRequest) {
        HttpServletRequest req = webRequest.getHttpServletRequest();
        HttpServletResponse resp = webRequest.getHttpServletResponse();

        String authzHeader = req.getHeader("Authorization");

        final boolean isFirstRequest = (authzHeader == null);

        if (!shouldSpnegoAuthenticateThisRequest(webRequest, req, isFirstRequest)) {
            return CONTINUE;
        }

        if (isFirstRequest) {
            return spnegoAuthenticator.createNegotiateHeader(resp, spnegoConfig);
        }

        AuthenticationResult result = null;
        if (!spnegoConfig.isSpnGssCredentialEmpty()) {
            result = spnegoAuthenticator.authenticate(req, resp, authzHeader, spnegoConfig);
            if (result != null && (result.getStatus() == AuthResult.CONTINUE || result.getStatus() == AuthResult.SUCCESS)) {
                return result;
            }
        } else {
            // already issued the error message earlier and do not want to issue the same message for every request.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No GSSCredential for any of the service principal names.");
            }
            result = new AuthenticationResult(AuthResult.FAILURE, "No GSSCredential for any of the service principal names.");
        }

        if (!spnegoConfig.getDisableFailOverToAppAuthType()) {
            result = CONTINUE;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "failOverToAppAuthType is allowed, so continue...");
            }
        }

        return result;
    }

    /**
     * @param webRequest
     * @param isFirstRequest
     */
    protected boolean shouldSpnegoAuthenticateThisRequest(WebRequest webRequest, HttpServletRequest req, boolean isFirstRequest) {
        if (isFirstRequest && webRequest.isUnprotectedURI() && !webRequest.isProviderSpecialUnprotectedURI()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "un-protectedURI request and no SPNEGO token so do not authenticate with SPNEGO web");
            }
            return false;
        }
        if (webRequest.isCallAfterSSO() && spnegoConfig.isInvokeAfterSSO() ||
            !webRequest.isCallAfterSSO() && !spnegoConfig.isInvokeAfterSSO()) {
            return isAuthFilterAccept(req);
        }

        return false;
    }

    /*
     */
    protected boolean isAuthFilterAccept(HttpServletRequest req) {
        AuthenticationFilter authFilter = authFilterServiceRef.getService();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "authFilter:" + authFilter);
        }
        if (authFilter != null) {
            return authFilter.isAccepted(req);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Authentication filter service is not avaliale, all HTTP requests will use SPNEGO authentication");
        }
        return true;
    }

    @Override
    public AuthenticationResult authenticate(HttpServletRequest request, HttpServletResponse response, HashMap props) throws Exception {
        return null;
    }
}
