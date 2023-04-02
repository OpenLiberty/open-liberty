/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequestParameters;
import test.common.SharedOutputManager;

public class OidcAuthorizationRequestTest {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final String authorizationEndpointUrl = "https://localhost:8020/oidc/op/authorize";
    private final String scope = "openid";
    private final String responseType = "code";
    private final String clientId = "oidcClientId";
    private final String redirectUri = "https://localhost:9020/oidc/rp/callback";
    private final String state = "statevalue";

    private final HttpServletRequest request = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock.mock(HttpServletResponse.class);
    private final WebAppSecurityConfig webAppSecConfig = mock.mock(WebAppSecurityConfig.class);
    private final ConvergedClientConfig convClientConfig = mock.mock(ConvergedClientConfig.class);
    private final ReferrerURLCookieHandler referrerURLCookieHandler = mock.mock(ReferrerURLCookieHandler.class);

    private OidcAuthorizationRequest oidcAuthzReq;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
        mock.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(clientId));
                one(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(referrerURLCookieHandler));
            }
        });
        oidcAuthzReq = new OidcAuthorizationRequest(request, response, convClientConfig);
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
        mock.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_addForwardLoginParamsToQuery_forwardLoginParametersNull() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();
            final List<String> configuredValue = null;
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                }
            });
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();
            assertEquals("Returned query should have matched original query.", originalUrl, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_forwardLoginParametersEmpty() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();
            final List<String> configuredValue = new ArrayList<String>();
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                }
            });
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();
            assertEquals("Returned query should have matched original query.", originalUrl, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_emptyString_requestMissingThatParameter() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();
            final String paramName = "";
            final List<String> configuredValue = Arrays.asList(paramName);
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(request).getParameter(paramName);
                    will(returnValue(null));
                }
            });
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();
            assertEquals("Returned query should have matched original query.", originalUrl, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_emptyString_matchingParam_emptyString() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();
            final String paramName = "";
            final List<String> configuredValue = Arrays.asList(paramName);
            final String paramValue = "";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(request).getParameter(paramName);
                    will(returnValue(paramValue));
                }
            });
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();

            String expectedQuery = originalUrl + "&=";
            assertEquals("Returned query did not match expected value.", expectedQuery, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_emptyString_matchingParam_whitespaceOnly() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();
            final String paramName = "";
            final List<String> configuredValue = Arrays.asList(paramName);
            final String paramValue = " \t\n \r";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(request).getParameter(paramName);
                    will(returnValue(paramValue));
                }
            });
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();

            // Parameter value should have been encoded
            String expectedQuery = originalUrl + "&=+%09%0A+%0D";
            assertEquals("Returned query did not match expected value.", expectedQuery, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_emptyString_matchingParam_nonEmpty() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();
            final String paramName = "";
            final List<String> configuredValue = Arrays.asList(paramName);
            final String paramValue = "some_simple_param_value";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(request).getParameter(paramName);
                    will(returnValue(paramValue));
                }
            });
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();

            String expectedQuery = originalUrl + "&=" + paramValue;
            assertEquals("Returned query did not match expected value.", expectedQuery, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_whitespace_requestMissingThatParameter() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();
            final String paramName = " ";
            final List<String> configuredValue = Arrays.asList(paramName);
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(request).getParameter(paramName);
                    will(returnValue(null));
                }
            });
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();

            assertEquals("Returned query should have matched original query.", originalUrl, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_whitespace_matchingParam_whitespaceOnly() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();
            final String paramName = "\n\r\t";
            final List<String> configuredValue = Arrays.asList(paramName);
            final String paramValue = "    ";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(request).getParameter(paramName);
                    will(returnValue(paramValue));
                }
            });
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();

            // Parameter name and value should have been encoded
            String expectedQuery = originalUrl + "&" + "%0A%0D%09" + "=" + "++++";
            assertEquals("Returned query did not match expected value.", expectedQuery, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_whitespace_matchingParam_nonEmpty() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();
            final String paramName = "\n \n";
            final List<String> configuredValue = Arrays.asList(paramName);
            final String paramValue = "some parameter value";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(request).getParameter(paramName);
                    will(returnValue(paramValue));
                }
            });
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();

            // Parameter name and value should have been encoded
            String expectedQuery = originalUrl + "&" + "%0A+%0A" + "=" + "some+parameter+value";
            assertEquals("Returned query did not match expected value.", expectedQuery, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_nonEmpty_requestMissingThatParameter() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();
            final String paramName = "missingParam";
            final List<String> configuredValue = Arrays.asList(paramName);
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(request).getParameter(paramName);
                    will(returnValue(null));
                }
            });
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();

            assertEquals("Returned query should have matched original query.", originalUrl, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_specialChars_matchingParam_specialChars() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();
            final String paramName = "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?";
            final List<String> configuredValue = Arrays.asList(paramName);
            final String paramValue = paramName;
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(request).getParameter(paramName);
                    will(returnValue(paramValue));
                }
            });
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();

            String encodedSpecialChars = "%60%7E%21%40%23%24%25%5E%26*%28%29-_%3D%2B%5B%7B%5D%7D%5C%7C%3B%3A%27%22%2C%3C.%3E%2F%3F";
            // Parameter name and value should have been encoded
            String expectedQuery = originalUrl + "&" + encodedSpecialChars + "=" + encodedSpecialChars;
            assertEquals("Returned query did not match expected value.", expectedQuery, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_multipleParameters_noneInRequest() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();
            final List<String> configuredValues = Arrays.asList("", "my param", "Special! \n\t (Param) ", " 1234567890 ");
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValues));
                }
            });
            for (final String configuredVal : configuredValues) {
                mock.checking(new Expectations() {
                    {
                        one(request).getParameter(configuredVal);
                        will(returnValue(null));
                    }
                });
            }
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();

            assertEquals("Returned query should have matched original query.", originalUrl, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_multipleParameters_oneInRequest() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();

            final String emptyParam = "";
            final String paramWithSpace = "my param";
            final String paramWithSpecialChars = "Special! \n\t (Param) ";
            final String paramWithNumbers = " 1234567890 ";
            final List<String> configuredValues = Arrays.asList(emptyParam, paramWithSpace, paramWithSpecialChars, paramWithNumbers);
            final String foundParamValue = "My\nParam\rValue";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValues));
                    one(request).getParameter(emptyParam);
                    will(returnValue(null));
                    // The request happens to have this parameter
                    one(request).getParameter(paramWithSpace);
                    will(returnValue(foundParamValue));
                    one(request).getParameter(paramWithSpecialChars);
                    will(returnValue(null));
                    one(request).getParameter(paramWithNumbers);
                    will(returnValue(null));
                }
            });
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();

            // Parameter name and value should have been encoded
            String expectedQuery = originalUrl + "&" + "my+param" + "=" + "My%0AParam%0DValue";
            assertEquals("Returned query did not match expected value.", expectedQuery, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_multipleParameters_multipleInRequest() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();

            final String emptyParam = "";
            final String paramWithSpace = "my param";
            final String paramWithSpecialChars = "Special! \n\t (Param) ";
            final String paramWithNumbers = " 1234567890 ";
            final List<String> configuredValues = Arrays.asList(emptyParam, paramWithSpace, paramWithSpecialChars, paramWithNumbers);
            final String foundParamValue1 = "My\nParam\rValue";
            final String foundParamValue2 = "a_simple_param_value";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValues));
                    one(request).getParameter(emptyParam);
                    will(returnValue(null));
                    one(request).getParameter(paramWithSpace);
                    will(returnValue(foundParamValue1));
                    one(request).getParameter(paramWithSpecialChars);
                    will(returnValue(null));
                    one(request).getParameter(paramWithNumbers);
                    will(returnValue(foundParamValue2));
                }
            });
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();

            // Parameter names and values should have been encoded
            String expectedQuery = originalUrl + "&" + "my+param" + "=" + "My%0AParam%0DValue" + "&" + "+1234567890+" + "=" + foundParamValue2;
            assertEquals("Returned query did not match expected value.", expectedQuery, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_multipleParameters_allInRequest() {
        try {
            AuthorizationRequestParameters parameters = createDefaultAuthorizationRequestParameters();
            String originalUrl = parameters.buildRequestUrl();

            final String paramName1 = "name1";
            final String paramName2 = "name2";
            final String paramName3 = "name3";
            final String paramName4 = "name4";
            final String paramValue1 = "value1";
            final String paramValue2 = "value2";
            final String paramValue3 = "value3";
            final String paramValue4 = "value4";
            final List<String> configuredValues = Arrays.asList(paramName1, paramName2, paramName3, paramName4);
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValues));
                    one(request).getParameter(paramName1);
                    will(returnValue(paramValue1));
                    one(request).getParameter(paramName2);
                    will(returnValue(paramValue2));
                    one(request).getParameter(paramName3);
                    will(returnValue(paramValue3));
                    one(request).getParameter(paramName4);
                    will(returnValue(paramValue4));
                }
            });
            oidcAuthzReq.addForwardLoginParams(parameters);
            String newUrl = parameters.buildRequestUrl();

            // Parameter names and values should have been encoded
            String expectedQuery = originalUrl + "&" + paramName1 + "=" + paramValue1 + "&" + paramName2 + "=" + paramValue2 + "&" + paramName3 + "=" + paramValue3 + "&" + paramName4 + "=" + paramValue4;
            assertEquals("Returned query did not match expected value.", expectedQuery, newUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private AuthorizationRequestParameters createDefaultAuthorizationRequestParameters() {
        return new AuthorizationRequestParameters(authorizationEndpointUrl, scope, responseType, clientId, redirectUri, state);
    }

}
