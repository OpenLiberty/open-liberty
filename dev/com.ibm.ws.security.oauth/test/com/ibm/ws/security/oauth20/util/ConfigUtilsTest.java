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
package com.ibm.ws.security.oauth20.util;

import java.util.List;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

/**
 *
 */
public class ConfigUtilsTest extends TestCase {

    private static final int NUM_OF_SAMPLE_OIDCBASECLIENTS = 5;
    private static final String PROVIDER_NAME = "ConfigUtilOP";

    private final Mockery mockery = new JUnit4Mockery();
    private ConfigUtils configUtils;
    private ComponentContext cc;
    private final String oidcProviderName = "testOidcProvider";
    private final String oauth20providerName = "testOAuth20Provider";
    private ServiceReference<OidcServerConfig> testOidcServerConfigRef;
    private OidcServerConfig testOidcServerConfig;
    private ServiceReference<OidcServerConfig> postTestReferenceToRemove;

    @Override
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        configUtils = new ConfigUtils();
        cc = mockery.mock(ComponentContext.class);
        testOidcServerConfigRef = mockery.mock(ServiceReference.class);
        testOidcServerConfig = mockery.mock(OidcServerConfig.class);
        postTestReferenceToRemove = null;
        createComponentContextExpectations();
        configUtils.activate(cc);
    }

    private void createComponentContextExpectations() {
        mockery.checking(new Expectations() {
            {
                allowing(cc).locateService(ConfigUtils.KEY_OIDC_SERVER_CONFIG, testOidcServerConfigRef);
                will(returnValue(testOidcServerConfig));
            }
        });
    }

    @Override
    @After
    public void tearDown() {
        if (postTestReferenceToRemove != null) {
            configUtils.unsetOidcServerConfig(postTestReferenceToRemove);
        }
        configUtils.deactivate(cc);
    }

    private void deleteSampleOidcBaseClientsFromConfigUtils() {
        ConfigUtils.deleteClients(PROVIDER_NAME);

        assertEquals("Error deleting existing clients in ConfigUtils.", 0, ConfigUtils.getClients().size());
    }

    private void insertSampleOidcBaseClientIntoConfigUtils() {
        //Hydrate ConfigUtils with some sample OidcBaseClients
        List<OidcBaseClient> sampleOidcBaseClients = AbstractOidcRegistrationBaseTest.getsampleOidcBaseClients(NUM_OF_SAMPLE_OIDCBASECLIENTS, PROVIDER_NAME);
        ConfigUtils.setClients(sampleOidcBaseClients);

        List<OidcBaseClient> retrievedSampleOidcBaseClients = ConfigUtils.getClients();

        assertEquals(sampleOidcBaseClients.size(), retrievedSampleOidcBaseClients.size());
        assertEquals(sampleOidcBaseClients, retrievedSampleOidcBaseClients);
    }

    /**
     * 
     */
    @Test
    public void testIsCustomPropStringGood() {
        try {
            assertTrue("The String provided is a good one", ConfigUtils.isCustomPropStringGood("\"ka=va\",\"kb=vb\""));
        } catch (Exception e) {
            fail("Should not get exception but get " + e);
        }
    }

    @Test
    public void testGetOidcServerConfigForOAuth20ProviderNoConfig() {
        createOidcServerConfigExpectations(oidcProviderName, oauth20providerName);

        OidcServerConfig oidcServerConfig = ConfigUtils.getOidcServerConfigForOAuth20Provider(oauth20providerName);

        assertNull("There must not be an oidc server config for the OAuth20 provider name.", oidcServerConfig);
    }

    @Test
    public void testGetOidcServerConfigForOAuth20Provider() {
        createOidcServerConfigExpectations(oidcProviderName, oauth20providerName);
        configUtils.setOidcServerConfig(testOidcServerConfigRef);

        OidcServerConfig oidcServerConfig = ConfigUtils.getOidcServerConfigForOAuth20Provider(oauth20providerName);

        assertEquals("The oidc server config must be obtained for the OAuth20 provider name.",
                     testOidcServerConfig, oidcServerConfig);
    }

    @Test
    public void testGetOidcServerConfigForOAuth20ProviderDifferentName() {
        createOidcServerConfigExpectations(oidcProviderName, oauth20providerName);
        configUtils.setOidcServerConfig(testOidcServerConfigRef);

        OidcServerConfig oidcServerConfig = ConfigUtils.getOidcServerConfigForOAuth20Provider("anotherOAuth20Provider");

        assertNull("There must not be an oidc server config for the OAuth20 provider name.", oidcServerConfig);
    }

    @Test
    public void testGetOidcServerConfigForOAuth20ProviderRemoved() {
        createOidcServerConfigExpectations(oidcProviderName, oauth20providerName);
        configUtils.setOidcServerConfig(testOidcServerConfigRef);
        configUtils.unsetOidcServerConfig(testOidcServerConfigRef);

        OidcServerConfig oidcServerConfig = ConfigUtils.getOidcServerConfigForOAuth20Provider(oauth20providerName);

        assertNull("There must not be an oidc server config for the OAuth20 provider name.", oidcServerConfig);
    }

    private void createOidcServerConfigExpectations(final String oidcProviderName, final String oauth20providerName) {
        oidcServerConfigRefExpectations(oidcProviderName);
        oidcServerConfigExpectations(oauth20providerName);
    }

    private void oidcServerConfigRefExpectations(final String oidcProviderName) {
        mockery.checking(new Expectations() {
            {
                allowing(testOidcServerConfigRef).getProperty("id");
                will(returnValue(oidcProviderName));
                allowing(testOidcServerConfigRef).getProperty("service.id"); // Related to ConcurrentServiceReferenceMap internal code
                will(returnValue(Long.valueOf(1234)));
                allowing(testOidcServerConfigRef).getProperty("service.ranking"); // Related to ConcurrentServiceReferenceMap internal code
                will(returnValue(Integer.valueOf(0)));
            }
        });
        postTestReferenceToRemove = testOidcServerConfigRef;
    }

    private void oidcServerConfigExpectations(final String oauth20providerName) {
        mockery.checking(new Expectations() {
            {
                allowing(testOidcServerConfig).getProviderId(); //
                will(returnValue(oidcProviderName)); //
                allowing(testOidcServerConfig).getOauthProviderName();
                will(returnValue(oauth20providerName));
            }
        });
    }

    @Test
    public void testAccessorModfierForOidcBaseClients() {
        //Reset ConfigUtils to contain 0 clients
        deleteSampleOidcBaseClientsFromConfigUtils();

        List<OidcBaseClient> sampleOidcBaseClients = AbstractOidcRegistrationBaseTest.getsampleOidcBaseClients(NUM_OF_SAMPLE_OIDCBASECLIENTS, PROVIDER_NAME);
        ConfigUtils.setClients(sampleOidcBaseClients);

        List<OidcBaseClient> retrievedSampleOidcBaseClients = ConfigUtils.getClients();

        assertEquals(sampleOidcBaseClients.size(), retrievedSampleOidcBaseClients.size());
        assertEquals(sampleOidcBaseClients, retrievedSampleOidcBaseClients);

        //Behavior of setClients is to continually add on of existing, not replace
        List<OidcBaseClient> sampleOidcBaseClients2 = AbstractOidcRegistrationBaseTest.getsampleOidcBaseClients(NUM_OF_SAMPLE_OIDCBASECLIENTS, PROVIDER_NAME);
        ConfigUtils.setClients(sampleOidcBaseClients2);

        List<OidcBaseClient> retrievedSampleOidcBaseClients2 = ConfigUtils.getClients();

        //Verify size doubled
        assertEquals(sampleOidcBaseClients.size() + sampleOidcBaseClients2.size(), retrievedSampleOidcBaseClients2.size());

        //Verify that the doubled content is equal
        assertEquals(retrievedSampleOidcBaseClients2.subList(0, 0 + NUM_OF_SAMPLE_OIDCBASECLIENTS), sampleOidcBaseClients);
        assertEquals(retrievedSampleOidcBaseClients2.subList(0 + NUM_OF_SAMPLE_OIDCBASECLIENTS, 0 + 2 * NUM_OF_SAMPLE_OIDCBASECLIENTS), sampleOidcBaseClients2);
    }

    @Test
    public void testDeleteOidcBaseClients() {
        //Reset ConfigUtils to contain 0 clients
        deleteSampleOidcBaseClientsFromConfigUtils();
        //Hydrate ConfigUtils with sample clients for this test
        insertSampleOidcBaseClientIntoConfigUtils();

        List<OidcBaseClient> retrievedSampleOidcBaseClients = ConfigUtils.getClients();

        //Verify that all the retrieved clients provider name are the same as the constants, this is needed to perform test
        for (OidcBaseClient sampleOidcBaseClient : retrievedSampleOidcBaseClients) {
            if (!sampleOidcBaseClient.getComponentId().equals(PROVIDER_NAME)) {
                fail("Error in setup of test, because the component id's retrieved should have all been: " + PROVIDER_NAME);
            }
        }

        ConfigUtils.deleteClients(PROVIDER_NAME);

        List<OidcBaseClient> retrieveDeletedOidcBaseClients = ConfigUtils.getClients();
        assertEquals(0, retrieveDeletedOidcBaseClients.size());
    }
}
