/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.ready.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.ready.SecurityReadyService;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.ws.security.token.TokenService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * This class determines when everything that is "needed" to have a meaningful
 * security runtime is ready.
 */
//TODO just use AtamicRefeence, no need for our fancy stuff.
//TODO why does this care about UserRegistries? Can't it just use the UserRegistryService?  If not, lets ditch the UserRegstryService.
@Component(service = {},
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public class SecurityReadyServiceImpl implements SecurityReadyService {
    private static final TraceComponent tc = Tr.register(SecurityReadyServiceImpl.class);

    static final String KEY_TOKEN_SERVICE = "tokenService";
    static final String KEY_TOKEN_MANAGER = "tokenManager";
    static final String KEY_AUTHENTICATION_SERVICE = "authenticationService";
    static final String KEY_AUTHORIZATION_SERVICE = "authorizationService";
    static final String KEY_USER_REGISTRY = "userRegistry";
    static final String KEY_USER_REGISTRY_FACTORY = "userRegistryFactory";
    static final String KEY_USER_REGISTRY_SERVICE = "userRegistryService";

    private final AtomicServiceReference<TokenService> tokenService = new AtomicServiceReference<TokenService>(KEY_TOKEN_SERVICE);
    private final AtomicServiceReference<TokenManager> tokenManager = new AtomicServiceReference<TokenManager>(KEY_TOKEN_MANAGER);
    private final AtomicServiceReference<AuthenticationService> authenticationService = new AtomicServiceReference<AuthenticationService>(KEY_AUTHENTICATION_SERVICE);
    private final AtomicServiceReference<AuthorizationService> authorizationService = new AtomicServiceReference<AuthorizationService>(KEY_AUTHORIZATION_SERVICE);
    private final AtomicServiceReference<UserRegistry> userRegistry = new AtomicServiceReference<UserRegistry>(KEY_USER_REGISTRY);
    private final AtomicServiceReference<UserRegistryService> userRegistryService = new AtomicServiceReference<UserRegistryService>(KEY_USER_REGISTRY_SERVICE);
    private volatile boolean activated = false;
    private volatile boolean securityReady = false;

    private ServiceRegistration<SecurityReadyService> reg;
    private ComponentContext cc;

    @Reference(name = KEY_TOKEN_SERVICE, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setTokenService(ServiceReference<TokenService> ref) {
        tokenService.setReference(ref);
        updateSecurityReadyState();
    }

    protected void unsetTokenService(ServiceReference<TokenService> ref) {
        tokenService.unsetReference(ref);
        updateSecurityReadyState();
    }

    @Reference(name = KEY_TOKEN_MANAGER, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setTokenManager(ServiceReference<TokenManager> ref) {
        tokenManager.setReference(ref);
        updateSecurityReadyState();
    }

    protected void unsetTokenManager(ServiceReference<TokenManager> ref) {
        tokenManager.unsetReference(ref);
        updateSecurityReadyState();
    }

    @Reference(name = KEY_AUTHENTICATION_SERVICE, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setAuthenticationService(ServiceReference<AuthenticationService> ref) {
        authenticationService.setReference(ref);
        updateSecurityReadyState();
    }

    protected void unsetAuthenticationService(ServiceReference<AuthenticationService> ref) {
        authenticationService.unsetReference(ref);
        updateSecurityReadyState();
    }

    @Reference(name = KEY_AUTHORIZATION_SERVICE, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setAuthorizationService(ServiceReference<AuthorizationService> ref) {
        authorizationService.setReference(ref);
        updateSecurityReadyState();
    }

    protected void unsetAuthorizationService(ServiceReference<AuthorizationService> ref) {
        authorizationService.unsetReference(ref);
        updateSecurityReadyState();
    }

    @Reference(name = KEY_USER_REGISTRY, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setUserRegistry(ServiceReference<UserRegistry> ref) {
        userRegistry.setReference(ref);
        updateSecurityReadyState();
    }

    protected void unsetUserRegistry(ServiceReference<UserRegistry> ref) {
        userRegistry.unsetReference(ref);
        updateSecurityReadyState();
    }

    @Reference(name = KEY_USER_REGISTRY_SERVICE, service = UserRegistryService.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setUserRegistryService(ServiceReference<UserRegistryService> ref) {
        userRegistryService.setReference(ref);
        updateSecurityReadyState();
    }

    protected void unsetUserRegistryService(ServiceReference<UserRegistryService> ref) {
        userRegistryService.unsetReference(ref);
        updateSecurityReadyState();
    }

    protected void activate(ComponentContext cc) {
        Tr.info(tc, "SECURITY_SERVICE_STARTING");
        this.cc = cc;

        tokenService.activate(cc);
        tokenManager.activate(cc);
        authenticationService.activate(cc);
        authorizationService.activate(cc);
        userRegistry.activate(cc);
        userRegistryService.activate(cc);

        activated = true;

        updateSecurityReadyState();
    }

    protected void deactivate(ComponentContext cc) {
        activated = false;
        securityReady = false;

        Tr.info(tc, "SECURITY_SERVICE_STOPPED");
        tokenService.deactivate(cc);
        tokenManager.deactivate(cc);
        authenticationService.deactivate(cc);
        authorizationService.deactivate(cc);
        userRegistry.deactivate(cc);
        userRegistryService.deactivate(cc);

        if (reg != null) {
            reg.unregister();
        }
    }

    /**
     * Construct a String that lists all of the missing services.
     * This is very useful for debugging.
     *
     * @return {@code null} if all services are present, or a non-empty
     *         String if any are missing
     */
    private String getUnavailableServices() {
        StringBuilder missingServices = new StringBuilder();
        if (tokenService.getReference() == null) {
            missingServices.append("tokenService, ");
        }
        if (tokenManager.getReference() == null) {
            missingServices.append("tokenManager, ");
        }
        if (authenticationService.getReference() == null) {
            missingServices.append("authenticationService, ");
        }
        if (authorizationService.getReference() == null) {
            missingServices.append("authorizationService, ");
        }
        if (userRegistry.getReference() == null) {
            missingServices.append("userRegistry, ");
        }
        if (userRegistryService.getReference() == null) {
            missingServices.append("userRegistryService, ");
        }

        if (missingServices.length() == 0) {
            return null;
        } else {
            // Return everything but the last ", "
            return missingServices.substring(0, missingServices.length() - 2);
        }
    }

    /**
     * Security is ready when all of these required services have been registered.
     */
    private void updateSecurityReadyState() {
        if (!activated) {
            return;
        }

        String unavailableServices = getUnavailableServices();
        if (unavailableServices == null) {
            Tr.info(tc, "SECURITY_SERVICE_READY");
            securityReady = true;

            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("service.vendor", "IBM");
            reg = cc.getBundleContext().registerService(SecurityReadyService.class, this, props);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "The following required security services are not available: " + unavailableServices);
            }
            securityReady = false;
            if (reg != null) {
                reg.unregister();
                reg = null;
            }
        }
    }

    /**
     * Answers if the security service as a whole is ready to process requests.
     *
     * @return boolean indiciating if the security service is ready to process
     *         requests.
     */
    @Override
    public boolean isSecurityReady() {
        return securityReady;
    }
}
