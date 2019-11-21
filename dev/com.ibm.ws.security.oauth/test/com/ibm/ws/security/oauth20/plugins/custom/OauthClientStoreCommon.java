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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.google.gson.JsonObject;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.security.oauth20.store.OAuthClient;
import com.ibm.websphere.security.oauth20.store.OAuthStore;
import com.ibm.websphere.security.oauth20.store.OAuthStoreException;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.HashSecretUtils;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;

import test.common.SharedOutputManager;

@Ignore
public class OauthClientStoreCommon extends AbstractOidcRegistrationBaseTest {

    protected static final String componentId = "OP1";
    private static final String PROVIDER2 = "OP2";

    protected OauthClientStore oauthClientStore;
    protected OAuthStore oauthStore;
    private List<OidcBaseClient> op1Clients;
    private OidcBaseClient op1Client0;
    private OidcBaseClient op1Client1;
    private OidcBaseClient op2Client;
    JsonObject op1Client0AsJson;
    JsonObject op1Client1AsJson;
    JsonObject op2ClientAsJson;

    private final Mockery mockery = new JUnit4Mockery();

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.oauth20.plugins.custom.*=all");

    @Rule
    public TestRule managerRule = outputMgr;

    @Before
    public void setUp() throws Exception {

        op1Clients = AbstractOidcRegistrationBaseTest.getsampleOidcBaseClients(2, componentId);
        op2Client = AbstractOidcRegistrationBaseTest.getSampleOidcBaseClient(PROVIDER2);
        op1Client0 = op1Clients.get(0);
        op1Client1 = op1Clients.get(1);
        op1Client0AsJson = getJsonWithExtraProperties(op1Client0);
        op1Client1AsJson = getJsonWithExtraProperties(op1Client1);
        op2ClientAsJson = getJsonWithExtraProperties(op2Client);

        oauthStore = mockery.mock(OAuthStore.class);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthClientStore#exists(java.lang.String)}.
     */
    @Test
    public void testExists() throws Exception {
        OAuthClient oauthClient = getOAuthClient(op1Client0);
        String clientId = op1Client0.getClientId();
        oauthStoreReadWillReturnClient(clientId, oauthClient);

        assertTrue("The client must exist in the store.", oauthClientStore.exists(clientId));
    }

    @Test
    public void testExists_OAuthStoreException() throws Exception {
        String clientId = op1Client0.getClientId();
        readThrowsOAuthStoreException(clientId);

        try {
            boolean result = oauthClientStore.exists(clientId);
            fail("Should have thrown exception but did not. Result was " + result);
        } catch (OidcServerException e) {
            String msgRegex = "CWWKS1467E.+" + clientId;
            verifyExceptionAndLogMessagesOidc(e, msgRegex);
        }
    }

    private void readThrowsOAuthStoreException(final String clientId) throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(oauthStore).readClient(componentId, clientId);
                will(throwException(new OAuthStoreException(defaultExceptionMsg)));
            }
        });
    }

    void verifyExceptionAndLogMessages(Exception e, String msgRegex) {
        verifyException(e, msgRegex);
        assertFalse("Exception message should not have contained OAuthStoreException message but did.", e.getLocalizedMessage().contains(defaultExceptionMsg));
        verifyLogMessage(outputMgr, msgRegex + ".+" + Pattern.quote(defaultExceptionMsg));
    }

    void verifyExceptionAndLogMessagesOidc(OidcServerException e, String msgRegex) {
        verifyExceptionString(e.getErrorDescription(), msgRegex);
        assertFalse("Exception message should not have contained OAuthStoreException message but did.", e.getErrorDescription().contains(defaultExceptionMsg));
        verifyLogMessage(outputMgr, msgRegex + ".+" + Pattern.quote(defaultExceptionMsg));
    }

    private OAuthClient getOAuthClient(OidcBaseClient client) {
        JsonObject clientMetadataAsJson = OidcOAuth20Util.getJsonObj(client);

        String clientSecret = client.getClientSecret();

        if (clientSecret != null && !clientSecret.isEmpty()) {
            if (isHash) {
                clientSecret = HashSecretUtils.hashSecret(client.getClientSecret(), client.getClientId(), true, clientMetadataAsJson);
            } else {
                clientSecret = PasswordUtil.passwordEncode(clientSecret);
            }
        }

        if (clientMetadataAsJson != null && clientMetadataAsJson.has(OAuth20Constants.CLIENT_SECRET)) {
            String metaClientSecret = clientMetadataAsJson.get(OAuth20Constants.CLIENT_SECRET).getAsString();
            if (metaClientSecret != null && !metaClientSecret.isEmpty()) {
                if (isHash) {
                    HashSecretUtils.hashClientMetaTypeSecret(clientMetadataAsJson, client.getClientId(), true);
                } else {
                    clientMetadataAsJson.addProperty(OAuth20Constants.CLIENT_SECRET, PasswordUtil.passwordEncode(metaClientSecret));
                }
            }
        }

        String clientMetadata = clientMetadataAsJson.toString(); // TODO: Determine if client_id needs to be removed from metadata.
        return new OAuthClient(componentId, client.getClientId(), clientSecret, client.getClientName(), client.isEnabled(), clientMetadata);
    }

    @Test
    public void testExists_multitenantStore_clientFromDifferentProviderReturnsFalse() throws Exception {
        String clientId = op2Client.getClientId();
        oauthStoreReadWillReturnClient(clientId, null);

        assertFalse("The client must not exist in the store.", oauthClientStore.exists(clientId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthClientStore#get(java.lang.String)}.
     */
    @Test
    public void testGet() throws Exception {
        final String clientId = op1Client0.getClientId();
        OAuthClient oauthClient = getOAuthClient(op1Client0);
        oauthStoreReadWillReturnClient(clientId, oauthClient);

        OidcBaseClient retrievedClient = oauthClientStore.get(clientId);

        assertNotNull("The client must be found in the store.", retrievedClient);
        AbstractOidcRegistrationBaseTest.assertEqualsOidcBaseClients(op1Client0, retrievedClient);
    }

    @Test
    public void testGet_OAuthStoreException() throws Exception {
        final String clientId = op1Client0.getClientId();
        readThrowsOAuthStoreException(clientId);

        try {
            OidcBaseClient retrievedClient = oauthClientStore.get(clientId);
            fail("Should have thrown exception but did not. Result was " + retrievedClient);
        } catch (OidcServerException e) {
            String msgRegex = "CWWKS1467E.+" + clientId;
            verifyExceptionAndLogMessagesOidc(e, msgRegex);
        }
    }

    private JsonObject getJsonWithExtraProperties(OidcBaseClient client) {
        JsonObject clientAsJson = getJsonWithComponetIdAndEnabledProperties(client);
        String secret = null;
        if (isHash) {
            secret = HashSecretUtils.hashSecret(client.getClientSecret(), client.getClientId(), true, clientAsJson);
        } else {
            secret = PasswordUtil.passwordEncode(client.getClientSecret());
        }
        clientAsJson.addProperty(OAuth20Constants.CLIENT_SECRET, secret);
        return clientAsJson;
    }

    private JsonObject getJsonWithComponetIdAndEnabledProperties(OidcBaseClient client) {
        JsonObject clientAsJson = OidcOAuth20Util.getJsonObj(client);
        clientAsJson.addProperty("component_id", componentId);
        clientAsJson.addProperty("enabled", client.isEnabled());
        return clientAsJson;
    }

    private void oauthStoreReadWillReturnClient(final String clientId, final OAuthClient oauthClient) throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(oauthStore).readClient(componentId, clientId);
                will(returnValue(oauthClient));
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthClientStore#validateClient(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testValidateClient() throws Exception {
        OAuthClient oauthClient = getOAuthClient(op2Client);
        oauthStoreReadWillReturnClient(op2Client.getClientId(), oauthClient);

        assertTrue("The client must be valid.", oauthClientStore.validateClient(op2Client.getClientId(), op2Client.getClientSecret()));
    }

    @Test
    public void testValidateClient_OAuthStoreException() throws Exception {
        String clientId = op2Client.getClientId();
        readThrowsOAuthStoreException(clientId);

        try {
            boolean result = oauthClientStore.validateClient(op2Client.getClientId(), op2Client.getClientSecret());
            fail("Should have thrown exception but did not. Result was " + result);
        } catch (OidcServerException e) {
            String msgRegex = "CWWKS1467E.+" + clientId;
            verifyExceptionAndLogMessagesOidc(e, msgRegex);
        }
    }

    @Test
    public void testValidateClient_nullClientSecret() throws Exception {
        doesNotReadClientFromStore(op2Client);

        assertFalse("The client must not be valid because the client secret is null.", oauthClientStore.validateClient(op2Client.getClientId(), null));
    }

    @Test
    public void testValidateClient_emptyClientSecret() throws Exception {
        doesNotReadClientFromStore(op2Client);

        assertFalse("The client must not be valid because the client secret is empty.", oauthClientStore.validateClient(op2Client.getClientId(), " "));
    }

    private void doesNotReadClientFromStore(final OidcBaseClient client) throws Exception {
        mockery.checking(new Expectations() {
            {
                never(oauthStore).readClient(componentId, client.getClientId());
            }
        });
    }

    @Test
    public void testValidateClient_badClientSecret() throws Exception {
        OAuthClient oauthClient = getOAuthClient(op2Client);
        oauthStoreReadWillReturnClient(op2Client.getClientId(), oauthClient);

        assertFalse("The client must not be valid because the client secret is bad.", oauthClientStore.validateClient(op2Client.getClientId(), "badClientSecret"));
    }

    @Test
    public void testValidateClient_doesNotExist() throws Exception {
        String clientId = "clientDoesNotExist";
        oauthStoreReadWillReturnClient(clientId, null);

        assertFalse("The client must not be valid because it does not exist.", oauthClientStore.validateClient(clientId, op2Client.getClientSecret()));
    }

    @Test
    public void testValidateClient_clientNotConfidentialEmptySecret() throws Exception {
        op2Client.setClientSecret("");
        OAuthClient oauthClient = getOAuthClient(op2Client);
        oauthStoreReadWillReturnClient(op2Client.getClientId(), oauthClient);

        assertFalse("The client must not be valid because the client is not confidential and has empty secret.", oauthClientStore.validateClient(op2Client.getClientId(), ""));
    }

    @Test
    public void testValidateClient_clientNotConfidentialAnotherEmptySecret() throws Exception {
        op2Client.setClientSecret(" ");
        OAuthClient oauthClient = getOAuthClient(op2Client);
        oauthStoreReadWillReturnClient(op2Client.getClientId(), oauthClient);

        assertFalse("The client must not be valid because the client is not confidential and has null secret.", oauthClientStore.validateClient(op2Client.getClientId(), " "));
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthClientStore#delete(java.lang.String)}.
     */
    @Test
    public void testDelete() throws Exception {
        final String lookupKey = "0";
        mockery.checking(new Expectations() {
            {
                one(oauthStore).deleteClient(componentId, lookupKey);
            }
        });

        assertTrue("The client must be deleted from the store.", oauthClientStore.delete(lookupKey));
    }

    @Test
    public void testDelete_OAuthStoreException() throws Exception {
        final String clientId = "0";
        mockery.checking(new Expectations() {
            {
                one(oauthStore).deleteClient(componentId, clientId);
                will(throwException(new OAuthStoreException(defaultExceptionMsg)));
            }
        });

        try {
            boolean result = oauthClientStore.delete(clientId);
            fail("Should have thrown exception but did not. Result was " + result);
        } catch (OidcServerException e) {
            String msgRegex = "CWWKS1476E.+" + clientId;
            verifyExceptionAndLogMessagesOidc(e, msgRegex);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthClientStore#getAll()}.
     */
    @Test
    public void testGetAll() throws Exception {
        oauthStoreReadWillReturnClients();

        Collection<OidcBaseClient> clients = oauthClientStore.getAll();

        assertEquals("There must be a collection of clients.", 2, clients.size());
        assertEquals("The registration URI must not be changed in the clients.", AbstractOidcRegistrationBaseTest.REGISTRATION_CLIENT_URI,
                clients.iterator().next().getRegistrationClientUri());
    }

    @Test
    public void testGetAll_OAuthException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oauthStore).readAllClients(componentId, "");
                will(throwException(new OAuthStoreException(defaultExceptionMsg)));
            }
        });

        try {
            Collection<OidcBaseClient> clients = oauthClientStore.getAll();
            fail("Should have thrown exception but did not. Result was " + clients);
        } catch (OidcServerException e) {
            String msgRegex = "CWWKS1468E";
            verifyExceptionAndLogMessagesOidc(e, msgRegex);
        }
    }

    @Test
    public void testGetAll_noClientsReturnsEmptyCollection() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oauthStore).readAllClients(componentId, ""); // TODO: Fix when attribute is determined.
                will(returnValue(null));
            }
        });

        Collection<OidcBaseClient> clients = oauthClientStore.getAll();

        assertTrue("There must be an empty collection. Collection contained " + clients, clients.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthClientStore#getAll(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void testGetAllHttpServletRequest() throws Exception {
        final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
        final String registrationURLString = "https://localhost:8020/oidc/endpoint/" + componentId + "/registration/";
        mockery.checking(new Expectations() {
            {
                allowing(request).getRequestURL();
                will(returnValue(new StringBuffer(registrationURLString)));
            }
        });
        oauthStoreReadWillReturnClients();

        Collection<OidcBaseClient> clients = oauthClientStore.getAll(request);

        assertEquals("There must be a collection of clients.", 2, clients.size());
        OidcBaseClient client = clients.iterator().next();
        assertEquals("The registration URI must be set in the clients.", registrationURLString + client.getClientId(), client.getRegistrationClientUri());
    }

    private void oauthStoreReadWillReturnClients() throws Exception {
        OAuthClient oauthClient0 = getOAuthClient(op1Client0);
        OAuthClient oauthClient1 = getOAuthClient(op1Client0);
        final Collection<OAuthClient> oauthClients = new ArrayList<OAuthClient>();
        oauthClients.add(oauthClient0);
        oauthClients.add(oauthClient1);

        mockery.checking(new Expectations() {
            {
                one(oauthStore).readAllClients(componentId, "");
                will(returnValue(oauthClients));
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthClientStore#put(com.ibm.ws.security.oauth20.plugins.OidcBaseClient)}.
     */
    @Test
    public void testPut() throws Exception {
        final OAuthClient oauthClient = getOAuthClient(op2Client);
        mockery.checking(new Expectations() {
            {
                one(oauthStore).create(oauthClient);
            }
        });

        AbstractOidcRegistrationBaseTest.assertEqualsOidcBaseClients(op2Client, oauthClientStore.put(op2Client));
    }

    @Test
    public void testPut_OAuthStoreException() throws Exception {
        final OAuthClient oauthClient = getOAuthClient(op2Client);
        mockery.checking(new Expectations() {
            {
                one(oauthStore).create(oauthClient);
                will(throwException(new OAuthStoreException(defaultExceptionMsg)));
            }
        });

        try {
            OidcBaseClient result = oauthClientStore.put(op2Client);
            fail("Should have thrown exception but did not. Result was " + result);
        } catch (OidcServerException e) {
            String msgRegex = "CWWKS1464E.+" + op2Client.getClientId();
            verifyExceptionAndLogMessagesOidc(e, msgRegex);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthClientStore#update(com.ibm.ws.security.oauth20.plugins.OidcBaseClient)}.
     */
    @Test
    public void testUpdate() throws Exception {
        final OAuthClient oauthClient = getOAuthClient(op2Client);
        mockery.checking(new Expectations() {
            {
                one(oauthStore).update(oauthClient);
            }
        });

        OidcBaseClient newOp2Client = oauthClientStore.update(op2Client);

        AbstractOidcRegistrationBaseTest.assertEqualsOidcBaseClients(op2Client, newOp2Client);
    }

    @Test
    public void testUpdate_OAuthStoreException() throws Exception {
        final OAuthClient oauthClient = getOAuthClient(op2Client);
        mockery.checking(new Expectations() {
            {
                one(oauthStore).update(oauthClient);
                will(throwException(new OAuthStoreException(defaultExceptionMsg)));
            }
        });

        try {
            OidcBaseClient result = oauthClientStore.update(op2Client);
            fail("Should have thrown exception but did not. Result was " + result);
        } catch (OidcServerException e) {
            String msgRegex = "CWWKS1473E.+" + op2Client.getClientId();
            verifyExceptionAndLogMessagesOidc(e, msgRegex);
        }
    }

}
