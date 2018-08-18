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
package com.ibm.ws.security.social.tai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.websphere.security.social.UserProfile;
import com.ibm.ws.security.common.jwk.subject.mapping.AttributeToSubject;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.CacheToken;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.SocialHashUtils;
import com.ibm.ws.security.social.tai.TAISubjectUtils.SettingCustomPropertiesException;
import com.ibm.ws.security.social.test.CommonTestClass;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.token.AttributeNameConstants;

import test.common.SharedOutputManager;

public class TAISubjectUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    private static final String USERNAME = "John Q. Doe";
    private static String ACCESS_TOKEN = "EAANQIE2J5nMBAErWBIFfkmu9r6yQeGoIMg39mHRJrZA7L0jbiD7GEpLSZBm96tgqvvlbQI3UIgQXSJaO6sRJGaFEZCwn5kolWgSjs5q71rrNg0GdbHk5yxrtsZAWsZBv3XV1xFmJ4reZBKA6sx5PqQJejg5RtTWKPg4jJoP0zk1AZDZD";
    private static String REFRESH_TOKEN = "67890";
    private static long EXPIRES_IN = 5177064;
    // IdToken with claims {"at_hash":"0HbzhW49bhEP2b3SVHfeGg","sub":"user1","realmName":"OpBasicRealm","uniqueSecurityName":"user1","iss":"https://localhost:8020/oidc/endpoint/OP","nonce":"v1zg5OZ9vXP5h0lEiYs1","aud":"rp","exp":1455909058,"iat":1455901858}
    private static final String ID_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo4MDIwL29pZGMvZW5kcG9pbnQvT1AiLCJub25jZSI6InYxemc1T1o5dlhQNWgwbEVpWXMxIiwiaWF0IjoxNDU1OTAxODU4LCJzdWIiOiJ1c2VyMSIsImV4cCI6MTQ1NTkwOTA1OCwiYXVkIjoicnAiLCJyZWFsbU5hbWUiOiJPcEJhc2ljUmVhbG0iLCJ1bmlxdWVTZWN1cml0eU5hbWUiOiJ1c2VyMSIsImF0X2hhc2giOiIwSGJ6aFc0OWJoRVAyYjNTVkhmZUdnIn0.VJNknPRe0BhzfMA4MpQIEeVczaHYiMzPiBYejp72zIs";
    private final String uniqueId = "facebookLogin";
    private final String userApiResponseString = "{\"id\":\"104747623349374\",\"name\":\"Teddy Torres\",\"email\":\"teddyjtorres\\u0040hotmail.com\",\"gender\":\"male\"}";
    private final String socialMediaName = "facebookLogin";
    private final String userNameAttribute = "sub";
    private final String realmName = "facebook";
    private final String clientConfigScope = "email user_friends public_profile user_about_me";

    public interface MockInterface {
        public Hashtable<String, Object> setAllCustomProperties() throws SettingCustomPropertiesException;

        public Hashtable<String, Object> setUsernameAndCustomProperties() throws SettingCustomPropertiesException;

        Hashtable<String, Object> setUsernameAndCustomPropertiesUsingAttributeToSubjectMapping() throws SettingCustomPropertiesException;

        Hashtable<String, Object> setUsernameAndCustomPropertiesUsingJwt() throws SettingCustomPropertiesException;

        public Hashtable<String, Object> createCustomPropertiesFromSubjectMapping() throws SettingCustomPropertiesException;

        public Hashtable<String, Object> createCustomPropertiesFromConfig() throws SettingCustomPropertiesException;

        Subject buildSubject() throws SocialLoginException;

        UserProfile createUserProfile() throws SocialLoginException;

        Hashtable<String, Object> createCustomProperties() throws SocialLoginException;

        CacheToken createCacheToken();
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    private final JwtToken jwt = mockery.mock(JwtToken.class);
    private final JwtToken issuedJwt = mockery.mock(JwtToken.class, "issuedJwt");
    private final Claims claims = mockery.mock(Claims.class);
    private final AttributeToSubject attributeToSubject = mockery.mock(AttributeToSubject.class);
    private final SocialLoginConfig config = mockery.mock(SocialLoginConfig.class, "mockSocialLoginConfig");
    private final TAIEncryptionUtils taiEncryptionUtils = mockery.mock(TAIEncryptionUtils.class);
    private final TAIWebUtils taiWebUtils = mockery.mock(TAIWebUtils.class);
    private final UserProfile userProfile = mockery.mock(UserProfile.class);
    @SuppressWarnings("unchecked")
    private final Map<String, Object> userApiTokens = mockery.mock(Map.class);

    TAISubjectUtils subjectUtils = null;

    class MockTAISubjectUtils extends TAISubjectUtils {
        public MockTAISubjectUtils(String accessToken, JwtToken jwt, JwtToken issuedJwt, Map<String, Object> userApiResponseTokens, String userApiResponse) {
            super(accessToken, jwt, issuedJwt, userApiResponseTokens, userApiResponse);
        }

        AttributeToSubject createAttributeToSubject(SocialLoginConfig config) {
            return attributeToSubject;
        }
    }

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());

        subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, userApiResponseString);
        mockProtectedClassMembers(subjectUtils);
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

    private void mockProtectedClassMembers(TAISubjectUtils subjectUtils) {
        subjectUtils.taiWebUtils = taiWebUtils;
        subjectUtils.taiEncryptionUtils = taiEncryptionUtils;
    }

    /****************************************** createResult ******************************************/

    @Test
    public void createResult_errorSettingCustomProperties() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, userApiResponseString) {
            @Override
            Hashtable<String, Object> setAllCustomProperties(SocialLoginConfig config) throws SettingCustomPropertiesException {
                return mockInterface.setAllCustomProperties();
            }
        };
        mockProtectedClassMembers(subjectUtils);

        try {
            int errorCode = HttpServletResponse.SC_UNAUTHORIZED;
            final TAIResult unsuccessfulResult = TAIResult.create(errorCode);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).setAllCustomProperties();
                    will(throwException(new TAISubjectUtils.SettingCustomPropertiesException()));
                    one(taiWebUtils).sendToErrorPage(with(any(HttpServletResponse.class)), with(any(TAIResult.class)));
                    will(returnValue(unsuccessfulResult));
                }
            });

            TAIResult result = subjectUtils.createResult(response, config);
            assertResultStatus(errorCode, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createResult_buildingSubjectThrowsException() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, userApiResponseString) {
            @Override
            Hashtable<String, Object> setAllCustomProperties(SocialLoginConfig config) throws SettingCustomPropertiesException {
                return mockInterface.setAllCustomProperties();
            }

            Subject buildSubject(SocialLoginConfig config, Hashtable<String, Object> customProperties) throws SocialLoginException {
                return mockInterface.buildSubject();
            }
        };
        mockProtectedClassMembers(subjectUtils);

        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).setAllCustomProperties();
                    one(mockInterface).buildSubject();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });
            try {
                TAIResult result = subjectUtils.createResult(response, config);
                fail("Should have thrown an exception but got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createResult_success() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, userApiResponseString) {
            @Override
            Hashtable<String, Object> setAllCustomProperties(SocialLoginConfig config) throws SettingCustomPropertiesException {
                return mockInterface.setAllCustomProperties();
            }

            Subject buildSubject(SocialLoginConfig config, Hashtable<String, Object> customProperties) throws SocialLoginException {
                return mockInterface.buildSubject();
            }
        };
        mockProtectedClassMembers(subjectUtils);

        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).setAllCustomProperties();
                    one(mockInterface).buildSubject();
                }
            });
            try {
                TAIResult result = subjectUtils.createResult(response, config);
                fail("Should have thrown an exception because of a missing user principal, but got result: " + result);
            } catch (WebTrustAssociationFailedException e) {
                // Expected because we've mocked out the calls that would have set the principal name string, so a null principal value is being used
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** setAllCustomProperties ******************************************/

    @Test
    public void setAllCustomProperties_errorSettingUsernameOrProperties() throws Exception {
        final TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, userApiResponseString) {
            @Override
            Hashtable<String, Object> setUsernameAndCustomProperties(SocialLoginConfig clientConfig) throws SettingCustomPropertiesException {
                return mockInterface.setUsernameAndCustomProperties();
            }
        };
        mockProtectedClassMembers(subjectUtils);

        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).setUsernameAndCustomProperties();
                    will(throwException(new TAISubjectUtils.SettingCustomPropertiesException()));
                }
            });

            try {
                Hashtable<String, Object> result = subjectUtils.setAllCustomProperties(config);
                fail("Should have thrown an exception but got custom properties: " + result);
            } catch (SettingCustomPropertiesException e) {
                // No need to check anything
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setAllCustomProperties_missingUsername() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, userApiResponseString) {
            @Override
            Hashtable<String, Object> setUsernameAndCustomProperties(SocialLoginConfig clientConfig) throws SettingCustomPropertiesException {
                return mockInterface.setUsernameAndCustomProperties();
            }
        };
        mockProtectedClassMembers(subjectUtils);

        try {
            mockery.checking(new Expectations() {
                {
                    // Mock the method to return normally, but also to ensure the username variable isn't set
                    one(mockInterface).setUsernameAndCustomProperties();
                    will(returnValue(new Hashtable<String, Object>()));
                }
            });

            try {
                Hashtable<String, Object> result = subjectUtils.setAllCustomProperties(config);
                fail("Should have thrown an exception but got custom properties: " + result);
            } catch (SettingCustomPropertiesException e) {
                // No need to check anything
            }

            verifyLogMessage(outputMgr, CWWKS5435E_USERNAME_NOT_FOUND);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setAllCustomProperties_missingAccessToken() throws Exception {
        String userApiResponse = "{\"" + userNameAttribute + "\":\"" + USERNAME + "\"}";

        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(null, null, null, null, userApiResponse);
        mockProtectedClassMembers(subjectUtils);

        try {
            mockery.checking(new Expectations() {
                {
                    one(attributeToSubject).getMappedUser();
                    will(returnValue(USERNAME));
                    one(config).getMapToUserRegistry();
                    will(returnValue(true));
                }
            });

            try {
                Hashtable<String, Object> result = subjectUtils.setAllCustomProperties(config);
                fail("Should have thrown an exception but got custom properties: " + result);
            } catch (SettingCustomPropertiesException e) {
                // No need to check anything
            }

            verifyLogMessageWithInserts(outputMgr, CWWKS5455E_ACCESS_TOKEN_MISSING, USERNAME);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setAllCustomProperties_success() throws Exception {
        String userApiResponse = "{\"" + userNameAttribute + "\":\"" + USERNAME + "\"}";

        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, userApiResponse) {
            @Override
            Hashtable<String, Object> createCustomPropertiesFromSubjectMapping(SocialLoginConfig clientConfig, AttributeToSubject attributeToSubject) throws SettingCustomPropertiesException {
                return mockInterface.createCustomPropertiesFromSubjectMapping();
            }
        };
        mockProtectedClassMembers(subjectUtils);

        try {
            mockery.checking(new Expectations() {
                {
                    one(attributeToSubject).getMappedUser();
                    will(returnValue(USERNAME));
                    one(config).getMapToUserRegistry();
                    will(returnValue(false));
                    one(mockInterface).createCustomPropertiesFromSubjectMapping();
                    will(returnValue(new Hashtable<String, Object>()));
                }
            });

            Hashtable<String, Object> result = subjectUtils.setAllCustomProperties(config);
            assertEquals("Result did not contain the expected number of custom properties. Props were: " + result, 2, result.size());

            assertEquals("Security name custom property did not match expected value.", USERNAME, result.get(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME));
            assertEquals("Access token custom property did not match expected value.", ACCESS_TOKEN, result.get(ClientConstants.ACCESS_TOKEN));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** setUsernameAndCustomProperties ******************************************/

    @Test
    public void setUsernameAndCustomProperties_nonNullUserApiResponse() throws Exception {
        String userApiResponse = "";

        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, userApiResponse) {
            Hashtable<String, Object> setUsernameAndCustomPropertiesUsingAttributeToSubjectMapping(SocialLoginConfig config) throws SettingCustomPropertiesException {
                return mockInterface.setUsernameAndCustomPropertiesUsingAttributeToSubjectMapping();
            }
        };
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).setUsernameAndCustomPropertiesUsingAttributeToSubjectMapping();
                    will(returnValue(new Hashtable<String, Object>()));
                }
            });

            Hashtable<String, Object> result = subjectUtils.setUsernameAndCustomProperties(config);
            assertTrue("Result should have been empty but was: " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setUsernameAndCustomProperties_nullUserApiResponse() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, null, null, null, null) {
            Hashtable<String, Object> setUsernameAndCustomPropertiesUsingJwt(SocialLoginConfig config) throws SettingCustomPropertiesException {
                return mockInterface.setUsernameAndCustomPropertiesUsingJwt();
            }
        };
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).setUsernameAndCustomPropertiesUsingJwt();
                    will(returnValue(new Hashtable<String, Object>()));
                }
            });
            Hashtable<String, Object> result = subjectUtils.setUsernameAndCustomProperties(config);
            assertTrue("Properties result should have been empty but was: " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** setUsernameAndCustomPropertiesUsingAttributeToSubjectMapping ******************************************/

    @Test
    public void setUsernameAndCustomPropertiesUsingAttributeToSubjectMapping_mapToUserRegistry() throws Exception {
        String userApiResponse = "";

        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, userApiResponse);
        mockProtectedClassMembers(subjectUtils);

        try {
            mockery.checking(new Expectations() {
                {
                    one(attributeToSubject).getMappedUser();
                    will(returnValue(USERNAME));
                    one(config).getMapToUserRegistry();
                    will(returnValue(true));
                }
            });

            Hashtable<String, Object> result = subjectUtils.setUsernameAndCustomPropertiesUsingAttributeToSubjectMapping(config);
            assertTrue("Result should have been empty but was: " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setUsernameAndCustomPropertiesUsingAttributeToSubjectMapping_doNotMapToUserRegistry() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, userApiResponseString) {
            @Override
            Hashtable<String, Object> createCustomPropertiesFromSubjectMapping(SocialLoginConfig clientConfig, AttributeToSubject attributeToSubject) throws SettingCustomPropertiesException {
                return mockInterface.createCustomPropertiesFromSubjectMapping();
            }
        };
        mockProtectedClassMembers(subjectUtils);

        try {
            mockery.checking(new Expectations() {
                {
                    one(attributeToSubject).getMappedUser();
                    will(returnValue(USERNAME));
                    one(config).getMapToUserRegistry();
                    will(returnValue(false));
                    one(mockInterface).createCustomPropertiesFromSubjectMapping();
                    will(returnValue(new Hashtable<String, Object>()));
                }
            });

            Hashtable<String, Object> result = subjectUtils.setUsernameAndCustomPropertiesUsingAttributeToSubjectMapping(config);
            assertTrue("Result should have been empty but was: " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setUsernameAndCustomPropertiesUsingAttributeToSubjectMapping_doNotMapToUserRegistry_errorResult() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, null, null, null, userApiResponseString) {
            @Override
            Hashtable<String, Object> createCustomPropertiesFromSubjectMapping(SocialLoginConfig clientConfig, AttributeToSubject attributeToSubject) throws SettingCustomPropertiesException {
                return mockInterface.createCustomPropertiesFromSubjectMapping();
            }
        };
        mockProtectedClassMembers(subjectUtils);

        try {
            mockery.checking(new Expectations() {
                {
                    one(attributeToSubject).getMappedUser();
                    will(returnValue(USERNAME));
                    one(config).getMapToUserRegistry();
                    will(returnValue(false));
                    one(mockInterface).createCustomPropertiesFromSubjectMapping();
                    will(throwException(new TAISubjectUtils.SettingCustomPropertiesException()));
                }
            });
            try {
                Hashtable<String, Object> result = subjectUtils.setUsernameAndCustomPropertiesUsingAttributeToSubjectMapping(config);
                fail("Should have thrown an exception but got custom properties: " + result);
            } catch (SettingCustomPropertiesException e) {
                // No need to check anything
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** setUsernameAndCustomPropertiesUsingJwt ******************************************/

    @Test
    public void setUsernameAndCustomPropertiesUsingJwt_nullJwt() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, null, null, null, null);
        try {
            Hashtable<String, Object> result = subjectUtils.setUsernameAndCustomPropertiesUsingJwt(config);
            assertTrue("Properties result should have been empty but was: " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setUsernameAndCustomPropertiesUsingJwt_validJwt_nullClaims() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, null);
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwt).getClaims();
                    will(returnValue(null));
                }
            });

            Hashtable<String, Object> result = subjectUtils.setUsernameAndCustomPropertiesUsingJwt(config);
            assertTrue("Result should have been empty but was: " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setUsernameAndCustomPropertiesUsingJwt_validJwt_missingUsernameAttributeClaim() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, null);
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwt).getClaims();
                    will(returnValue(claims));
                    one(config).getUserNameAttribute();
                    will(returnValue(userNameAttribute));
                    one(claims).get(userNameAttribute);
                    will(returnValue(null));
                }
            });

            Hashtable<String, Object> result = subjectUtils.setUsernameAndCustomPropertiesUsingJwt(config);
            assertTrue("Result should have been empty but was: " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setUsernameAndCustomPropertiesUsingJwt_validJwt_mapToUserRegsitry() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, null);
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwt).getClaims();
                    will(returnValue(claims));
                    one(config).getUserNameAttribute();
                    will(returnValue(userNameAttribute));
                    one(claims).get(userNameAttribute);
                    will(returnValue(USERNAME));
                    one(config).getMapToUserRegistry();
                    will(returnValue(true));
                }
            });

            Hashtable<String, Object> result = subjectUtils.setUsernameAndCustomPropertiesUsingJwt(config);
            assertTrue("Result should have been empty but was: " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setUsernameAndCustomPropertiesUsingJwt_validJwt_doNotMapToUserRegsitry_errorResult() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, null) {
            @Override
            Hashtable<String, Object> createCustomPropertiesFromConfig(SocialLoginConfig config) throws SettingCustomPropertiesException {
                return mockInterface.createCustomPropertiesFromConfig();
            }
        };
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwt).getClaims();
                    will(returnValue(claims));
                    one(config).getUserNameAttribute();
                    will(returnValue(userNameAttribute));
                    one(claims).get(userNameAttribute);
                    will(returnValue(USERNAME));
                    one(config).getMapToUserRegistry();
                    will(returnValue(false));
                    one(mockInterface).createCustomPropertiesFromConfig();
                    will(throwException(new TAISubjectUtils.SettingCustomPropertiesException()));
                }
            });
            try {
                Hashtable<String, Object> result = subjectUtils.setUsernameAndCustomPropertiesUsingJwt(config);
                fail("Should have thrown an exception but got custom properties: " + result);
            } catch (SettingCustomPropertiesException e) {
                // No need to check anything
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setUsernameAndCustomPropertiesUsingJwt_validJwt_doNotMapToUserRegsitry_nullResult() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, null, null, null) {
            @Override
            Hashtable<String, Object> createCustomPropertiesFromConfig(SocialLoginConfig config) throws SettingCustomPropertiesException {
                return mockInterface.createCustomPropertiesFromConfig();
            }
        };
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwt).getClaims();
                    will(returnValue(claims));
                    one(config).getUserNameAttribute();
                    will(returnValue(userNameAttribute));
                    one(claims).get(userNameAttribute);
                    will(returnValue(USERNAME));
                    one(config).getMapToUserRegistry();
                    will(returnValue(false));
                    one(mockInterface).createCustomPropertiesFromConfig();
                    will(returnValue(new Hashtable<String, Object>()));
                }
            });

            Hashtable<String, Object> result = subjectUtils.setUsernameAndCustomPropertiesUsingJwt(config);
            assertTrue("Result should have been empty but was: " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** buildSubject ******************************************/

    @Test
    public void buildSubject_nullJwt_nullIssuedJwt_nullUserProfile() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, null, null, userApiTokens, null) {
            @Override
            UserProfile createUserProfile(SocialLoginConfig config) throws SocialLoginException {
                return mockInterface.createUserProfile();
            }

            @Override
            CacheToken createCacheToken(SocialLoginConfig clientConfig) {
                return mockInterface.createCacheToken();
            }
        };
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createUserProfile();
                    will(returnValue(null));
                    one(mockInterface).createCacheToken();
                }
            });
            Subject subject = subjectUtils.buildSubject(config, new Hashtable<String, Object>());

            // Subject should have one private cred: An empty Hashtable
            Set<Object> privateCreds = subject.getPrivateCredentials();
            assertEquals("Did not find the expected number of private credentials: " + privateCreds, 1, privateCreds.size());

            Hashtable<String, Object> hashtableCreds = assertHashtablePrivateCredentialExists(subject);
            assertTrue("Hashtable credentials should have been empty but were : " + hashtableCreds, hashtableCreds.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void buildSubject_nullUserProfile() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, null) {
            @Override
            UserProfile createUserProfile(SocialLoginConfig config) throws SocialLoginException {
                return mockInterface.createUserProfile();
            }

            @Override
            CacheToken createCacheToken(SocialLoginConfig clientConfig) {
                return mockInterface.createCacheToken();
            }
        };
        try {
            final String compactedIssuedJwt = "compactedIssuedJwt";
            mockery.checking(new Expectations() {
                {
                    one(issuedJwt).compact();
                    will(returnValue(compactedIssuedJwt));
                    one(mockInterface).createUserProfile();
                    will(returnValue(null));
                    one(mockInterface).createCacheToken();
                }
            });
            Hashtable<String, Object> inputProps = new Hashtable<String, Object>();
            inputProps.put("prop1", "value1");
            inputProps.put("prop2", "value2");

            Subject subject = subjectUtils.buildSubject(config, inputProps);

            // Subject should have two private creds: A JwtToken, and a Hashtable with an issued JWT entry
            Set<Object> privateCreds = subject.getPrivateCredentials();
            assertEquals("Did not find the expected number of private credentials: " + privateCreds, 2, privateCreds.size());

            assertJwtTokenPrivateCredentialExists(subject, jwt);

            Hashtable<String, Object> hashtableCreds = assertHashtablePrivateCredentialExists(subject);
            // Hashtable creds should include the issued JWT entry AND the original props that were passed into the method
            assertEquals("Did not find the expected number of hashtable credentials: " + hashtableCreds, 3, hashtableCreds.size());

            assertIssuedJwtPrivateCredentialExists(hashtableCreds, compactedIssuedJwt);
            // Ensure original properties are still in the Hashtable
            for (Entry<String, Object> entry : inputProps.entrySet()) {
                assertEquals("Hashtable credential for original prop [" + entry.getKey() + "] did not match expected value.", entry.getValue(), hashtableCreds.get(entry.getKey()));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void buildSubject_exceptionThrownCreatingUserProfile() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, null) {
            @Override
            UserProfile createUserProfile(SocialLoginConfig config) throws SocialLoginException {
                return mockInterface.createUserProfile();
            }

            @Override
            CacheToken createCacheToken(SocialLoginConfig clientConfig) {
                return mockInterface.createCacheToken();
            }
        };
        try {
            mockery.checking(new Expectations() {
                {
                    one(issuedJwt).compact();
                    one(mockInterface).createUserProfile();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });
            try {
                Subject subject = subjectUtils.buildSubject(config, new Hashtable<String, Object>());
                fail("Should have thrown an exception but got subject: " + subject);
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void buildSubject_nullJwts() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, null, null, null, null) {
            @Override
            UserProfile createUserProfile(SocialLoginConfig config) throws SocialLoginException {
                return mockInterface.createUserProfile();
            }

            @Override
            CacheToken createCacheToken(SocialLoginConfig clientConfig) {
                return mockInterface.createCacheToken();
            }
        };
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createUserProfile();
                    will(returnValue(userProfile));
                    one(userProfile).getEncryptedAccessToken();
                    one(userProfile).getAccessTokenAlias();
                    one(mockInterface).createCacheToken();
                }
            });
            Subject subject = subjectUtils.buildSubject(config, new Hashtable<String, Object>());

            // Subject should have two private creds: An empty Hashtable, and a UserProfile
            Set<Object> privateCreds = subject.getPrivateCredentials();
            assertEquals("Did not find the expected number of private credentials: " + privateCreds, 2, privateCreds.size());

            Hashtable<String, Object> hashtableCreds = assertHashtablePrivateCredentialExists(subject);
            assertTrue("Hashtable credentials should have been empty but were : " + hashtableCreds, hashtableCreds.isEmpty());

            assertUserProfilePrivateCredentialExists(subject, userProfile);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void buildSubject_nonNullEncryptedAccessToken_nonNullAccessTokenAlias() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, null, null) {
            @Override
            UserProfile createUserProfile(SocialLoginConfig config) throws SocialLoginException {
                return mockInterface.createUserProfile();
            }

            @Override
            CacheToken createCacheToken(SocialLoginConfig clientConfig) {
                return mockInterface.createCacheToken();
            }
        };
        try {
            final String compactedIssuedJwt = "compactedIssuedJwt";
            mockery.checking(new Expectations() {
                {
                    one(issuedJwt).compact();
                    will(returnValue(compactedIssuedJwt));
                    one(mockInterface).createUserProfile();
                    will(returnValue(userProfile));
                    one(userProfile).getEncryptedAccessToken();
                    will(returnValue("encryptedAccessToken"));
                    one(userProfile).getAccessTokenAlias();
                    will(returnValue("accessTokenAlias"));
                    one(mockInterface).createCacheToken();
                }
            });
            Subject subject = subjectUtils.buildSubject(config, new Hashtable<String, Object>());

            // Subject should have three private creds: A JwtToken, A Hashtable with an issued JWT entry, and a UserProfile
            Set<Object> privateCreds = subject.getPrivateCredentials();
            assertEquals("Did not find the expected number of private credentials: " + privateCreds, 3, privateCreds.size());

            assertJwtTokenPrivateCredentialExists(subject, jwt);

            Hashtable<String, Object> hashtableCreds = assertHashtablePrivateCredentialExists(subject);
            assertEquals("Did not find the expected number of hashtable credentials: " + hashtableCreds, 1, hashtableCreds.size());
            assertIssuedJwtPrivateCredentialExists(hashtableCreds, compactedIssuedJwt);

            assertUserProfilePrivateCredentialExists(subject, userProfile);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createCustomPropertiesFromSubjectMapping ******************************************/

    @Test
    public void createCustomPropertiesFromSubjectMapping_nullRealm() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(attributeToSubject).getMappedRealm();
                    will(returnValue(null));
                    one(taiWebUtils).getAuthorizationEndpoint(config);
                    will(returnValue(null));
                }
            });
            try {
                Hashtable<String, Object> result = subjectUtils.createCustomPropertiesFromSubjectMapping(config, attributeToSubject);
                fail("Should have thrown an exception but got custom properties: " + result);
            } catch (SettingCustomPropertiesException e) {
                // No need to check anything
            }

            verifyLogMessage(outputMgr, CWWKS5436E_REALM_NOT_FOUND);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createCustomPropertiesFromSubjectMapping_nullUniqueUser_noGroups() throws Exception {
        try {
            final String uniqueUser = null;
            mockery.checking(new Expectations() {
                {
                    one(attributeToSubject).getMappedRealm();
                    will(returnValue(realmName));
                    one(attributeToSubject).getMappedUniqueUser();
                    will(returnValue(uniqueUser));
                    one(attributeToSubject).getMappedGroups();
                }
            });

            Hashtable<String, Object> result = subjectUtils.createCustomPropertiesFromSubjectMapping(null, attributeToSubject);
            assertEquals("Did not find the expected number of custom properties. Props were: " + result, 2, result.size());

            String expectedUniqueId = "user:" + realmName + "/" + uniqueUser;
            assertEquals("Unique ID custom property did not match expected value.", expectedUniqueId, result.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID));
            assertEquals("Realm custom property did not match expected value.", realmName, result.get(AttributeNameConstants.WSCREDENTIAL_REALM));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createCustomPropertiesFromSubjectMapping_withGroups() throws Exception {
        try {
            final String uniqueUser = " a / user ";
            final List<String> groups = new ArrayList<String>();
            groups.add("group1");
            groups.add("my 2nd \n\t group");

            mockery.checking(new Expectations() {
                {
                    one(attributeToSubject).getMappedRealm();
                    will(returnValue(realmName));
                    one(attributeToSubject).getMappedUniqueUser();
                    will(returnValue(uniqueUser));
                    one(attributeToSubject).getMappedGroups();
                    will(returnValue(groups));
                }
            });

            Hashtable<String, Object> result = subjectUtils.createCustomPropertiesFromSubjectMapping(config, attributeToSubject);
            assertEquals("Did not find the expected number of custom properties. Props were: " + result, 3, result.size());

            String expectedUniqueId = "user:" + realmName + "/" + uniqueUser;
            assertEquals("Unique ID custom property did not match expected value.", expectedUniqueId, result.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID));
            assertEquals("Realm custom property did not match expected value.", realmName, result.get(AttributeNameConstants.WSCREDENTIAL_REALM));

            List<String> groupsProp = (List<String>) result.get(AttributeNameConstants.WSCREDENTIAL_GROUPS);
            assertNotNull("Groups entry should not have been null but was. Props were: " + result, groupsProp);
            for (String groupName : groups) {
                String expectedGroupId = "group:" + realmName + "/" + groupName;
                assertTrue("Groups property did not contain expected entry for [" + expectedGroupId + "]. Groups were: " + groupsProp, groupsProp.contains(expectedGroupId));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createCustomPropertiesFromConfig ******************************************/

    @Test
    public void createCustomPropertiesFromConfig_nullRealm() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getRealmName();
                    will(returnValue(null));
                    one(taiWebUtils).getAuthorizationEndpoint(config);
                    will(returnValue(null));
                }
            });
            try {
                Hashtable<String, Object> result = subjectUtils.createCustomPropertiesFromConfig(config);
                fail("Should have thrown an exception but got custom properties: " + result);
            } catch (SettingCustomPropertiesException e) {
                // No need to check anything
            }

            verifyLogMessage(outputMgr, CWWKS5436E_REALM_NOT_FOUND);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createCustomPropertiesFromConfig_emptyRealmNullUsername() throws Exception {
        try {
            // Username member variable is set by another method so will only be null here
            final String username = null;
            final String realm = "";
            mockery.checking(new Expectations() {
                {
                    one(config).getRealmName();
                    will(returnValue(realm));
                }
            });

            Hashtable<String, Object> result = subjectUtils.createCustomPropertiesFromConfig(config);
            assertEquals("Did not find the expected number of custom properties. Props were: " + result, 2, result.size());

            String expectedUniqueId = "user:" + realm + "/" + username;
            assertEquals("Unique ID custom property did not match expected value.", expectedUniqueId, result.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID));
            assertEquals("Realm custom property did not match expected value.", realm, result.get(AttributeNameConstants.WSCREDENTIAL_REALM));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createCustomPropertiesFromConfig() throws Exception {
        try {
            // Username member variable is set by another method so will only be null here
            final String username = null;
            final String realm = "http://my unique realm| name. value";
            mockery.checking(new Expectations() {
                {
                    one(config).getRealmName();
                    will(returnValue(realm));
                }
            });

            Hashtable<String, Object> result = subjectUtils.createCustomPropertiesFromConfig(config);
            assertEquals("Did not find the expected number of custom properties. Props were: " + result, 2, result.size());

            String expectedUniqueId = "user:" + realm + "/" + username;
            assertEquals("Unique ID custom property did not match expected value.", expectedUniqueId, result.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID));
            assertEquals("Realm custom property did not match expected value.", realm, result.get(AttributeNameConstants.WSCREDENTIAL_REALM));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** getDefaultRealmFromAuthorizationEndpoint ******************************************/

    @Test
    public void getDefaultRealmFromAuthorizationEndpoint_errorGettingAuthorizationEndpoint() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).getAuthorizationEndpoint(config);
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                String result = subjectUtils.getDefaultRealmFromAuthorizationEndpoint(config);
                fail("Should have thrown an exception but got: [" + result + "].");
            } catch (SettingCustomPropertiesException e) {
                // Do nothing - this is expected
            }

            verifyLogMessage(outputMgr, Pattern.quote(defaultExceptionMsg));
            verifyLogMessage(outputMgr, CWWKS5436E_REALM_NOT_FOUND);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getDefaultRealmFromAuthorizationEndpoint_authzEndpointNotHttps_noPath() throws Exception {
        try {
            final String authzEndpoint = "http://my-domain.com:80";
            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).getAuthorizationEndpoint(config);
                    will(returnValue(authzEndpoint));
                }
            });

            String result = subjectUtils.getDefaultRealmFromAuthorizationEndpoint(config);
            assertEquals("Non-HTTPS authorization endpoint should still have returned host and port as the realm.", authzEndpoint, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getDefaultRealmFromAuthorizationEndpoint_authzEndpointNotHttps_withPath() throws Exception {
        try {
            final String host = "http://my-domain.com:80";
            final String authzEndpoint = host + "/some/path";
            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).getAuthorizationEndpoint(config);
                    will(returnValue(authzEndpoint));
                }
            });

            String result = subjectUtils.getDefaultRealmFromAuthorizationEndpoint(config);
            assertEquals("Non-HTTPS authorization endpoint should still have returned host and port as the realm.", host, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getDefaultRealmFromAuthorizationEndpoint_authzEndpointHttps_noPath() throws Exception {
        try {
            final String authzEndpoint = "https://my-domain.com:80";
            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).getAuthorizationEndpoint(config);
                    will(returnValue(authzEndpoint));
                }
            });

            String result = subjectUtils.getDefaultRealmFromAuthorizationEndpoint(config);
            assertEquals("HTTPS authorization endpoint without path component should have returned host and port as the realm.", authzEndpoint, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getDefaultRealmFromAuthorizationEndpoint_authzEndpointHttps_withPath() throws Exception {
        try {
            final String host = "https://my-domain.com:80";
            final String authzEndpoint = host + "/some/path";
            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).getAuthorizationEndpoint(config);
                    will(returnValue(authzEndpoint));
                }
            });

            String result = subjectUtils.getDefaultRealmFromAuthorizationEndpoint(config);
            assertEquals("HTTPS authorization endpoint with path component should have returned host and port as the realm.", host, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getDefaultRealmFromAuthorizationEndpoint_authzEndpointHttps_withPathAndQuery() throws Exception {
        try {
            final String host = "https://my-domain.com:80";
            final String authzEndpoint = host + "/some/path?and=stuff";
            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).getAuthorizationEndpoint(config);
                    will(returnValue(authzEndpoint));
                }
            });

            String result = subjectUtils.getDefaultRealmFromAuthorizationEndpoint(config);
            assertEquals("HTTPS authorization endpoint with query string should have returned host and port as the realm.", host, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createUserProfile ******************************************/

    @Test
    public void createUserProfile_exceptionThrownCreatingProperties() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, userApiResponseString) {
            Hashtable<String, Object> createCustomProperties(SocialLoginConfig config, boolean getRefreshAndIdTokens) throws SocialLoginException {
                return mockInterface.createCustomProperties();
            }
        };
        mockProtectedClassMembers(subjectUtils);

        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createCustomProperties();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });
            try {
                UserProfile result = subjectUtils.createUserProfile(config);
                fail("Should have thrown SocialLoginException for missing access token. Result was: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createUserProfile_nullUserApiResponse() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, null) {
            Hashtable<String, Object> createCustomProperties(SocialLoginConfig config, boolean getRefreshAndIdTokens) throws SocialLoginException {
                return mockInterface.createCustomProperties();
            }
        };
        mockProtectedClassMembers(subjectUtils);

        try {
            String accessTokenAlias = "accessTokenAlias";
            final Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
            customProperties.put("access_token", ACCESS_TOKEN);
            customProperties.put("refresh_token", REFRESH_TOKEN);
            customProperties.put("expires_in", 1234L);
            customProperties.put("social_media", socialMediaName);
            customProperties.put("scope", clientConfigScope);
            customProperties.put("id_token", ID_TOKEN);
            customProperties.put("accessTokenAlias", accessTokenAlias);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createCustomProperties();
                    will(returnValue(customProperties));
                }
            });
            UserProfile result = subjectUtils.createUserProfile(config);
            assertUserProfile(result, ACCESS_TOKEN, REFRESH_TOKEN, jwt, 1234L, socialMediaName, clientConfigScope, accessTokenAlias);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createUserProfile_emptyCustomProperties() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, userApiResponseString) {
            Hashtable<String, Object> createCustomProperties(SocialLoginConfig config, boolean getRefreshAndIdTokens) throws SocialLoginException {
                return mockInterface.createCustomProperties();
            }
        };
        mockProtectedClassMembers(subjectUtils);

        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createCustomProperties();
                    will(returnValue(new Hashtable<String, Object>()));
                }
            });
            UserProfile result = subjectUtils.createUserProfile(config);
            assertUserProfile(result, null, null, null, 0, null, null, null);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createCustomProperties ******************************************/

    @Test
    public void createCustomProperties_nullUserApiTokens() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, null, userApiResponseString);
        try {
            try {
                Hashtable<String, Object> result = subjectUtils.createCustomProperties(config, false);
                fail("Should have thrown SocialLoginException for missing access token but did not. Result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5459E_SOCIAL_LOGIN_RESULT_MISSING_ACCESS_TOKEN);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createCustomProperties_emptyUserApiTokens() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, new HashMap<String, Object>(), userApiResponseString);
        mockProtectedClassMembers(subjectUtils);
        try {
            try {
                Hashtable<String, Object> result = subjectUtils.createCustomProperties(config, false);
                fail("Should have thrown SocialLoginException for missing access token but did not. Result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5459E_SOCIAL_LOGIN_RESULT_MISSING_ACCESS_TOKEN);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createCustomProperties_accessTokenOnlyInUserApiTokens_getRefreshAndIdTokens() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(ACCESS_TOKEN, null, null, null);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(config).getScope();
                    will(returnValue(clientConfigScope));
                    one(taiEncryptionUtils).getEncryptedAccessToken(with(any(SocialLoginConfig.class)), with(any(String.class)));
                }
            });

            Hashtable<String, Object> result = subjectUtils.createCustomProperties(config, true);

            // Expect entries for: access token, config ID, scope, access token alias
            assertEquals("Unexpected number of entries in the properties table. Properties were: " + result, 4, result.size());

            assertEquals("Access token did not match expected value. Properties were: " + result, ACCESS_TOKEN, result.get(ClientConstants.ACCESS_TOKEN));
            assertEquals("Config ID did not match expected value. Properties were: " + result, uniqueId, result.get(ClientConstants.SOCIAL_MEDIA));
            assertEquals("Scope did not match expected value. Properties were: " + result, clientConfigScope, result.get(ClientConstants.SCOPE));
            String expectedAccessTokenAlias = SocialHashUtils.digest(ACCESS_TOKEN);
            assertEquals("Access token alias value did not match expected value. Properties were: " + result, expectedAccessTokenAlias, result.get(ClientConstants.ACCESS_TOKEN_ALIAS));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createCustomProperties_allTokensInUserApiTokens_doNotGetRefreshOrIdToken() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(ACCESS_TOKEN, REFRESH_TOKEN, ID_TOKEN, EXPIRES_IN);
            userApiResponseTokens.put(ClientConstants.SCOPE, clientConfigScope);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(taiEncryptionUtils).getEncryptedAccessToken(with(any(SocialLoginConfig.class)), with(any(String.class)));
                }
            });

            Hashtable<String, Object> result = subjectUtils.createCustomProperties(config, false);

            // Expect entries for: access token, expires_in, config ID, scope, access token alias
            assertEquals("Unexpected number of entries in the properties table. Properties were: " + result, 5, result.size());

            assertEquals("Access token did not match expected value. Properties were: " + result, ACCESS_TOKEN, result.get(ClientConstants.ACCESS_TOKEN));
            assertEquals("Lifetime value did not match expected value. Properties were: " + result, EXPIRES_IN, result.get(ClientConstants.EXPIRES_IN));
            assertEquals("Config ID did not match expected value. Properties were: " + result, uniqueId, result.get(ClientConstants.SOCIAL_MEDIA));
            assertEquals("Scope did not match expected value. Properties were: " + result, clientConfigScope, result.get(ClientConstants.SCOPE));
            String expectedAccessTokenAlias = SocialHashUtils.digest(ACCESS_TOKEN);
            assertEquals("Access token alias value did not match expected value. Properties were: " + result, expectedAccessTokenAlias, result.get(ClientConstants.ACCESS_TOKEN_ALIAS));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createCustomProperties_allTokens() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(ACCESS_TOKEN, REFRESH_TOKEN, ID_TOKEN, EXPIRES_IN);
            userApiResponseTokens.put(ClientConstants.SCOPE, clientConfigScope);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(taiEncryptionUtils).getEncryptedAccessToken(with(any(SocialLoginConfig.class)), with(any(String.class)));
                }
            });

            Hashtable<String, Object> result = subjectUtils.createCustomProperties(config, true);

            // Expect entries for: access token, refresh token, ID token, expires_in, config ID, scope, access token alias
            assertEquals("Unexpected number of entries in the properties table. Properties were: " + result, 7, result.size());

            assertEquals("Access token did not match expected value. Properties were: " + result, ACCESS_TOKEN, result.get(ClientConstants.ACCESS_TOKEN));
            assertEquals("Refresh token did not match expected value. Properties were: " + result, REFRESH_TOKEN, result.get(ClientConstants.REFRESH_TOKEN));
            assertEquals("ID token did not match expected value. Properties were: " + result, ID_TOKEN, result.get(ClientConstants.ID_TOKEN));
            assertEquals("Lifetime value did not match expected value. Properties were: " + result, EXPIRES_IN, result.get(ClientConstants.EXPIRES_IN));
            assertEquals("Config ID did not match expected value. Properties were: " + result, uniqueId, result.get(ClientConstants.SOCIAL_MEDIA));
            assertEquals("Scope did not match expected value. Properties were: " + result, clientConfigScope, result.get(ClientConstants.SCOPE));
            String expectedAccessTokenAlias = SocialHashUtils.digest(ACCESS_TOKEN);
            assertEquals("Access token alias value did not match expected value. Properties were: " + result, expectedAccessTokenAlias, result.get(ClientConstants.ACCESS_TOKEN_ALIAS));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** getAccessTokenAndAddCustomProp ******************************************/

    @Test
    public void getAccessTokenAndAddCustomProp_missingAccessToken() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(null, null, null, null);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            try {
                String accessToken = subjectUtils.getAccessTokenAndAddCustomProp(props);
                fail("Should have thrown SocialLoginException, but got access token: [" + accessToken + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5459E_SOCIAL_LOGIN_RESULT_MISSING_ACCESS_TOKEN);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAccessTokenAndAddCustomProp_emptyAccessToken() throws Exception {
        try {
            String inputAccessToken = "";
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(inputAccessToken, null, null, null);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            String accessToken = subjectUtils.getAccessTokenAndAddCustomProp(props);
            assertEquals("Returned access token should match the original (empty) token.", inputAccessToken, accessToken);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAccessTokenAndAddCustomProp_nonEmptyAccessToken() throws Exception {
        try {
            String inputAccessToken = " \t" + ACCESS_TOKEN + " \t";
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(inputAccessToken, null, null, null);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            String accessToken = subjectUtils.getAccessTokenAndAddCustomProp(props);
            assertEquals("Returned access token should match the original token.", inputAccessToken, accessToken);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** addRefreshTokenCustomProp ******************************************/

    @Test
    public void addRefreshTokenCustomProp_missingRefreshToken() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(null, null, null, null);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addRefreshTokenCustomProp(props);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addRefreshTokenCustomProp_emptyRefreshToken() throws Exception {
        try {
            String inputRefreshToken = "";
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(null, inputRefreshToken, null, null);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addRefreshTokenCustomProp(props);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addRefreshTokenCustomProp_whitespaceRefreshToken() throws Exception {
        try {
            String inputRefreshToken = " \n\r \t ";
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(null, inputRefreshToken, null, null);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addRefreshTokenCustomProp(props);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addRefreshTokenCustomProp_nonEmptyRefreshToken() throws Exception {
        try {
            String inputRefreshToken = "myRefreshToken";
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(null, inputRefreshToken, null, null);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addRefreshTokenCustomProp(props);

            assertFalse("Property map should not have remained empty but did.", props.isEmpty());
            assertEquals("Refresh token entry did not match original value.", inputRefreshToken, props.get(ClientConstants.REFRESH_TOKEN));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addRefreshTokenCustomProp_nonEmptyRefreshTokenWithWhitespace() throws Exception {
        try {
            String inputRefreshToken = "\t my Refresh\n\rToken  ";
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(null, inputRefreshToken, null, null);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addRefreshTokenCustomProp(props);

            assertFalse("Property map should not have remained empty but did.", props.isEmpty());
            assertEquals("Refresh token entry did not match original value.", inputRefreshToken, props.get(ClientConstants.REFRESH_TOKEN));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** addIdTokenCustomProp ******************************************/

    @Test
    public void addIdTokenCustomProp_missingIdToken() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(null, null, null, null);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addIdTokenCustomProp(props);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addIdTokenCustomProp_emptyIdToken() throws Exception {
        try {
            String inputIdToken = "";
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(null, null, inputIdToken, null);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addIdTokenCustomProp(props);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addIdTokenCustomProp_whitespaceIdToken() throws Exception {
        try {
            String inputIdToken = " \n\r \t ";
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(null, null, inputIdToken, null);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addIdTokenCustomProp(props);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addIdTokenCustomProp_nonEmptyIdToken() throws Exception {
        try {
            String inputIdToken = "myIdToken";
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(null, null, inputIdToken, null);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addIdTokenCustomProp(props);

            assertFalse("Property map should not have remained empty but did.", props.isEmpty());
            assertEquals("ID token entry did not match original value.", inputIdToken, props.get(ClientConstants.ID_TOKEN));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addIdTokenCustomProp_nonEmptyIdTokenWithWhitespace() throws Exception {
        try {
            String inputIdToken = "\t my Id\n\rToken  ";
            Map<String, Object> userApiResponseTokens = createUserApiTokenMap(null, null, inputIdToken, null);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addIdTokenCustomProp(props);

            assertFalse("Property map should not have remained empty but did.", props.isEmpty());
            assertEquals("ID token entry did not match original value.", inputIdToken, props.get(ClientConstants.ID_TOKEN));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** addAccessTokenLifetimeCustomProp ******************************************/

    @Test
    public void addAccessTokenLifetimeCustomProp_missingLifetime() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = new HashMap<String, Object>();

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addAccessTokenLifetimeCustomProp(props);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // TODO - Is this a valid test?
    //@Test
    public void addAccessTokenLifetimeCustomProp_lifetimeEntryIsString() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = new HashMap<String, Object>();
            userApiResponseTokens.put(ClientConstants.EXPIRES_IN, "123");

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addAccessTokenLifetimeCustomProp(props);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addAccessTokenLifetimeCustomProp_negativeLifetime() throws Exception {
        try {
            long inputExpiresIn = -123L;
            Map<String, Object> userApiResponseTokens = new HashMap<String, Object>();
            userApiResponseTokens.put(ClientConstants.EXPIRES_IN, inputExpiresIn);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addAccessTokenLifetimeCustomProp(props);

            assertFalse("Property map should not have remained empty but did.", props.isEmpty());
            assertEquals("Expires in entry did not match original value.", inputExpiresIn, props.get(ClientConstants.EXPIRES_IN));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addAccessTokenLifetimeCustomProp_largeLifetime() throws Exception {
        try {
            long inputExpiresIn = 1234567890123456789L;
            Map<String, Object> userApiResponseTokens = new HashMap<String, Object>();
            userApiResponseTokens.put(ClientConstants.EXPIRES_IN, inputExpiresIn);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addAccessTokenLifetimeCustomProp(props);

            assertFalse("Property map should not have remained empty but did.", props.isEmpty());
            assertEquals("Expires in entry did not match original value.", inputExpiresIn, props.get(ClientConstants.EXPIRES_IN));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** addSocialMediaNameCustomProp ******************************************/

    @Test
    public void addSocialMediaNameCustomProp_emptyName() throws Exception {
        try {
            Hashtable<String, Object> props = new Hashtable<String, Object>();

            final String configId = "";
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(configId));
                }
            });

            subjectUtils.addSocialMediaNameCustomProp(props, config);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addSocialMediaNameCustomProp_whitespaceName() throws Exception {
        try {
            Hashtable<String, Object> props = new Hashtable<String, Object>();

            final String configId = "\n \r\t ";
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(configId));
                }
            });

            subjectUtils.addSocialMediaNameCustomProp(props, config);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addSocialMediaNameCustomProp_nonEmptyName() throws Exception {
        try {
            Hashtable<String, Object> props = new Hashtable<String, Object>();

            final String configId = " my\t config ID\r\t ";
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(configId));
                }
            });

            subjectUtils.addSocialMediaNameCustomProp(props, config);

            assertFalse("Property map should not have remained empty but did.", props.isEmpty());
            assertEquals("Social media name entry did not match original value.", configId, props.get(ClientConstants.SOCIAL_MEDIA));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** addScopeCustomProp ******************************************/

    @Test
    public void addScopeCustomProp_tokensIncludeScope_empty() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = new HashMap<String, Object>();
            userApiResponseTokens.put(ClientConstants.SCOPE, "");

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            final String configScope = clientConfigScope;
            mockery.checking(new Expectations() {
                {
                    one(config).getScope();
                    will(returnValue(configScope));
                }
            });

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addScopeCustomProp(props, config);

            assertFalse("Property map should not have remained empty but did.", props.isEmpty());
            assertEquals("Scope entry did not match original value.", configScope, props.get(ClientConstants.SCOPE));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addScopeCustomProp_tokensIncludeScope_whitespace() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = new HashMap<String, Object>();
            userApiResponseTokens.put(ClientConstants.SCOPE, "\n \r \t");

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            final String configScope = clientConfigScope;
            mockery.checking(new Expectations() {
                {
                    one(config).getScope();
                    will(returnValue(configScope));
                }
            });

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addScopeCustomProp(props, config);

            assertFalse("Property map should not have remained empty but did.", props.isEmpty());
            assertEquals("Scope entry did not match original value.", configScope, props.get(ClientConstants.SCOPE));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addScopeCustomProp_tokensIncludeScope_nonEmpty() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = new HashMap<String, Object>();
            userApiResponseTokens.put(ClientConstants.SCOPE, clientConfigScope);

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addScopeCustomProp(props, config);

            assertFalse("Property map should not have remained empty but did.", props.isEmpty());
            assertEquals("Scope entry did not match original value.", clientConfigScope, props.get(ClientConstants.SCOPE));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addScopeCustomProp_tokensMissingScope_configMissingScope() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = new HashMap<String, Object>();

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            mockery.checking(new Expectations() {
                {
                    one(config).getScope();
                    will(returnValue(null));
                }
            });

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addScopeCustomProp(props, config);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addScopeCustomProp_tokensMissingScope_configEmptyScope() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = new HashMap<String, Object>();

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            final String configScope = "";
            mockery.checking(new Expectations() {
                {
                    one(config).getScope();
                    will(returnValue(configScope));
                }
            });

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addScopeCustomProp(props, config);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addScopeCustomProp_tokensMissingScope_configWhitespaceScope() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = new HashMap<String, Object>();

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            final String configScope = "   \n\r";
            mockery.checking(new Expectations() {
                {
                    one(config).getScope();
                    will(returnValue(configScope));
                }
            });

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addScopeCustomProp(props, config);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addScopeCustomProp_tokensMissingScope_configNonEmptyScope() throws Exception {
        try {
            Map<String, Object> userApiResponseTokens = new HashMap<String, Object>();

            TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiResponseTokens, userApiResponseString);
            mockProtectedClassMembers(subjectUtils);

            final String configScope = "My config\t scope ";
            mockery.checking(new Expectations() {
                {
                    one(config).getScope();
                    will(returnValue(configScope));
                }
            });

            Hashtable<String, Object> props = new Hashtable<String, Object>();

            subjectUtils.addScopeCustomProp(props, config);

            assertFalse("Property map should not have remained empty but did.", props.isEmpty());
            assertEquals("Scope entry did not match original value.", configScope, props.get(ClientConstants.SCOPE));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** addEncryptedAccessTokenCustomProp ******************************************/

    @Test
    public void addEncryptedAccessTokenCustomProp_errorGettingToken() throws Exception {
        try {
            Hashtable<String, Object> props = new Hashtable<String, Object>();

            mockery.checking(new Expectations() {
                {
                    one(taiEncryptionUtils).getEncryptedAccessToken(config, ACCESS_TOKEN);
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });
            try {
                subjectUtils.addEncryptedAccessTokenCustomProp(props, config, ACCESS_TOKEN);
                fail("Should have thrown a SocialLoginException but did not. Properties were set to: " + props);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5438E_ERROR_GETTING_ENCRYPTED_ACCESS_TOKEN);
            }

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addEncryptedAccessTokenCustomProp_emptyEncryptedToken() throws Exception {
        try {
            Hashtable<String, Object> props = new Hashtable<String, Object>();

            final String encryptedToken = "";
            mockery.checking(new Expectations() {
                {
                    one(taiEncryptionUtils).getEncryptedAccessToken(config, ACCESS_TOKEN);
                    will(returnValue(encryptedToken));
                }
            });
            subjectUtils.addEncryptedAccessTokenCustomProp(props, config, ACCESS_TOKEN);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addEncryptedAccessTokenCustomProp_nonEmptyEncryptedToken() throws Exception {
        try {
            Hashtable<String, Object> props = new Hashtable<String, Object>();

            final String encryptedToken = "\nnon-empty \t token";
            mockery.checking(new Expectations() {
                {
                    one(taiEncryptionUtils).getEncryptedAccessToken(config, ACCESS_TOKEN);
                    will(returnValue(encryptedToken));
                }
            });
            subjectUtils.addEncryptedAccessTokenCustomProp(props, config, ACCESS_TOKEN);

            assertFalse("Property map should not have remained empty but did.", props.isEmpty());
            assertEquals("Encrypted access token entry did not match original value.", encryptedToken, props.get(ClientConstants.ENCRYPTED_TOKEN));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** addAccessTokenAliasCustomProp ******************************************/

    @Test
    public void addAccessTokenAliasCustomProp_emptyAccessToken() throws Exception {
        try {
            Hashtable<String, Object> props = new Hashtable<String, Object>();

            final String accessToken = "";

            subjectUtils.addAccessTokenAliasCustomProp(props, accessToken);

            assertTrue("Property map should have remained empty but is now: " + props, props.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addAccessTokenAliasCustomProp_nonEmptyAccessToken() throws Exception {
        try {
            Hashtable<String, Object> props = new Hashtable<String, Object>();

            final String accessToken = "my token";

            subjectUtils.addAccessTokenAliasCustomProp(props, accessToken);

            assertFalse("Property map should not have remained empty but did.", props.isEmpty());
            assertNotNull("Access token alias entry should not have been null but was. Props were: " + props, props.get(ClientConstants.ACCESS_TOKEN_ALIAS));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createCacheToken ******************************************/

    @Test
    public void createCacheToken_missingAccessToken_nullTokensMap() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(null, jwt, issuedJwt, null, null);
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });
            CacheToken token = subjectUtils.createCacheToken(config);

            String idToken = token.getIdToken();
            assertNull("ID token should not have been set but was [" + idToken + "].", idToken);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createCacheToken_nullTokensMap() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, null, null);
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });
            CacheToken token = subjectUtils.createCacheToken(config);

            String idToken = token.getIdToken();
            assertNull("ID token should not have been set but was [" + idToken + "].", idToken);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createCacheToken_missingIdToken() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, null);
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(userApiTokens).get(ClientConstants.ID_TOKEN);
                    will(returnValue(null));
                }
            });
            CacheToken token = subjectUtils.createCacheToken(config);

            String idToken = token.getIdToken();
            assertNull("ID token should not have been set but was [" + idToken + "].", idToken);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createCacheToken_tokensIncludeEmptyIdToken() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, null);
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(userApiTokens).get(ClientConstants.ID_TOKEN);
                    will(returnValue(""));
                }
            });
            CacheToken token = subjectUtils.createCacheToken(config);

            String idToken = token.getIdToken();
            assertNull("ID token should not have been set but was [" + idToken + "].", idToken);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createCacheToken_tokensIncludeIdToken() throws Exception {
        TAISubjectUtils subjectUtils = new MockTAISubjectUtils(ACCESS_TOKEN, jwt, issuedJwt, userApiTokens, null);
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(userApiTokens).get(ClientConstants.ID_TOKEN);
                    will(returnValue(ID_TOKEN));
                }
            });
            CacheToken token = subjectUtils.createCacheToken(config);

            assertEquals("ID token did not match expected value.", ID_TOKEN, token.getIdToken());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** Helper methods ******************************************/

    private Map<String, Object> createUserApiTokenMap(String accessToken, String refreshToken, String idToken, Long expiresIn) {
        Map<String, Object> tokens = new HashMap<String, Object>();
        if (accessToken != null) {
            tokens.put(ClientConstants.ACCESS_TOKEN, accessToken);
        }
        if (refreshToken != null) {
            tokens.put(ClientConstants.REFRESH_TOKEN, refreshToken);
        }
        if (idToken != null) {
            tokens.put(ClientConstants.ID_TOKEN, idToken);
        }
        if (expiresIn != null) {
            tokens.put(ClientConstants.EXPIRES_IN, expiresIn);
        }
        return tokens;
    }

    private void assertResultStatus(int expected, TAIResult result) {
        assertEquals("Result code did not match expected result.", expected, result.getStatus());
    }

    private void assertJwtTokenPrivateCredentialExists(Subject subject, JwtToken jwt) {
        Set<JwtToken> jwtPrivateCreds = subject.getPrivateCredentials(JwtToken.class);
        assertEquals("JWT private credential did not match the expected object.", jwt, jwtPrivateCreds.iterator().next());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Hashtable<String, Object> assertHashtablePrivateCredentialExists(Subject subject) {
        Set<Hashtable> hashtablePrivateCreds = subject.getPrivateCredentials(Hashtable.class);
        assertEquals("Did not find the expected number of hashtable private credentials: " + hashtablePrivateCreds, 1, hashtablePrivateCreds.size());
        return (Hashtable<String, Object>) (hashtablePrivateCreds.iterator().next());
    }

    private void assertIssuedJwtPrivateCredentialExists(Hashtable<String, Object> hashtableCreds, String expectedIssuedJwt) {
        Object issuedJwtCred = hashtableCreds.get(ClientConstants.ISSUED_JWT_TOKEN);
        assertEquals("Issued JWT credential did not match expected value.", expectedIssuedJwt, issuedJwtCred);
    }

    private void assertUserProfilePrivateCredentialExists(Subject subject, UserProfile userProfile) {
        Set<UserProfile> userProfilePrivateCreds = subject.getPrivateCredentials(UserProfile.class);
        assertEquals("Did not find the expected number of UserProfile private credentials: " + userProfilePrivateCreds, 1, userProfilePrivateCreds.size());
        assertEquals("UserProfile private credential did not match expected object.", userProfile, userProfilePrivateCreds.iterator().next());
    }

    private void assertUserProfile(UserProfile userProfile, String accessToken, String refreshToken, JwtToken idToken, long tokenLifetime, String socialMediaName, String scopes, String accessTokenAlias) {
        assertNotNull("A non-null user profile is expected to be created.", userProfile);
        assertEquals("The access token in the user profile did not match expected value.", accessToken, userProfile.getAccessToken());
        assertEquals("The refresh token in the user profile did not match expected value.", refreshToken, userProfile.getRefreshToken());
        assertEquals("The ID token in the user profile did not match expected value.", idToken, userProfile.getIdToken());
        assertEquals("The token lifetime in the user profile did not match expected value.", tokenLifetime, userProfile.getAccessTokenLifeTime());
        assertEquals("The social media name in the user profile did not match expected value.", socialMediaName, userProfile.getSocialMediaName());
        assertEquals("The scopes in the user profile did not match expected value.", scopes, userProfile.getScopes());
        assertEquals("The access token alias in the user profile did not match expected value.", accessTokenAlias, userProfile.getAccessTokenAlias());
    }

}
