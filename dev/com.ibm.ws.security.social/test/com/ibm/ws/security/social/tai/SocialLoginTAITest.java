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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.regex.Pattern;

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

import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.SocialTaiRequest;
import com.ibm.ws.security.social.test.CommonTestClass;
import com.ibm.ws.security.social.web.SelectionPageGenerator;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;
import com.ibm.wsspi.security.tai.TAIResult;

import test.common.SharedOutputManager;

public class SocialLoginTAITest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    private final String uniqueId = "facebookLogin";

    final static String successfulTAIPrinciple = "myPrinciple";

    public interface MockInterface {
        public SelectionPageGenerator getSelectionPageGenerator();

        public TAIResult getAssociatedConfigAndHandleRequest() throws WebTrustAssociationFailedException;

        public TAIResult handleRequestBasedOnSocialLoginConfig() throws WebTrustAssociationFailedException;

        public TAIResult handleTwitterLoginRequest() throws WebTrustAssociationFailedException;

        public TAIResult handleOAuthLoginRequest() throws WebTrustAssociationFailedException;

        public TAIResult handleOidc() throws WebTrustAssociationFailedException;

        public TAIResult displaySocialMediaSelectionPage() throws WebTrustAssociationFailedException;

        public void removeCachedDataFromLocalAuthentication();

        public boolean isTwitterConfig();
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    @SuppressWarnings("unchecked")
    private final ServiceReference<SocialLoginConfig> socialLoginConfigServiceReference = mockery.mock(ServiceReference.class);
    private final SocialLoginConfig config = mockery.mock(SocialLoginConfig.class, "mockSocialLoginConfig");
    private final SocialTaiRequest socialTaiReq = mockery.mock(SocialTaiRequest.class);
    private final SocialWebUtils socialWebUtils = mockery.mock(SocialWebUtils.class);
    private final TAIRequestHelper requestHelper = mockery.mock(TAIRequestHelper.class);
    private final SelectionPageGenerator selectionPageGenerator = mockery.mock(SelectionPageGenerator.class);

    SocialLoginTAI tai = new SocialLoginTAI();

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());

        tai = new SocialLoginTAI();
        mockProtectedClassMembers(tai);
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

    private void mockProtectedClassMembers(SocialLoginTAI tai) {
        tai.taiRequestHelper = requestHelper;
        tai.webUtils = socialWebUtils;
    }

    /****************************************** negotiateValidateandEstablishTrust ******************************************/

    @Test
    public void negotiateValidateandEstablishTrust_missingTaiRequestAttribute() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getAttribute(Constants.ATTRIBUTE_TAI_REQUEST);
                    will(returnValue(null));
                }
            });
            handleErrorExpectations(HttpServletResponse.SC_FORBIDDEN);

            TAIResult result = tai.negotiateValidateandEstablishTrust(request, response);
            assertResultStatus(HttpServletResponse.SC_FORBIDDEN, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void negotiateValidateandEstablishTrust_WebTrustAssociationFailedException() {
        SocialLoginTAI tai = new SocialLoginTAI() {
            @Override
            TAIResult getAssociatedConfigAndHandleRequest(HttpServletRequest request, HttpServletResponse response, SocialTaiRequest socialTaiRequest, TAIResult defaultTaiResult) throws WebTrustAssociationFailedException {
                return mockInterface.getAssociatedConfigAndHandleRequest();
            }
        };
        try {
            negotiateValidateandEstablishTrustInitialExpectations(config);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getAssociatedConfigAndHandleRequest();
                    will(throwException(new WebTrustAssociationFailedException(defaultExceptionMsg)));
                }
            });

            try {
                TAIResult result = tai.negotiateValidateandEstablishTrust(request, response);
                fail("Should have thrown WebTrustAssociationFailedException but did not. Got result: " + result);
            } catch (WebTrustAssociationFailedException e) {
                verifyException(e, defaultExceptionMsg);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void negotiateValidateandEstablishTrust_noCode() {
        SocialLoginTAI tai = new SocialLoginTAI() {
            @Override
            TAIResult getAssociatedConfigAndHandleRequest(HttpServletRequest request, HttpServletResponse response, SocialTaiRequest socialTaiRequest, TAIResult defaultTaiResult) throws WebTrustAssociationFailedException {
                return mockInterface.getAssociatedConfigAndHandleRequest();
            }
        };
        try {
            final TAIResult successfulTAIResult = TAIResult.create(HttpServletResponse.SC_OK, successfulTAIPrinciple);

            negotiateValidateandEstablishTrustInitialExpectations(config);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getAssociatedConfigAndHandleRequest();
                    will(returnValue(successfulTAIResult));
                }
            });

            TAIResult result = tai.negotiateValidateandEstablishTrust(request, response);
            assertSuccesfulTAIResult(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** isTargetInterceptor ******************************************/

    @Test
    public void isTargetInterceptor_false() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(requestHelper).createSocialTaiRequestAndSetRequestAttribute(request);
                    will(returnValue(socialTaiReq));
                    one(requestHelper).requestShouldBeHandledByTAI(request, socialTaiReq);
                    will(returnValue(false));
                }
            });
            boolean result = tai.isTargetInterceptor(request);
            assertFalse("Result should not have been confirmed as target interceptor.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void isTargetInterceptor_true() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(requestHelper).createSocialTaiRequestAndSetRequestAttribute(request);
                    will(returnValue(socialTaiReq));
                    one(requestHelper).requestShouldBeHandledByTAI(request, socialTaiReq);
                    will(returnValue(true));
                }
            });
            boolean result = tai.isTargetInterceptor(request);
            assertTrue("Result should have been confirmed as target interceptor.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** getAssociatedConfigAndHandleRequest ******************************************/

    @Test
    public void getAssociatedConfigAndHandleRequest_multipleConfigs_exceptionThrown() {
        SocialLoginTAI tai = new SocialLoginTAI() {
            @Override
            TAIResult displaySocialMediaSelectionPage(HttpServletRequest request, HttpServletResponse response, SocialTaiRequest socialTaiRequest) throws WebTrustAssociationFailedException {
                return mockInterface.displaySocialMediaSelectionPage();
            }
        };
        try {
            final TAIResult successfulTaiResult = TAIResult.create(HttpServletResponse.SC_OK, successfulTAIPrinciple);
            mockery.checking(new Expectations() {
                {
                    one(socialTaiReq).getTheOnlySocialLoginConfig();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                    one(mockInterface).displaySocialMediaSelectionPage();
                    will(returnValue(successfulTaiResult));
                }
            });

            TAIResult result = tai.getAssociatedConfigAndHandleRequest(request, response, socialTaiReq, successfulTaiResult);

            assertSuccesfulTAIResult(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAssociatedConfigAndHandleRequest_oneConfig_WebTrustAssociationFailedException() {
        SocialLoginTAI tai = new SocialLoginTAI() {
            @Override
            TAIResult handleRequestBasedOnSocialLoginConfig(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config, TAIResult defaultTaiResult) throws WebTrustAssociationFailedException {
                return mockInterface.handleRequestBasedOnSocialLoginConfig();
            }
        };
        try {
            mockery.checking(new Expectations() {
                {
                    one(socialTaiReq).getTheOnlySocialLoginConfig();
                    will(returnValue(config));
                    one(mockInterface).handleRequestBasedOnSocialLoginConfig();
                    will(throwException(new WebTrustAssociationFailedException(defaultExceptionMsg)));
                }
            });

            try {
                final TAIResult successfulTaiResult = TAIResult.create(HttpServletResponse.SC_OK, successfulTAIPrinciple);
                TAIResult result = tai.getAssociatedConfigAndHandleRequest(request, response, socialTaiReq, successfulTaiResult);
                fail("Should have thrown WebTrustAssociationFailedException but did not. Got result: " + result);
            } catch (WebTrustAssociationFailedException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAssociatedConfigAndHandleRequest_oneConfig() {
        SocialLoginTAI tai = new SocialLoginTAI() {
            @Override
            TAIResult handleRequestBasedOnSocialLoginConfig(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config, TAIResult defaultTaiResult) throws WebTrustAssociationFailedException {
                return mockInterface.handleRequestBasedOnSocialLoginConfig();
            }
        };
        try {
            final TAIResult successfulTaiResult = TAIResult.create(HttpServletResponse.SC_OK, successfulTAIPrinciple);
            mockery.checking(new Expectations() {
                {
                    one(socialTaiReq).getTheOnlySocialLoginConfig();
                    will(returnValue(config));
                    one(mockInterface).handleRequestBasedOnSocialLoginConfig();
                    will(returnValue(successfulTaiResult));
                }
            });

            TAIResult result = tai.getAssociatedConfigAndHandleRequest(request, response, socialTaiReq, null);

            assertSuccesfulTAIResult(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** isAuthenticationRequired ******************************************/

    @Test
    public void isAuthenticationRequired() {
        SocialLoginTAI tai = new SocialLoginTAI();
        mockery.checking(new Expectations() {
            {
                one(request).getContextPath();
            }
        });
        boolean result = tai.isAuthenticationRequired(request);
        assertFalse("Result did not match expected result.", result);

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /****************************************** logout ******************************************/

    @Test
    public void logout() {
        tai = createActivatedSocialLoginTAI(config);
        boolean result = tai.logout(request, response, null);
        assertFalse("Result should have been false to indicate no actions were taken.", result);

        tai.deactivate(cc);
        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /****************************************** displaySocialMediaSelectionPage ******************************************/

    @Test
    public void displaySocialMediaSelectionPage_throwsIOException() {
        SocialLoginTAI tai = new SocialLoginTAI() {
            @Override
            SelectionPageGenerator getSelectionPageGenerator() {
                return mockInterface.getSelectionPageGenerator();
            }
        };

        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getSelectionPageGenerator();
                    will(returnValue(selectionPageGenerator));
                    one(selectionPageGenerator).displaySelectionPage(request, response, null);
                    will(throwException(new IOException(defaultExceptionMsg)));
                }
            });
            handleErrorExpectations(HttpServletResponse.SC_FORBIDDEN);

            TAIResult result = tai.displaySocialMediaSelectionPage(request, response, null);

            // 403 status is returned to end the TAI flow, not necessarily to say the user is forbidden. The end user will be redirected to sign in anyway.
            assertResultStatus(HttpServletResponse.SC_FORBIDDEN, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void displaySocialMediaSelectionPage() {
        SocialLoginTAI tai = new SocialLoginTAI() {
            @Override
            SelectionPageGenerator getSelectionPageGenerator() {
                return mockInterface.getSelectionPageGenerator();
            }
        };

        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getSelectionPageGenerator();
                    will(returnValue(selectionPageGenerator));
                    one(selectionPageGenerator).displaySelectionPage(request, response, null);
                }
            });

            TAIResult result = tai.displaySocialMediaSelectionPage(request, response, null);

            // 403 status is returned to end the TAI flow, not necessarily to say the user is forbidden. The end user will be redirected to sign in anyway.
            assertResultStatus(HttpServletResponse.SC_FORBIDDEN, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** handleRequestBasedOnSocialLoginConfig ******************************************/

    @Test
    public void handleRequestBasedOnSocialLoginConfig_nullConfig() {
        SocialLoginTAI tai = new SocialLoginTAI();

        try {
            // Because the TAIResult argument is null, ultimately we will default to a 403 status code
            handleErrorExpectations(HttpServletResponse.SC_FORBIDDEN);

            tai.handleRequestBasedOnSocialLoginConfig(request, response, null, null);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleRequestBasedOnSocialLoginConfig_twitter() {
        SocialLoginTAI tai = new SocialLoginTAI() {
            @Override
            void removeCachedDataFromLocalAuthentication(HttpServletRequest request, HttpServletResponse response) {
                mockInterface.removeCachedDataFromLocalAuthentication();
            }

            @Override
            boolean isTwitterConfig(SocialLoginConfig config) {
                return mockInterface.isTwitterConfig();
            }

            @Override
            TAIResult handleTwitterLoginRequest(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config) throws WebTrustAssociationFailedException {
                return mockInterface.handleTwitterLoginRequest();
            }
        };

        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).removeCachedDataFromLocalAuthentication();
                    one(mockInterface).isTwitterConfig();
                    will(returnValue(true));
                    one(mockInterface).handleTwitterLoginRequest();
                }
            });

            tai.handleRequestBasedOnSocialLoginConfig(request, response, config, null);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleRequestBasedOnSocialLoginConfig_nonTwitter() {
        SocialLoginTAI tai = new SocialLoginTAI() {
            @Override
            void removeCachedDataFromLocalAuthentication(HttpServletRequest request, HttpServletResponse response) {
                mockInterface.removeCachedDataFromLocalAuthentication();
            }

            @Override
            boolean isTwitterConfig(SocialLoginConfig config) {
                return mockInterface.isTwitterConfig();
            }

            @Override
            TAIResult handleOAuthLoginRequest(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config) throws WebTrustAssociationFailedException {
                return mockInterface.handleOAuthLoginRequest();
            }

        };

        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).removeCachedDataFromLocalAuthentication();
                    one(mockInterface).isTwitterConfig();
                    will(returnValue(false));
                    allowing(config).getUserApi();
                    will(returnValue("userapi"));
                    one(mockInterface).handleOAuthLoginRequest();
                }
            });

            tai.handleRequestBasedOnSocialLoginConfig(request, response, config, TAIResult.create(HttpServletResponse.SC_FORBIDDEN));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** removeCachedDataFromLocalAuthentication ******************************************/

    @Test
    public void removeCachedDataFromLocalAuthentication() {
        SocialLoginTAI tai = new SocialLoginTAI();
        tai.webUtils = socialWebUtils;

        try {
            mockery.checking(new Expectations() {
                {
                    one(socialWebUtils).removeRequestUrlAndParameters(request, response);
                }
            });

            tai.removeCachedDataFromLocalAuthentication(request, response);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** Helper methods ******************************************/

    @SuppressWarnings("static-access")
    private SocialLoginTAI createActivatedSocialLoginTAI(final SocialLoginConfig socialLoginConfig) {
        SocialLoginTAI tai = new SocialLoginTAI();
        mockery.checking(new Expectations() {
            {
                allowing(socialLoginConfigServiceReference).getProperty("id");
                will(returnValue(uniqueId));
                allowing(socialLoginConfigServiceReference).getProperty("service.id");
                will(returnValue(123L));
                allowing(socialLoginConfigServiceReference).getProperty("service.ranking");
                will(returnValue(1L));
                allowing(cc).locateService(SocialLoginTAI.KEY_SOCIAL_LOGIN_CONFIG, socialLoginConfigServiceReference);
                will(returnValue(socialLoginConfig));
            }
        });
        if (tai.authFilterServiceRef.toString().contains("isActive=true")) {
            tai.authFilterServiceRef.deactivate(cc);
        }
        if (tai.socialLoginConfigRef.toString().contains("isActive=true")) {
            tai.socialLoginConfigRef.deactivate(cc);
        }
        tai.activate(cc, Collections.<String, Object> emptyMap());
        tai.setSocialLoginConfig(socialLoginConfigServiceReference);
        return tai;
    }

    private void negotiateValidateandEstablishTrustInitialExpectations(final SocialLoginConfig config) throws SocialLoginException {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(Constants.ATTRIBUTE_TAI_REQUEST);
                will(returnValue(socialTaiReq));
            }
        });
    }

    private void assertResultStatus(int expected, TAIResult result) {
        assertEquals("Result code did not match expected result.", expected, result.getStatus());
    }

    private void assertSuccesfulTAIResult(TAIResult result) {
        assertEquals("TAIResult code did not match expected value.", HttpServletResponse.SC_OK, result.getStatus());
        assertEquals("TAIResult principle did not match expected value.", successfulTAIPrinciple, result.getAuthenticatedPrincipal());
    }

}
