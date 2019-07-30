/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.custom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.regex.Pattern;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.action.VoidAction;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.google.gson.JsonObject;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.security.oauth20.store.OAuthConsent;
import com.ibm.websphere.security.oauth20.store.OAuthStore;
import com.ibm.websphere.security.oauth20.store.OAuthStoreException;
import com.ibm.ws.security.oauth20.api.OauthConsentStore;

import test.common.SharedOutputManager;

public class OauthConsentStoreImplTest {

    private static final String defaultExceptionMsg = "This is an exception message.";

    private final Mockery mockery = new JUnit4Mockery();
    private final String clientId = "clientId";
    private final String user = "test user";
    private final String scopeAsString = "scope1";
    private final String resource = "test resource";
    private final String providerId = "OP";
    private final int lifetimeInSeconds = 300;
    private long expires;
    private String consentProperties;
    private OauthConsentStore oauthConsentStore;
    private OAuthStore oauthStore;

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.oauth20.plugins.custom.*=all");

    @Rule
    public TestRule managerRule = outputMgr;

    @Before
    public void setUp() throws Exception {
        expires = getExpires(lifetimeInSeconds);
        consentProperties = getConsentProperties(resource);
        oauthStore = mockery.mock(OAuthStore.class);
        oauthConsentStore = new OauthConsentStoreImpl(providerId, oauthStore, 0);
    }

    private String getConsentProperties(String resource) {
        JsonObject extendedFields = new JsonObject();
        if (resource != null) {
            extendedFields.addProperty(OAuth20Constants.RESOURCE, resource);
        } else {
            extendedFields.addProperty("", "");
        }

        return extendedFields.toString();
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.oauth20.plugins.custom.OauthConsentStoreImpl#addConsent(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int)}.
     */
    @Test
    public void testAddConsent() throws Exception {
        createsConsent(new VoidAction());

        oauthConsentStore.addConsent(clientId, user, scopeAsString, resource, providerId, lifetimeInSeconds);
    }

    @Test
    public void testAddConsent_OAuthStoreException() throws Exception {
        createsConsent(Expectations.throwException(new OAuthStoreException(defaultExceptionMsg)));

        oauthConsentStore.addConsent(clientId, user, scopeAsString, resource, providerId, lifetimeInSeconds);

        String msgRegex = "CWWKS1466E";
        verifyLogMessage(outputMgr, msgRegex);
    }

    private void createsConsent(final Action action) throws OAuthStoreException {
        mockery.checking(new Expectations() {
            {
                one(oauthStore).create(with(new BaseMatcher<OAuthConsent>() {
                    {
                    }

                    @Override
                    public boolean matches(Object arg0) {
                        if (arg0 == null || arg0 instanceof OAuthConsent == false) {
                            return false;
                        }

                        OAuthConsent oauthConsent = (OAuthConsent) arg0;

                        // Expiration time is dynamic and equals cannot be used. Check all other fields.
                        return clientId.equals(oauthConsent.getClientId()) && user.equals(oauthConsent.getUser()) && scopeAsString.equals(oauthConsent.getScope()) &&
                               resource.equals(oauthConsent.getResource()) && providerId.equals(oauthConsent.getProviderId())
                               && consentProperties.equals(oauthConsent.getConsentProperties());
                    }

                    @Override
                    public void describeTo(Description arg0) {
                        // TODO Auto-generated method stub

                    }
                }));
                will(action);
            }
        });
    }

    private long getExpires(int lifetimeInSeconds) {
        long expires = 0;
        if (lifetimeInSeconds > 0) {
            expires = new Date().getTime() + (1000L * lifetimeInSeconds);
        }
        return expires;
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.oauth20.plugins.custom.OauthConsentStoreImpl#validateConsent(java.lang.String, java.lang.String, java.lang.String, java.lang.String[], java.lang.String)}.
     */
    @Test
    public void testValidateConsent() throws Exception {
        final OAuthConsent oauthConsent = new OAuthConsent(clientId, user, scopeAsString, resource, providerId, expires, consentProperties);
        mockery.checking(new Expectations() {
            {
                one(oauthStore).readConsent(providerId, user, clientId, resource);
                will(returnValue(oauthConsent));
            }
        });

        String[] scopes = new String[] { scopeAsString };

        boolean valid = oauthConsentStore.validateConsent(clientId, user, providerId, scopes, resource);

        assertTrue("The consent must be valid.", valid);
    }

    @Test
    public void testValidateConsent_OAuthStoreException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oauthStore).readConsent(providerId, user, clientId, resource);
                will(throwException(new OAuthStoreException(defaultExceptionMsg)));
            }
        });

        String[] scopes = new String[] { scopeAsString };

        boolean valid = oauthConsentStore.validateConsent(clientId, user, providerId, scopes, resource);

        assertFalse("The consent must not be valid.", valid);
        String msgRegex = "CWWKS1472E";
        verifyLogMessage(outputMgr, msgRegex);
    }

    private void verifyLogMessage(SharedOutputManager outputMgr, String msgRegex) {
        String messageRegex = msgRegex + ".+" + Pattern.quote(defaultExceptionMsg);
        assertTrue("Did not find message [" + messageRegex + "] in log.", outputMgr.checkForMessages(messageRegex));
    }

}
