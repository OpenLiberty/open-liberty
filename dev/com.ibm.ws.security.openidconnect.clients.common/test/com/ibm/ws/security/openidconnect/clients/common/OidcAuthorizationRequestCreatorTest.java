/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import test.common.SharedOutputManager;

public class OidcAuthorizationRequestCreatorTest {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String TEST_URL = "http://harmonic.austin.ibm.com:8010/formlogin/SimpleServlet";

    private final HttpServletRequest request = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock.mock(HttpServletResponse.class);
    private final WebAppSecurityConfig webAppSecConfig = mock.mock(WebAppSecurityConfig.class);
    private final ConvergedClientConfig convClientConfig = mock.mock(ConvergedClientConfig.class);

    private OidcAuthorizationRequestCreator creator;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
        creator = new OidcAuthorizationRequestCreator(request, response, convClientConfig);
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
            String query = "";
            final List<String> configuredValue = null;
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                }
            });
            String newQuery = creator.addForwardLoginParamsToQuery(query);
            assertEquals("Returned query should have matched original query.", query, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_forwardLoginParametersEmpty() {
        try {
            String query = "The quick brown fox jumps over the lazy dog.";
            final List<String> configuredValue = new ArrayList<String>();
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                }
            });
            String newQuery = creator.addForwardLoginParamsToQuery(query);
            assertEquals("Returned query should have matched original query.", query, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_emptyString_requestMissingThatParameter() {
        try {
            String query = "scope=myScope";
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
            String newQuery = creator.addForwardLoginParamsToQuery(query);
            assertEquals("Returned query should have matched original query.", query, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_emptyString_matchingParam_emptyString() {
        try {
            String query = "some existing query string";
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
            String newQuery = creator.addForwardLoginParamsToQuery(query);

            String expectedQuery = query + "&=";
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_emptyString_matchingParam_whitespaceOnly() {
        try {
            String query = "some existing query string";
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
            String newQuery = creator.addForwardLoginParamsToQuery(query);

            // Parameter value should have been encoded
            String expectedQuery = query + "&=+%09%0A+%0D";
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_emptyString_matchingParam_nonEmpty() {
        try {
            String query = "some existing query string";
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
            String newQuery = creator.addForwardLoginParamsToQuery(query);

            String expectedQuery = query + "&=" + paramValue;
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_whitespace_requestMissingThatParameter() {
        try {
            String query = "some existing query string";
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
            String newQuery = creator.addForwardLoginParamsToQuery(query);
            assertEquals("Returned query should have matched original query.", query, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_whitespace_matchingParam_whitespaceOnly() {
        try {
            String query = "some existing query string";
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
            String newQuery = creator.addForwardLoginParamsToQuery(query);

            // Parameter name and value should have been encoded
            String expectedQuery = query + "&" + "%0A%0D%09" + "=" + "++++";
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_whitespace_matchingParam_nonEmpty() {
        try {
            String query = "some existing query string";
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
            String newQuery = creator.addForwardLoginParamsToQuery(query);

            // Parameter name and value should have been encoded
            String expectedQuery = query + "&" + "%0A+%0A" + "=" + "some+parameter+value";
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_nonEmpty_requestMissingThatParameter() {
        try {
            String query = "scope=mySCope";
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
            String newQuery = creator.addForwardLoginParamsToQuery(query);
            assertEquals("Returned query should have matched original query.", query, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_oneParameter_specialChars_matchingParam_specialChars() {
        try {
            String query = "scope=myScope&redirect_uri=some value";
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
            String newQuery = creator.addForwardLoginParamsToQuery(query);

            String encodedSpecialChars = "%60%7E%21%40%23%24%25%5E%26*%28%29-_%3D%2B%5B%7B%5D%7D%5C%7C%3B%3A%27%22%2C%3C.%3E%2F%3F";
            // Parameter name and value should have been encoded
            String expectedQuery = query + "&" + encodedSpecialChars + "=" + encodedSpecialChars;
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_multipleParameters_noneInRequest() {
        try {
            String query = "initial query";
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
            String newQuery = creator.addForwardLoginParamsToQuery(query);
            assertEquals("Returned query should have matched original query.", query, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_multipleParameters_oneInRequest() {
        try {
            String query = "initial query";
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
            String newQuery = creator.addForwardLoginParamsToQuery(query);

            // Parameter name and value should have been encoded
            String expectedQuery = query + "&" + "my+param" + "=" + "My%0AParam%0DValue";
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_multipleParameters_multipleInRequest() {
        try {
            String query = "initial query";
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
            String newQuery = creator.addForwardLoginParamsToQuery(query);

            // Parameter names and values should have been encoded
            String expectedQuery = query + "&" + "my+param" + "=" + "My%0AParam%0DValue" + "&" + "+1234567890+" + "=" + foundParamValue2;
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addForwardLoginParamsToQuery_multipleParameters_allInRequest() {
        try {
            String query = "initial query";
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
            String newQuery = creator.addForwardLoginParamsToQuery(query);

            // Parameter names and values should have been encoded
            String expectedQuery = query + "&" + paramName1 + "=" + paramValue1 + "&" + paramName2 + "=" + paramValue2 + "&" + paramName3 + "=" + paramValue3 + "&" + paramName4 + "=" + paramValue4;
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_appendParameterToQuery_nameNull() {
        String query = "start";
        String parameterName = null;
        String parameterValue = "value";

        String result = creator.appendParameterToQuery(query, parameterName, parameterValue);
        assertEquals("Query string should not have been modified.", query, result);
    }

    @Test
    public void test_appendParameterToQuery_nameEmpty() {
        String query = "start";
        String parameterName = "";
        String parameterValue = "value";

        String result = creator.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = query + "&" + parameterName + "=" + parameterValue;
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_nameContainsSpecialCharacters() {
        String query = "start";
        String parameterName = "special=&,?/:chars";
        String parameterValue = "value";

        String result = creator.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = query + "&" + "special%3D%26%2C%3F%2F%3Achars" + "=" + parameterValue;
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_valueNull() {
        String query = "start";
        String parameterName = "name";
        String parameterValue = null;

        String result = creator.appendParameterToQuery(query, parameterName, parameterValue);
        assertEquals("Query string should not have been modified.", query, result);
    }

    @Test
    public void test_appendParameterToQuery_valueEmpty() {
        String query = "start";
        String parameterName = "name";
        String parameterValue = "";

        String result = creator.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = query + "&" + parameterName + "=" + parameterValue;
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_valueContainsSpecialCharacters() {
        String query = "start";
        String parameterName = "name";
        String parameterValue = "special=&,?/:chars";

        String result = creator.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = query + "&" + parameterName + "=" + "special%3D%26%2C%3F%2F%3Achars";
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_queryNull() {
        String query = null;
        String parameterName = "name";
        String parameterValue = "value";

        String result = creator.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = parameterName + "=" + parameterValue;
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_queryEmpty() {
        String query = "";
        String parameterName = "name";
        String parameterValue = "value";

        String result = creator.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = parameterName + "=" + parameterValue;
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_queryNotEmpty() {
        String query = "start";
        String parameterName = "name";
        String parameterValue = "value";

        String result = creator.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = query + "&" + parameterName + "=" + parameterValue;
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_queryContainsOtherParameters() {
        String query = "value=1&other_value=some+other+value";
        String parameterName = "name";
        String parameterValue = "value";

        String result = creator.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = query + "&" + parameterName + "=" + parameterValue;
        assertEquals(expected, result);
    }

    @Test
    public void test_getReqUrlNull() {
        try {
            createReqUrlExpectations(null);
            String strUrl = creator.getReqURL();

            assertEquals("The URL must not contain a query string.", TEST_URL, strUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getReqUrlQuery() {
        try {
            final String query = "response_type=code";
            createReqUrlExpectations(query);
            String strUrl = creator.getReqURL();
            String expect = TEST_URL + "?" + query;

            assertEquals("The URL must contain the query string.", expect, strUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getReqUrlQuery_withSpecialCharacters() {
        try {
            String value = "code>\"><script>alert(100)</script>";
            final String query = "response_type=" + value;
            createReqUrlExpectations(query);
            String strUrl = creator.getReqURL();
            String expect = TEST_URL + "?response_type=" + value;

            assertEquals("The URL must contain the unencoded query string.", expect, strUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private void createReqUrlExpectations(final String queryString) {
        mock.checking(new Expectations() {
            {
                allowing(request).getScheme();
                will(returnValue("https"));
                one(request).getServerPort();
                will(returnValue(8020));
                one(request).getRequestURL();
                will(returnValue(new StringBuffer(TEST_URL)));
                one(request).getQueryString();
                will(returnValue(queryString));
            }
        });
    }

}
