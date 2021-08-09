/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.feature.internal;

import static org.junit.Assert.assertNull;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.ws.webcontainer.security.UnprotectedResourceService;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.WebAuthenticatorFactory;
import com.ibm.ws.webcontainer.security.internal.WebAuthenticatorFactoryImpl;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

/**
 *
 */
public class FeatureWebSecurityCollaboratorImplTest {

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<SecurityService> securityServiceRef = mock.mock(ServiceReference.class, "securityServiceRef");
    private final SecurityService securityService = mock.mock(SecurityService.class);
    private final AuthenticationService authnService = mock.mock(AuthenticationService.class);
    private final AuthCacheService authCacheService = mock.mock(AuthCacheService.class);

    @SuppressWarnings("unchecked")
    private final ServiceReference<TAIService> taiServiceRef = mock.mock(ServiceReference.class, "taiServiceRef");
    private final TAIService taiService = mock.mock(TAIService.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<TrustAssociationInterceptor> interceptorServiceRef = mock.mock(ServiceReference.class, "interceptorServiceRef");
    private final TrustAssociationInterceptor interceptorService = mock.mock(TrustAssociationInterceptor.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<WebAuthenticator> webAuthenticatorRef = mock.mock(ServiceReference.class, "webAuthenticatorRef");
    private final WebAuthenticator webAuthenticator = mock.mock(WebAuthenticator.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<UnprotectedResourceService> unprotectedServiceRef = mock.mock(ServiceReference.class, "unprotectedServiceRef");
    private final UnprotectedResourceService unprotectedService = mock.mock(UnprotectedResourceService.class);

    private FeatureWebSecurityCollaboratorImpl featureCollab;
    private final WebAuthenticatorFactory authenticatorFactory = new WebAuthenticatorFactoryImpl();

    @Before
    public void setUp() {
        // A bunch of mocking required for activation
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(FeatureWebSecurityCollaboratorImpl.KEY_SECURITY_SERVICE, securityServiceRef);
                will(returnValue(securityService));

                allowing(cc).locateService(FeatureWebSecurityCollaboratorImpl.KEY_TAI_SERVICE, taiServiceRef);
                will(returnValue(taiService));

                allowing(cc).locateService(FeatureWebSecurityCollaboratorImpl.KEY_INTERCEPTOR_SERVICE, interceptorServiceRef);
                will(returnValue(interceptorService));

                allowing(cc).locateService(FeatureWebSecurityCollaboratorImpl.KEY_WEB_AUTHENTICATOR, webAuthenticatorRef);
                will(returnValue(webAuthenticator));

                allowing(cc).locateService(FeatureWebSecurityCollaboratorImpl.KEY_UNPROTECTED_RESOURCE_SERVICE, unprotectedServiceRef);
                will(returnValue(unprotectedService));

                allowing(interceptorServiceRef).getProperty("id");
                will(returnValue("1"));

                allowing(interceptorServiceRef).getProperty("service.id");
                will(returnValue(1L));

                allowing(interceptorServiceRef).getProperty("service.ranking");
                will(returnValue(1L));

                allowing(webAuthenticatorRef).getProperty("component.name");
                will(returnValue("name"));

                allowing(webAuthenticatorRef).getProperty("service.id");
                will(returnValue(1L));

                allowing(webAuthenticatorRef).getProperty("service.ranking");
                will(returnValue(1L));

                allowing(unprotectedServiceRef).getProperty("service.id");
                will(returnValue(11L));

                allowing(unprotectedServiceRef).getProperty("service.ranking");
                will(returnValue(1L));

                allowing(securityService).getAuthenticationService();
                will(returnValue(authnService));

                allowing(authnService).getAuthCacheService();
                will(returnValue(authCacheService));
            }
        });
        featureCollab = new FeatureWebSecurityCollaboratorImpl();

        featureCollab.setAuthenticatorFactory(authenticatorFactory);
        featureCollab.setSecurityService(securityServiceRef);
        featureCollab.setTaiService(taiServiceRef);
        featureCollab.setInterceptorService(interceptorServiceRef);
        featureCollab.setWebAuthenticator(webAuthenticatorRef);
        featureCollab.setUnprotectedResourceService(unprotectedServiceRef);
        featureCollab.activate(cc, null);
    }

    @After
    public void tearDown() {
        featureCollab.unsetAuthenticatorFactory(authenticatorFactory);
        featureCollab.unsetSecurityService(securityServiceRef);
        featureCollab.unsetTaiService(taiServiceRef);
        featureCollab.unsetInterceptorService(interceptorServiceRef);
        featureCollab.unsetWebAuthenticator(webAuthenticatorRef);
        featureCollab.unsetUnprotectedResourceService(unprotectedServiceRef);
        featureCollab.deactivate(cc);

        mock.assertIsSatisfied();
    }

    /**
     * Test method for
     * {@link com.ibm.ws.webcontainer.security.feature.internal.FeatureWebSecurityCollaboratorImpl#activate(org.osgi.service.component.ComponentContext, java.util.Map)}.
     */
    @Test
    public void getApplicationName_noConfig() {
        assertNull("The feature application name must be set in its config. If not, report null",
                   featureCollab.getApplicationName());
    }

}
