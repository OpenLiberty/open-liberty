/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.slo;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;

import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class SLOMessageContextUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.saml.*=all");

    @Rule
    public TestRule managerRule = outputMgr;

    SLOMessageContextUtils utils = null;

    final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    final PrintWriter writer = mockery.mock(PrintWriter.class);
    final BasicMessageContext<?, ?, ?> msgCtx = mockery.mock(BasicMessageContext.class);
    final Status sloResponseStatus = mockery.mock(Status.class);
    final StatusCode sloStatusCode = mockery.mock(StatusCode.class);

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        utils = new SLOMessageContextUtils(msgCtx);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    /************************************** getSloStatusCode **************************************/

    /**
     * Tests:
     * - Utils instantiated with null message context
     * Expects:
     * - Result should match the UNKNOWN status string
     */
    @Test
    public void test_getSloStatusCode_nullMessageContext() {
        utils = new SLOMessageContextUtils(null);
        String result = utils.getSloStatusCode();
        String expectedStatus = SLOMessageContextUtils.STATUS_UNKNOWN;
        assertEquals("Result did not match the expected status.", expectedStatus, result);
    }

    /**
     * Tests:
     * - SLO response status is null
     * Expects:
     * - Result should match the UNKNOWN status string
     */
    @Test
    public void test_getSloStatusCode_nullResponseStatus() {
        mockery.checking(new Expectations() {
            {
                one(msgCtx).getSLOResponseStatus();
                will(returnValue(null));
            }
        });
        String result = utils.getSloStatusCode();
        String expectedStatus = SLOMessageContextUtils.STATUS_UNKNOWN;
        assertEquals("Result did not match the expected status.", expectedStatus, result);
    }

    /**
     * Tests:
     * - SLO response status code is null
     * Expects:
     * - Result should match the UNKNOWN status string
     */
    @Test
    public void test_getSloStatusCode_nullStatusCode() {
        mockery.checking(new Expectations() {
            {
                one(msgCtx).getSLOResponseStatus();
                will(returnValue(sloResponseStatus));
                one(sloResponseStatus).getStatusCode();
                will(returnValue(null));
            }
        });
        String result = utils.getSloStatusCode();
        String expectedStatus = SLOMessageContextUtils.STATUS_UNKNOWN;
        assertEquals("Result did not match the expected status.", expectedStatus, result);
    }

    /**
     * Tests:
     * - SLO response status code value is null
     * Expects:
     * - Result should match the UNKNOWN status string
     */
    @Test
    public void test_getSloStatusCode_nullStatusValue() {
        final String statusValue = null;
        setSloResponseStatusExpectations(statusValue);

        String result = utils.getSloStatusCode();
        String expectedStatus = SLOMessageContextUtils.STATUS_UNKNOWN;
        assertEquals("Result did not match the expected status.", expectedStatus, result);
    }

    /**
     * Tests:
     * - SLO response status code value is empty string
     * Expects:
     * - Result should match the status string
     */
    @Test
    public void test_getSloStatusCode_emptyStatusValue() {
        final String statusValue = "";
        setSloResponseStatusExpectations(statusValue);

        String result = utils.getSloStatusCode();
        assertEquals("Result did not match the expected status.", statusValue, result);
    }

    /**
     * Tests:
     * - SLO response status code value is some non-empty string
     * Expects:
     * - Result should match the status string
     */
    @Test
    public void test_getSloStatusCode_nonEmptyStatusValue() {
        final String statusValue = "some non-empty value";
        setSloResponseStatusExpectations(statusValue);

        String result = utils.getSloStatusCode();
        assertEquals("Result did not match the expected status.", statusValue, result);
    }

    /**
     * Tests:
     * - SLO response status code value is success string
     * Expects:
     * - Result should contain the status string
     */
    @Test
    public void test_getSloStatusCode_successStatusValue() {
        final String statusValue = "urn:oasis:names:tc:SAML:2.0:status:Success";
        setSloResponseStatusExpectations(statusValue);

        String result = utils.getSloStatusCode();
        assertEquals("Result did not match the expected status.", statusValue, result);
    }

    /**
     * Tests:
     * - SLO response status code value is a failure status string
     * Expects:
     * - Result should contain the status string
     */
    @Test
    public void test_getSloStatusCode_errorStatusValue() {
        final String statusValue = "urn:oasis:names:tc:SAML:2.0:status:Requester";
        setSloResponseStatusExpectations(statusValue);

        String result = utils.getSloStatusCode();
        assertEquals("Result did not match the expected status.", statusValue, result);
    }

    /************************************** Helper methods **************************************/

    private void setSloResponseStatusExpectations(final String status) {
        mockery.checking(new Expectations() {
            {
                one(msgCtx).getSLOResponseStatus();
                will(returnValue(sloResponseStatus));
                one(sloResponseStatus).getStatusCode();
                will(returnValue(sloStatusCode));
                one(sloStatusCode).getValue();
                will(returnValue(status));
            }
        });
    }
}
