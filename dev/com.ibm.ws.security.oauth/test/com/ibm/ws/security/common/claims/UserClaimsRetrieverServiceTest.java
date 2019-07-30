/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.claims;

import static org.junit.Assert.assertNotNull;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;

public class UserClaimsRetrieverServiceTest {

    private final Mockery mockery = new JUnit4Mockery();
    UserClaimsRetrieverService userClaimsRetrieverService;
    private final ServiceReference<AuthCacheService> authCacheServiceRef = mockery.mock(ServiceReference.class, "authCacheServiceRef");
    private final ServiceReference<UserRegistryService> userRegistryServiceRef = mockery.mock(ServiceReference.class, "userRegistryServiceRef");
    private final ComponentContext componentContext = mockery.mock(ComponentContext.class);
    private final AuthCacheService authCacheService = mockery.mock(AuthCacheService.class);
    private final UserRegistryService userRegistryService = mockery.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mockery.mock(UserRegistry.class);
    private final String realm = "TestRealm";
    private final String username = "user1";
    private final String groupIdentifier = "groups";
    private final String cacheKey = realm + ":" + username;

    @Before
    public void setUp() throws Exception {
        userClaimsRetrieverService = new UserClaimsRetrieverService();
        setupComponentContextExpectations();
        setupUserRegistryExpectations();
        setupAuthCacheExpectations();
    }

    @After
    public void tearDown() throws Exception {
        userClaimsRetrieverService.deactivate(componentContext);
        mockery.assertIsSatisfied();
    }

    private void setupComponentContextExpectations() {
        mockery.checking(new Expectations() {
            {
                one(componentContext).locateService("authCacheService", authCacheServiceRef);
                will(returnValue(authCacheService));
                one(componentContext).locateService("userRegistryService", userRegistryServiceRef);
                will(returnValue(userRegistryService));
            }
        });
    }

    private void setupUserRegistryExpectations() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistry();
                will(returnValue(userRegistry));
                one(userRegistry).getRealm();
                will(returnValue(realm));
                allowing(userRegistry).getUniqueUserId(username);
                will(throwException(new EntryNotFoundException("This is a test and we just interested in the relationship.")));
            }
        });
    }

    private void setupAuthCacheExpectations() {
        mockery.checking(new Expectations() {
            {
                one(authCacheService).getSubject(cacheKey);
                will(returnValue(null));
            }
        });
    }

    @Test
    public void test() {
        UserClaimsRetrieverService userClaimsRetrieverService = new UserClaimsRetrieverService();
        userClaimsRetrieverService.setAuthCacheService(authCacheServiceRef);
        userClaimsRetrieverService.setUserRegistryService(userRegistryServiceRef);
        userClaimsRetrieverService.activate(componentContext);
        UserClaims userClaims = userClaimsRetrieverService.getUserClaims(username, groupIdentifier);
        assertNotNull("There must be user claims.", userClaims);
    }

}
