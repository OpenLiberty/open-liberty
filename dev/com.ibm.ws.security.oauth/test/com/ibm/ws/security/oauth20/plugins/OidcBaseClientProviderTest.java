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
package com.ibm.ws.security.oauth20.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;

/**
 * Validate the OidcBaseClientProviderTest functionality
 */
public class OidcBaseClientProviderTest extends AbstractOidcRegistrationBaseTest {
    private static SharedOutputManager outputMgr;
    private static final int NUM_OF_SAMPLE_OIDCBASECLIENTS = 5;
    private static final String PROVIDER_NAME = "OIDCProviderTestOP";
    private static final List<OidcBaseClient> sampleOidcBaseClients = getsampleOidcBaseClients(NUM_OF_SAMPLE_OIDCBASECLIENTS, PROVIDER_NAME);
    private static final String REQUEST_URL_STRING = "https://localhost:8020/oidc/endpoint/" + PROVIDER_NAME + "/registration";

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final OAuthComponentConfiguration config = mock.mock(OAuthComponentConfiguration.class);
    private final HttpServletRequest request = mock.mock(HttpServletRequest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void setUpAfterClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.restoreStreams();
    }

    /**
     * Test initialization, loadClients and getAll via the 'initialize' function
     */
    @Test
    public void testInitializeAndGetAllClients() {
        OidcBaseClientProvider oidcBaseClientProvider = invokeConstructAndInitializeWithSampleOidcBaseClients();

        testInitializeAndGetAllClientsCommon(oidcBaseClientProvider);

        ConfigUtils.deleteClients(PROVIDER_NAME);
        assertEquals(ConfigUtils.getClients().size(), 0);
    }

    /**
     * Test initialization, loadClients and getAll via the 'init(OAuthComponentConfiguration)' function
     */
    @Test
    public void testInitAndGetAllClients() {
        String methodName = "testInitAndGetAllClients";

        assertEquals("Error setting up test with sample OidcBaseClients.", sampleOidcBaseClients.size(), NUM_OF_SAMPLE_OIDCBASECLIENTS);

        //Instantiate through constructor
        assertEquals("Error in test precondition. ConfigUtils should have zero clients.", ConfigUtils.getClients().size(), 0);
        ConfigUtils.setClients(sampleOidcBaseClients);

        OidcBaseClientProvider oidcBaseClientProvider = new OidcBaseClientProvider(PROVIDER_NAME, null);
        final String[] emptyStringArr = new String[0];

        try {
            mock.checking(new Expectations() {
                {
                    allowing(config).getUniqueId();
                    will(returnValue(PROVIDER_NAME));
                    allowing(config).getConfigPropertyValues(Constants.CLIENT_URI_SUBSTITUTIONS);
                    will(returnValue(emptyStringArr));
                }
            });

            oidcBaseClientProvider.init(config);

            testInitializeAndGetAllClientsCommon(oidcBaseClientProvider);

            ConfigUtils.deleteClients(PROVIDER_NAME);
            assertEquals(ConfigUtils.getClients().size(), 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testExists() {
        OidcBaseClientProvider oidcBaseClientProvider = invokeConstructAndInitializeWithSampleOidcBaseClients();
        for (OidcBaseClient client : sampleOidcBaseClients) {
            assertTrue(oidcBaseClientProvider.exists(client.getClientId()));
        }

        ConfigUtils.deleteClients(PROVIDER_NAME);
        assertEquals(ConfigUtils.getClients().size(), 0);
    }

    @Test
    public void testGet() {
        OidcBaseClientProvider oidcBaseClientProvider = invokeConstructAndInitializeWithSampleOidcBaseClients();

        for (OidcBaseClient sampleClient : sampleOidcBaseClients) {
            OidcBaseClient retrievedClient = oidcBaseClientProvider.get(sampleClient.getClientId());
            assertNotNull(retrievedClient);
            assertEqualsOidcBaseClients(retrievedClient, sampleClient);
        }

        ConfigUtils.deleteClients(PROVIDER_NAME);
        assertEquals(ConfigUtils.getClients().size(), 0);
    }

    @Test
    public void testGetAllRequest() {
        String methodName = "testGetAllRequest";

        OidcBaseClientProvider oidcBaseClientProvider = invokeConstructAndInitializeWithSampleOidcBaseClients();

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(new StringBuffer(REQUEST_URL_STRING)));
                }
            });

            Collection<OidcBaseClient> clients = oidcBaseClientProvider.getAll(request);

            assertEquals(clients.size(), NUM_OF_SAMPLE_OIDCBASECLIENTS);

            Map<String, OidcBaseClient> clientsMap = getOidcBaseClientMap(clients);
            Map<String, OidcBaseClient> sampleClientMap = getOidcBaseClientMap(sampleOidcBaseClients);

            //Ensure the clients from the client provider match what was expected (the sample clients)
            //while adding the modification to check for expected registration uri based on mocked request
            for (int i = 0; i < NUM_OF_SAMPLE_OIDCBASECLIENTS; i++) {
                OidcBaseClient clientProviderClient = clientsMap.remove(String.valueOf(i));
                OidcBaseClient sampleClient = sampleClientMap.remove(String.valueOf(i));

                assertEqualsOidcBaseClients(clientProviderClient, sampleClient);

                //Add check for checking registration URI which isn't in assertEqualsOidcBaseClients
                assertEquals(clientProviderClient.getRegistrationClientUri(), REQUEST_URL_STRING + "/" + clientProviderClient.getClientId());
            }

            assertEquals(clientsMap.size(), 0);
            assertEquals(sampleClientMap.size(), 0);

            ConfigUtils.deleteClients(PROVIDER_NAME);
            assertEquals(ConfigUtils.getClients().size(), 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testValidateClient() {
        OidcBaseClientProvider oidcBaseClientProvider = invokeConstructAndInitializeWithSampleOidcBaseClients();

        for (OidcBaseClient sampleClient : sampleOidcBaseClients) {
            assertTrue(oidcBaseClientProvider.validateClient(sampleClient.getClientId(), sampleClient.getClientSecret()));
        }

        ConfigUtils.deleteClients(PROVIDER_NAME);
        assertEquals(ConfigUtils.getClients().size(), 0);
    }

    @Test
    public void testDeleteOverride() {
        String methodName = "testDeleteOverride";

        OidcBaseClientProvider oidcBaseClientProvider = invokeConstructAndInitializeWithSampleOidcBaseClients();

        try {
            Collection<OidcBaseClient> clients = oidcBaseClientProvider.getAll();

            OidcBaseClient clientToDelete = null;

            //Retrieve one client to update
            for (OidcBaseClient client : clients) {
                clientToDelete = client;
                break;
            }

            assertTrue(oidcBaseClientProvider.exists(clientToDelete.getClientId()));

            oidcBaseClientProvider.deleteOverride(clientToDelete.getClientId());

            assertFalse(oidcBaseClientProvider.exists(clientToDelete.getClientId()));

            ConfigUtils.deleteClients(PROVIDER_NAME);
            assertEquals(ConfigUtils.getClients().size(), 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testUpdate() {
        String methodName = "testUpdate";

        OidcBaseClientProvider oidcBaseClientProvider = invokeConstructAndInitializeWithSampleOidcBaseClients();

        try {
            Collection<OidcBaseClient> clients = oidcBaseClientProvider.getAll();

            OidcBaseClient updatedClient = null;

            //Retrieve one client to update
            for (OidcBaseClient client : clients) {
                updatedClient = client;
                break;
            }

            assertNotNull("Error, client provider should have provided a client.", updatedClient);

            updatedClient.setScope("some new scopes");

            boolean expectedErrorOccurred = false;
            try {
                oidcBaseClientProvider.update(updatedClient);
            } catch (OidcServerException e) {
                expectedErrorOccurred = true;
                assertUnImplementedException(e);
            }

            assertTrue("Exception indicating unimplemented method should have been thrown, but was not.", expectedErrorOccurred);

            ConfigUtils.deleteClients(PROVIDER_NAME);
            assertEquals(ConfigUtils.getClients().size(), 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testDelete() {
        String methodName = "testDelete";

        OidcBaseClientProvider oidcBaseClientProvider = invokeConstructAndInitializeWithSampleOidcBaseClients();

        try {
            Collection<OidcBaseClient> clients = oidcBaseClientProvider.getAll();

            OidcBaseClient clientToDelete = null;

            //Retrieve one client to update
            for (OidcBaseClient client : clients) {
                clientToDelete = client;
                break;
            }

            assertNotNull("Error, client provider should have provided a client.", clientToDelete);

            boolean expectedErrorOccurred = false;
            try {
                oidcBaseClientProvider.delete(clientToDelete.getClientId());
            } catch (OidcServerException e) {
                expectedErrorOccurred = true;
                assertUnImplementedException(e);
            }

            assertTrue("Exception indicating unimplemented method should have been thrown, but was not.", expectedErrorOccurred);

            ConfigUtils.deleteClients(PROVIDER_NAME);
            assertEquals(ConfigUtils.getClients().size(), 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPut() {
        String methodName = "testPut";

        OidcBaseClientProvider oidcBaseClientProvider = invokeConstructAndInitializeWithSampleOidcBaseClients();

        try {
            OidcBaseClient newClient = getSampleOidcBaseClient();
            newClient.setClientId("newClient1234");

            boolean expectedErrorOccurred = false;
            try {
                oidcBaseClientProvider.put(newClient);
            } catch (OidcServerException e) {
                expectedErrorOccurred = true;
                assertUnImplementedException(e);
            }

            assertTrue("Exception indicating unimplemented method should have been thrown, but was not.", expectedErrorOccurred);

            ConfigUtils.deleteClients(PROVIDER_NAME);
            assertEquals(ConfigUtils.getClients().size(), 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private OidcBaseClientProvider invokeConstructAndInitializeWithSampleOidcBaseClients() {
        assertEquals("Error setting up test with sample OidcBaseClients.", sampleOidcBaseClients.size(), NUM_OF_SAMPLE_OIDCBASECLIENTS);

        assertEquals("Error in test precondition. ConfigUtils should have zero clients.", ConfigUtils.getClients().size(), 0);
        ConfigUtils.setClients(sampleOidcBaseClients);

        OidcBaseClientProvider oidcBaseClientProvider = new OidcBaseClientProvider(PROVIDER_NAME, null);

        //Initialize and load clients through 'initialize' routine
        oidcBaseClientProvider.initialize();

        return oidcBaseClientProvider;
    }

    private void testInitializeAndGetAllClientsCommon(OidcBaseClientProvider oidcBaseClientProvider) {
        String methodName = "testInitializeAndGetAllClientsCommon";

        try {
            Collection<OidcBaseClient> clients = oidcBaseClientProvider.getAll();
            assertEquals(clients.size(), NUM_OF_SAMPLE_OIDCBASECLIENTS);

            Map<String, OidcBaseClient> clientsMap = getOidcBaseClientMap(clients);
            Map<String, OidcBaseClient> sampleClientMap = getOidcBaseClientMap(sampleOidcBaseClients);

            //Ensure the clients from the client provider match what was expected (the sample clients)
            for (int i = 0; i < NUM_OF_SAMPLE_OIDCBASECLIENTS; i++) {
                OidcBaseClient clientProviderClient = clientsMap.remove(String.valueOf(i));
                OidcBaseClient sampleClient = sampleClientMap.remove(String.valueOf(i));

                assertEqualsOidcBaseClients(clientProviderClient, sampleClient);
            }

            assertEquals(clientsMap.size(), 0);
            assertEquals(sampleClientMap.size(), 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
