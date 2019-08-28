/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidResponseTypeException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.responsetype.OAuth20ResponseTypeHandler;
import com.ibm.oauth.core.internal.oauth20.responsetype.impl.OAuth20ResponseTypeHandlerCodeImpl;
import com.ibm.ws.security.oauth20.util.OIDCConstants;

public class OIDCResponseTypeHandlerFactoryImplTest {
    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    final OAuthComponentConfiguration oacc = mock.mock(OAuthComponentConfiguration.class, "oacc");
    final OAuth20ConfigProvider oa20cp = mock.mock(OAuth20ConfigProvider.class, "oa20cp");

    public final String GRANT_TYPES_CONFIG_PROP_ERROR_MSG = "Error with configuration property: oauth20.grant.types.allowed value: ";

    public final String INVALID_RESPONSE_TYPE_MSG = "CWOAU0027E: The response_type parameter was invalid: ";

    public final String MUTUALLY_EXCLUSIVE_RESPONSE_TYPE_MSG_START = "CWOAU0057E: The response_type parameter [";

    public final String MUTUALLY_EXCLUSIVE_RESPONSE_TYPE_MSG_END = "] in the OAuth or OpenID Connect request cannot include both [" + OAuth20Constants.RESPONSE_TYPE_CODE + "]" +
                                                                   " and [" + OAuth20Constants.RESPONSE_TYPE_TOKEN + " " + OIDCConstants.RESPONSE_TYPE_ID_TOKEN
                                                                   + "] as response types.";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void testConstructor() {
        final String methodName = "testConstructor";
        try {
            OIDCResponseTypeHandlerFactoryImpl oidcResponseTypeHandlerFactoryImpl = new OIDCResponseTypeHandlerFactoryImpl();
            oidcResponseTypeHandlerFactoryImpl.init(oacc); // this is supposed to set the configuration field but nothing else
            assertNotNull("Cannot instantiate an oidcResponseTypehandlerCodeImpl", oidcResponseTypeHandlerFactoryImpl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerBadResponseType() {
        final String methodName = "testGetHandlerBadResponseType";
        try {
            OIDCResponseTypeHandlerFactoryImpl oidcResponseTypeHandlerFactoryImpl = new OIDCResponseTypeHandlerFactoryImpl();
            oidcResponseTypeHandlerFactoryImpl.init(oacc); // this is supposed to set the configuration field but nothing else
            assertNotNull("Cannot instantiate an oidcResponseTypehandlerCodeImpl", oidcResponseTypeHandlerFactoryImpl);
            oidcResponseTypeHandlerFactoryImpl.getHandler("BadResponseType", oa20cp);
            fail("Expected to get an OAuth20InvalidResponseTypeException but did not");
        } catch (OAuth20InvalidResponseTypeException e) {
            // This is what we expected
            assertEquals(INVALID_RESPONSE_TYPE_MSG + "BadResponseType", e.getMessage());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerBadResponseTypeByMultipleTokens() {
        final String methodName = "testGetHandlerBadResponseTypeByMultipleTokens";
        final String responseType = OAuth20Constants.RESPONSE_TYPE_CODE + " " + OAuth20Constants.RESPONSE_TYPE_TOKEN;
        try {
            OIDCResponseTypeHandlerFactoryImpl oidcResponseTypeHandlerFactoryImpl = new OIDCResponseTypeHandlerFactoryImpl();
            oidcResponseTypeHandlerFactoryImpl.init(oacc); // this is supposed to set the configuration field but nothing else
            assertNotNull("Cannot instantiate an oidcResponseTypehandlerCodeImpl", oidcResponseTypeHandlerFactoryImpl);
            mock.checking(new Expectations() {
                {
                    one(oa20cp).isGrantTypeAllowed(OAuth20Constants.GRANT_TYPE_IMPLICIT);
                    will(returnValue(false));
                }
            });
            oidcResponseTypeHandlerFactoryImpl.getHandler(responseType, oa20cp);
            fail("Expected to get an OAuthConfigurationException but did not");
        } catch (OAuthConfigurationException e) {
            // This is what we expected
            assertEquals(GRANT_TYPES_CONFIG_PROP_ERROR_MSG + responseType, e.getMessage());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerResponseTypeNull() {
        final String methodName = "testGetHandlerResponseTypeNull";
        try {
            OIDCResponseTypeHandlerFactoryImpl oidcResponseTypeHandlerFactoryImpl = new OIDCResponseTypeHandlerFactoryImpl();
            oidcResponseTypeHandlerFactoryImpl.init(oacc); // this is supposed to set the configuration field but nothing else
            assertNotNull("Cannot instantiate an oidcResponseTypehandlerCodeImpl", oidcResponseTypeHandlerFactoryImpl);
            oidcResponseTypeHandlerFactoryImpl.getHandler((String) null, oa20cp);
            fail("Expected to get an OAuth20InvalidResponseTypeException but did not");
        } catch (OAuth20InvalidResponseTypeException e) {
            // This is what we expected
            assertEquals(INVALID_RESPONSE_TYPE_MSG + ((String) null), e.getMessage());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerResponseTypeNotAllowed() {
        final String methodName = "testGetHandlerResponseTypeNotAllowed";
        try {
            OIDCResponseTypeHandlerFactoryImpl oidcResponseTypeHandlerFactoryImpl = new OIDCResponseTypeHandlerFactoryImpl();
            oidcResponseTypeHandlerFactoryImpl.init(oacc); // this is supposed to set the configuration field but nothing else
            assertNotNull("Cannot instantiate an oidcResponseTypehandlerCodeImpl", oidcResponseTypeHandlerFactoryImpl);
            final String responseType = OAuth20Constants.RESPONSE_TYPE_CODE;
            mock.checking(new Expectations() {
                {
                    one(oa20cp).isGrantTypeAllowed(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
                    will(returnValue(false));
                }
            });
            oidcResponseTypeHandlerFactoryImpl.getHandler(responseType, oa20cp);
            fail("Expected to get an OAuthConfigurationException but did not");
        } catch (OAuthConfigurationException e) {
            // This is what we expected
            assertEquals(GRANT_TYPES_CONFIG_PROP_ERROR_MSG + OAuth20Constants.RESPONSE_TYPE_CODE, e.getMessage());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerResponseTypeCode() {
        final String methodName = "testGetHandlerResponseTypeCode";
        try {
            OIDCResponseTypeHandlerFactoryImpl oidcResponseTypeHandlerFactoryImpl = new OIDCResponseTypeHandlerFactoryImpl();
            oidcResponseTypeHandlerFactoryImpl.init(oacc); // this is supposed to set the configuration field but nothing else
            assertNotNull("Can not instantiate an oidcResponseTypehandlerCodeImpl", oidcResponseTypeHandlerFactoryImpl);
            final String responseType1 = OAuth20Constants.RESPONSE_TYPE_CODE;
            mock.checking(new Expectations() {
                {
                    one(oa20cp).isGrantTypeAllowed(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
                    will(returnValue(true));
                }
            });
            OAuth20ResponseTypeHandler oa20gth = oidcResponseTypeHandlerFactoryImpl.getHandler(responseType1, oa20cp);
            assertTrue("Expected to get OAuth20ResponseTypeHandlerCodeImpl but got " + oa20gth.getClass().getName(),
                       oa20gth instanceof OAuth20ResponseTypeHandlerCodeImpl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerResponseTypeCodeBad() {
        final String methodName = "testGetHandlerResponseTypeCodeBad";
        try {
            OIDCResponseTypeHandlerFactoryImpl oidcResponseTypeHandlerFactoryImpl = new OIDCResponseTypeHandlerFactoryImpl();
            oidcResponseTypeHandlerFactoryImpl.init(oacc); // this is supposed to set the configuration field but nothing else
            assertNotNull("Cannot instantiate an oidcResponseTypehandlerCodeImpl", oidcResponseTypeHandlerFactoryImpl);
            final String responseType1 = OAuth20Constants.RESPONSE_TYPE_CODE + " badtoken";
            OAuth20ResponseTypeHandler oa20gth = oidcResponseTypeHandlerFactoryImpl.getHandler(responseType1, oa20cp);
            fail("Expected NOT to get OAuth20ResponseTypeHandlerCodeImpl but got " + oa20gth.getClass().getName());
        } catch (OAuth20InvalidResponseTypeException e) {
            // This is what we expected
            assertEquals(INVALID_RESPONSE_TYPE_MSG + OAuth20Constants.RESPONSE_TYPE_CODE + " badtoken", e.getMessage());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerResponseTypeToken() {
        final String methodName = "testGetHandlerResponseTypeToken";
        try {
            OIDCResponseTypeHandlerFactoryImpl oidcResponseTypeHandlerFactoryImpl = new OIDCResponseTypeHandlerFactoryImpl();
            oidcResponseTypeHandlerFactoryImpl.init(oacc); // this is supposed to set the configuration field but nothing else
            assertNotNull("Cannot instantiate an oidcResponseTypehandlerCodeImpl", oidcResponseTypeHandlerFactoryImpl);
            final String responseType2 = OAuth20Constants.RESPONSE_TYPE_TOKEN;
            mock.checking(new Expectations() {
                {
                    one(oa20cp).isGrantTypeAllowed(OAuth20Constants.GRANT_TYPE_IMPLICIT);
                    will(returnValue(true));
                }
            });
            OAuth20ResponseTypeHandler oa20gth = oidcResponseTypeHandlerFactoryImpl.getHandler(responseType2, oa20cp);
            assertTrue("Expected to get OIDCResponseTypeHandlerCodeImpl but got " + oa20gth.getClass().getName(),
                       oa20gth instanceof OIDCResponseTypeHandlerImplicitImpl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerResponseTypeIdToken() {
        final String methodName = "testGetHandlerResponseTypeIdToken";
        try {
            OIDCResponseTypeHandlerFactoryImpl oidcResponseTypeHandlerFactoryImpl = new OIDCResponseTypeHandlerFactoryImpl();
            oidcResponseTypeHandlerFactoryImpl.init(oacc); // this is supposed to set the configuration field but nothing else
            assertNotNull("Cannot instantiate an oidcResponseTypehandlerCodeImpl", oidcResponseTypeHandlerFactoryImpl);
            final String responseType2 = OIDCConstants.RESPONSE_TYPE_ID_TOKEN + " " + OAuth20Constants.RESPONSE_TYPE_TOKEN;
            mock.checking(new Expectations() {
                {
                    allowing(oa20cp).isGrantTypeAllowed(OAuth20Constants.GRANT_TYPE_IMPLICIT);
                    will(returnValue(true));
                }
            });
            OAuth20ResponseTypeHandler oa20gth = oidcResponseTypeHandlerFactoryImpl.getHandler(responseType2, oa20cp);
            assertTrue("Expected to get OIDCResponseTypeHandlerCodeImpl but got " + oa20gth.getClass().getName(),
                       oa20gth instanceof OIDCResponseTypeHandlerImplicitImpl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerMutuallyExclusiveCodeAndImplicitResponseTypesSpecifiedBothAllowed() {
        final String methodName = "testGetHandlerMutuallyExclusiveCodeAndImplicitResponseTypesSpecifiedBothAllowed";
        final String responseType = OAuth20Constants.RESPONSE_TYPE_CODE + " " + OAuth20Constants.RESPONSE_TYPE_TOKEN;
        try {
            OIDCResponseTypeHandlerFactoryImpl oidcResponseTypeHandlerFactoryImpl = new OIDCResponseTypeHandlerFactoryImpl();
            oidcResponseTypeHandlerFactoryImpl.init(oacc); // this is supposed to set the configuration field but nothing else
            assertNotNull("Cannot instantiate an oidcResponseTypehandlerCodeImpl", oidcResponseTypeHandlerFactoryImpl);
            mock.checking(new Expectations() {
                {
                    allowing(oa20cp).isGrantTypeAllowed(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
                    will(returnValue(true));
                    allowing(oa20cp).isGrantTypeAllowed(OAuth20Constants.GRANT_TYPE_IMPLICIT);
                    will(returnValue(true));
                }
            });
            oidcResponseTypeHandlerFactoryImpl.getHandler(responseType, oa20cp);
            fail("Expected to get an OAuth20InvalidResponseTypeException but did not");
        } catch (OAuth20InvalidResponseTypeException e) {
            // This is what we expected
            assertEquals(MUTUALLY_EXCLUSIVE_RESPONSE_TYPE_MSG_START + responseType + MUTUALLY_EXCLUSIVE_RESPONSE_TYPE_MSG_END, e.getMessage());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerMutuallyExclusiveCodeAndIdTokenResponseTypesSpecifiedBothAllowed() {
        final String methodName = "testGetHandlerMutuallyExclusiveCodeAndIdTokenResponseTypesSpecifiedBothAllowed";
        final String responseType = OAuth20Constants.RESPONSE_TYPE_CODE + " " + OIDCConstants.RESPONSE_TYPE_ID_TOKEN;
        try {
            OIDCResponseTypeHandlerFactoryImpl oidcResponseTypeHandlerFactoryImpl = new OIDCResponseTypeHandlerFactoryImpl();
            oidcResponseTypeHandlerFactoryImpl.init(oacc); // this is supposed to set the configuration field but nothing else
            assertNotNull("Cannot instantiate an oidcResponseTypehandlerCodeImpl", oidcResponseTypeHandlerFactoryImpl);
            mock.checking(new Expectations() {
                {
                    allowing(oa20cp).isGrantTypeAllowed(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
                    will(returnValue(true));
                    allowing(oa20cp).isGrantTypeAllowed(OAuth20Constants.GRANT_TYPE_IMPLICIT);
                    will(returnValue(true));
                }
            });
            oidcResponseTypeHandlerFactoryImpl.getHandler(responseType, oa20cp);
            fail("Expected to get an OAuth20InvalidResponseTypeException but did not");
        } catch (OAuth20InvalidResponseTypeException e) {
            // This is what we expected
            assertEquals(MUTUALLY_EXCLUSIVE_RESPONSE_TYPE_MSG_START + responseType + MUTUALLY_EXCLUSIVE_RESPONSE_TYPE_MSG_END, e.getMessage());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
