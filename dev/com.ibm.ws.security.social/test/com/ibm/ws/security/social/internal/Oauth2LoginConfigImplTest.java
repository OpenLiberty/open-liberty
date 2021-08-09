/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Pattern;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.SocialLoginService;
import com.ibm.ws.security.social.UserApiConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

import test.common.SharedOutputManager;

public class Oauth2LoginConfigImplTest extends CommonConfigTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance()
            .trace("com.ibm.ws.security.social.*=all");

    protected Oauth2LoginConfigImpl configImpl = null;

    public interface MockInterface {
        public Configuration handleJwtElement();
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);
    final Configuration configuration = mockery.mock(Configuration.class);
    final ConfigurationAdmin configAdmin = mockery.mock(ConfigurationAdmin.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        configImpl = new Oauth2LoginConfigImpl();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /************************************** getContextRoot **************************************/

    @SuppressWarnings("static-access")
    @Test
    public void getContextRoot_default() {
        try {
            assertEquals("Default context root did not match expected value.", configImpl.contextRoot,
                    Oauth2LoginConfigImpl.getContextRoot());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("static-access")
    @Test
    public void getContextRoot_setToNull() {
        try {
            Oauth2LoginConfigImpl.setContextRoot(null);
            assertNull("Context root was expected to be null.", configImpl.getContextRoot());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** initProps **************************************/

    @Test
    public void initProps_emptyProps() {
        try {
            configImpl.initProps(cc, new HashMap<String, Object>());

            verifyAllMissingRequiredAttributes(outputMgr);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initProps_minimumProps() {
        try {
            Map<String, Object> minimumProps = getRequiredConfigProps();

            configImpl.initProps(cc, minimumProps);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testUseJvmProps() {
        try {
            Map<String, Object> minimumProps = getRequiredConfigProps();
            minimumProps.put(Oauth2LoginConfigImpl.KEY_USE_SYSPROPS_FOR_HTTPCLIENT_CONNECTONS, new Boolean(true));

            configImpl.initProps(cc, minimumProps);
            configImpl.setAllConfigAttributes(minimumProps);
            assertTrue(configImpl.getUseSystemPropertiesForHttpClientConnections());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** initializeJwt **************************************/

    @Test
    public void initializeJwt_uninitialized() {
        try {
            Oauth2LoginConfigImpl config = getConfigImplWithHandleJwtElementMocked();
            Map<String, Object> props = new HashMap<String, Object>();

            config.initializeJwt(props);

            String jwtRef = config.getJwtRef();
            assertNull("jwtRef should have been null but was [" + jwtRef + "].", jwtRef);
            String[] jwtClaims = config.getJwtClaims();
            assertNull("jwtClaims should have been null but were [" + Arrays.toString(jwtClaims) + "].", jwtClaims);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initializeJwt_nullJwtConfig() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    one(socialLoginService).getConfigAdmin();
                    will(returnValue(configAdmin));
                    one(mockInterface).handleJwtElement();
                    will(returnValue(null));
                }
            });

            Oauth2LoginConfigImpl config = getConfigImplWithHandleJwtElementMocked();
            Map<String, Object> props = getRequiredConfigProps();
            config = activateConfigAndSetSocialLoginService(config, props, socialLoginService);

            config.initializeJwt(props);

            String jwtRef = config.getJwtRef();
            assertNull("jwtRef should have been null but was [" + jwtRef + "].", jwtRef);
            String[] jwtClaims = config.getJwtClaims();
            assertNull("jwtClaims should have been null but were [" + Arrays.toString(jwtClaims) + "].", jwtClaims);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initializeJwt_jwtProps_null() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    one(socialLoginService).getConfigAdmin();
                    will(returnValue(configAdmin));
                    one(mockInterface).handleJwtElement();
                    will(returnValue(configuration));
                    one(configuration).getProperties();
                    will(returnValue(null));
                }
            });

            Oauth2LoginConfigImpl config = getConfigImplWithHandleJwtElementMocked();
            Map<String, Object> props = getRequiredConfigProps();
            config = activateConfigAndSetSocialLoginService(config, props, socialLoginService);

            config.initializeJwt(getRequiredConfigProps());

            String jwtRef = config.getJwtRef();
            assertNull("jwtRef should have been null but was [" + jwtRef + "].", jwtRef);
            String[] jwtClaims = config.getJwtClaims();
            assertNull("jwtClaims should have been null but were [" + Arrays.toString(jwtClaims) + "].", jwtClaims);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initializeJwt_jwtProps_empty() {
        try {
            final Dictionary<String, Object> jwtProps = new Hashtable<String, Object>();

            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    one(socialLoginService).getConfigAdmin();
                    will(returnValue(configAdmin));
                    one(mockInterface).handleJwtElement();
                    will(returnValue(configuration));
                    one(configuration).getProperties();
                    will(returnValue(jwtProps));
                }
            });

            Oauth2LoginConfigImpl config = getConfigImplWithHandleJwtElementMocked();
            Map<String, Object> props = getRequiredConfigProps();
            config = activateConfigAndSetSocialLoginService(config, props, socialLoginService);

            config.initializeJwt(getRequiredConfigProps());

            String jwtRef = config.getJwtRef();
            assertNull("jwtRef should have been null but was [" + jwtRef + "].", jwtRef);
            String[] jwtClaims = config.getJwtClaims();
            assertNull("jwtClaims should have been null but were [" + Arrays.toString(jwtClaims) + "].", jwtClaims);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initializeJwt_jwtProps_valuesEmpty() {
        try {

            final Dictionary<String, Object> jwtProps = new Hashtable<String, Object>();
            jwtProps.put(Oauth2LoginConfigImpl.CFG_KEY_jwtRef, "");
            jwtProps.put(Oauth2LoginConfigImpl.CFG_KEY_jwtClaims, new String[0]);

            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    one(socialLoginService).getConfigAdmin();
                    will(returnValue(configAdmin));
                    one(mockInterface).handleJwtElement();
                    will(returnValue(configuration));
                    one(configuration).getProperties();
                    will(returnValue(jwtProps));
                }
            });

            Oauth2LoginConfigImpl config = getConfigImplWithHandleJwtElementMocked();
            Map<String, Object> props = getRequiredConfigProps();
            config = activateConfigAndSetSocialLoginService(config, props, socialLoginService);

            config.initializeJwt(getRequiredConfigProps());

            String jwtRef = config.getJwtRef();
            assertNull("jwtRef should have been null but was [" + jwtRef + "].", jwtRef);
            String[] jwtClaims = config.getJwtClaims();
            assertNull("jwtClaims should have been null but were [" + Arrays.toString(jwtClaims) + "].", jwtClaims);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initializeJwt_jwtProps_valuesWhitespace() {
        try {
            final Dictionary<String, Object> jwtProps = new Hashtable<String, Object>();
            jwtProps.put(Oauth2LoginConfigImpl.CFG_KEY_jwtRef, " \n\r \t");
            jwtProps.put(Oauth2LoginConfigImpl.CFG_KEY_jwtClaims, new String[] { " ", "\t \r " });

            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    one(socialLoginService).getConfigAdmin();
                    will(returnValue(configAdmin));
                    one(mockInterface).handleJwtElement();
                    will(returnValue(configuration));
                    one(configuration).getProperties();
                    will(returnValue(jwtProps));
                }
            });

            Oauth2LoginConfigImpl config = getConfigImplWithHandleJwtElementMocked();
            Map<String, Object> props = getRequiredConfigProps();
            config = activateConfigAndSetSocialLoginService(config, props, socialLoginService);

            config.initializeJwt(getRequiredConfigProps());

            String jwtRef = config.getJwtRef();
            assertNull("jwtRef should have been null but was [" + jwtRef + "].", jwtRef);
            String[] jwtClaims = config.getJwtClaims();
            assertNull("jwtClaims should have been null but were [" + Arrays.toString(jwtClaims) + "].", jwtClaims);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initializeJwt_jwtProps_validValues() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                }
            });

            Oauth2LoginConfigImpl config = getConfigImplWithHandleJwtElementMocked();
            Map<String, Object> props = getRequiredConfigProps();
            config = activateConfigAndSetSocialLoginService(config, props, socialLoginService);

            String expectedJwtRef = "some jwt ref";
            String expectedClaim1 = "claim1";
            String expectedClaim2 = "claim 2";

            final Dictionary<String, Object> jwtProps = new Hashtable<String, Object>();
            jwtProps.put(Oauth2LoginConfigImpl.CFG_KEY_jwtRef, expectedJwtRef);
            jwtProps.put(Oauth2LoginConfigImpl.CFG_KEY_jwtClaims,
                    new String[] { expectedClaim1 + " ", "\t \r " + expectedClaim2 });

            mockery.checking(new Expectations() {
                {

                    one(socialLoginService).getConfigAdmin();
                    will(returnValue(configAdmin));
                    one(mockInterface).handleJwtElement();
                    will(returnValue(configuration));
                    one(configuration).getProperties();
                    will(returnValue(jwtProps));
                }
            });

            config.initializeJwt(getRequiredConfigProps());

            String jwtRef = config.getJwtRef();
            assertEquals("jwtRef value did not match expected value.", expectedJwtRef, jwtRef);
            String[] jwtClaims = config.getJwtClaims();
            assertEquals("jwtClaims value did not match expected value.",
                    Arrays.toString(new String[] { expectedClaim1, expectedClaim2 }), Arrays.toString(jwtClaims));

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** handleJwtElement **************************************/

    @Test
    public void handleJwtElement_emptyProps() {
        try {
            Map<String, Object> props = new HashMap<String, Object>();

            Configuration result = configImpl.handleJwtElement(props, configAdmin);

            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleJwtElement_missingKey() {
        try {
            Map<String, Object> props = getRequiredConfigProps();

            Configuration result = configImpl.handleJwtElement(props, configAdmin);

            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleJwtElement_nullConfigAdmin() {
        try {
            String jwt = "Some JWT value";
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(Oauth2LoginConfigImpl.CFG_KEY_jwt, jwt);

            Configuration result = configImpl.handleJwtElement(props, null);

            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleJwtElement_exceptionThrown() {
        try {
            final String jwt = "Some JWT value";
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(Oauth2LoginConfigImpl.CFG_KEY_jwt, jwt);

            mockery.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(jwt, null);
                    will(throwException(new IOException(defaultExceptionMsg)));
                }
            });

            Configuration result = configImpl.handleJwtElement(props, configAdmin);

            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleJwtElement_validConfiguration() {
        try {
            final String jwt = "Some JWT value";
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(Oauth2LoginConfigImpl.CFG_KEY_jwt, jwt);

            mockery.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(jwt, null);
                    will(returnValue(configuration));
                }
            });

            Configuration result = configImpl.handleJwtElement(props, configAdmin);

            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Configuration result did not match expected value.", configuration, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************
     * getRequiredSerializableProtectedStringConfigAttribute
     **************************************/

    @Test
    public void getRequiredSerializableProtectedStringConfigAttribute_emptyProps() {
        try {
            String chosenAttr = getRandomRequiredConfigAttribute();

            String result = configImpl
                    .getRequiredSerializableProtectedStringConfigAttribute(new HashMap<String, Object>(), chosenAttr);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyLogMessageWithInserts(outputMgr, CWWKS5479E_CONFIG_REQUIRED_ATTRIBUTE_NULL, chosenAttr);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredSerializableProtectedStringConfigAttribute_missingKey() {
        try {
            String chosenAttr = getRandomRequiredConfigAttribute();

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");

            String result = configImpl.getRequiredSerializableProtectedStringConfigAttribute(props, chosenAttr);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyLogMessageWithInserts(outputMgr, CWWKS5479E_CONFIG_REQUIRED_ATTRIBUTE_NULL, chosenAttr);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredSerializableProtectedStringConfigAttribute_withKey_valueEmpty() {
        try {
            String chosenAttr = getRandomRequiredConfigAttribute();
            String value = "";
            SerializableProtectedString protectedStringVal = new SerializableProtectedString(value.toCharArray());

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, protectedStringVal);

            String result = configImpl.getRequiredSerializableProtectedStringConfigAttribute(props, chosenAttr);
            assertEquals(
                    "Value for " + chosenAttr + " property did not match expected value. Properties were: " + props,
                    value, result);

            // Empty values are allowed
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredSerializableProtectedStringConfigAttribute_withKey_valueWhitespace() {
        try {
            String chosenAttr = getRandomRequiredConfigAttribute();
            String value = " \n\t\r";
            SerializableProtectedString protectedStringVal = new SerializableProtectedString(value.toCharArray());

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, protectedStringVal);

            String result = configImpl.getRequiredSerializableProtectedStringConfigAttribute(props, chosenAttr);
            assertEquals(
                    "Value for " + chosenAttr + " property did not match expected value. Properties were: " + props,
                    value, result);

            // Empty values are allowed
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredSerializableProtectedStringConfigAttribute_withKey_plaintext() {
        try {
            String chosenAttr = getRandomRequiredConfigAttribute();
            String value = "Some plaintext value";
            SerializableProtectedString protectedStringVal = new SerializableProtectedString(value.toCharArray());

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, protectedStringVal);

            String result = configImpl.getRequiredSerializableProtectedStringConfigAttribute(props, chosenAttr);
            assertEquals(
                    "Value for " + chosenAttr + " property did not match expected value. Properties were: " + props,
                    value, result);

            // Empty values are allowed
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredSerializableProtectedStringConfigAttribute_withKey_xor() {
        try {
            String chosenAttr = getRandomRequiredConfigAttribute();
            String value = "{xor}LDo8LTor";
            String decodedValue = "secret";
            SerializableProtectedString protectedStringVal = new SerializableProtectedString(value.toCharArray());

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, protectedStringVal);

            String result = configImpl.getRequiredSerializableProtectedStringConfigAttribute(props, chosenAttr);
            assertEquals(
                    "Value for " + chosenAttr + " property did not match expected value. Properties were: " + props,
                    decodedValue, result);

            // Empty values are allowed
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** defaultJwtBuilder **************************************/

    @Test
    public void defaultJwtBuilder() {
        try {
            assertEquals("Default JWT builder ID did not match expected value.", DEFAULT_JWT_BUILDER,
                    configImpl.defaultJwtBuilder());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** initUserApiConfigs **************************************/

    @Test
    public void initUserApiConfigs_nullArg() {
        try {
            UserApiConfig[] result = configImpl.initUserApiConfigs(null);

            assertNull("User API config array should be null with null input. Result was: " + Arrays.toString(result),
                    result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initUserApiConfigs_emptyArg() {
        try {
            UserApiConfig[] result = configImpl.initUserApiConfigs("");

            assertUserApiArray(result, "");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initUserApiConfigs_someString() {
        try {
            final String userApiString = "Some user API string.";
            UserApiConfig[] result = configImpl.initUserApiConfigs(userApiString);

            assertNotNull("User API config array should not be null with empty input.", result);
            assertEquals("User API config array should have only one entry. Result was: " + Arrays.toString(result), 1,
                    result.length);
            assertEquals("API value did not match expected value.", userApiString, result[0].getApi());
            assertEquals("API method did not match expected value.", Constants.client_secret_basic,
                    result[0].getMethod());
            assertNull(
                    "API parameter value should be null but was not. Parameter was [" + result[0].getParameter() + "].",
                    result[0].getParameter());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getUniqueId **************************************/

    @Test
    public void getUniqueId_configMissingUniqueId() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                }
            });
            configImpl = getActivatedConfig(getRequiredConfigProps());

            String result = configImpl.getUniqueId();
            assertNull("Unique ID not included in config props so result should have been null. Result was [" + result
                    + "].", result);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUniqueId() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                }
            });
            Map<String, Object> props = getRequiredConfigProps();
            props.put(Oauth2LoginConfigImpl.KEY_UNIQUE_ID, configId);
            configImpl = getActivatedConfig(props);

            String result = configImpl.getUniqueId();
            assertEquals("Unique ID did not match expected value.", configId, result);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getAuthFilter **************************************/

    @Test
    public void getAuthFilter_configMissingAuthFilterRef() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                }
            });
            configImpl = getActivatedConfig(getRequiredConfigProps());

            AuthenticationFilter result = configImpl.getAuthFilter();
            assertNull("Auth filter ref not included in config props so result should have been null. Result was ["
                    + result + "].", result);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAuthFilter() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                }
            });
            Map<String, Object> props = getRequiredConfigProps();
            props.put(Oauth2LoginConfigImpl.KEY_authFilterRef, authFilterRef);
            configImpl = getActivatedConfig(props);

            AuthenticationFilter result = configImpl.getAuthFilter();
            assertNull("Auth filter ref not activated so result should have been null. Result was [" + result + "].",
                    result);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getUserApis **************************************/

    @Test
    public void getUserApis_configMissingUserApi() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                }
            });
            configImpl = getActivatedConfig(getRequiredConfigProps());

            UserApiConfig[] result = configImpl.getUserApis();
            assertNull("User API confgs not included in config props so result should have been null. Result was ["
                    + result + "].", result);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApis_userApiWithDelimiters() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                }
            });
            Map<String, Object> props = getRequiredConfigProps();
            final String userApi = "Some=value, with possible, delimiters";
            props.put(Oauth2LoginConfigImpl.KEY_userApi, userApi);
            configImpl = getActivatedConfig(props);

            UserApiConfig[] result = configImpl.getUserApis();
            assertUserApiArray(result, userApi);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApis() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                }
            });
            Map<String, Object> props = getRequiredConfigProps();
            props.put(Oauth2LoginConfigImpl.KEY_userApi, userApi);
            configImpl = getActivatedConfig(props);

            UserApiConfig[] result = configImpl.getUserApis();
            assertUserApiArray(result, userApi);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getSocialLoginCookieCache **************************************/

    @Test
    public void getSocialLoginCookieCache() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                }
            });
            configImpl = getActivatedConfig(getRequiredConfigProps());

            Cache result = configImpl.getSocialLoginCookieCache();
            assertNotNull("Cache should have been created for us, so result should not have been null.", result);
            assertEquals("Size of new cache did not match expected value.", DEFAULT_CACHE_LIMIT, result.size());

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getPublicKeys **************************************/

    @Test
    public void getPublicKeys_noSocialLoginService() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                }
            });
            configImpl = getActivatedConfig(getRequiredConfigProps());
            configImpl = setSocialLoginServiceReference(configImpl, null);

            HashMap<String, PublicKey> result = configImpl.getPublicKeys();
            assertNull("Result should have been null because no social login service should be available.", result);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKeys_throwsSocialLoginException() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    one(socialLoginService).getSslSupport();
                    will(returnValue(sslSupport));
                    one(socialLoginService).getKeyStoreServiceRef();
                    will(returnValue(keyStoreServiceRef));
                    one(sslSupport).getJSSEHelper();
                    one(keyStoreServiceRef).getService();
                    will(returnValue(null));
                }
            });

            configImpl = getActivatedConfig(getRequiredConfigProps());
            configImpl = setSocialLoginServiceReference(configImpl, socialLoginService);

            try {
                HashMap<String, PublicKey> result = configImpl.getPublicKeys();
                fail("Should have thrown SocialLoginException but did not. Result was [" + result + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5467E_KEYSTORE_SERVICE_NOT_FOUND);
            }

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKeys_successful() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    one(socialLoginService).getSslSupport();
                    will(returnValue(sslSupport));
                    one(socialLoginService).getKeyStoreServiceRef();
                    will(returnValue(keyStoreServiceRef));
                    one(sslSupport).getJSSEHelper();
                    one(keyStoreServiceRef).getService();
                    will(returnValue(keyStoreService));
                    one(keyStoreService).getTrustedCertEntriesInKeyStore(with(any(String.class)));
                }
            });

            configImpl = getActivatedConfig(getRequiredConfigProps());
            configImpl = setSocialLoginServiceReference(configImpl, socialLoginService);

            HashMap<String, PublicKey> result = configImpl.getPublicKeys();
            assertTrue("Result should be an empty map, but was [" + result + "].", result.isEmpty());

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getPublicKey **************************************/

    @Test
    public void getPublicKey_noSocialLoginService() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                }
            });
            configImpl = getActivatedConfig(getStandardConfigProps());
            configImpl = setSocialLoginServiceReference(configImpl, null);

            PublicKey result = configImpl.getPublicKey();
            assertNull("Public key result should have been null but was [" + result + "].", result);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKey_noServices() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    one(socialLoginService).getSslSupport();
                    will(returnValue(null));
                    one(socialLoginService).getKeyStoreServiceRef();
                    will(returnValue(null));
                }
            });

            configImpl = getActivatedConfig(getStandardConfigProps());
            configImpl = setSocialLoginServiceReference(configImpl, socialLoginService);

            PublicKey result = configImpl.getPublicKey();
            assertNull("Public key result should have been null but was [" + result + "].", result);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKey_throwsException() {
        try {
            final Map<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(com.ibm.websphere.ssl.Constants.CONNECTION_INFO_DIRECTION,
                    com.ibm.websphere.ssl.Constants.DIRECTION_INBOUND);

            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    one(socialLoginService).getSslSupport();
                    will(returnValue(sslSupport));
                    one(socialLoginService).getKeyStoreServiceRef();
                    will(returnValue(keyStoreServiceRef));
                    one(sslSupport).getJSSEHelper();
                    will(returnValue(jsseHelper));
                    one(jsseHelper).getProperties(null, connectionInfo, null, true);
                    will(throwException(new SSLException(defaultExceptionMsg)));
                }
            });

            configImpl = getActivatedConfig(getStandardConfigProps());
            configImpl = setSocialLoginServiceReference(configImpl, socialLoginService);

            try {
                PublicKey result = configImpl.getPublicKey();
                fail("Should have thrown SocialLoginException but did not. Result was [" + result + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5466E_ERROR_LOADING_SSL_PROPS + ".*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getPrivateKey **************************************/

    @Test
    public void getPrivateKey_noSocialLoginService() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                }
            });
            configImpl = getActivatedConfig(getStandardConfigProps());
            configImpl = setSocialLoginServiceReference(configImpl, null);

            PrivateKey result = configImpl.getPrivateKey();
            assertNull("Private key result should have been null but was [" + result + "].", result);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPrivateKey_noServices() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    one(socialLoginService).getSslSupport();
                    will(returnValue(null));
                    one(socialLoginService).getKeyStoreServiceRef();
                    will(returnValue(null));
                }
            });

            configImpl = getActivatedConfig(getStandardConfigProps());
            configImpl = setSocialLoginServiceReference(configImpl, socialLoginService);

            PrivateKey result = configImpl.getPrivateKey();
            assertNull("Private key result should have been null but was [" + result + "].", result);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPrivateKey_throwsException() {
        try {
            final Map<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(com.ibm.websphere.ssl.Constants.CONNECTION_INFO_DIRECTION,
                    com.ibm.websphere.ssl.Constants.DIRECTION_INBOUND);

            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    one(socialLoginService).getSslSupport();
                    will(returnValue(sslSupport));
                    one(socialLoginService).getKeyStoreServiceRef();
                    will(returnValue(keyStoreServiceRef));
                    one(sslSupport).getJSSEHelper();
                    will(returnValue(jsseHelper));
                    one(jsseHelper).getProperties(null, connectionInfo, null, true);
                    will(throwException(new SSLException(defaultExceptionMsg)));
                }
            });

            configImpl = getActivatedConfig(getStandardConfigProps());
            configImpl = setSocialLoginServiceReference(configImpl, socialLoginService);

            try {
                PrivateKey result = configImpl.getPrivateKey();
                fail("Should have thrown SocialLoginException but did not. Result was [" + result + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5466E_ERROR_LOADING_SSL_PROPS + ".*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************
     * Helper methods
     **************************************/

    protected Oauth2LoginConfigImpl getActivatedConfig(Map<String, Object> props) throws SocialLoginException {
        Oauth2LoginConfigImpl config = new Oauth2LoginConfigImpl();
        config.activate(cc, props);
        return config;
    }

    protected Oauth2LoginConfigImpl setSocialLoginServiceReference(final Oauth2LoginConfigImpl configImpl,
            final SocialLoginService socialLoginService) throws SocialLoginException, KeyStoreException {
        mockery.checking(new Expectations() {
            {
                allowing(socialLoginServiceRef).getProperty("id");
                will(returnValue(SOCIAL_LOGIN_SERVICE_ID));
                allowing(socialLoginServiceRef).getProperty("service.id");
                will(returnValue(1L));
                allowing(socialLoginServiceRef).getProperty("service.ranking");
                will(returnValue(1L));
                allowing(cc).locateService(KEY_SOCIAL_LOGIN_SERVICE, socialLoginServiceRef);
                will(returnValue(socialLoginService));
            }
        });
        configImpl.setSocialLoginService(socialLoginServiceRef);
        return configImpl;
    }

    protected Map<String, Object> getRequiredConfigProps() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Oauth2LoginConfigImpl.KEY_authorizationEndpoint, authzEndpoint);
        //props.put(Oauth2LoginConfigImpl.KEY_scope, scope);
        props.put(Oauth2LoginConfigImpl.KEY_clientId, clientId);
        props.put(Oauth2LoginConfigImpl.KEY_clientSecret, clientSecretPS);
        return props;
    }

    protected Oauth2LoginConfigImpl getConfigImplWithHandleJwtElementMocked() {
        return new Oauth2LoginConfigImpl() {
            @Override
            protected Configuration handleJwtElement(Map<String, Object> props, ConfigurationAdmin configurationAdmin) {
                return mockInterface.handleJwtElement();
            }
        };
    }

    Oauth2LoginConfigImpl activateConfigAndSetSocialLoginService(Oauth2LoginConfigImpl config,
            Map<String, Object> props, SocialLoginService service) throws Exception {
        config.activate(cc, props);
        config = setSocialLoginServiceReference(config, service);
        return config;
    }

    protected void assertUserApiArray(UserApiConfig[] apiConfigs, String apiValue) {
        assertNotNull("User API config array should not be null.", apiConfigs);
        assertEquals("User API config array should have only one entry. Result was " + Arrays.toString(apiConfigs), 1,
                apiConfigs.length);
        assertEquals("API value did not match expected value.", apiValue, apiConfigs[0].getApi());
        assertEquals("API method did not match expected value.", Constants.client_secret_basic,
                apiConfigs[0].getMethod());
        assertNull(
                "API parameter value should be null but was not. Parameter was [" + apiConfigs[0].getParameter() + "].",
                apiConfigs[0].getParameter());
    }

}
