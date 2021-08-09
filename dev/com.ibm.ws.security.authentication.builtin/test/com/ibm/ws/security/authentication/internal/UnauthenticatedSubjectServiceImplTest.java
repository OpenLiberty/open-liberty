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
package com.ibm.ws.security.authentication.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.credentials.CredentialProvider;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;

/**
 *
 */
@SuppressWarnings("unchecked")
public class UnauthenticatedSubjectServiceImplTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<SecurityService> securityServiceRef = mock.mock(ServiceReference.class, "securityServiceRef");
    private final SecurityService securityService = mock.mock(SecurityService.class);
    private final AuthenticationService authenticationService = mock.mock(AuthenticationService.class);
    private final UserRegistryService userRegistryService = mock.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);
    private final ServiceReference<CredentialsService> credentialServiceRef = mock.mock(ServiceReference.class, "credentialServiceRef");
    private final CredentialsService credentialService = mock.mock(CredentialsService.class);
    private final ServiceReference<CredentialProvider> credentialProviderRef = mock.mock(ServiceReference.class, "credentialProviderRef");
    private UnauthenticatedSubjectServiceImpl unauthSrv;

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(credentialServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(credentialServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(cc).locateService(UnauthenticatedSubjectServiceImpl.KEY_CREDENTIALS_SERVICE, credentialServiceRef);
                will(returnValue(credentialService));

                allowing(securityServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(securityServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(cc).locateService(UnauthenticatedSubjectServiceImpl.KEY_SECURITY_SERVICE, securityServiceRef);
                will(returnValue(securityService));

                allowing(securityService).getAuthenticationService();
                will(returnValue(authenticationService));
                allowing(securityService).getUserRegistryService();
                will(returnValue(userRegistryService));
            }
        });

        unauthSrv = new UnauthenticatedSubjectServiceImpl();
        unauthSrv.setCredentialsService(credentialServiceRef);
        unauthSrv.setSecurityService(securityServiceRef);
        unauthSrv.activate(cc);
    }

    @After
    public void tearDown() {
        unauthSrv.unsetSecurityService(securityServiceRef);
        unauthSrv.unsetCredentialsService(credentialServiceRef);
        unauthSrv.deactivate(cc);

        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.UnauthenticatedSubjectService#getUnauthenticatedSubject()}.
     */
    @Test
    public void getUnauthenticatedSubject_noRegistry() throws Exception {
        mock.checking(new Expectations() {
            {
                one(credentialService).getUnauthenticatedUserid();
                will(returnValue("UNAUTHENTICATED"));
                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(false));

                Subject unauthSubject = new Subject();
                one(authenticationService).authenticate(with(JaasLoginConfigConstants.SYSTEM_UNAUTHENTICATED), with(any(Subject.class)));
                will(returnValue(unauthSubject));
            }
        });
        Subject unauthSubject = unauthSrv.getUnauthenticatedSubject();
        assertNotNull("Unauthenticated subject not created",
                      unauthSubject);
        assertSame("Did not get back cached Subject",
                   unauthSubject, unauthSrv.getUnauthenticatedSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.UnauthenticatedSubjectService#getUnauthenticatedSubject()}.
     */
    @Test
    public void getUnauthenticatedSubject_withRegistry() throws Exception {
        mock.checking(new Expectations() {
            {
                one(credentialService).getUnauthenticatedUserid();
                will(returnValue("UNAUTHENTICATED"));
                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(true));
                one(userRegistryService).getUserRegistry();
                will(returnValue(userRegistry));
                one(userRegistry).getRealm();
                will(returnValue("myRealm"));

                Subject unauthSubject = new Subject();
                one(authenticationService).authenticate(with(JaasLoginConfigConstants.SYSTEM_UNAUTHENTICATED), with(any(Subject.class)));
                will(returnValue(unauthSubject));
            }
        });
        Subject unauthSubject = unauthSrv.getUnauthenticatedSubject();
        assertNotNull("Unauthenticated subject not created",
                      unauthSubject);
        assertSame("Did not get back cached Subject",
                   unauthSubject, unauthSrv.getUnauthenticatedSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.UnauthenticatedSubjectService#setCredentialProvider(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setCredentialProvider() throws Exception {
        mock.checking(new Expectations() {
            {
                one(credentialService).getUnauthenticatedUserid();
                will(returnValue("UNAUTHENTICATED"));
                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(false));

                Subject unauthSubject = new Subject();
                one(authenticationService).authenticate(with(JaasLoginConfigConstants.SYSTEM_UNAUTHENTICATED), with(any(Subject.class)));
                will(returnValue(unauthSubject));
            }
        });
        Subject unauthSubject = unauthSrv.getUnauthenticatedSubject();
        assertNotNull("Unauthenticated subject not created",
                      unauthSubject);

        mock.checking(new Expectations() {
            {
                one(credentialService).getUnauthenticatedUserid();
                will(returnValue("UNAUTHENTICATED"));
                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(false));

                Subject unauthSubject = new Subject();
                one(authenticationService).authenticate(with(JaasLoginConfigConstants.SYSTEM_UNAUTHENTICATED), with(any(Subject.class)));
                will(returnValue(unauthSubject));
            }
        });
        unauthSrv.setCredentialProvider(credentialProviderRef);
        assertNotSame("Incorrectly got back cached Subject",
                      unauthSubject, unauthSrv.getUnauthenticatedSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.UnauthenticatedSubjectService#unsetCredentialProvider(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void unsetCredentialProvider() throws Exception {
        mock.checking(new Expectations() {
            {
                one(credentialService).getUnauthenticatedUserid();
                will(returnValue("UNAUTHENTICATED"));
                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(false));

                Subject unauthSubject = new Subject();
                one(authenticationService).authenticate(with(JaasLoginConfigConstants.SYSTEM_UNAUTHENTICATED), with(any(Subject.class)));
                will(returnValue(unauthSubject));
            }
        });
        Subject unauthSubject = unauthSrv.getUnauthenticatedSubject();
        assertNotNull("Unauthenticated subject not created",
                      unauthSubject);

        mock.checking(new Expectations() {
            {
                one(credentialService).getUnauthenticatedUserid();
                will(returnValue("UNAUTHENTICATED"));
                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(false));

                Subject unauthSubject = new Subject();
                one(authenticationService).authenticate(with(JaasLoginConfigConstants.SYSTEM_UNAUTHENTICATED), with(any(Subject.class)));
                will(returnValue(unauthSubject));
            }
        });
        unauthSrv.unsetCredentialProvider(credentialProviderRef);
        assertNotSame("Incorrectly got back cached Subject",
                      unauthSubject, unauthSrv.getUnauthenticatedSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.UnauthenticatedSubjectService#notifyOfUserRegistryChange()}.
     */
    @Test
    public void notifyOfUserRegistryChange() throws Exception {
        mock.checking(new Expectations() {
            {
                one(credentialService).getUnauthenticatedUserid();
                will(returnValue("UNAUTHENTICATED"));
                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(false));

                Subject unauthSubject = new Subject();
                one(authenticationService).authenticate(with(JaasLoginConfigConstants.SYSTEM_UNAUTHENTICATED), with(any(Subject.class)));
                will(returnValue(unauthSubject));
            }
        });
        Subject unauthSubject = unauthSrv.getUnauthenticatedSubject();
        assertNotNull("Unauthenticated subject not created",
                      unauthSubject);

        mock.checking(new Expectations() {
            {
                one(credentialService).getUnauthenticatedUserid();
                will(returnValue("UNAUTHENTICATED"));
                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(false));

                Subject unauthSubject = new Subject();
                one(authenticationService).authenticate(with(JaasLoginConfigConstants.SYSTEM_UNAUTHENTICATED), with(any(Subject.class)));
                will(returnValue(unauthSubject));
            }
        });
        unauthSrv.notifyOfUserRegistryChange();
        assertNotSame("Incorrectly got back cached Subject",
                      unauthSubject, unauthSrv.getUnauthenticatedSubject());
    }

}
