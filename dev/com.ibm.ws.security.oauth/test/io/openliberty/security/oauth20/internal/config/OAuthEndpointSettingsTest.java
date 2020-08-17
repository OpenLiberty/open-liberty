/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oauth20.internal.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.Configuration;

import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;
import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.common.http.SupportedHttpMethodHandler.HttpMethod;
import test.common.SharedOutputManager;

public class OAuthEndpointSettingsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.oauth.*=all:com.ibm.ws.security.oauth.*=all");

    private final Configuration config = mockery.mock(Configuration.class);

    private OAuthEndpointSettings settings;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        settings = new OAuthEndpointSettings();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_addOAuthEndpointSettings_nullConfig() {
        settings.addOAuthEndpointSettings(null);

        Map<EndpointType, SpecificOAuthEndpointSettings> allSettings = settings.getAllOAuthEndpointSettings();
        assertTrue("Should not have recorded any endpoint settings, but did. Settings were: " + allSettings, allSettings.isEmpty());
    }

    @Test
    public void test_addOAuthEndpointSettings_noProperties() {
        mockery.checking(new Expectations() {
            {
                one(config).getProperties();
                will(returnValue(null));
            }
        });
        settings.addOAuthEndpointSettings(config);

        Map<EndpointType, SpecificOAuthEndpointSettings> allSettings = settings.getAllOAuthEndpointSettings();
        assertTrue("Should not have recorded any endpoint settings, but did. Settings were: " + allSettings, allSettings.isEmpty());
    }

    @Test
    public void test_addOAuthEndpointSettings_emptyProperties() {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        mockery.checking(new Expectations() {
            {
                one(config).getProperties();
                will(returnValue(props));
            }
        });
        settings.addOAuthEndpointSettings(config);

        Map<EndpointType, SpecificOAuthEndpointSettings> allSettings = settings.getAllOAuthEndpointSettings();
        assertTrue("Should not have recorded any endpoint settings, but did. Settings were: " + allSettings, allSettings.isEmpty());
    }

    @Test
    public void test_addOAuthEndpointSettings_unknownEndpoint() {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(OAuthEndpointSettings.KEY_NAME, "unknown");
        mockery.checking(new Expectations() {
            {
                one(config).getProperties();
                will(returnValue(props));
            }
        });
        settings.addOAuthEndpointSettings(config);

        Map<EndpointType, SpecificOAuthEndpointSettings> allSettings = settings.getAllOAuthEndpointSettings();
        assertTrue("Should not have recorded any endpoint settings, but did. Settings were: " + allSettings, allSettings.isEmpty());
    }

    @Test
    public void test_addOAuthEndpointSettings_knownEndpoint_noOtherSettings() {
        EndpointType endpointUnderTest = EndpointType.coverage_map;

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(OAuthEndpointSettings.KEY_NAME, "coverageMap");
        mockery.checking(new Expectations() {
            {
                one(config).getProperties();
                will(returnValue(props));
            }
        });
        settings.addOAuthEndpointSettings(config);

        SpecificOAuthEndpointSettings testSettings = new SpecificOAuthEndpointSettings(endpointUnderTest);
        assertSettingsPresentAndSupportedHttpMethodsAreCorrect(testSettings);
    }

    @Test
    public void test_addOAuthEndpointSettings_knownEndpoint_supportedHttpMethodsString() {
        EndpointType endpointUnderTest = EndpointType.token;
        final Dictionary<String, Object> props = new Hashtable<String, Object>();

        props.put(OAuthEndpointSettings.KEY_NAME, String.valueOf(endpointUnderTest));
        props.put(OAuthEndpointSettings.KEY_SUPPORTED_HTTP_METHODS, "simple string");
        mockery.checking(new Expectations() {
            {
                one(config).getProperties();
                will(returnValue(props));
            }
        });
        settings.addOAuthEndpointSettings(config);

        SpecificOAuthEndpointSettings testSettings = new SpecificOAuthEndpointSettings(endpointUnderTest);
        assertSettingsPresentAndSupportedHttpMethodsAreCorrect(testSettings);
    }

    @Test
    public void test_addOAuthEndpointSettings_knownEndpoint_supportedHttpMethodsArray() {
        EndpointType endpointUnderTest = EndpointType.introspect;
        String[] intputSupportedHttpMethods = new String[] { "get", "GET", "post", "other" };

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(OAuthEndpointSettings.KEY_NAME, String.valueOf(endpointUnderTest));
        props.put(OAuthEndpointSettings.KEY_SUPPORTED_HTTP_METHODS, intputSupportedHttpMethods);
        mockery.checking(new Expectations() {
            {
                one(config).getProperties();
                will(returnValue(props));
            }
        });
        settings.addOAuthEndpointSettings(config);

        SpecificOAuthEndpointSettings testSettings = new SpecificOAuthEndpointSettings(endpointUnderTest);
        testSettings.setSupportedHttpMethods(intputSupportedHttpMethods);
        assertSettingsPresentAndSupportedHttpMethodsAreCorrect(testSettings);
    }

    @Test
    public void test_getEndpointTypeFromConfigName_emptyString() {
        String endpointName = "";
        EndpointType endpoint = settings.getEndpointTypeFromConfigName(endpointName);
        assertEquals("Should not have found a matching endpoint type for input [" + endpointName + "].", null, endpoint);
    }

    @Test
    public void test_getEndpointTypeFromConfigName_unknownValue() {
        String endpointName = "some unknown value";
        EndpointType endpoint = settings.getEndpointTypeFromConfigName(endpointName);
        assertEquals("Should not have found a matching endpoint type for input [" + endpointName + "].", null, endpoint);
    }

    @Test
    public void test_getEndpointTypeFromConfigName_authorize() {
        String endpointName = "authorize";
        EndpointType endpoint = settings.getEndpointTypeFromConfigName(endpointName);
        assertEquals("Did not get correct matching endpoint type for input [" + endpointName + "].", EndpointType.authorize, endpoint);
    }

    @Test
    public void test_getEndpointTypeFromConfigName_token() {
        String endpointName = "token";
        EndpointType endpoint = settings.getEndpointTypeFromConfigName(endpointName);
        assertEquals("Did not get correct matching endpoint type for input [" + endpointName + "].", EndpointType.token, endpoint);
    }

    @Test
    public void test_getEndpointTypeFromConfigName_appPasswords() {
        String endpointName = "appPasswords";
        EndpointType endpoint = settings.getEndpointTypeFromConfigName(endpointName);
        assertEquals("Did not get correct matching endpoint type for input [" + endpointName + "].", EndpointType.app_password, endpoint);
    }

    @Test
    public void test_getEndpointTypeFromConfigName_appTokens() {
        String endpointName = "appTokens";
        EndpointType endpoint = settings.getEndpointTypeFromConfigName(endpointName);
        assertEquals("Did not get correct matching endpoint type for input [" + endpointName + "].", EndpointType.app_token, endpoint);
    }

    @Test
    public void test_getEndpointTypeFromConfigName_coverageMap() {
        String endpointName = "coverageMap";
        EndpointType endpoint = settings.getEndpointTypeFromConfigName(endpointName);
        assertEquals("Did not get correct matching endpoint type for input [" + endpointName + "].", EndpointType.coverage_map, endpoint);
    }

    @Test
    public void test_updateAllEndpointSettings_nullSettings() {
        settings.updateAllEndpointSettings(null);

        Map<EndpointType, SpecificOAuthEndpointSettings> allSettings = settings.getAllOAuthEndpointSettings();
        assertTrue("Should not have recorded any endpoint settings, but did. Settings were: " + allSettings, allSettings.isEmpty());
    }

    @Test
    public void test_updateAllEndpointSettings_oneEndpoint_noSupportedMethods() {
        EndpointType endpointUnderTest = EndpointType.revoke;
        SpecificOAuthEndpointSettings newSettings = new SpecificOAuthEndpointSettings(endpointUnderTest);
        settings.updateAllEndpointSettings(newSettings);

        assertSettingsPresentAndSupportedHttpMethodsAreCorrect(newSettings);
    }

    @Test
    public void test_updateAllEndpointSettings_addSameEndpointTwice() {
        EndpointType endpointUnderTest = EndpointType.clientMetatype;
        SpecificOAuthEndpointSettings newSettings = new SpecificOAuthEndpointSettings(endpointUnderTest);
        settings.updateAllEndpointSettings(newSettings);

        SpecificOAuthEndpointSettings recordedSettings = assertSettingsPresentForEndpoint(endpointUnderTest);
        assertTrue("Should not have found any supported HTTP methods, but did: " + recordedSettings, recordedSettings.getSupportedHttpMethods().isEmpty());

        SpecificOAuthEndpointSettings settingsForSameEndpoint = new SpecificOAuthEndpointSettings(endpointUnderTest);
        settingsForSameEndpoint.setSupportedHttpMethods("get", "post");
        settings.updateAllEndpointSettings(settingsForSameEndpoint);

        // Verify that the second set of settings weren't accepted since we already have settings for this endpoint
        assertSettingsPresentAndSupportedHttpMethodsAreCorrect(newSettings);
    }

    @Test
    public void test_updateAllEndpointSettings_addMultipleEndpoints() {
        SpecificOAuthEndpointSettings newSettings1 = new SpecificOAuthEndpointSettings(EndpointType.authorize);

        SpecificOAuthEndpointSettings newSettings2 = new SpecificOAuthEndpointSettings(EndpointType.introspect);
        String[] newSettings2SupportedHttpMethods = new String[] { "get", "post", "unknown" };
        newSettings2.setSupportedHttpMethods(newSettings2SupportedHttpMethods);

        SpecificOAuthEndpointSettings newSettings3 = new SpecificOAuthEndpointSettings(EndpointType.token);
        String[] newSettings3SupportedHttpMethods = new String[] { "post" };
        newSettings3.setSupportedHttpMethods(newSettings3SupportedHttpMethods);

        settings.updateAllEndpointSettings(newSettings1);
        settings.updateAllEndpointSettings(newSettings2);
        settings.updateAllEndpointSettings(newSettings3);

        assertSettingsPresentAndSupportedHttpMethodsAreCorrect(newSettings1);
        assertSettingsPresentAndSupportedHttpMethodsAreCorrect(newSettings2);
        assertSettingsPresentAndSupportedHttpMethodsAreCorrect(newSettings3);

    }

    @SuppressWarnings("restriction")
    private void assertSettingsPresentAndSupportedHttpMethodsAreCorrect(SpecificOAuthEndpointSettings inputSettings) {
        SpecificOAuthEndpointSettings recordedSettings = assertSettingsPresentForEndpoint(inputSettings.getEndpointType());
        Set<HttpMethod> recordedSupportedHttpMethods = recordedSettings.getSupportedHttpMethods();
        Set<HttpMethod> inputSupportedHttpMethods = inputSettings.getSupportedHttpMethods();
        if (inputSupportedHttpMethods.isEmpty()) {
            assertTrue("Should not have found any supported HTTP methods, but did: " + recordedSettings, recordedSupportedHttpMethods.isEmpty());
        } else {
            assertFalse("Should have found supported HTTP methods, but did not.", recordedSupportedHttpMethods.isEmpty());
            for (HttpMethod method : inputSupportedHttpMethods) {
                assertTrue("Supported HTTP methods for endpoint [" + inputSettings.getEndpointType() + "] is missing expected value [" + method + "]. Recorded methods were: " + recordedSupportedHttpMethods, recordedSupportedHttpMethods.contains(method));
            }
        }
    }

    private SpecificOAuthEndpointSettings assertSettingsPresentForEndpoint(EndpointType endpoint) {
        Map<EndpointType, SpecificOAuthEndpointSettings> allSettings = settings.getAllOAuthEndpointSettings();
        assertFalse("Should have recorded at least one set of endpoint settings, but did not.", allSettings.isEmpty());
        SpecificOAuthEndpointSettings recordedSettings = settings.getSpecificOAuthEndpointSettings(endpoint);
        assertNotNull("Should have found an entry for the [" + endpoint + "] endpoint, but did not. All settings were: " + allSettings, recordedSettings);
        return recordedSettings;
    }

}
