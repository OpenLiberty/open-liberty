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
package com.ibm.ws.security.oauth20;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.oauth.core.api.oauth20.OAuth20Component;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;

public class ProvidersServiceTest {

    private final Mockery mockery = new JUnit4Mockery();
    private ProvidersService providersService;
    private ComponentContext cc;
    private ServiceReference<OAuth20Provider> providerRef;
    private final String providerId = "TestOAuth20Provider";
    private final long providerServiceId = 123;
    private ServiceReference<OAuth20Provider> anotherProviderRef;
    private final String anotherProviderId = "AnotherTestOAuth20Provider";
    private final long anotherProviderServiceId = 456;
    private Set<ServiceReference<OAuth20Provider>> referencesToRemove;

    @Before
    public void setUp() {
        referencesToRemove = new HashSet<ServiceReference<OAuth20Provider>>();
        cc = mockery.mock(ComponentContext.class);
        providersService = new ProvidersService();
        providersService.activate(cc);
    }

    @After
    public void tearDown() {
        removeServiceReferences();
        providersService.deactivate(cc);
    }

    private void removeServiceReferences() {
        for (ServiceReference<OAuth20Provider> ref : referencesToRemove) {
            providersService.unsetOAuth20Provider(ref);
        }
    }

    @Test
    public void unitialized() {
        OAuth20Provider provider = ProvidersService.getOAuth20Provider(providerId);
        assertNull("There must not be provider for an unitialized providers service.", provider);
    }

    @Test
    public void oneProvider() {
        providerRef = createProviderRef(providerId, providerServiceId);
        providersService.setOAuth20Provider(providerRef);

        assertNotNull("There must be a provider.", ProvidersService.getOAuth20Provider(providerId));
    }

    @Test
    public void multipleProviders() {
        providerRef = createProviderRef(providerId, providerServiceId);
        anotherProviderRef = createProviderRef(anotherProviderId, anotherProviderServiceId);
        providersService.setOAuth20Provider(providerRef);
        providersService.setOAuth20Provider(anotherProviderRef);

        assertNotNull("There must be a provider.", ProvidersService.getOAuth20Provider(anotherProviderId));
    }

    @Test
    public void removeProvider() {
        providerRef = createProviderRef(providerId, providerServiceId);
        anotherProviderRef = createProviderRef(anotherProviderId, anotherProviderServiceId);
        providersService.setOAuth20Provider(providerRef);
        providersService.setOAuth20Provider(anotherProviderRef);

        OAuth20Provider provider = ProvidersService.getOAuth20Provider(anotherProviderId);
        assertNotNull("There must be a provider.", provider);
        providersService.unsetOAuth20Provider(anotherProviderRef);
        assertNull("The provider must not be found after it was removed.", ProvidersService.getOAuth20Provider(anotherProviderId));
    }

    @Test
    public void providerForRequest() {
        final OAuth20Provider provider = createProvider(providerId);
        providerRef = createProviderRef(providerId, providerServiceId, provider);
        providersService.setOAuth20Provider(providerRef);
        final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
        createProviderRequestAcceptanceExpectation(provider, request, true);

        List<OAuth20Provider> providersRetrieved = ProvidersService.getProvidersMatchingRequest(request);
        assertEquals("The provider must match the request.", provider, providersRetrieved.get(0));
        assertEquals("The provider sould be one.", 1, providersRetrieved.size());
    }

    @Test
    public void noProviderForRequest() {
        final OAuth20Provider provider = createProvider(providerId);
        providerRef = createProviderRef(providerId, providerServiceId, provider);
        providersService.setOAuth20Provider(providerRef);
        final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
        createRequestExpectation(provider, request, "https://somehost:9080/oidc/endpoint/"+providerId+"/ep");
        createProviderRequestAcceptanceExpectation(provider, request, false);

        List<OAuth20Provider> providersRetrieved = ProvidersService.getProvidersMatchingRequest(request);
        assertNull("There must not be a provider that matches the request.", providersRetrieved);
    }

    private ServiceReference<OAuth20Provider> createProviderRef(final String providerId, final long providerServiceId) {
        final OAuth20Provider provider = createProvider(providerId);
        return createProviderRef(providerId, providerServiceId, provider);
    }

    @SuppressWarnings("unchecked")
    private ServiceReference<OAuth20Provider> createProviderRef(final String providerId, final long providerServiceId, final OAuth20Provider provider) {
        final ServiceReference<OAuth20Provider> providerRef = mockery.mock(ServiceReference.class, providerId + "Ref");
        createServiceReferenceExpectations(providerId, providerServiceId, providerRef);
        createComponentContextExpectations(providerRef, provider);
        referencesToRemove.add(providerRef);
        return providerRef;
    }

    private void createServiceReferenceExpectations(final String providerId, final long providerServiceId, final ServiceReference<OAuth20Provider> providerRef) {
        mockery.checking(new Expectations() {
            {
                allowing(providerRef).getProperty("id");
                will(returnValue(providerId));
                allowing(providerRef).getProperty("service.id");
                will(returnValue(providerServiceId));
                allowing(providerRef).getProperty("service.ranking");
                will(returnValue(0));
            }
        });
    }

    private OAuth20Provider createProvider(String providerId) {
        final OAuth20Provider provider = mockery.mock(OAuth20Provider.class, providerId);
        final OAuth20Component component = null;
        mockery.checking(new Expectations() {
            {
                allowing(provider).getComponent();
                will(returnValue(component));
                allowing(provider).createCoreClasses();
                allowing(provider).isValid();
                will(returnValue(true));
            }
        });
        return provider;
    }

    private void createComponentContextExpectations(final ServiceReference<OAuth20Provider> providerRef, final OAuth20Provider provider) {
        mockery.checking(new Expectations() {
            {
                allowing(cc).locateService("oauth20Provider", providerRef);
                will(returnValue(provider));
            }
        });
    }

    private OAuth20Provider createProviderRequestAcceptanceExpectation(final OAuth20Provider provider, final HttpServletRequest request, final boolean accepted) {
        mockery.checking(new Expectations() {
            {
                allowing(provider).isRequestAccepted(request);
                will(returnValue(accepted));
            }
        });
        return provider;
    }

    private OAuth20Provider createRequestExpectation(final OAuth20Provider provider, final HttpServletRequest request, final String expected) {
        mockery.checking(new Expectations() {
            {
                allowing(request).getRequestURI();
                will(returnValue(expected));
            }
        });
        return provider;
    }

}
