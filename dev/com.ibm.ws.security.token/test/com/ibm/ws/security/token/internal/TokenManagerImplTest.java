/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

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

import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenCreationFailedException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.security.token.TokenService;
import com.ibm.wsspi.security.ltpa.Token;

import test.common.SharedOutputManager;

@SuppressWarnings("unchecked")
public class TokenManagerImplTest {
    private final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String TEST_TOKEN_TYPE = "TestTokenType";
    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<TokenService> testTokenServiceRef = mock.mock(ServiceReference.class);
    private final TokenService testTokenService = mock.mock(TokenService.class);
    private TokenManagerImpl tokenManager;
    private Map<String, Object> testTokenData;
    private final byte[] tokenBytes = new byte[] {};
    private final String[] removeAttrs = new String[] {};

    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    @Before
    public void setUp() {
        testTokenData = new HashMap<String, Object>();
        testTokenData.put("unique_id", "user:BasicRealm/user1");

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(TokenManagerImpl.CFG_KEY_SSO_TOKEN_TYPE, TEST_TOKEN_TYPE);

        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(TokenManagerImpl.KEY_TOKEN_SERVICE, testTokenServiceRef);
                will(returnValue(testTokenService));

                allowing(testTokenServiceRef).getProperty("tokenType");
                will(returnValue(TEST_TOKEN_TYPE));
                allowing(testTokenServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(testTokenServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
            }
        });

        tokenManager = new TokenManagerImpl();
        tokenManager.setTokenService(testTokenServiceRef);
        tokenManager.activate(cc, props);
    }

    @After
    public void tearDown() throws Exception {
        tokenManager.deactivate(cc);
        tokenManager.unsetTokenService(testTokenServiceRef);
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.TokenManager#createToken(java.lang.String, java.util.Map)}.
     */
    @Test
    public void createToken_noSuchService() {
        String tokenType = "unknownTokenType";
        String expectedExceptionMessage = "CWWKS4000E: A configuration exception has occurred. The requested TokenService instance of type " + tokenType
                                          + " could not be found.";
        try {
            tokenManager.createToken(tokenType, testTokenData);
            fail("Expected TokenCreationFailedException since there should be no service of requested type");
        } catch (TokenCreationFailedException e) {
            assertEquals("Expection did not contain expected message",
                         expectedExceptionMessage, e.getLocalizedMessage());
            assertTrue("Expected message was not logged",
                       outputMgr.checkForStandardErr(expectedExceptionMessage));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.TokenManager#createToken(java.lang.String, java.util.Map)}.
     */
    @Test
    public void createToken_TokenCreationFailedException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(testTokenService).createToken(testTokenData);
                will(throwException(new TokenCreationFailedException("Expected test exception")));
            }
        });
        try {
            tokenManager.createToken(TEST_TOKEN_TYPE, testTokenData);
            fail("createToken should have throw an TokenCreationFailedException as per the mock setting");
        } catch (TokenCreationFailedException e) {
            // Success, we expect this exception type
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.TokenManager#createToken(java.lang.String, java.util.Map)}.
     */
    @Test
    public void createToken() throws Exception {
        mock.checking(new Expectations() {
            {
                one(testTokenService).createToken(testTokenData);
            }
        });
        assertNotNull("Mock should return a non-null token",
                      tokenManager.createToken(TEST_TOKEN_TYPE, testTokenData));
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.TokenManager#createSSOToken(java.util.Map)}.
     */
    @Test
    public void createSSOToken_invalidSSOType() throws Exception {
        String tokenType = "unknownTokenType";
        String expectedExceptionMessage = "CWWKS4000E: A configuration exception has occurred. The requested TokenService instance of type " + tokenType
                                          + " could not be found.";
        try {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(TokenManagerImpl.CFG_KEY_SSO_TOKEN_TYPE, tokenType);
            tokenManager.modified(props);
            tokenManager.createSSOToken(testTokenData);
            fail("Expected TokenCreationFailedException since there should be no service of configured SSO type");
        } catch (TokenCreationFailedException e) {
            assertEquals("Expection did not contain expected message",
                         expectedExceptionMessage, e.getLocalizedMessage());
            assertTrue("Expected message was not logged",
                       outputMgr.checkForStandardErr(expectedExceptionMessage));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.TokenManager#createSSOToken(java.util.Map)}.
     */
    @Test
    public void createSSOToken_TokenCreationFailedException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(testTokenService).createToken(testTokenData);
                will(throwException(new TokenCreationFailedException("Expected test exception")));
            }
        });
        try {
            tokenManager.createToken(TEST_TOKEN_TYPE, testTokenData);
            fail("createToken should have throw an TokenCreationFailedException as per the mock setting");
        } catch (TokenCreationFailedException e) {
            // Success, we expect this exception type
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.TokenManager#createSSOToken(java.util.Map)}.
     */
    @Test
    public void createSSOToken() throws Exception {
        mock.checking(new Expectations() {
            {
                one(testTokenService).createToken(testTokenData);
            }
        });
        assertNotNull("Mock should return a non-null token",
                      tokenManager.createSSOToken(testTokenData));
    }

    @Test
    public void createSSOToken_fromLtpa2Token() throws Exception {
        Token ssoLtpaToken = mock.mock(Token.class);
        assertNotNull("There must be an SSO token",
                      tokenManager.createSSOToken(ssoLtpaToken));
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.TokenManager#recreateTokenFromBytes(byte[])}.
     */
    @Test
    public void recreateTokenFromBytes_noSuchService() throws Exception {
        String tokenType = "unknownTokenType";
        String expectedCauseMessage = "CWWKS4000E: A configuration exception has occurred. The requested TokenService instance of type " + tokenType
                                      + " could not be found.";
        try {
            tokenManager.recreateTokenFromBytes(tokenType, tokenBytes);
            fail("Expected TokenCreationFailedException since there should be no service of requested type");
        } catch (InvalidTokenException e) {
            assertTrue("InvalidTokenException did not contain expected message",
                       e.getMessage().startsWith("CWWKS4001I: The security token cannot be validated."));
            Throwable cause = e.getCause();
            assertEquals("Expection did not contain expected message",
                         expectedCauseMessage, cause.getLocalizedMessage());
            assertTrue("Expected message was not logged", outputMgr.checkForStandardErr(expectedCauseMessage));
        }

    }

    /**
     * Test method for {@link com.ibm.ws.security.token.TokenManager#recreateTokenFromBytes(byte[])}.
     */
    @Test
    public void recreateTokenFromBytes_TokenExpiredException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(testTokenService).recreateTokenFromBytes(tokenBytes, removeAttrs);
                will(throwException(new TokenExpiredException("Expected test exception")));
                one(testTokenService).recreateTokenFromBytes(tokenBytes);
                will(throwException(new TokenExpiredException("Expected test exception")));
            }
        });

        try {
            tokenManager.recreateTokenFromBytes(tokenBytes);
            fail("recreateTokenFromBytes should have throw an TokenExpiredException as per the mock setting");
            tokenManager.recreateTokenFromBytes(tokenBytes, removeAttrs);
            fail("recreateTokenFromBytes should have throw an TokenExpiredException as per the mock setting");
        } catch (TokenExpiredException e) {
            // Success, we expect this exception type
        }

    }

    /**
     * Test method for {@link com.ibm.ws.security.token.TokenManager#recreateTokenFromBytes(byte[])}.
     */
    @Test
    public void recreateTokenFromBytes_InvalidTokenException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(testTokenService).recreateTokenFromBytes(tokenBytes, removeAttrs);
                will(throwException(new InvalidTokenException("Expected test exception")));
                one(testTokenService).recreateTokenFromBytes(tokenBytes);
                will(throwException(new InvalidTokenException("Expected test exception")));
            }
        });

        try {
            tokenManager.recreateTokenFromBytes(tokenBytes);
            fail("recreateTokenFromBytes should have throw an InvalidTokenException as per the mock setting");
            tokenManager.recreateTokenFromBytes(tokenBytes, removeAttrs);
            fail("recreateTokenFromBytes should have throw an InvalidTokenException as per the mock setting");
        } catch (InvalidTokenException e) {
            // Success, we expect this exception type
            String msg = "CWWKS4001I: The security token cannot be validated.";
            assertTrue("Unable to find token expiration message",
                       outputMgr.checkForMessages(msg));
            msg = "1. The security token was generated on another server using different keys.";
            assertTrue("Unable to find token expiration message part 1",
                       outputMgr.checkForMessages(msg));
            msg = "2. The token configuration or the security keys of the token service which created the token has been changed.";
            assertTrue("Unable to find token expiration message part 2",
                       outputMgr.checkForMessages(msg));
            msg = "3. The token service which created the token is no longer available.";
            assertTrue("Unable to find token expiration message part 3",
                       outputMgr.checkForMessages(msg));
        }

    }

    /**
     * Test method for {@link com.ibm.ws.security.token.TokenManager#recreateTokenFromBytes(byte[])}.
     */
    @Test
    public void recreateTokenFromBytes() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(testTokenService).recreateTokenFromBytes(tokenBytes);
                allowing(testTokenService).recreateTokenFromBytes(tokenBytes, removeAttrs);
            }
        });

        assertNotNull("Mock should return a non-null token",
                      tokenManager.recreateTokenFromBytes(tokenBytes));
        assertNotNull("Mock should return a non-null token",
                      tokenManager.recreateTokenFromBytes(tokenBytes, removeAttrs));

    }

    /**
     * Test method for {@link com.ibm.ws.security.token.TokenManager#recreateTokenFromBytes(java.lang.String, byte[])}.
     */
    @Test
    public void recreateTokenFromBytesForType_noSuchService() throws Exception {
        String tokenType = "unknownTokenType";
        String expectedCauseMessage = "CWWKS4000E: A configuration exception has occurred. The requested TokenService instance of type " + tokenType
                                      + " could not be found.";
        try {
            tokenManager.recreateTokenFromBytes(tokenType, tokenBytes);
            fail("Expected TokenCreationFailedException since there should be no service of requested type");
        } catch (InvalidTokenException e) {
            assertTrue("InvalidTokenException did not contain expected message",
                       e.getMessage().startsWith("CWWKS4001I: The security token cannot be validated."));
            Throwable cause = e.getCause();
            assertEquals("Expection did not contain expected message",
                         expectedCauseMessage, cause.getLocalizedMessage());
            assertTrue("Expected message was not logged", outputMgr.checkForStandardErr(expectedCauseMessage));
        }

    }

    /**
     * Test method for {@link com.ibm.ws.security.token.TokenManager#recreateTokenFromBytes(java.lang.String, byte[])}.
     */
    @Test
    public void recreateTokenFromBytesForType_TokenExpiredException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(testTokenService).recreateTokenFromBytes(tokenBytes, removeAttrs);
                one(testTokenService).recreateTokenFromBytes(tokenBytes);
                will(throwException(new TokenExpiredException("Expected test exception")));
            }
        });

        try {
            tokenManager.recreateTokenFromBytes(TEST_TOKEN_TYPE, tokenBytes);
            fail("recreateTokenFromBytes should have throw an TokenExpiredException as per the mock setting");
        } catch (TokenExpiredException e) {
            // Success, we expect this exception type
        }

    }

    /**
     * Test method for {@link com.ibm.ws.security.token.TokenManager#recreateTokenFromBytes(java.lang.String, byte[])}.
     */
    @Test
    public void recreateTokenFromBytesForType_InvalidTokenException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(testTokenService).recreateTokenFromBytes(tokenBytes, removeAttrs);
                one(testTokenService).recreateTokenFromBytes(tokenBytes);
                will(throwException(new InvalidTokenException("Expected test exception")));
            }
        });

        try {
            tokenManager.recreateTokenFromBytes(TEST_TOKEN_TYPE, tokenBytes);
            fail("recreateTokenFromBytes should have throw an InvalidTokenException as per the mock setting");
        } catch (InvalidTokenException e) {
            // Success, we expect this exception type
        }

    }

    /**
     * Test method for {@link com.ibm.ws.security.token.TokenManager#recreateTokenFromBytes(java.lang.String, byte[])}.
     */
    @Test
    public void recreateTokenFromBytesForType() throws Exception {
        mock.checking(new Expectations() {
            {
                one(testTokenService).recreateTokenFromBytes(tokenBytes, removeAttrs);
                one(testTokenService).recreateTokenFromBytes(tokenBytes);
            }
        });

        assertNotNull("Mock should return a non-null token",
                      tokenManager.recreateTokenFromBytes(TEST_TOKEN_TYPE, tokenBytes));
    }

}
