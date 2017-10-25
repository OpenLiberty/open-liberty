/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test Method parameter injection.
 * <p>
 * We had an issue where method parameters were sometimes injected in the wrong order.
 * <p>
 * This test tests method parameter injection on a servlet, an EJB and a CDI bean.
 */
@Mode(TestMode.FULL)
public class InjectParameterTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12EJB32Server");

    private static String EXPECTED_RESPONSE = "test1, test2, test3, test4, test5, test6, test7, test8, test9, test10, test11, test12, test13, test14, test15, test16";

    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testServletParameterInjection() throws Exception {
        verifyResponse("/injectParameters/TestServlet", EXPECTED_RESPONSE);
    }

    @Test
    public void testEjbParameterInjection() throws Exception {
        verifyResponse("/injectParameters/TestEjb", EXPECTED_RESPONSE);
    }

    @Test
    public void testCdiBeanParameterInjection() throws Exception {
        verifyResponse("/injectParameters/TestCdiBean", EXPECTED_RESPONSE);
    }
}
