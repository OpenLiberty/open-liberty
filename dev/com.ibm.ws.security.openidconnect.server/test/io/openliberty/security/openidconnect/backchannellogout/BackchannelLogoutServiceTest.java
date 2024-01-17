/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.openidconnect.backchannellogout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.Set;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.security.sso.common.Constants;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import test.common.SharedOutputManager;

public class BackchannelLogoutServiceTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final OidcServerConfig oidcServerConfig = mockery.mock(OidcServerConfig.class);

    private BackchannelLogoutService service;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        service = new BackchannelLogoutService();
    }

    @After
    public void tearDown() throws Exception {
        WSSubject.setRunAsSubject(null);
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_normalizeUsername_simple() {
        String input = "jdoe";
        String result = service.normalizeUserName(input);
        assertEquals(input, result);
    }

    @Test
    public void test_normalizeUsername_accessId_simple() {
        String expectedUserName = "jdoe";
        String input = "user:BasicRealm/" + expectedUserName;
        String result = service.normalizeUserName(input);
        assertEquals(expectedUserName, result);
    }

    @Test
    public void test_normalizeUsername_accessId_complex() {
        String expectedUserName = "jdoe";
        String input = "user:http://1.2.3.4/" + expectedUserName;
        String result = service.normalizeUserName(input);
        assertEquals(expectedUserName, result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_requestUriEmpty() {
        String requestUri = "";
        String providerId = "OP";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertFalse("An empty request URI should not have been matched.", result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_requestUriCompletelyDifferent() {
        String requestUri = "/some/other/path/to/logout";
        String providerId = "OP";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertFalse("Request URI [" + requestUri + "] should not have been matched to the provider [" + providerId + "].", result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_contextPathSubstringShouldNotMatch() {
        String providerId = "OP";
        String requestUri = "/someother" + providerId + "/logout";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertFalse("Request URI [" + requestUri + "] should not have been matched to the provider [" + providerId + "].", result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_endpointSubstringShouldNotMatch() {
        String providerId = "OP";
        String requestUri = "/" + providerId + "/logoutEndpoint";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertFalse("Request URI [" + requestUri + "] should not have been matched to the provider [" + providerId + "].", result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_simpleMatch_logout() {
        String providerId = "OP";
        String requestUri = "/" + providerId + "/logout";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertTrue("Request URI [" + requestUri + "] should have been matched to the provider [" + providerId + "].", result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_simpleMatch_endSession() {
        String providerId = "OP";
        String requestUri = "/" + providerId + "/end_session";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertTrue("Request URI [" + requestUri + "] should have been matched to the provider [" + providerId + "].", result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_matchWithContextRoot_logout() {
        String providerId = "OP";
        String requestUri = "/lengthy/context/root/" + providerId + "/logout";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertTrue("Request URI [" + requestUri + "] should have been matched to the provider [" + providerId + "].", result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_matchWithContextRoot_endSession() {
        String providerId = "OP";
        String requestUri = "/lengthy/context/root/" + providerId + "/end_session";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertTrue("Request URI [" + requestUri + "] should have been matched to the provider [" + providerId + "].", result);
    }

    @Test
    public void test_isDelegatedLogoutRequestForConfig_noRunAsSubject() throws WSSecurityException {
        WSSubject.setRunAsSubject(null);
        String expectedProviderId = "OP";
        String expectedIdpId = "SAML_IDP";
        String requestUri = "/" + expectedIdpId + "/slo";

        boolean result = service.isDelegatedLogoutRequestForConfig(requestUri, expectedProviderId);
        assertFalse("Request to [" + requestUri + "] with no run-as subject should not have been considered a delegated logout request.", result);
    }

    @Test
    public void test_isDelegatedLogoutRequestForConfig_noHashtableCredentialInSubject() throws WSSecurityException {
        String expectedProviderId = "OP";
        String expectedIdpId = "SAML_IDP";
        String requestUri = "/" + expectedIdpId + "/slo";

        Subject runAsSubject = new Subject();

        service = new BackchannelLogoutService() {
            @Override
            Subject getRunAsSubject() {
                return runAsSubject;
            }
        };

        boolean result = service.isDelegatedLogoutRequestForConfig(requestUri, expectedProviderId);
        assertFalse("Request to [" + requestUri + "] with no hashtable credentials in the run-as subject should not have been considered a delegated logout request.", result);
    }

    @Test
    public void test_isDelegatedLogoutRequestForConfig_subjectMissingOpProperty() throws WSSecurityException {
        String expectedProviderId = "OP";
        String expectedIdpId = "SAML_IDP";
        String requestUri = "/" + expectedIdpId + "/slo";

        Subject runAsSubject = new Subject();
        Hashtable<String, Object> creds = new Hashtable<>();
        Set<Object> privateCreds = runAsSubject.getPrivateCredentials();
        privateCreds.add(creds);

        service = new BackchannelLogoutService() {
            @Override
            Subject getRunAsSubject() {
                return runAsSubject;
            }
        };

        boolean result = service.isDelegatedLogoutRequestForConfig(requestUri, expectedProviderId);
        assertFalse("Request to [" + requestUri
                    + "] missing the OIDC OP property in the hashtable credentials in the run-as subject should not have been considered a delegated logout request.", result);
    }

    @Test
    public void test_isDelegatedLogoutRequestForConfig_opPropertyMismatch() throws WSSecurityException {
        String expectedProviderId = "OP";
        String providerIdFromSubject = "some other OP";
        String expectedIdpId = "SAML_IDP";
        String requestUri = "/" + expectedIdpId + "/slo";

        Subject runAsSubject = new Subject();
        Hashtable<String, Object> creds = new Hashtable<>();
        creds.put(Constants.WSCREDENTIAL_OIDC_OP_USED, providerIdFromSubject);
        Set<Object> privateCreds = runAsSubject.getPrivateCredentials();
        privateCreds.add(creds);

        service = new BackchannelLogoutService() {
            @Override
            Subject getRunAsSubject() {
                return runAsSubject;
            }
        };

        boolean result = service.isDelegatedLogoutRequestForConfig(requestUri, expectedProviderId);
        assertFalse("Request to [" + requestUri
                    + "] whose run-as subject credential's OIDC OP property doesn't match the expected OP should not have been considered a delegated logout request.", result);
    }

    @Test
    public void test_isDelegatedLogoutRequestForConfig_subjectMissingSamlIdpProperty() throws WSSecurityException {
        String expectedProviderId = "OP";
        String expectedIdpId = "SAML_IDP";
        String requestUri = "/" + expectedIdpId + "/slo";

        Subject runAsSubject = new Subject();
        Hashtable<String, Object> creds = new Hashtable<>();
        creds.put(Constants.WSCREDENTIAL_OIDC_OP_USED, expectedProviderId);
        Set<Object> privateCreds = runAsSubject.getPrivateCredentials();
        privateCreds.add(creds);

        service = new BackchannelLogoutService() {
            @Override
            Subject getRunAsSubject() {
                return runAsSubject;
            }
        };

        boolean result = service.isDelegatedLogoutRequestForConfig(requestUri, expectedProviderId);
        assertFalse("Request to [" + requestUri
                    + "] missing the SAML IDP property in the hashtable credentials in the run-as subject should not have been considered a delegated logout request.", result);
    }

    @Test
    public void test_isDelegatedLogoutRequestForConfig_samlIdpPropertyMismatch() throws WSSecurityException {
        String expectedProviderId = "OP";
        String expectedIdpId = "SAML_IDP";
        String idpIdFromSubject = "Some other SAML_IDP";
        String requestUri = "/" + expectedIdpId + "/slo";

        Subject runAsSubject = new Subject();
        Hashtable<String, Object> creds = new Hashtable<>();
        creds.put(Constants.WSCREDENTIAL_OIDC_OP_USED, expectedProviderId);
        creds.put(Constants.WSCREDENTIAL_SAML_IDP_USED, idpIdFromSubject);
        Set<Object> privateCreds = runAsSubject.getPrivateCredentials();
        privateCreds.add(creds);

        service = new BackchannelLogoutService() {
            @Override
            Subject getRunAsSubject() {
                return runAsSubject;
            }
        };

        boolean result = service.isDelegatedLogoutRequestForConfig(requestUri, expectedProviderId);
        assertFalse("Request to [" + requestUri
                    + "] whose run-as subject credential's SAML IDP property doesn't match the expected OP should not have been considered a delegated logout request.", result);
    }

    @Test
    public void test_isDelegatedLogoutRequestForConfig_requestUriNotSlo() throws WSSecurityException {
        String expectedProviderId = "OP";
        String expectedIdpId = "SAML_IDP";
        String requestUri = "/" + expectedIdpId + "/logout";

        Subject runAsSubject = new Subject();
        Hashtable<String, Object> creds = new Hashtable<>();
        creds.put(Constants.WSCREDENTIAL_OIDC_OP_USED, expectedProviderId);
        creds.put(Constants.WSCREDENTIAL_SAML_IDP_USED, expectedIdpId);
        Set<Object> privateCreds = runAsSubject.getPrivateCredentials();
        privateCreds.add(creds);

        service = new BackchannelLogoutService() {
            @Override
            Subject getRunAsSubject() {
                return runAsSubject;
            }
        };

        boolean result = service.isDelegatedLogoutRequestForConfig(requestUri, expectedProviderId);
        assertFalse("Request to [" + requestUri + "] should not have been considered a delegated logout request.", result);
    }

    @Test
    public void test_isDelegatedLogoutRequestForConfig_samlIdpPropertySupersetOfExpectedIdp() throws WSSecurityException {
        String expectedProviderId = "OP";
        String expectedIdpId = "SAML_IDP";
        String requestUri = "/prefix" + expectedIdpId + "/slo";

        Subject runAsSubject = new Subject();
        Hashtable<String, Object> creds = new Hashtable<>();
        creds.put(Constants.WSCREDENTIAL_OIDC_OP_USED, expectedProviderId);
        creds.put(Constants.WSCREDENTIAL_SAML_IDP_USED, expectedIdpId);
        Set<Object> privateCreds = runAsSubject.getPrivateCredentials();
        privateCreds.add(creds);

        service = new BackchannelLogoutService() {
            @Override
            Subject getRunAsSubject() {
                return runAsSubject;
            }
        };

        boolean result = service.isDelegatedLogoutRequestForConfig(requestUri, expectedProviderId);
        assertFalse("Request to [" + requestUri + "] should not have been considered a delegated logout request.", result);
    }

    @Test
    public void test_isDelegatedLogoutRequestForConfig_valid() throws WSSecurityException {
        String expectedProviderId = "OP";
        String expectedIdpId = "SAML_IDP";
        String requestUri = "/" + expectedIdpId + "/slo";

        Subject runAsSubject = new Subject();
        Hashtable<String, Object> creds = new Hashtable<>();
        creds.put(Constants.WSCREDENTIAL_OIDC_OP_USED, expectedProviderId);
        creds.put(Constants.WSCREDENTIAL_SAML_IDP_USED, expectedIdpId);
        Set<Object> privateCreds = runAsSubject.getPrivateCredentials();
        privateCreds.add(creds);

        service = new BackchannelLogoutService() {
            @Override
            Subject getRunAsSubject() {
                return runAsSubject;
            }
        };

        boolean result = service.isDelegatedLogoutRequestForConfig(requestUri, expectedProviderId);
        assertTrue("Request to [" + requestUri + "] should have been considered a delegated logout request.", result);
    }

    @Test
    public void test_isDelegatedLogoutRequestForConfig_valid_extendedUri() throws WSSecurityException {
        String expectedProviderId = "OP";
        String expectedIdpId = "SAML_IDP";
        String requestUri = "some/extended/path/to/" + expectedIdpId + "/slo";

        Subject runAsSubject = new Subject();
        Hashtable<String, Object> creds = new Hashtable<>();
        creds.put(Constants.WSCREDENTIAL_OIDC_OP_USED, expectedProviderId);
        creds.put(Constants.WSCREDENTIAL_SAML_IDP_USED, expectedIdpId);
        Set<Object> privateCreds = runAsSubject.getPrivateCredentials();
        privateCreds.add(creds);

        service = new BackchannelLogoutService() {
            @Override
            Subject getRunAsSubject() {
                return runAsSubject;
            }
        };

        boolean result = service.isDelegatedLogoutRequestForConfig(requestUri, expectedProviderId);
        assertTrue("Request to [" + requestUri + "] should have been considered a delegated logout request.", result);
    }

    @Test
    public void test_isIdTokenHintIssuedByConfig_issuerIdentifierConfigured_matches() {
        String providerId = "OP";
        String issuerIdentifier = "https://localhost/oidc/endpoint/OP";
        String issuerFromIdTokenHint = issuerIdentifier;
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        boolean result = service.isIdTokenHintIssuedByConfig(providerId, oidcServerConfig, issuerFromIdTokenHint);
        assertTrue("The ID Token should have been considered issued by the OIDC server.", result);
    }

    @Test
    public void test_isIdTokenHintIssuedByConfig_issuerIdentifierConfigured_doesNotMatch() {
        String providerId = "OP";
        String issuerIdentifier = "https://localhost/oidc/endpoint/OP";
        String issuerFromIdTokenHint = "https://localhost/oidc/endpoint/OP2";
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        boolean result = service.isIdTokenHintIssuedByConfig(providerId, oidcServerConfig, issuerFromIdTokenHint);
        assertFalse("The ID Token should not have been considered issued by the OIDC server.", result);
    }

    @Test
    public void test_isIdTokenHintIssuedByConfig_issuerIdentifierNotConfigured_matches() {
        String providerId = "OP";
        String issuerFromIdTokenHint = "https://localhost/oidc/endpoint/" + providerId;
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(null));
            }
        });
        boolean result = service.isIdTokenHintIssuedByConfig(providerId, oidcServerConfig, issuerFromIdTokenHint);
        assertTrue("The ID Token should have been considered issued by the OIDC server.", result);
    }

    @Test
    public void test_isIdTokenHintIssuedByConfig_issuerIdentifierNotConfigured_doesNotMatch() {
        String providerId = "OP";
        String issuerFromIdTokenHint = "https://localhost/oidc/endpoint/OP2";
        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(null));
            }
        });
        boolean result = service.isIdTokenHintIssuedByConfig(providerId, oidcServerConfig, issuerFromIdTokenHint);
        assertFalse("The ID Token should not have been considered issued by the OIDC server.", result);
    }

}
