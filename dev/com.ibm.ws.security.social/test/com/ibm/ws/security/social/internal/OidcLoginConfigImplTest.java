/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.KeyStoreException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.ws.security.common.jwk.impl.JWKSet;
import com.ibm.ws.security.jwt.config.ConsumerUtils;
import com.ibm.ws.security.social.SocialLoginService;
import com.ibm.ws.security.social.SslRefInfo;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.Oauth2LoginConfigImplTest.MockInterface;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

import test.common.SharedOutputManager;

public class OidcLoginConfigImplTest extends CommonConfigTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    protected OidcLoginConfigImpl configImpl = null;

    final MockInterface mockInterface = mockery.mock(MockInterface.class);
    final SslRefInfo sslRefInfo = mockery.mock(SslRefInfo.class);

    String issuer = "http://some/valid/issuer";
    String token = "https://some/token/endpoint/abc";
    SerializableProtectedString secret = new SerializableProtectedString("secret".toCharArray());

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        configImpl = new OidcLoginConfigImpl();
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

    /************************************** getIssuer **************************************/

    @Test
    public void getIssuer_configPropsNotInitialized() {
        try {
            String result = configImpl.getIssuer();
            assertNull("Computed issuer should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getIssuer_issuerMissingInProps() {
        try {
            Map<String, Object> props = getStandardConfigProps();
            configImpl.initProps(cc, props);

            String result = configImpl.getIssuer();

            assertNull("Computed issuer should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getIssuer_issuerMissingInProps_malformedTokenEndpiont_short() {
        try {
            Map<String, Object> props = getStandardConfigProps();
            props.put(OidcLoginConfigImpl.KEY_tokenEndpoint, "Nope");
            configImpl.initProps(cc, props);

            String result = configImpl.getIssuer();

            assertNull("Computed issuer should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getIssuer_issuerMissingInProps_malformedTokenEndpiont() {
        try {
            Map<String, Object> props = getStandardConfigProps();
            props.put(OidcLoginConfigImpl.KEY_tokenEndpoint, "ftp://not-an-http-url");
            configImpl.initProps(cc, props);

            String result = configImpl.getIssuer();

            assertNull("Computed issuer should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getIssuer_issuerMissingInProps_simpleTokenEndpiont() {
        try {
            String tokenEndpoint = "http://a";
            Map<String, Object> props = getStandardConfigProps();
            props.put(OidcLoginConfigImpl.KEY_tokenEndpoint, tokenEndpoint);
            configImpl.initProps(cc, props);

            String result = configImpl.getIssuer();

            assertEquals("Issuer did not match expected value.", tokenEndpoint, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getIssuer_issuerMissingInProps_extendedTokenEndpiont() {
        try {
            String tokenEndpoint = hostAndPortWithPath + "/token";
            Map<String, Object> props = getStandardConfigProps();
            props.put(OidcLoginConfigImpl.KEY_tokenEndpoint, tokenEndpoint);
            configImpl.initProps(cc, props);

            String result = configImpl.getIssuer();

            assertEquals("Issuer did not match expected value.", hostAndPortWithPath, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getIssuer_issuerInProps_notUrl() {
        try {
            String issuer = "Some non-URL value";
            Map<String, Object> props = getStandardConfigProps();
            props.put(OidcLoginConfigImpl.KEY_ISSUER, issuer);
            configImpl.initProps(cc, props);

            String result = configImpl.getIssuer();

            assertEquals("Issuer did not match expected value.", issuer, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getIssuer_issuerInProps() {
        try {
            Map<String, Object> props = getStandardConfigProps();
            props.put(OidcLoginConfigImpl.KEY_ISSUER, issuer);
            configImpl.initProps(cc, props);

            String result = configImpl.getIssuer();

            assertEquals("Issuer did not match expected value.", issuer, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getAudiences **************************************/

    @Test
    public void getAudiences_configNotInitialized() {
        try {
            List<String> result = configImpl.getAudiences();

            assertTrue("Audiences list should have been empty but was " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAudiences() {
        try {
            Map<String, Object> props = getStandardConfigProps();
            configImpl.initProps(cc, props);

            List<String> result = configImpl.getAudiences();

            assertEquals("Audiences list size did not match expected value. Audience list was " + result, 1, result.size());
            assertEquals("Client ID in audience list did not match expected value. Audience list was " + result, clientId, result.get(0));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** isValidationRequired **************************************/

    @Test
    public void isValidationRequired_configNotInitialized() {
        try {
            assertFalse("Validation required value was not false as expected.", configImpl.isValidationRequired());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isValidationRequired_propsMissingJwksUri() {
        try {
            Map<String, Object> props = getStandardConfigProps();
            configImpl.initProps(cc, props);

            assertFalse("Validation required value was not false as expected.", configImpl.isValidationRequired());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isValidationRequired_propsIncludeJwksUri() {
        try {
            Map<String, Object> props = getStandardConfigProps();
            props.put(OidcLoginConfigImpl.KEY_jwksUri, "some value");
            configImpl.initProps(cc, props);

            assertFalse("Validation required value was not false as expected.", configImpl.isValidationRequired());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getTrustStoreRef **************************************/

    @Test
    public void getTrustStoreRef_noSocialLoginService() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                }
            });
            configImpl = getActivatedConfig(getStandardConfigProps());
            configImpl = setSocialLoginServiceReference(configImpl, null);

            String result = configImpl.getTrustStoreRef();
            assertNull("Trust store ref name should have been null but was [" + result + "].", result);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTrustStoreRef_noServices() {
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

            String result = configImpl.getTrustStoreRef();
            assertNull("Trust store ref name should have been null but was [" + result + "].", result);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTrustStoreRef_throwsException() {
        try {
            configImpl.sslRefInfo = sslRefInfo;

            mockery.checking(new Expectations() {
                {
                    one(sslRefInfo).getTrustStoreName();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            String result = configImpl.getTrustStoreRef();
            assertNull("Trust store ref name should have been null but was [" + result + "].", result);

            // TODO - Might add NLS message in 3Q
            verifyLogMessage(outputMgr, defaultExceptionMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTrustStoreRef_valid() {
        try {
            configImpl.sslRefInfo = sslRefInfo;
            final String name = "someTrustName";

            mockery.checking(new Expectations() {
                {
                    one(sslRefInfo).getTrustStoreName();
                    will(returnValue(name));
                }
            });

            String result = configImpl.getTrustStoreRef();
            assertEquals("Trust store ref name did not match expected result.", name, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getJwkEnabled **************************************/

    @Test
    public void getJwkEnabled_configNotInitialized() {
        try {
            assertFalse("JWK enabled value was not false as expected.", configImpl.getJwkEnabled());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getJwkEnabled_propsMissingJwksUri() {
        try {
            Map<String, Object> props = getStandardConfigProps();
            configImpl.initProps(cc, props);

            assertFalse("JWK enabled value was not false as expected.", configImpl.getJwkEnabled());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getJwkEnabled_propsIncludeJwksUri() {
        try {
            Map<String, Object> props = getStandardConfigProps();
            props.put(OidcLoginConfigImpl.KEY_jwksUri, "some value");
            configImpl.initProps(cc, props);

            assertTrue("JWK enabled value was not true as expected.", configImpl.getJwkEnabled());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getConsumerUtils **************************************/

    @Test
    public void getConsumerUtils_noSocialLoginService() {
        try {
            ConsumerUtils result = configImpl.getConsumerUtils();
            assertNull("ConsumerUtils object should have been null but was " + result, result);

            verifyLogMessageWithInserts(outputMgr, CWWKS5464E_SERVICE_NOT_FOUND_JWT_CONSUMER_NOT_AVAILABLE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConsumerUtils_missingKeyStoreServiceRef() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    one(socialLoginService).getKeyStoreServiceRef();
                    will(returnValue(null));
                }
            });
            configImpl = getActivatedConfig(getStandardConfigProps());
            configImpl = setSocialLoginServiceReference(configImpl, socialLoginService);

            ConsumerUtils result = configImpl.getConsumerUtils();
            assertNotNull("ConsumerUtils object should not have been null but was.", result);

            verifyLogMessage(outputMgr, CWWKS5400I_SOCIAL_LOGIN_CONFIG_PROCESSED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getJwkSet **************************************/

    //@Test
    public void getJwkSet_configNotInitialized() {
        try {
            JWKSet result = configImpl.getJwkSet();
            assertNotNull("JWK set should not have been null but was.", result);
            assertNotNull("List of JWKs should not have been null but was.", result.getJWKs());
            assertEquals("Size of JWK list did not match expected value.", 0, result.getJWKs().size());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }
    
    @Test
    public void getUseSystemPropertiesForHttpClientConnections(){
        try {
            Map<String, Object> props = getStandardConfigProps();
            props.put(OidcLoginConfigImpl.KEY_USE_SYSPROPS_FOR_HTTPCLIENT_CONNECTONS, new Boolean(true));
            configImpl.initProps(cc, props);

            assertTrue("useSysprops not true as expected", configImpl.getUseSystemPropertiesForHttpClientConnections());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/
    
    
   

    protected OidcLoginConfigImpl getActivatedConfig(Map<String, Object> props) throws SocialLoginException {
        OidcLoginConfigImpl config = new OidcLoginConfigImpl();
        config.activate(cc, props);
        return config;
    }

    protected OidcLoginConfigImpl setSocialLoginServiceReference(final OidcLoginConfigImpl configImpl, final SocialLoginService socialLoginService) throws SocialLoginException, KeyStoreException {
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
        props.put(OidcLoginConfigImpl.KEY_clientId, clientId);
        props.put(OidcLoginConfigImpl.KEY_clientSecret, clientSecretPS);
        return props;
    }

    protected OidcLoginConfigImpl getConfigImplWithHandleJwtElementMocked() {
        return new OidcLoginConfigImpl() {
            @Override
            protected Configuration handleJwtElement(Map<String, Object> props, ConfigurationAdmin configurationAdmin) {
                return mockInterface.handleJwtElement();
            }
        };
    }

}
