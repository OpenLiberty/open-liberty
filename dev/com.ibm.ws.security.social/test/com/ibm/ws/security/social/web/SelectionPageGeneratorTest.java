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
package com.ibm.ws.security.social.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.common.web.WebUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.SocialLoginWebappConfig;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.SocialTaiRequest;
import com.ibm.ws.security.social.test.CommonTestClass;
import com.ibm.ws.security.social.web.utils.ConfigInfoJsonBuilder;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;

import test.common.SharedOutputManager;

public class SelectionPageGeneratorTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    private final String CWWKS5427E_SIGN_IN_NO_CONFIGS = "CWWKS5427E";
    private final String CWWKS5428W_SIGN_IN_MISSING_EXPECTED_CONFIG = "CWWKS5428W";
    private final String CWWKS5429E_ERROR_DISPLAYING_SIGN_IN_PAGE = "CWWKS5429E";

    private static final String PARAM_1 = "param1";
    private static final String PARAM_1_VALUE = "value1";
    private static final String CONFIG_1_ID = "myConfig1";
    private static final String CONFIG_1_DISPLAY_NAME = "My 1st Social Media";
    private static final String URL = "https://some-domain.com:80/context/path";
    private static final Map<String, String[]> PARAM_MAP = new HashMap<String, String[]>();
    static {
        PARAM_MAP.put(PARAM_1, new String[] { PARAM_1_VALUE });
    }

    private static final String HTML_REGEX = "<!DOCTYPE html>\\n?<html[^>]*>.+</html>";
    private static final String HEAD_REGEX = "<head>.*</head>";
    private static final String BODY_REGEX = "<body>.*</body>";
    private static final String FORM_REGEX = "<form .*</form>";

    private static final String HTML_PAGE_TITLE = "Social Media Selection Form";
    private static final String HTML_PAGE_HEADER = "Sign in";

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    public interface MockInterface {
        public Collection<SocialLoginConfig> getSocialLoginConfigs();

        public void generateOrSendToAppropriateSelectionPage() throws IOException;

        public String getRequestUrl();

        public SocialLoginWebappConfig getSocialLoginWebappConfig();

        public void saveRequestUrlAndParametersForLocalAuthentication();

        public boolean isCustomSelectionPageConfigured();

        public void redirectToCustomSelectionPage();

        public void redirectToCustomSelectionPageUrl();

        public String buildCustomRedirectUriQuery();

        public String getConfigInformationParameterString();

        public void generateDefaultSelectionPage();

        public String createSignInHtml();

        public String getHtmlLang();

        public String createHtmlHead();

        public String getHtmlTitle();

        public String createHtmlBody();

        public String createHtmlMainContent();

        public String getPageHeader();

        public String createHtmlFormWithButtons();

        public String createButtonHtml();

        public String getObscuredConfigId();

        public String createCssContentString();
    }

    final SocialLoginConfig config = mockery.mock(SocialLoginConfig.class);
    final SocialLoginConfig config1 = mockery.mock(SocialLoginConfig.class, "config1");
    final SocialLoginConfig config2 = mockery.mock(SocialLoginConfig.class, "config2");
    final SocialLoginConfig config3 = mockery.mock(SocialLoginConfig.class, "config3");
    final SocialTaiRequest socialTaiRequest = mockery.mock(SocialTaiRequest.class);
    final SocialLoginWebappConfig webAppConfig = mockery.mock(SocialLoginWebappConfig.class);
    final SocialWebUtils webUtils = mockery.mock(SocialWebUtils.class);

    SelectionPageGenerator generator = new SelectionPageGenerator();
    SelectionPageGenerator generatorMockedSetReqInfo = new MockSelectionPageGenerator();

    /**
     * Mocks out the method calls that set member variables so that we can more directly set those members or use mock objects.
     */
    private class MockSelectionPageGenerator extends SelectionPageGenerator {
        @Override
        Collection<SocialLoginConfig> getSocialLoginConfigs(SocialTaiRequest socialTaiRequest) {
            return mockInterface.getSocialLoginConfigs();
        }

        @Override
        String getRequestUrl(HttpServletRequest request) {
            return mockInterface.getRequestUrl();
        }

        @Override
        void saveRequestUrlAndParametersForLocalAuthentication(HttpServletRequest request, HttpServletResponse response) {
            mockInterface.saveRequestUrlAndParametersForLocalAuthentication();
        }
    };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        generator = new SelectionPageGenerator();
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

    /************************************** displaySelectionPage **************************************/

    @Test
    public void test_displaySelectionPage_noConfigs() {
        try {
            @SuppressWarnings("unchecked")
            final Collection<SocialLoginConfig> configs = RandomUtils.getRandomSelection(null, new ArrayList<SocialLoginConfig>());

            setRequestAndConfigInformationExpectations(configs, URL, "POST", PARAM_MAP);
            handleErrorExpectations();

            generatorMockedSetReqInfo.displaySelectionPage(request, response, socialTaiRequest);

            verifyLogMessage(outputMgr, CWWKS5429E_ERROR_DISPLAYING_SIGN_IN_PAGE + ".+" + CWWKS5427E_SIGN_IN_NO_CONFIGS);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_displaySelectionPage_withConfigs() {
        try {
            SelectionPageGenerator mockGenerator = new MockSelectionPageGenerator() {
                @Override
                protected void generateOrSendToAppropriateSelectionPage(HttpServletResponse response) throws IOException {
                    mockInterface.generateOrSendToAppropriateSelectionPage();
                }
            };
            final List<SocialLoginConfig> configs = Arrays.asList(config);

            setRequestAndConfigInformationExpectations(configs, URL, "POST", PARAM_MAP);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).generateOrSendToAppropriateSelectionPage();
                }
            });

            mockGenerator.displaySelectionPage(request, response, socialTaiRequest);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** setRequestAndConfigInformation **************************************/

    @Test
    public void test_setRequestAndConfigInformation_nullConfigs() {
        try {
            final List<SocialLoginConfig> configs = Arrays.asList(config);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getSocialLoginConfigs();
                    will(returnValue(configs));
                    one(mockInterface).getRequestUrl();
                    one(request).getMethod();
                    one(request).getParameterMap();
                    one(mockInterface).saveRequestUrlAndParametersForLocalAuthentication();
                }
            });

            generatorMockedSetReqInfo.setRequestAndConfigInformation(request, response, socialTaiRequest);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getSocialLoginConfigs **************************************/

    @Test
    public void test_getSocialLoginConfigs_noConfigs() {
        try {
            @SuppressWarnings("unchecked")
            final Collection<SocialLoginConfig> configs = RandomUtils.getRandomSelection(null, new HashSet<SocialLoginConfig>());
            mockery.checking(new Expectations() {
                {
                    one(socialTaiRequest).getAllMatchingConfigs();
                    will(returnValue(configs));
                }
            });

            Collection<SocialLoginConfig> result = generator.getSocialLoginConfigs(socialTaiRequest);
            assertEquals("Result did not match the expected value.", configs, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getSocialLoginConfigs_withConfigs() {
        try {
            final Set<SocialLoginConfig> configs = new HashSet<SocialLoginConfig>();
            configs.add(config1);
            configs.add(config2);
            configs.add(config3);
            mockery.checking(new Expectations() {
                {
                    one(socialTaiRequest).getAllMatchingConfigs();
                    will(returnValue(configs));
                }
            });

            Collection<SocialLoginConfig> result = generator.getSocialLoginConfigs(socialTaiRequest);
            assertEquals("Result did not match the expected value.", configs, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** generateOrSendToAppropriateSelectionPage **************************************/

    @Test
    public void test_generateOrSendToAppropriateSelectionPage_customPage() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                boolean isCustomSelectionPageConfigured() {
                    return mockInterface.isCustomSelectionPageConfigured();
                }

                @Override
                void redirectToCustomSelectionPage(HttpServletResponse response) throws IOException {
                    mockInterface.redirectToCustomSelectionPage();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isCustomSelectionPageConfigured();
                    will(returnValue(true));
                    one(mockInterface).redirectToCustomSelectionPage();
                }
            });

            mockGenerator.generateOrSendToAppropriateSelectionPage(response);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_generateOrSendToAppropriateSelectionPage_defaultPage() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                boolean isCustomSelectionPageConfigured() {
                    return mockInterface.isCustomSelectionPageConfigured();
                }

                @Override
                void generateDefaultSelectionPage(HttpServletResponse response) throws IOException {
                    mockInterface.generateDefaultSelectionPage();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isCustomSelectionPageConfigured();
                    will(returnValue(false));
                    one(mockInterface).generateDefaultSelectionPage();
                }
            });

            mockGenerator.generateOrSendToAppropriateSelectionPage(response);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getRequestUrl **************************************/

    @Test
    public void test_getRequestUrl() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getRequestURL();
                    will(returnValue(new StringBuffer(URL)));
                }
            });

            String result = generator.getRequestUrl(request);
            assertEquals("Result did not match expected URL.", URL, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** saveRequestUrlAndParametersForLocalAuthentication ******************************************/

    @Test
    public void saveRequestUrlAndParametersForLocalAuthentication() {
        try {
            generator.webUtils = webUtils;

            mockery.checking(new Expectations() {
                {
                    one(webUtils).saveRequestUrlAndParameters(request, response);
                }
            });

            generator.saveRequestUrlAndParametersForLocalAuthentication(request, response);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** isCustomSelectionPageConfigured **************************************/

    @Test
    public void test_isCustomSelectionPageConfigured_missingWebAppConfig() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                SocialLoginWebappConfig getSocialLoginWebappConfig() {
                    return mockInterface.getSocialLoginWebappConfig();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getSocialLoginWebappConfig();
                    will(returnValue(null));
                }
            });

            assertFalse("Result should have been false when the web app config is missing.", mockGenerator.isCustomSelectionPageConfigured());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isCustomSelectionPageConfigured_missingSelectionUrl() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                SocialLoginWebappConfig getSocialLoginWebappConfig() {
                    return mockInterface.getSocialLoginWebappConfig();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getSocialLoginWebappConfig();
                    will(returnValue(webAppConfig));
                    one(webAppConfig).getSocialMediaSelectionPageUrl();
                    will(returnValue(null));
                }
            });

            assertFalse("Result should have been false when the selection page URL in web app config is missing.", mockGenerator.isCustomSelectionPageConfigured());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isCustomSelectionPageConfigured_emptySelectionUrl() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                SocialLoginWebappConfig getSocialLoginWebappConfig() {
                    return mockInterface.getSocialLoginWebappConfig();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getSocialLoginWebappConfig();
                    will(returnValue(webAppConfig));
                    one(webAppConfig).getSocialMediaSelectionPageUrl();
                    will(returnValue(""));
                }
            });

            assertTrue("Result should have been true for an empty selection page URL in web app config.", mockGenerator.isCustomSelectionPageConfigured());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isCustomSelectionPageConfigured_nonEmptySelectionUrl() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                SocialLoginWebappConfig getSocialLoginWebappConfig() {
                    return mockInterface.getSocialLoginWebappConfig();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getSocialLoginWebappConfig();
                    will(returnValue(webAppConfig));
                    one(webAppConfig).getSocialMediaSelectionPageUrl();
                    will(returnValue(URL));
                }
            });

            assertTrue("Result should have been true for a normal selection page URL in web app config.", mockGenerator.isCustomSelectionPageConfigured());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** redirectToCustomSelectionPage **************************************/

    @Test
    public void test_redirectToCustomSelectionPage_missingWebAppConfig() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                SocialLoginWebappConfig getSocialLoginWebappConfig() {
                    return mockInterface.getSocialLoginWebappConfig();
                }

                @Override
                void generateDefaultSelectionPage(HttpServletResponse response) throws IOException {
                    mockInterface.generateDefaultSelectionPage();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getSocialLoginWebappConfig();
                    will(returnValue(null));
                    one(mockInterface).generateDefaultSelectionPage();
                }
            });

            mockGenerator.redirectToCustomSelectionPage(response);

            verifyLogMessage(outputMgr, CWWKS5432W_CUSTOM_SELECTION_INITED_MISSING_WEBAPP_CONFIG);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_redirectToCustomSelectionPage_nullSelectionPageUrl() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                SocialLoginWebappConfig getSocialLoginWebappConfig() {
                    return mockInterface.getSocialLoginWebappConfig();
                }

                @Override
                void redirectToCustomSelectionPageUrl(HttpServletResponse response, String url) {
                    mockInterface.redirectToCustomSelectionPageUrl();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getSocialLoginWebappConfig();
                    will(returnValue(webAppConfig));
                    one(webAppConfig).getSocialMediaSelectionPageUrl();
                    will(returnValue(null));
                    one(mockInterface).redirectToCustomSelectionPageUrl();
                }
            });

            mockGenerator.redirectToCustomSelectionPage(response);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_redirectToCustomSelectionPage_normalSelectionPageUrl() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                SocialLoginWebappConfig getSocialLoginWebappConfig() {
                    return mockInterface.getSocialLoginWebappConfig();
                }

                @Override
                void redirectToCustomSelectionPageUrl(HttpServletResponse response, String url) {
                    mockInterface.redirectToCustomSelectionPageUrl();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getSocialLoginWebappConfig();
                    will(returnValue(webAppConfig));
                    one(webAppConfig).getSocialMediaSelectionPageUrl();
                    will(returnValue(URL));
                    one(mockInterface).redirectToCustomSelectionPageUrl();
                }
            });

            mockGenerator.redirectToCustomSelectionPage(response);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** redirectToCustomSelectionPageUrl **************************************/

    @Test
    public void test_redirectToCustomSelectionPageUrl_nullUrl() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                String buildCustomRedirectUriQuery() {
                    return mockInterface.buildCustomRedirectUriQuery();
                }
            };
            final String queryString = "";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).buildCustomRedirectUriQuery();
                    will(returnValue(queryString));
                    // URL should be well validated by now, so the method does no null or format checking of the URL
                    one(response).sendRedirect(null + "?" + queryString);
                }
            });

            mockGenerator.redirectToCustomSelectionPageUrl(response, null);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_redirectToCustomSelectionPageUrl_invalidUrl() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                String buildCustomRedirectUriQuery() {
                    return mockInterface.buildCustomRedirectUriQuery();
                }
            };
            final String url = "Some#?Bad `~ url";
            final String queryString = "A query string";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).buildCustomRedirectUriQuery();
                    will(returnValue(queryString));
                    // URL should be well validated by now, so the method does no null or format checking of the URL
                    one(response).sendRedirect(url + "?" + queryString);
                }
            });

            mockGenerator.redirectToCustomSelectionPageUrl(response, url);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_redirectToCustomSelectionPageUrl_relativeUrl() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                String buildCustomRedirectUriQuery() {
                    return mockInterface.buildCustomRedirectUriQuery();
                }
            };
            final String url = "/context/path-value";
            final String queryString = "some=query%3E&string=val%20ue+string";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).buildCustomRedirectUriQuery();
                    will(returnValue(queryString));
                    one(response).sendRedirect(url + "?" + queryString);
                }
            });

            mockGenerator.redirectToCustomSelectionPageUrl(response, url);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_redirectToCustomSelectionPageUrl_absoluteUrl() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                String buildCustomRedirectUriQuery() {
                    return mockInterface.buildCustomRedirectUriQuery();
                }
            };
            final String url = "http://localhost:8010/context/path-value";
            final String queryString = "some=query%3E&string=val%20ue+string";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).buildCustomRedirectUriQuery();
                    will(returnValue(queryString));
                    one(response).sendRedirect(url + "?" + queryString);
                }
            });

            mockGenerator.redirectToCustomSelectionPageUrl(response, url);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** buildCustomRedirectUriQuery **************************************/

    @Test
    public void test_buildCustomRedirectUriQuery_uninitializedAndNullConfigInfo() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                String getConfigInformationParameterString() {
                    return mockInterface.getConfigInformationParameterString();
                }
            };
            final String configInfoString = null;
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getConfigInformationParameterString();
                    will(returnValue(configInfoString));
                }
            });

            String result = mockGenerator.buildCustomRedirectUriQuery();

            // The parameters will always be included in the query string even if the values are null. The values should not ever be null anyway.
            verifyPattern(result, SelectionPageGenerator.PARAM_ORIGINAL_REQ_URL + "=(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_REQUEST_METHOD + "=" + null + "(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_CONFIG_JSON_DATA + "=" + null + "(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_SUBMIT_PARAM_NAME + "=" + ClientConstants.LOGIN_HINT + "(&|$)");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildCustomRedirectUriQuery_nullConfigInfo() {
        try {
            SelectionPageGenerator mockGenerator = new MockSelectionPageGenerator() {
                @Override
                String getConfigInformationParameterString() {
                    return mockInterface.getConfigInformationParameterString();
                }
            };
            final String url = URL;
            final String method = "POST";
            final String configInfoString = null;
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getConfigInformationParameterString();
                    will(returnValue(configInfoString));
                }
            });

            setRequestAndConfigInformationExpectations(null, url, method, PARAM_MAP);

            // Set appropriate information that will be used to create the query string (config info, request URL, and request method)
            mockGenerator.setRequestAndConfigInformation(request, response, socialTaiRequest);

            String result = mockGenerator.buildCustomRedirectUriQuery();

            // The parameters will always be included in the query string even if the values are null. The values should not ever be null anyway.
            String encodedUrl = WebUtils.urlEncode(url + "?" + PARAM_1 + "=" + PARAM_1_VALUE);
            String encodedMethod = WebUtils.urlEncode(method);

            verifyPattern(result, SelectionPageGenerator.PARAM_ORIGINAL_REQ_URL + "=" + Pattern.quote(encodedUrl) + "(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_REQUEST_METHOD + "=" + Pattern.quote(encodedMethod) + "(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_CONFIG_JSON_DATA + "=" + null + "(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_SUBMIT_PARAM_NAME + "=" + ClientConstants.LOGIN_HINT + "(&|$)");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildCustomRedirectUriQuery_invalidUrlUnknownMethod() {
        try {
            SelectionPageGenerator mockGenerator = new MockSelectionPageGenerator() {
                @Override
                String getConfigInformationParameterString() {
                    return mockInterface.getConfigInformationParameterString();
                }
            };
            final String url = "Inva^#(~lid_ uRL://@";
            final String method = "unknown method";
            final String configInfoString = null;
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getConfigInformationParameterString();
                    will(returnValue(configInfoString));
                }
            });

            setRequestAndConfigInformationExpectations(null, url, method, new HashMap<String, String[]>());

            // Set appropriate information that will be used to create the query string (config info, request URL, and request method)
            mockGenerator.setRequestAndConfigInformation(request, response, socialTaiRequest);

            String result = mockGenerator.buildCustomRedirectUriQuery();

            // The URL value is not validated because it should be obtained from the original request, which MUST have a valid URL
            String encodedUrl = WebUtils.urlEncode(url);
            String encodedMethod = WebUtils.urlEncode(method);

            verifyPattern(result, SelectionPageGenerator.PARAM_ORIGINAL_REQ_URL + "=" + Pattern.quote(encodedUrl) + "(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_REQUEST_METHOD + "=" + Pattern.quote(encodedMethod) + "(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_CONFIG_JSON_DATA + "=" + null + "(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_SUBMIT_PARAM_NAME + "=" + ClientConstants.LOGIN_HINT + "(&|$)");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildCustomRedirectUriQuery_emptyConfigInfo() {
        try {
            SelectionPageGenerator mockGenerator = new MockSelectionPageGenerator() {
                @Override
                String getConfigInformationParameterString() {
                    return mockInterface.getConfigInformationParameterString();
                }
            };
            final String url = URL;
            final String method = "POST";
            final String configInfoString = "{}";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getConfigInformationParameterString();
                    will(returnValue(configInfoString));
                }
            });

            setRequestAndConfigInformationExpectations(null, url, method, PARAM_MAP);

            // Set appropriate information that will be used to create the query string (config info, request URL, and request method)
            mockGenerator.setRequestAndConfigInformation(request, response, socialTaiRequest);

            String result = mockGenerator.buildCustomRedirectUriQuery();

            String encodedUrl = WebUtils.urlEncode(url + "?" + PARAM_1 + "=" + PARAM_1_VALUE);
            String encodedMethod = WebUtils.urlEncode(method);
            String encodedConfigInfoString = WebUtils.urlEncode(configInfoString);

            verifyPattern(result, SelectionPageGenerator.PARAM_ORIGINAL_REQ_URL + "=" + Pattern.quote(encodedUrl) + "(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_REQUEST_METHOD + "=" + Pattern.quote(encodedMethod) + "(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_CONFIG_JSON_DATA + "=" + Pattern.quote(encodedConfigInfoString) + "(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_SUBMIT_PARAM_NAME + "=" + ClientConstants.LOGIN_HINT + "(&|$)");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildCustomRedirectUriQuery_normalConfigInfo() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                String getConfigInformationParameterString() {
                    return mockInterface.getConfigInformationParameterString();
                }
            };
            final String configInfoString = "{\"social-media\":[]}";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getConfigInformationParameterString();
                    will(returnValue(configInfoString));
                }
            });

            String result = mockGenerator.buildCustomRedirectUriQuery();

            String encodedConfigInfoString = WebUtils.urlEncode(configInfoString);

            verifyPattern(result, SelectionPageGenerator.PARAM_ORIGINAL_REQ_URL + "=(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_REQUEST_METHOD + "=" + null + "(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_CONFIG_JSON_DATA + "=" + Pattern.quote(encodedConfigInfoString) + "(&|$)");
            verifyPattern(result, SelectionPageGenerator.PARAM_SUBMIT_PARAM_NAME + "=" + ClientConstants.LOGIN_HINT + "(&|$)");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** buildCustomRedirectUriQuery(Map) **************************************/

    @Test
    public void test_buildCustomRedirectUriQuery_nullMap() {
        try {
            String result = generator.buildCustomRedirectUriQuery(null);

            assertEquals("Result should have been an empty string for a null map input.", "", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildCustomRedirectUriQuery_emptyMap() {
        try {
            Map<String, String> params = new HashMap<String, String>();

            String result = generator.buildCustomRedirectUriQuery(params);

            assertEquals("Result should have been an empty string for an empty map input.", "", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildCustomRedirectUriQuery_singleEntryMap() {
        try {
            Map<String, String> params = new HashMap<String, String>();
            params.put("abc", "123");

            String result = generator.buildCustomRedirectUriQuery(params);

            verifyPattern(result, "^" + "abc" + "=" + "123" + "$");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildCustomRedirectUriQuery_multipleEntryMap() {
        try {
            Map<String, String> params = new HashMap<String, String>();
            params.put("abc", "123");
            params.put("&&&", "a complex\"\t\n\r\" value");

            String result = generator.buildCustomRedirectUriQuery(params);

            verifyPattern(result, "abc" + "=" + "123" + "(&|$)");
            verifyPattern(result, Pattern.quote("%26%26%26") + "=" + Pattern.quote("a+complex%22%09%0A%0D%22+value") + "(&|$)");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getConfigInformationParameterString **************************************/

    @Test
    public void test_getConfigInformationParameterString_nullConfigs() {
        try {
            String result = generator.getConfigInformationParameterString();

            assertEquals("Result for a null configs object should have been an empty JSON object.", "{}", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getConfigInformationParameterString_noConfigs() {
        try {
            setRequestAndConfigInformationExpectations(new HashSet<SocialLoginConfig>(), null, null, new HashMap<String, String[]>());

            // Set appropriate information that will be used to create the query string (config info, request URL, and request method)
            generatorMockedSetReqInfo.setRequestAndConfigInformation(request, response, socialTaiRequest);

            String result = generatorMockedSetReqInfo.getConfigInformationParameterString();

            assertNotNull("Result should not have been null but was.", result);

            String expectedValue = "{\"" + ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA + "\":[]}";
            assertEquals("Result for an empty configs object should have been a JSON object with one key with an empty array value.", expectedValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** generateDefaultSelectionPage **************************************/

    @Test
    public void test_generateDefaultSelectionPage() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                String createSignInHtml() {
                    return mockInterface.createSignInHtml();
                }
            };
            final String html = "<html></html>";
            mockery.checking(new Expectations() {
                {
                    one(response).getWriter();
                    will(returnValue(writer));
                    one(mockInterface).createSignInHtml();
                    will(returnValue(html));
                    one(writer).print(html);
                    one(writer).close();
                }
            });

            mockGenerator.generateDefaultSelectionPage(response);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createSignInHtml **************************************/

    @Test
    public void test_createSignInHtml() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                protected String getHtmlLang() {
                    return mockInterface.getHtmlLang();
                }

                @Override
                protected String createHtmlHead() {
                    return mockInterface.createHtmlHead();
                }

                @Override
                protected String createHtmlBody() {
                    return mockInterface.createHtmlBody();
                }
            };
            final String body = "<body></body>";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getHtmlLang();
                    will(returnValue(""));
                    one(mockInterface).createHtmlHead();
                    will(returnValue(""));
                    one(mockInterface).createHtmlBody();
                    will(returnValue(body));
                }
            });

            String result = mockGenerator.createSignInHtml();
            assertStringFound(result, "^" + HTML_REGEX + "\\n?$", "Result should be a valid HTML page.", Pattern.DOTALL);
            assertStringFound(result, body, "Result should contain the expected body tags.");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getHtmlLang **************************************/

    @Test
    public void test_getHtmlLang_missingRequestInfo() {
        try {
            String result = generator.getHtmlLang();
            assertEquals("Result should have been an empty string when the request information is missing.", "", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getHtmlLang_withRequestLocale() {
        try {
            final String localeStr = "en";
            final Locale locale = new Locale(localeStr);
            mockery.checking(new Expectations() {
                {
                    one(request).getLocale();
                    will(returnValue(locale));
                }
            });

            // Set initial request information
            setRequestAndConfigInformationExpectations(null, URL, "POST", PARAM_MAP);
            generatorMockedSetReqInfo.setRequestAndConfigInformation(request, response, socialTaiRequest);

            String result = generatorMockedSetReqInfo.getHtmlLang();
            assertStringFound(result, "^lang=\"" + locale + "\"$", "Result did not match expected pattern.");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createHtmlHead **************************************/

    @Test
    public void test_createHtmlHead() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                String getHtmlTitle() {
                    return mockInterface.getHtmlTitle();
                }

                @Override
                String createCssContentString() {
                    return mockInterface.createCssContentString();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getHtmlTitle();
                    will(returnValue(HTML_PAGE_TITLE));
                    one(mockInterface).createCssContentString();
                    will(returnValue(""));
                }
            });

            String result = mockGenerator.createHtmlHead();
            assertStringFound(result, "^" + HEAD_REGEX + "\\n?$", "Result should be a valid HTML head.", Pattern.DOTALL);
            assertStringFound(result, "<title>" + HTML_PAGE_TITLE + "</title>", "Result did not include the expected HTML title.");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createHtmlBody **************************************/

    @Test
    public void test_createHtmlBody() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                String createHtmlMainContent() {
                    return mockInterface.createHtmlMainContent();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createHtmlMainContent();
                    will(returnValue(""));
                }
            });

            String result = mockGenerator.createHtmlBody();
            assertStringFound(result, "^" + BODY_REGEX + "\\n?$", "Result should be a valid HTML body.", Pattern.DOTALL);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createHtmlMainContent **************************************/

    @Test
    public void test_createHtmlMainContent() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                String getPageHeader() {
                    return mockInterface.getPageHeader();
                }

                @Override
                String createHtmlFormWithButtons() {
                    return mockInterface.createHtmlFormWithButtons();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getPageHeader();
                    will(returnValue(HTML_PAGE_HEADER));
                    one(mockInterface).createHtmlFormWithButtons();
                    will(returnValue(""));
                }
            });

            String result = mockGenerator.createHtmlMainContent();
            assertStringFound(result, "^<div class=\"" + SelectionPageGenerator.HTML_CLASS_MAIN_CONTENT + ".+</div>\\n?$", "Result should be a valid HTML div.", Pattern.DOTALL);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createHtmlFormWithButtons **************************************/

    @Test
    public void test_createHtmlFormWithButtons_unsetTargetUrlAndRequestMethod() {
        try {
            // HTML should be successfully created, but form should not contain any buttons since there are no configs
            String result = generator.createHtmlFormWithButtons();
            assertStringFound(result, "^" + FORM_REGEX + "\\n?$", "Result should be a valid HTML form.", Pattern.DOTALL);
            assertStringFound(result, createExpectedEmptyHtmlFormRegex("", null), "Result should contain an empty form with empty action URL and null form method.");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createHtmlFormWithButtons_noConfigs_encodeUrl() {
        try {
            final String url = "http://<script>alert(1)</script>";
            String encodedrl = "http://&lt;script&gt;alert(1)&lt;/script&gt;";
            final String method = RandomUtils.getRandomSelection("GET", "POST", "PUT", "DELETE");

            setRequestAndConfigInformationExpectations(null, url, method, new HashMap<String, String[]>());

            // Set appropriate information that will be used to create the form (config info, request URL, and request method)
            generatorMockedSetReqInfo.setRequestAndConfigInformation(request, response, socialTaiRequest);

            // HTML should be successfully created, but form should not contain any buttons since there are no configs
            String result = generatorMockedSetReqInfo.createHtmlFormWithButtons();

            assertStringFound(result, "^" + FORM_REGEX + "\\n?$", "Result should be a valid HTML form.", Pattern.DOTALL);
            assertStringFound(result, createExpectedEmptyHtmlFormRegex(Pattern.quote(encodedrl), method), "Result should contain an empty form with properly encoded URL and form method.");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createHtmlFormWithButtons_noConfigs_withQueryParameters() {
        try {
            String encodedrl = WebUtils.htmlEncode(URL);
            final String method = RandomUtils.getRandomSelection("GET", "POST", "PUT", "DELETE");

            Map<String, String[]> paramMap = new HashMap<String, String[]>();
            String param1 = "abc";
            String param2 = " k e y \r\n\t ";
            String param1Value1 = "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/? ";
            String param1Value2 = "test";
            String param2Value = "Some? other value.";
            paramMap.put(param1, new String[] { param1Value1, param1Value2 });
            paramMap.put(param2, new String[] { param2Value });

            setRequestAndConfigInformationExpectations(null, URL, method, paramMap);

            // Set appropriate information that will be used to create the form (config info, request URL, and request method)
            generatorMockedSetReqInfo.setRequestAndConfigInformation(request, response, socialTaiRequest);

            // HTML should be successfully created, but form should not contain any buttons since there are no configs
            String result = generatorMockedSetReqInfo.createHtmlFormWithButtons();

            assertStringFound(result, "^" + FORM_REGEX + "\\n?$", "Result should be a valid HTML form.", Pattern.DOTALL);
            assertStringFound(result, createExpectedHtmlFormRegex(Pattern.quote(encodedrl), method), "Result should contain a form with properly encoded URL and form method.", Pattern.DOTALL);
            assertStringFound(result, createExpectedHtmlHiddenInputRegex(Pattern.quote(WebUtils.htmlEncode(param1, false, true, true)), Pattern.quote(WebUtils.htmlEncode(param1Value1, false, true, true))), "Result should contain a hidden input with properly encoded name and value attributes.");
            assertStringFound(result, createExpectedHtmlHiddenInputRegex(Pattern.quote(WebUtils.htmlEncode(param2, false, true, true)), Pattern.quote(WebUtils.htmlEncode(param2Value, false, true, true))), "Result should contain a hidden input with properly encoded name and value attributes.");
            // Make sure additional values for parameter also appear as hidden inputs
            assertStringFound(result, createExpectedHtmlHiddenInputRegex(Pattern.quote(WebUtils.htmlEncode(param1, false, true, true)), Pattern.quote(WebUtils.htmlEncode(param1Value2, false, true, true))), "Result should contain a hidden input with properly encoded name and value attributes.");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createHtmlFormWithButtons_withConfigs() {
        try {
            SelectionPageGenerator mockGenerator = new MockSelectionPageGenerator() {
                @Override
                String createButtonHtml(SocialLoginConfig config) {
                    return mockInterface.createButtonHtml();
                }
            };
            final List<SocialLoginConfig> configs = Arrays.asList(config1, config2);
            final String method = RandomUtils.getRandomSelection("GET", "POST", "PUT", "DELETE");
            final String config1Html = "<button>config1</button>";
            final String config2Html = "<button>config2</button>";

            setRequestAndConfigInformationExpectations(configs, URL, method, PARAM_MAP);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createButtonHtml();
                    will(returnValue(config1Html));
                    one(mockInterface).createButtonHtml();
                    will(returnValue(config2Html));
                }
            });
            // Set appropriate information that will be used to create the form (config info, request URL, and request method)
            mockGenerator.setRequestAndConfigInformation(request, response, socialTaiRequest);

            String result = mockGenerator.createHtmlFormWithButtons();

            assertStringFound(result, "^" + FORM_REGEX + "\\n?$", "Result should be a valid HTML form.", Pattern.DOTALL);
            assertStringFound(result, createExpectedHtmlFormRegex(URL, method), "Result should contain a form with the action URL [" + URL + "] and method [" + method + "].", Pattern.DOTALL);
            assertStringFound(result, config1Html, "Result should contain the HTML button for the first provided config.");
            assertStringFound(result, config2Html, "Result should contain the HTML button for the second provided config.");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createButtonHtml **************************************/

    @Test
    public void test_createButtonHtml_nullArg() {
        try {
            String result = generator.createButtonHtml(null);
            assertEquals("Result for null argument should be an empty string.", "", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createButtonHtml_nullValues() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                String getObscuredConfigId(String configId) {
                    return mockInterface.getObscuredConfigId();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(null));
                    one(config).getDisplayName();
                    will(returnValue(null));
                    one(mockInterface).getObscuredConfigId();
                    will(returnValue(null));
                }
            });

            String result = mockGenerator.createButtonHtml(config);

            // null values will be encoded to empty strings
            String buttonRegex = createExpectedButtonHtmlRegex("", "");
            assertStringFound(result, "^" + buttonRegex + "$", "Button regex did not match.");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createButtonHtml_missingDisplayName() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                String getObscuredConfigId(String configId) {
                    return mockInterface.getObscuredConfigId();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(CONFIG_1_ID));
                    one(config).getDisplayName();
                    will(returnValue(null));
                    one(mockInterface).getObscuredConfigId();
                    will(returnValue(CONFIG_1_ID));
                }
            });

            String result = mockGenerator.createButtonHtml(config);

            // If the displayName is missing, the id attribute should be used
            String buttonRegex = createExpectedButtonHtmlRegex(CONFIG_1_ID, CONFIG_1_ID);
            assertStringFound(result, "^" + buttonRegex + "$", "Button regex did not match.");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createButtonHtml_validValues() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                String getObscuredConfigId(String configId) {
                    return mockInterface.getObscuredConfigId();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(CONFIG_1_ID));
                    one(config).getDisplayName();
                    will(returnValue(CONFIG_1_DISPLAY_NAME));
                    one(mockInterface).getObscuredConfigId();
                    will(returnValue(CONFIG_1_ID));
                }
            });

            String result = mockGenerator.createButtonHtml(config);

            String buttonRegex = createExpectedButtonHtmlRegex(CONFIG_1_ID, CONFIG_1_DISPLAY_NAME);
            assertStringFound(result, "^" + buttonRegex + "$", "Button regex did not match.");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createButtonHtml_encodeChars() {
        try {
            SelectionPageGenerator mockGenerator = new SelectionPageGenerator() {
                @Override
                String getObscuredConfigId(String configId) {
                    return mockInterface.getObscuredConfigId();
                }
            };
            final String idWithSpecialChars = "Config & 'ID'";
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(idWithSpecialChars));
                    one(config).getDisplayName();
                    will(returnValue("<Complex 'name' \"with\" quotes>"));
                    one(mockInterface).getObscuredConfigId();
                    will(returnValue(idWithSpecialChars));
                }
            });

            String result = mockGenerator.createButtonHtml(config);

            String buttonRegex = createExpectedButtonHtmlRegex("Config &amp; 'ID'", "&lt;Complex 'name' &quot;with&quot; quotes&gt;");
            assertStringFound(result, "^" + buttonRegex + "$", "Button regex did not match.");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** sendDisplayError **************************************/

    @Test
    public void test_sendDisplayError_noMsgKey() {
        try {
            handleErrorExpectations();

            generator.sendDisplayError(response, RandomUtils.getRandomSelection(null, ""));

            String logMsg = CWWKS5429E_ERROR_DISPLAYING_SIGN_IN_PAGE;
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_sendDisplayError_msgKeyNotInCatalog() {
        try {
            handleErrorExpectations();

            String errorMsg = "This is some error message.";
            generator.sendDisplayError(response, errorMsg);

            String logMsg = CWWKS5429E_ERROR_DISPLAYING_SIGN_IN_PAGE + ".+" + Pattern.quote(errorMsg);
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_sendDisplayError_msgKeyInCatalog() {
        try {
            handleErrorExpectations();

            generator.sendDisplayError(response, "SIGN_IN_MISSING_EXPECTED_CONFIG", new Object[] { uniqueId });

            String logMsg = CWWKS5429E_ERROR_DISPLAYING_SIGN_IN_PAGE + ".+" + CWWKS5428W_SIGN_IN_MISSING_EXPECTED_CONFIG + ".+\\[" + uniqueId + "\\]";
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createCssContentString **************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link SelectionPageGenerator#createCssContentString()}
     * </ul>
     */
    @Test
    public void test_createCssContentString() {
        try {
            // Don't care what the CSS content is, just check that it's enclosed in <style> HTML tags
            String result = generator.createCssContentString();
            assertStringFound(result, "^<style>.+</style>$", "CSS content was not enclosed in <style> tags.", Pattern.DOTALL);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** Helper methods ****************************************/

    private String createExpectedEmptyHtmlFormRegex(String actionUrl, String method) {
        StringBuilder regex = new StringBuilder();
        regex.append(Pattern.quote("<form action=\"") + actionUrl + Pattern.quote("\" method=\"") + method + Pattern.quote("\">") + "\\n?" + Pattern.quote("</form>") + "\\n?");
        return regex.toString();
    }

    private String createExpectedHtmlFormRegex(String actionUrl, String method) {
        StringBuilder regex = new StringBuilder();
        regex.append(Pattern.quote("<form action=\"") + actionUrl + Pattern.quote("\" method=\"") + method + Pattern.quote("\">"));
        regex.append(".+");
        regex.append("</form>\\n?");
        return regex.toString();
    }

    private String createExpectedButtonHtmlRegex(String uniqueId, String displayName) {
        StringBuilder regex = new StringBuilder();
        regex.append("<button[ ]+[^>]*");
        regex.append("type=\"submit\"[ ]+[^>]*");
        regex.append("value=\"" + uniqueId + "\"[ ]*[^>]*");
        regex.append(">" + displayName + "</button>");
        return regex.toString();
    }

    String createExpectedHtmlHiddenInputRegex(String name, String value) {
        StringBuilder regex = new StringBuilder();
        regex.append(Pattern.quote("<input type=\"hidden\" name=\"") + name + Pattern.quote("\" value=\"") + value + Pattern.quote("\" >"));
        return regex.toString();
    }

    void setRequestAndConfigInformationExpectations(final Collection<SocialLoginConfig> configs, final String url, final String method, final Map<String, String[]> paramMap) {
        mockery.checking(new Expectations() {
            {
                one(mockInterface).getSocialLoginConfigs();
                will(returnValue(configs));
                one(mockInterface).getRequestUrl();
                will(returnValue(url));
                one(request).getMethod();
                will(returnValue(method));
                one(request).getParameterMap();
                will(returnValue(paramMap));
                one(mockInterface).saveRequestUrlAndParametersForLocalAuthentication();
            }
        });
    }

    /**
     * Looks for the provided regex within the provided string. If the regex isn't found, the provided failure message is included
     * in the failed assertion. Any flags that are provided should be flags provided by the {@code java.util.regex.Pattern} class
     * to use in compiling the regex.
     *
     * @param searchIn
     * @param searchForRegex
     * @param failureMsg
     * @param flags
     */
    private void assertStringFound(String searchIn, String searchForRegex, String failureMsg, int... flags) {
        assertStringIsOrIsNotFound(true, searchIn, searchForRegex, failureMsg, flags);
    }

    /**
     * Looks for the provided regex within the provided string. If the regex isn't found, the provided failure message is included
     * in the failed assertion. Any flags that are provided should be flags provided by the {@code java.util.regex.Pattern} class
     * to use in compiling the regex.
     *
     * @param expected
     *            If true, the regex is expected to be found. Otherwise the regex is not expected to be found.
     * @param searchIn
     * @param searchForRegex
     * @param failureMsg
     * @param flags
     */
    private void assertStringIsOrIsNotFound(boolean expected, String searchIn, String searchForRegex, String failureMsg, int... flags) {
        Pattern p;
        if (flags != null && flags.length > 0) {
            // OR all flags together into a single int value
            int ordFlags = 0;
            for (int i = 0; i < flags.length; i++) {
                ordFlags = ordFlags | flags[i];
            }
            p = Pattern.compile(searchForRegex, ordFlags);
        } else {
            p = Pattern.compile(searchForRegex);
        }
        Matcher m = p.matcher(searchIn);
        if (expected) {
            assertTrue(failureMsg + " Did not find pattern [" + searchForRegex + "] in provided string [" + searchIn + "]", m.find());
        } else {
            assertFalse(failureMsg + " Found pattern [" + searchForRegex + "] in provided string [" + searchIn + "] but should not have.", m.find());
        }
    }

}
