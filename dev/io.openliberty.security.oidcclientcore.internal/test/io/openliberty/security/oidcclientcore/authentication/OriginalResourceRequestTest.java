/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.authentication;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.storage.OidcClientStorageConstants;
import io.openliberty.security.oidcclientcore.utils.Utils;
import test.common.SharedOutputManager;

public class OriginalResourceRequestTest extends CommonTestClass {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    // POST
    private static final String METHOD = "UE9TVA==";

    // { h1: [v11, v12], h2: [v2], h3: [3] }
    private static final String HEADERS = "aDM=:Mw==&aDI=:djI=&aDE=:djEx.djEy";

    // { p4: [v41, v42], p5: [v5] }
    private static final String PARAMS = "cDU=:djU=&cDQ=:djQx.djQy";

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private final HttpSession httpSession = mockery.mock(HttpSession.class);

    OriginalResourceRequest originalResourceRequest;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        String state = "1234";
        String stateHash = Utils.getStrHashCode(state);

        mockery.checking(new Expectations() {
            {
                one(request).getParameter("state");
                will(returnValue(state));
                allowing(request).getSession();
                will(returnValue(httpSession));

                one(httpSession).getAttribute(OidcClientStorageConstants.WAS_OIDC_REQ_METHOD + stateHash);
                will(returnValue(METHOD));
                one(httpSession).removeAttribute(OidcClientStorageConstants.WAS_OIDC_REQ_METHOD + stateHash);

                one(httpSession).getAttribute(OidcClientStorageConstants.WAS_OIDC_REQ_HEADERS + stateHash);
                will(returnValue(HEADERS));
                one(httpSession).removeAttribute(OidcClientStorageConstants.WAS_OIDC_REQ_HEADERS + stateHash);

                one(httpSession).getAttribute(OidcClientStorageConstants.WAS_OIDC_REQ_PARAMS + stateHash);
                will(returnValue(PARAMS));
                one(httpSession).removeAttribute(OidcClientStorageConstants.WAS_OIDC_REQ_PARAMS + stateHash);
            }
        });

        originalResourceRequest = new OriginalResourceRequest(request, response, true);
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_getMethod() {
        String method = originalResourceRequest.getMethod();

        assertThat(method, equalTo("POST"));
    }

    @Test
    public void test_getHeader() {
        String header = originalResourceRequest.getHeader("h1");

        assertThat(header, equalTo("v11"));
    }

    @Test
    public void test_getHeader_nameDoesNotExist() {
        String header = originalResourceRequest.getHeader("doesNotExist");

        assertThat(header, is(nullValue()));
    }

    @Test
    public void test_getHeaderNames() {
        List<String> expectedHeaderNames = Arrays.asList("h1", "h2", "h3");

        Enumeration<String> headerNames = originalResourceRequest.getHeaderNames();

        assertThat(Collections.list(headerNames), is(expectedHeaderNames));
    }

    @Test
    public void test_getHeaders() {
        List<String> expectedHeaders = Arrays.asList("v11", "v12");

        Enumeration<String> headers = originalResourceRequest.getHeaders("h1");

        assertThat(Collections.list(headers), is(expectedHeaders));
    }

    @Test
    public void test_getHeaders_nameDoesNotExist() {
        Enumeration<String> headers = originalResourceRequest.getHeaders("doesNotExist");

        assertThat(Collections.list(headers), is(empty()));
    }

    @Test
    public void test_getIntHeader() {
        int intHeader = originalResourceRequest.getIntHeader("h3");

        assertThat(intHeader, equalTo(3));
    }

    @Test
    public void test_getIntHeader_nameDoesNotExist() {
        int intHeader = originalResourceRequest.getIntHeader("doesNotExist");

        assertThat(intHeader, equalTo(-1));
    }

    @Test(expected = NumberFormatException.class)
    public void test_getIntHeader_valueNotInteger() {
        originalResourceRequest.getIntHeader("h1");
    }

    @Test
    public void test_getParameter() {
        String parameter = originalResourceRequest.getParameter("p4");

        assertThat(parameter, equalTo("v41"));
    }

    @Test
    public void test_getParameter_nameDoesNotExist() {
        String parameter = originalResourceRequest.getParameter("doesNotExist");

        assertThat(parameter, is(nullValue()));
    }

    @Test
    public void test_getParameterMap() {
        Map<String, String[]> expectedParameterMap = new HashMap<>();
        expectedParameterMap.put("p4", new String[] { "v41", "v42" });
        expectedParameterMap.put("p5", new String[] { "v5" });

        Map<String, String[]> parameterMap = originalResourceRequest.getParameterMap();

        assertThat(parameterMap.keySet(), is(expectedParameterMap.keySet()));
        assertThat(parameterMap.get("p4"), is(expectedParameterMap.get("p4")));
        assertThat(parameterMap.get("p5"), is(expectedParameterMap.get("p5")));
    }

    @Test
    public void test_getParameterNames() {
        List<String> expectedParameterNames = Arrays.asList("p4", "p5");

        Enumeration<String> parameterNames = originalResourceRequest.getParameterNames();

        assertThat(Collections.list(parameterNames), is(expectedParameterNames));
    }

    @Test
    public void test_getParameterValues() {
        String[] expectedParameterValues = new String[] { "v41", "v42" };

        String[] parameterValues = originalResourceRequest.getParameterValues("p4");

        assertThat(parameterValues, is(expectedParameterValues));
    }

    @Test
    public void test_getParameterValues_nameDoesNotExist() {
        String[] parameterValues = originalResourceRequest.getParameterValues("doesNotExist");

        assertThat(parameterValues, is(nullValue()));
    }

}
