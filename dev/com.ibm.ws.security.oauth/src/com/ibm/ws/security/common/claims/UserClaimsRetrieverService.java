/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.claims;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = UserClaimsRetrieverService.class,
        name = "com.ibm.ws.security.openidconnect.server.internal.userclaimsretriever",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.IGNORE,
        property = "service.vendor=IBM")
public class UserClaimsRetrieverService {

    private static final String KEY_AUTH_CACHE_SERVICE = "authCacheService";
    private static final String KEY_USER_REGISTRY_SERVICE = "userRegistryService";

    AtomicServiceReference<AuthCacheService> authCacheServiceRef = new AtomicServiceReference<AuthCacheService>(KEY_AUTH_CACHE_SERVICE);
    AtomicServiceReference<UserRegistryService> userRegistryServiceRef = new AtomicServiceReference<UserRegistryService>(KEY_USER_REGISTRY_SERVICE);
    private UserClaimsRetriever userClaimsRetriever;

    @Reference(service = AuthCacheService.class, name = KEY_AUTH_CACHE_SERVICE)
    protected void setAuthCacheService(ServiceReference<AuthCacheService> ref) {
        authCacheServiceRef.setReference(ref);
    }

    protected void unsetAuthCacheService(ServiceReference<AuthCacheService> ref) {
        authCacheServiceRef.unsetReference(ref);
    }

    @Reference(service = UserRegistryService.class, name = KEY_USER_REGISTRY_SERVICE)
    protected void setUserRegistryService(ServiceReference<UserRegistryService> ref) {
        userRegistryServiceRef.setReference(ref);
    }

    protected void unsetUserRegistryService(ServiceReference<UserRegistryService> ref) {
        userRegistryServiceRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        authCacheServiceRef.activate(cc);
        userRegistryServiceRef.activate(cc);
        ConfigUtils.setUserClaimsRetrieverService(this);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        authCacheServiceRef.deactivate(cc);
        userRegistryServiceRef.deactivate(cc);
        ConfigUtils.setUserClaimsRetrieverService(null);
    }

    @FFDCIgnore(Exception.class)
    public UserClaims getUserClaims(String username, String groupIdentifier) {
        try {
            UserClaimsRetriever userClaimsRetriever = getUserClaimsRetriever();
            return userClaimsRetriever.getUserClaims(username, groupIdentifier);
        } catch (Exception e) {
            // TODO: Use static empty user claims
            return new UserClaims((String) null, groupIdentifier);
        }
    }

    private UserClaimsRetriever getUserClaimsRetriever() throws RegistryException {

        return userClaimsRetriever = new UserClaimsRetriever(authCacheServiceRef.getService(),
                userRegistryServiceRef.getService().getUserRegistry());
    }
}
