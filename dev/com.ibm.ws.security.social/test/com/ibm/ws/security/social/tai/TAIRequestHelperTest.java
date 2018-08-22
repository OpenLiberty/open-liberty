/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.SocialTaiRequest;
import com.ibm.ws.security.social.test.CommonTestClass;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;

import test.common.SharedOutputManager;

public class TAIRequestHelperTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    public interface MockInterface {
        public boolean requestShouldBeHandledByTAI();

        public boolean isJmxConnectorRequest();

        public SocialTaiRequest setSocialTaiRequestConfigInfo();

        public SocialTaiRequest setGenericAndFilteredConfigTaiRequestInfo();

        public SocialTaiRequest setGenericAndFilteredConfigTaiRequestInfoFromConfigServices();

        public SocialTaiRequest setSpecificConfigTaiRequestInfo();

        public SocialLoginConfig getConfigAssociatedWithRequestAndId();

        public Iterator<SocialLoginConfig> getConfigServices();

        public SocialLoginConfig getConfig();

        public boolean configAuthFilterMatchesRequest();

    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    final AuthenticationFilter authFilter = mockery.mock(AuthenticationFilter.class);
    @SuppressWarnings("unchecked")
    final ServiceReference<SocialLoginConfig> socialLoginConfigServiceReference = mockery.mock(ServiceReference.class, "socialLoginConfigServiceReference");
    final SocialLoginConfig config = mockery.mock(SocialLoginConfig.class, "config");
    final SocialTaiRequest socialTaiReq = mockery.mock(SocialTaiRequest.class);
    final SocialWebUtils socialWebUtils = mockery.mock(SocialWebUtils.class);

    final static int successfulTAIStatus = HttpServletResponse.SC_OK;
    final static int unauthorizedTAIStatus = HttpServletResponse.SC_UNAUTHORIZED;
    final static String successfulTAIPrinciple = "myPrinciple";

    /**
     * Created for ease of mocking static SocialLoginTAI calls.
     */
    private class MockTAIRequestHelper extends TAIRequestHelper {
        @Override
        Iterator<SocialLoginConfig> getConfigServices() {
            return mockInterface.getConfigServices();
        }

        @Override
        SocialLoginConfig getConfig(String configId) {
            return mockInterface.getConfig();
        }
    }

    TAIRequestHelper helper = new MockTAIRequestHelper();

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());

        helper = new MockTAIRequestHelper();
        helper.webUtils = socialWebUtils;
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

    /****************************************** requestShouldBeHandledByTAI ******************************************/

    @Test
    public void requestShouldBeHandledByTAI_jmxConnectorRequest() {
        try {
            helper = new TAIRequestHelper() {
                @Override
                boolean isJmxConnectorRequest(HttpServletRequest request) {
                    return mockInterface.isJmxConnectorRequest();
                }
            };
            helper.webUtils = socialWebUtils;

            isJmxConnectorRequest(true);

            boolean result = helper.requestShouldBeHandledByTAI(request, null);
            assertFalse("JMX connector requests should not be handled by this TAI.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void requestShouldBeHandledByTAI_noLoginHint_missingConfig() {
        try {
            helper = new MockTAIRequestHelper() {
                @Override
                boolean isJmxConnectorRequest(HttpServletRequest request) {
                    return mockInterface.isJmxConnectorRequest();
                }
            };
            helper.webUtils = socialWebUtils;

            isJmxConnectorRequest(false);
            getLoginHint(null);
            createSocialTaiRequestAndSetRequestAttributeExpectations();
            getConfigServices(createConfigSet().iterator());

            boolean result = helper.requestShouldBeHandledByTAI(request, null);
            assertFalse("Result should not have been confirmed as target interceptor.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void requestShouldBeHandledByTAI_noLoginHint_withConfig_nullAuthFilter() {
        try {
            helper = new MockTAIRequestHelper() {
                @Override
                boolean isJmxConnectorRequest(HttpServletRequest request) {
                    return mockInterface.isJmxConnectorRequest();
                }
            };
            helper.webUtils = socialWebUtils;

            isJmxConnectorRequest(false);
            getLoginHint(null);
            createSocialTaiRequestAndSetRequestAttributeExpectations();
            getConfigServices(createConfigSet(config).iterator());
            mockery.checking(new Expectations() {
                {
                    one(config).getAuthFilter();
                    will(returnValue(null));
                }
            });

            boolean result = helper.requestShouldBeHandledByTAI(request, null);
            assertTrue("Result should have been confirmed as target interceptor since the config should be considered a generic matching config.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void requestShouldBeHandledByTAI_withLoginHint_authFilterNotAccepted() {
        try {
            helper = new MockTAIRequestHelper() {
                @Override
                boolean isJmxConnectorRequest(HttpServletRequest request) {
                    return mockInterface.isJmxConnectorRequest();
                }
            };
            helper.webUtils = socialWebUtils;

            isJmxConnectorRequest(false);
            getLoginHint(uniqueId);
            createSocialTaiRequestAndSetRequestAttributeExpectations();
            getConfig(config);
            mockery.checking(new Expectations() {
                {
                    one(config).getAuthFilter();
                    will(returnValue(authFilter));
                    one(authFilter).isAccepted(request);
                    will(returnValue(false));
                }
            });

            // Auth filter found for this config, but is not configured to service this request
            boolean result = helper.requestShouldBeHandledByTAI(request, null);
            assertFalse("Result should not have been confirmed as target interceptor.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void requestShouldBeHandledByTAI_authFilterAccepted() {
        try {
            helper = new MockTAIRequestHelper() {
                @Override
                boolean isJmxConnectorRequest(HttpServletRequest request) {
                    return mockInterface.isJmxConnectorRequest();
                }
            };
            helper.webUtils = socialWebUtils;

            isJmxConnectorRequest(false);
            getLoginHint(uniqueId);
            createSocialTaiRequestAndSetRequestAttributeExpectations();
            getConfig(config);
            mockery.checking(new Expectations() {
                {
                    one(config).getAuthFilter();
                    will(returnValue(authFilter));
                    one(authFilter).isAccepted(request);
                    will(returnValue(true));
                }
            });

            // Auth filter found for this config, and is configured to service this request
            boolean result = helper.requestShouldBeHandledByTAI(request, null);
            assertTrue("Result should have been confirmed as target interceptor.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** isJmxConnectorRequest ******************************************/

    @Test
    public void isJmxConnectorRequest_nullPath() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getContextPath();
                    will(returnValue(null));
                }
            });
            boolean result = helper.isJmxConnectorRequest(request);
            assertFalse("Request with null context path should not have been confirmed as a JMX connector request.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isJmxConnectorRequest_nonMatchingPath() {
        try {
            final String path = RandomUtils.getRandomSelection("", "some", "extended/path");
            mockery.checking(new Expectations() {
                {
                    one(request).getContextPath();
                    will(returnValue(path));
                }
            });
            boolean result = helper.isJmxConnectorRequest(request);
            assertFalse("Request with context path [" + path + "] should not have been confirmed as a JMX connector request.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isJmxConnectorRequest_pathMatches() {
        try {
            final String path = "/IBMJMXConnectorREST";
            mockery.checking(new Expectations() {
                {
                    one(request).getContextPath();
                    will(returnValue(path));
                }
            });
            boolean result = helper.isJmxConnectorRequest(request);
            assertTrue("Result with context path [" + path + "] should have been confirmed as a JMX connector request.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isJmxConnectorRequest_pathSuperstring() {
        try {
            final String path = "/IBMJMXConnectorRESTextra";
            mockery.checking(new Expectations() {
                {
                    one(request).getContextPath();
                    will(returnValue(path));
                }
            });
            boolean result = helper.isJmxConnectorRequest(request);
            assertFalse("Request with context path [" + path + "] should not have been confirmed as a JMX connector request.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** setSocialTaiRequestConfigInfo ******************************************/

    @Test
    public void setSocialTaiRequestConfigInfo_nullId() {
        try {
            // With null ID provided, the SocialTaiRequest object sent to setSocialTaiRequestConfigInfo() doesn't really matter since we mock the call where it's used
            SocialTaiRequest taiReqInput = RandomUtils.getRandomSelection(null, new SocialTaiRequest(request));

            helper = new TAIRequestHelper() {
                @Override
                SocialTaiRequest setGenericAndFilteredConfigTaiRequestInfo(HttpServletRequest request, SocialTaiRequest socialTaiRequest) {
                    return mockInterface.setGenericAndFilteredConfigTaiRequestInfo();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).setGenericAndFilteredConfigTaiRequestInfo();
                    will(returnValue(socialTaiReq));
                }
            });

            SocialTaiRequest result = helper.setSocialTaiRequestConfigInfo(request, null, taiReqInput);
            assertNotNull("Result should not have been null but was.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setSocialTaiRequestConfigInfo_idProvided() {
        try {
            helper = new MockTAIRequestHelper() {
                @Override
                SocialTaiRequest setSpecificConfigTaiRequestInfo(HttpServletRequest request, String configId, SocialTaiRequest socialTaiRequest) {
                    return mockInterface.setSpecificConfigTaiRequestInfo();
                }
            };

            // These values don't really matter as long as the ID is non-null; the actual use of them is mocked
            final String configId = RandomUtils.getRandomSelection("", uniqueId);
            final SocialTaiRequest taiReq = RandomUtils.getRandomSelection(null, new SocialTaiRequest(request));

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).setSpecificConfigTaiRequestInfo();
                    will(returnValue(socialTaiReq));
                }
            });

            SocialTaiRequest result = helper.setSocialTaiRequestConfigInfo(request, configId, taiReq);
            assertNotNull("Result should not have been null but was.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** setGenericAndFilteredConfigTaiRequestInfo ******************************************/

    @Test
    public void setGenericAndFilteredConfigTaiRequestInfo_nullTaiReq() {
        try {
            helper = new TAIRequestHelper() {
                @Override
                Iterator<SocialLoginConfig> getConfigServices() {
                    return mockInterface.getConfigServices();
                }

                @Override
                SocialTaiRequest setGenericAndFilteredConfigTaiRequestInfoFromConfigServices(HttpServletRequest request, SocialTaiRequest socialTaiRequest, Iterator<SocialLoginConfig> services) {
                    return mockInterface.setGenericAndFilteredConfigTaiRequestInfoFromConfigServices();
                }
            };

            createSocialTaiRequestAndSetRequestAttributeExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getConfigServices();
                    one(mockInterface).setGenericAndFilteredConfigTaiRequestInfoFromConfigServices();
                }
            });

            SocialTaiRequest result = helper.setGenericAndFilteredConfigTaiRequestInfo(request, null);
            assertNotNull("Result should not have been null but was.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setGenericAndFilteredConfigTaiRequestInfo() {
        try {
            helper = new TAIRequestHelper() {
                @Override
                Iterator<SocialLoginConfig> getConfigServices() {
                    return mockInterface.getConfigServices();
                }

                @Override
                SocialTaiRequest setGenericAndFilteredConfigTaiRequestInfoFromConfigServices(HttpServletRequest request, SocialTaiRequest socialTaiRequest, Iterator<SocialLoginConfig> services) {
                    return mockInterface.setGenericAndFilteredConfigTaiRequestInfoFromConfigServices();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getConfigServices();
                    one(mockInterface).setGenericAndFilteredConfigTaiRequestInfoFromConfigServices();
                }
            });

            SocialTaiRequest result = helper.setGenericAndFilteredConfigTaiRequestInfo(request, new SocialTaiRequest(request));
            assertNotNull("Result should not have been null but was.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** setGenericAndFilteredConfigTaiRequestInfoFromConfigServices ******************************************/

    @Test
    public void setGenericAndFilteredConfigTaiRequestInfoFromConfigServices_nullServices() {
        try {
            // If the services iterator is null, it doesn't matter what the SocialTaiRequest input is
            SocialTaiRequest reqInput = RandomUtils.getRandomSelection(null, new SocialTaiRequest(request));

            SocialTaiRequest result = helper.setGenericAndFilteredConfigTaiRequestInfoFromConfigServices(request, reqInput, null);
            assertEquals("Result did not match expected result.", reqInput, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setGenericAndFilteredConfigTaiRequestInfoFromConfigServices_emptyServices() {
        try {
            Iterator<SocialLoginConfig> services = createConfigSet().iterator();

            SocialTaiRequest result = helper.setGenericAndFilteredConfigTaiRequestInfoFromConfigServices(request, new SocialTaiRequest(request), services);
            assertNotNull("Result should not have been null but was.", result);

            assertNull("Result should not have any generic configs, but found: " + result.getFilteredConfigs(), result.getFilteredConfigs());
            assertNull("Result should not have any filtered configs, but found: " + result.getFilteredConfigs(), result.getFilteredConfigs());
            assertNull("Provider name should have been null but wasn't. Name was: [" + result.getProviderName() + "].", result.getProviderName());
            assertNull("Result should not have an associated config but found [" + result.getTheOnlySocialLoginConfig() + "].", result.getTheOnlySocialLoginConfig());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setGenericAndFilteredConfigTaiRequestInfo_nullTaiReq_missingAuthFilter() {
        try {
            Iterator<SocialLoginConfig> services = createConfigSet(config).iterator();

            createSocialTaiRequestAndSetRequestAttributeExpectations();
            mockery.checking(new Expectations() {
                {
                    one(config).getAuthFilter();
                    will(returnValue(null));
                }
            });

            SocialTaiRequest result = helper.setGenericAndFilteredConfigTaiRequestInfoFromConfigServices(request, null, services);
            assertNotNull("Result should not have been null but was.", result);

            // Without an auth filter configured, the config should be considered a matching generic config
            List<SocialLoginConfig> genericConfigs = result.getGenericConfigs();
            assertNotNull("Result should have a generic config, but didn't find one.", genericConfigs);
            assertEquals("Generic configs list did not match expected size. Found configs: " + genericConfigs, 1, genericConfigs.size());
            assertTrue("Did not find the proper config in the generic configs list. Found configs: " + genericConfigs, genericConfigs.contains(config));

            assertNull("Result should not have any filtered configs, but found: " + result.getFilteredConfigs(), result.getFilteredConfigs());
            assertNull("Provider name should have been null but wasn't. Name was: [" + result.getProviderName() + "].", result.getProviderName());
            assertEquals("The only social login config in the request did not match the expected config.", config, result.getTheOnlySocialLoginConfig());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setGenericAndFilteredConfigTaiRequestInfo_authFilterNotAccepted() {
        try {
            Iterator<SocialLoginConfig> services = createConfigSet(config).iterator();

            mockery.checking(new Expectations() {
                {
                    one(config).getAuthFilter();
                    will(returnValue(authFilter));
                    one(authFilter).isAccepted(request);
                    will(returnValue(false));
                }
            });

            SocialTaiRequest result = helper.setGenericAndFilteredConfigTaiRequestInfoFromConfigServices(request, new SocialTaiRequest(request), services);
            assertNotNull("Result should not have been null but was.", result);

            // If an auth filter is configured but doesn't match the request, this config is not meant to handle this request
            assertNull("Result should not have any generic configs, but found: " + result.getFilteredConfigs(), result.getFilteredConfigs());
            assertNull("Result should not have any filtered configs, but found: " + result.getFilteredConfigs(), result.getFilteredConfigs());
            assertNull("Provider name should have been null but wasn't. Name was: [" + result.getProviderName() + "].", result.getProviderName());
            assertNull("Result should not have an associated config but found [" + result.getTheOnlySocialLoginConfig() + "].", result.getTheOnlySocialLoginConfig());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setGenericAndFilteredConfigTaiRequestInfo_authFilterAccepted() {
        try {
            Iterator<SocialLoginConfig> services = createConfigSet(config).iterator();

            mockery.checking(new Expectations() {
                {
                    one(config).getAuthFilter();
                    will(returnValue(authFilter));
                    one(authFilter).isAccepted(request);
                    will(returnValue(true));
                }
            });

            SocialTaiRequest result = helper.setGenericAndFilteredConfigTaiRequestInfoFromConfigServices(request, new SocialTaiRequest(request), services);
            assertNotNull("Result should not have been null but was.", result);

            // If auth filter is configured and matches, this config should be considered a matching filtered config
            List<SocialLoginConfig> filteredConfigs = result.getFilteredConfigs();
            assertNotNull("Result should have a filtered config, but didn't find one.", filteredConfigs);
            assertEquals("Filtered configs list did not match expected size. Found configs: " + filteredConfigs, 1, filteredConfigs.size());
            assertTrue("Did not find the proper config in the filtered configs list. Found configs: " + filteredConfigs, filteredConfigs.contains(config));

            assertNull("Result should not have any generic configs, but found: " + result.getGenericConfigs(), result.getGenericConfigs());
            assertNull("Provider name should have been null but wasn't. Name was: [" + result.getProviderName() + "].", result.getProviderName());
            assertEquals("The only social login config in the request did not match the expected config.", config, result.getTheOnlySocialLoginConfig());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setGenericAndFilteredConfigTaiRequestInfo_multipleMixedConfigs() {
        try {
            final SocialLoginConfig configNoFilter = mockery.mock(SocialLoginConfig.class, "configNoFilter");
            final SocialLoginConfig configNoFilter2 = mockery.mock(SocialLoginConfig.class, "configNoFilter2");
            final SocialLoginConfig configFilterMatches = mockery.mock(SocialLoginConfig.class, "configFilterMatches");
            final SocialLoginConfig configFilterDoesntMatch = mockery.mock(SocialLoginConfig.class, "configFilterDoesntMatch");
            final SocialLoginConfig configFilterDoesntMatch2 = mockery.mock(SocialLoginConfig.class, "configFilterDoesntMatch2");
            final AuthenticationFilter authFilterNotAccepted = mockery.mock(AuthenticationFilter.class, "authFilterNotAccepted");
            final AuthenticationFilter authFilterAccepted = mockery.mock(AuthenticationFilter.class, "authFilter3");
            Iterator<SocialLoginConfig> services = createConfigSet(configNoFilter, configNoFilter2, configFilterMatches, configFilterDoesntMatch, configFilterDoesntMatch2).iterator();

            mockery.checking(new Expectations() {
                {
                    one(configNoFilter).getAuthFilter();
                    will(returnValue(null));
                    one(configNoFilter2).getAuthFilter();
                    will(returnValue(null));
                    one(configFilterMatches).getAuthFilter();
                    will(returnValue(authFilterAccepted));
                    one(authFilterAccepted).isAccepted(request);
                    will(returnValue(true));
                    one(configFilterDoesntMatch).getAuthFilter();
                    will(returnValue(authFilterNotAccepted));
                    one(authFilterNotAccepted).isAccepted(request);
                    will(returnValue(false));
                    one(configFilterDoesntMatch2).getAuthFilter();
                    will(returnValue(authFilterNotAccepted));
                    one(authFilterNotAccepted).isAccepted(request);
                    will(returnValue(false));
                }
            });

            SocialTaiRequest result = helper.setGenericAndFilteredConfigTaiRequestInfoFromConfigServices(request, new SocialTaiRequest(request), services);
            assertNotNull("Result should not have been null but was.", result);
            assertNull("Provider name should have been null but wasn't. Name was: [" + result.getProviderName() + "].", result.getProviderName());

            // If auth filter is configured and matches, this config should be considered a matching filtered config
            List<SocialLoginConfig> filteredConfigs = result.getFilteredConfigs();
            assertNotNull("Result should have a filtered config, but didn't find one.", filteredConfigs);
            assertEquals("Filtered configs list did not match expected size. Found configs: " + filteredConfigs, 1, filteredConfigs.size());
            assertTrue("Did not find the proper config [" + configFilterMatches + "] in the filtered configs list. Found configs: " + filteredConfigs, filteredConfigs.contains(configFilterMatches));

            List<SocialLoginConfig> genericConfigs = result.getGenericConfigs();
            assertNotNull("Result should have a generic config, but didn't find one.", genericConfigs);
            assertEquals("Generic configs list did not match expected size. Found configs: " + genericConfigs, 2, genericConfigs.size());
            assertTrue("Did not find the proper config [" + configNoFilter + "] in the generic configs list. Found configs: " + genericConfigs, genericConfigs.contains(configNoFilter));
            assertTrue("Did not find the proper config [" + configNoFilter2 + "] in the generic configs list. Found configs: " + genericConfigs, genericConfigs.contains(configNoFilter2));

            // Filtered config should take precedence over generic configs, and should be considered the "one" matching config
            assertEquals("The only social login config in the request did not match the expected config.", configFilterMatches, result.getTheOnlySocialLoginConfig());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** setSpecificConfigTaiRequestInfo ******************************************/

    @Test
    public void setSpecificConfigTaiRequestInfo_nullConfigId_nullTaiReq() {
        try {
            helper = new TAIRequestHelper() {
                SocialLoginConfig getConfigAssociatedWithRequestAndId(HttpServletRequest request, String configId) {
                    return mockInterface.getConfigAssociatedWithRequestAndId();
                }
            };

            createSocialTaiRequestAndSetRequestAttributeExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getConfigAssociatedWithRequestAndId();
                    will(returnValue(null));
                }
            });

            SocialTaiRequest result = helper.setSpecificConfigTaiRequestInfo(request, null, null);
            assertNotNull("Result should not have been null but was.", result);

            try {
                // Calling getTheOnlySocialLoginConfig() should expose the exception created by the setSocialTaiRequestConfigInfo() method
                SocialLoginConfig foundConfig = result.getTheOnlySocialLoginConfig();
                fail("Should have thrown SocialLoginException but did not. Found config [" + foundConfig.getUniqueId() + "].");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5405E_SOCIAL_LOGIN_NO_SUCH_PROVIDER, "null");
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setSpecificConfigTaiRequestInfo_validConfigId() {
        try {
            helper = new TAIRequestHelper() {
                SocialLoginConfig getConfigAssociatedWithRequestAndId(HttpServletRequest request, String configId) {
                    return mockInterface.getConfigAssociatedWithRequestAndId();
                }
            };

            // Ensure that various ID values shouldn't affect the outcome
            final String configId = RandomUtils.getRandomSelection("", uniqueId);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getConfigAssociatedWithRequestAndId();
                    will(returnValue(config));
                    one(config).getUniqueId();
                    will(returnValue(configId));
                }
            });

            SocialTaiRequest result = helper.setSpecificConfigTaiRequestInfo(request, configId, new SocialTaiRequest(request));
            assertNotNull("Result should not have been null but was.", result);
            assertNull("Result should not have any filtered configs, but found: " + result.getFilteredConfigs(), result.getFilteredConfigs());
            assertNull("Result should not have any generic configs, but found: " + result.getGenericConfigs(), result.getGenericConfigs());
            assertEquals("Provider name in result did not match expected value.", configId, result.getProviderName());
            assertEquals("The only social login config in the request did not match the expected config.", config, result.getTheOnlySocialLoginConfig());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** getConfigAssociatedWithRequestAndId ******************************************/

    @Test
    public void getConfigAssociatedWithRequestAndId_nullId_filterDoesntMatch() {
        try {
            helper = new TAIRequestHelper() {
                boolean configAuthFilterMatchesRequest(HttpServletRequest request, SocialLoginConfig config) {
                    return mockInterface.configAuthFilterMatchesRequest();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).configAuthFilterMatchesRequest();
                    will(returnValue(false));
                }
            });

            SocialLoginConfig result = helper.getConfigAssociatedWithRequestAndId(request, null);
            assertNull("Result should have been null but was: [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigAssociatedWithRequestAndId_validId_filterDoesntMatch() {
        try {
            helper = new MockTAIRequestHelper() {
                boolean configAuthFilterMatchesRequest(HttpServletRequest request, SocialLoginConfig config) {
                    return mockInterface.configAuthFilterMatchesRequest();
                }
            };

            final String configId = RandomUtils.getRandomSelection("", uniqueId);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getConfig();
                    will(returnValue(config));
                    one(mockInterface).configAuthFilterMatchesRequest();
                    will(returnValue(false));
                }
            });

            SocialLoginConfig result = helper.getConfigAssociatedWithRequestAndId(request, configId);
            assertNull("Result should have been null but was: [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigAssociatedWithRequestAndId_validId_filterMatches() {
        try {
            helper = new MockTAIRequestHelper() {
                boolean configAuthFilterMatchesRequest(HttpServletRequest request, SocialLoginConfig config) {
                    return mockInterface.configAuthFilterMatchesRequest();
                }
            };

            final String configId = RandomUtils.getRandomSelection("", uniqueId);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getConfig();
                    will(returnValue(config));
                    one(mockInterface).configAuthFilterMatchesRequest();
                    will(returnValue(true));
                }
            });

            SocialLoginConfig result = helper.getConfigAssociatedWithRequestAndId(request, configId);
            assertNotNull("Result should not have been null but was.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** configAuthFilterMatchesRequest ******************************************/

    @Test
    public void configAuthFilterMatchesRequest_nullConfig() {
        try {
            assertFalse("Null config should not produce a match.", helper.configAuthFilterMatchesRequest(request, null));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void configAuthFilterMatchesRequest_noAuthFilter() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getAuthFilter();
                    will(returnValue(null));
                }
            });

            assertTrue("Config without an auth filter should produce a match.", helper.configAuthFilterMatchesRequest(request, config));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void configAuthFilterMatchesRequest_authFilterDoesntMatch() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getAuthFilter();
                    will(returnValue(authFilter));
                    one(authFilter).isAccepted(request);
                    will(returnValue(false));
                }
            });

            assertFalse("Config with auth filter that doesn't match should not produce a match.", helper.configAuthFilterMatchesRequest(request, config));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void configAuthFilterMatchesRequest_authFilterMatches() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getAuthFilter();
                    will(returnValue(authFilter));
                    one(authFilter).isAccepted(request);
                    will(returnValue(true));
                }
            });

            assertTrue("Config with auth filter that matches should produce a match.", helper.configAuthFilterMatchesRequest(request, config));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** Helper methods ******************************************/

    private void getLoginHint(final String hintValue) {
        mockery.checking(new Expectations() {
            {
                one(socialWebUtils).getLoginHint(request);
                will(returnValue(hintValue));
            }
        });
    }

    private void getLoginHintExpectations(final String hintValue) {
        if (hintValue == null || hintValue.isEmpty()) {
            // Hint value is expected to be missing, so doesn't matter whether it's null or empty as either a header or parameter
            mockery.checking(new Expectations() {
                {
                    one(request).getHeader(ClientConstants.LOGIN_HINT);
                    will(returnValue(RandomUtils.getRandomSelection(null, "")));
                    one(request).getParameter(ClientConstants.LOGIN_HINT);
                    will(returnValue(RandomUtils.getRandomSelection(null, "")));
                }
            });
        } else {
            // Hint value is non-null and non-empty, so randomly choose whether it's found in as a header or as a parameter
            final String headerValue = RandomUtils.getRandomSelection(null, "", hintValue);
            mockery.checking(new Expectations() {
                {
                    one(request).getHeader(ClientConstants.LOGIN_HINT);
                    will(returnValue(headerValue));
                }
            });
            if (headerValue == null || headerValue.isEmpty()) {
                // If not present as a header, the value MUST be found as a parameter
                mockery.checking(new Expectations() {
                    {
                        one(request).getParameter(ClientConstants.LOGIN_HINT);
                        will(returnValue(hintValue));
                    }
                });
            }
            // If value found in header, we won't even call getParameter() so no expectation is needed to mock that call
        }
    }

    private void createSocialTaiRequestAndSetRequestAttributeExpectations() {
        mockery.checking(new Expectations() {
            {
                one(request).setAttribute(with(any(String.class)), with(any(SocialTaiRequest.class)));
            }
        });
    }

    private void isJmxConnectorRequest(final boolean value) {
        mockery.checking(new Expectations() {
            {
                one(mockInterface).isJmxConnectorRequest();
                will(returnValue(value));
            }
        });
    }

    private void getConfigServices(final Iterator<SocialLoginConfig> services) {
        mockery.checking(new Expectations() {
            {
                one(mockInterface).getConfigServices();
                will(returnValue(services));
            }
        });
    }

    private void getConfig(final SocialLoginConfig config) {
        mockery.checking(new Expectations() {
            {
                one(mockInterface).getConfig();
                will(returnValue(config));
            }
        });
    }

    private Set<SocialLoginConfig> createConfigSet(SocialLoginConfig... configs) {
        Set<SocialLoginConfig> configSet = new HashSet<SocialLoginConfig>();
        if (configs != null) {
            for (SocialLoginConfig config : configs) {
                configSet.add(config);
            }
        }
        return configSet;
    }

}
