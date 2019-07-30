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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

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
import com.ibm.oauth.core.internal.statistics.OAuthStatHelper;
import com.ibm.oauth.core.internal.statistics.OAuthStatisticsImpl;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;

/**
 * Unit test case to verify behavior integrity of OidcOAuth20ClientProviderWrapperTest
 */
public class OidcOAuth20ClientProviderWrapperTest extends AbstractOidcRegistrationBaseTest {
    private static SharedOutputManager outputMgr;
    private static final String ERROR_VALUE = "ERROR";

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final OidcOAuth20ClientProvider oidcOauthClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
    private final OAuthStatisticsImpl oAuthStats = mock.mock(OAuthStatisticsImpl.class);
    private final OAuthComponentConfiguration oAuthComponentConfig = mock.mock(OAuthComponentConfiguration.class);
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

    @Test
    public void testInitialize() {
        final String methodName = "testInitialize";
        try {
            mock.checking(new Expectations() {
                {
                    oneOf(oidcOauthClientProvider).initialize();
                }
            });

            OidcOAuth20ClientProviderWrapper oidcOAuth20ClientProviderWrapper = new OidcOAuth20ClientProviderWrapper(oidcOauthClientProvider, oAuthStats);
            oidcOAuth20ClientProviderWrapper.initialize();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testInitializeNegative() {
        final String methodName = "testInitializeNegative";
        try {
            mock.checking(new Expectations() {
                {
                    never(oidcOauthClientProvider).initialize();
                }
            });

            OidcOAuth20ClientProviderWrapper oidcOAuth20ClientProviderWrapper = new OidcOAuth20ClientProviderWrapper(null, oAuthStats);
            oidcOAuth20ClientProviderWrapper.initialize();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testInitializeWithComponent() {
        final String methodName = "testInitializeWithComponent";
        try {
            mock.checking(new Expectations() {
                {
                    oneOf(oidcOauthClientProvider).init(oAuthComponentConfig);
                }
            });

            OidcOAuth20ClientProviderWrapper oidcOAuth20ClientProviderWrapper = new OidcOAuth20ClientProviderWrapper(oidcOauthClientProvider, oAuthStats);
            oidcOAuth20ClientProviderWrapper.init(oAuthComponentConfig);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testExists() {
        final String methodName = "testExists";
        try {
            mock.checking(new Expectations() {
                {
                    oneOf(oAuthStats).addMeasurement(with(any(OAuthStatHelper.class)));
                    oneOf(oidcOauthClientProvider).exists(CLIENT_ID);
                    will(returnValue(true));
                }
            });

            OidcOAuth20ClientProviderWrapper oidcOAuth20ClientProviderWrapper = new OidcOAuth20ClientProviderWrapper(oidcOauthClientProvider, oAuthStats);
            assertTrue(oidcOAuth20ClientProviderWrapper.exists(CLIENT_ID));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testExistsException() {
        final String methodName = "testExistsException";
        try {
            mock.checking(new Expectations() {
                {
                    oneOf(oAuthStats).addMeasurement(with(any(OAuthStatHelper.class)));
                    oneOf(oidcOauthClientProvider).exists(ERROR_VALUE);
                    will(throwException(new OidcServerException("", "", 500)));
                }
            });

            OidcOAuth20ClientProviderWrapper oidcOAuth20ClientProviderWrapper = new OidcOAuth20ClientProviderWrapper(oidcOauthClientProvider, oAuthStats);
            assertFalse(oidcOAuth20ClientProviderWrapper.exists(ERROR_VALUE));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGet() {
        final String methodName = "testGet";
        final OidcBaseClient sampleOidcBaseClient = getSampleOidcBaseClient();
        try {
            mock.checking(new Expectations() {
                {
                    oneOf(oAuthStats).addMeasurement(with(any(OAuthStatHelper.class)));
                    oneOf(oidcOauthClientProvider).get(CLIENT_ID);
                    will(returnValue(sampleOidcBaseClient));
                }
            });

            OidcOAuth20ClientProviderWrapper oidcOAuth20ClientProviderWrapper = new OidcOAuth20ClientProviderWrapper(oidcOauthClientProvider, oAuthStats);
            assertEquals(sampleOidcBaseClient, oidcOAuth20ClientProviderWrapper.get(CLIENT_ID));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetException() {
        final String methodName = "testGetException";
        try {
            mock.checking(new Expectations() {
                {
                    oneOf(oAuthStats).addMeasurement(with(any(OAuthStatHelper.class)));
                    oneOf(oidcOauthClientProvider).get(ERROR_VALUE);
                    will(throwException(new OidcServerException("", "", 500)));
                }
            });

            OidcOAuth20ClientProviderWrapper oidcOAuth20ClientProviderWrapper = new OidcOAuth20ClientProviderWrapper(oidcOauthClientProvider, oAuthStats);
            assertNull(oidcOAuth20ClientProviderWrapper.get(ERROR_VALUE));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testValidateClient() {
        final String methodName = "testValidateClient";
        try {
            mock.checking(new Expectations() {
                {
                    oneOf(oAuthStats).addMeasurement(with(any(OAuthStatHelper.class)));
                    oneOf(oidcOauthClientProvider).validateClient(CLIENT_ID, CLIENT_SECRET);
                    will(returnValue(true));
                }
            });

            OidcOAuth20ClientProviderWrapper oidcOAuth20ClientProviderWrapper = new OidcOAuth20ClientProviderWrapper(oidcOauthClientProvider, oAuthStats);
            assertTrue(oidcOAuth20ClientProviderWrapper.validateClient(CLIENT_ID, CLIENT_SECRET));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testValidateClientException() {
        final String methodName = "testValidateClientException";
        try {
            mock.checking(new Expectations() {
                {
                    oneOf(oAuthStats).addMeasurement(with(any(OAuthStatHelper.class)));
                    oneOf(oidcOauthClientProvider).validateClient(ERROR_VALUE, ERROR_VALUE);
                    will(throwException(new OidcServerException("", "", 500)));

                }
            });

            OidcOAuth20ClientProviderWrapper oidcOAuth20ClientProviderWrapper = new OidcOAuth20ClientProviderWrapper(oidcOauthClientProvider, oAuthStats);
            assertFalse(oidcOAuth20ClientProviderWrapper.validateClient(ERROR_VALUE, ERROR_VALUE));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testDelete() {
        final String methodName = "testDelete";
        try {
            mock.checking(new Expectations() {
                {
                    oneOf(oidcOauthClientProvider).delete(CLIENT_ID);
                    will(returnValue(true));
                }
            });

            OidcOAuth20ClientProviderWrapper oidcOAuth20ClientProviderWrapper = new OidcOAuth20ClientProviderWrapper(oidcOauthClientProvider, oAuthStats);
            assertTrue(oidcOAuth20ClientProviderWrapper.delete(CLIENT_ID));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetAllWithNull() {
        final String methodName = "testGetAllWithNull";
        final List<OidcBaseClient> clients = new ArrayList<OidcBaseClient>();
        try {
            mock.checking(new Expectations() {
                {
                    oneOf(oidcOauthClientProvider).getAll(null);
                    will(returnValue(clients));
                }
            });

            OidcOAuth20ClientProviderWrapper oidcOAuth20ClientProviderWrapper = new OidcOAuth20ClientProviderWrapper(oidcOauthClientProvider, oAuthStats);
            assertEquals(oidcOAuth20ClientProviderWrapper.getAll(), clients);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetAllWithRequest() {
        final String methodName = "testGetAllWithRequest";
        final List<OidcBaseClient> clients = new ArrayList<OidcBaseClient>();
        try {
            mock.checking(new Expectations() {
                {
                    oneOf(oidcOauthClientProvider).getAll(with(any(HttpServletRequest.class)));
                    will(returnValue(clients));
                }
            });

            OidcOAuth20ClientProviderWrapper oidcOAuth20ClientProviderWrapper = new OidcOAuth20ClientProviderWrapper(oidcOauthClientProvider, oAuthStats);
            assertEquals(oidcOAuth20ClientProviderWrapper.getAll(request), clients);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPut() {
        final String methodName = "testPut";
        final OidcBaseClient client = getSampleOidcBaseClient();
        try {
            mock.checking(new Expectations() {
                {
                    oneOf(oidcOauthClientProvider).put(client);
                    will(returnValue(client));
                }
            });

            OidcOAuth20ClientProviderWrapper oidcOAuth20ClientProviderWrapper = new OidcOAuth20ClientProviderWrapper(oidcOauthClientProvider, oAuthStats);
            assertEquals(oidcOAuth20ClientProviderWrapper.put(client), client);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testUpdate() {
        final String methodName = "testUpdate";
        final OidcBaseClient client = getSampleOidcBaseClient();
        try {
            mock.checking(new Expectations() {
                {
                    oneOf(oidcOauthClientProvider).update(client);
                    will(returnValue(client));
                }
            });

            OidcOAuth20ClientProviderWrapper oidcOAuth20ClientProviderWrapper = new OidcOAuth20ClientProviderWrapper(oidcOauthClientProvider, oAuthStats);
            assertEquals(oidcOAuth20ClientProviderWrapper.update(client), client);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
