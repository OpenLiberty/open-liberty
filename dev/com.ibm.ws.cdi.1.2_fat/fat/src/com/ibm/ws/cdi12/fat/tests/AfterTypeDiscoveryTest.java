/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
public class AfterTypeDiscoveryTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapServer SHARED_SERVER = new ShrinkWrapServer("cdi12AfterTypeDiscoveryServer");

    /** {@inheritDoc} */
    @Override
    protected ShrinkWrapServer getSharedServer() {
        // TODO Auto-generated method stub
        return SHARED_SERVER;
    }

    @Test
    public void testAfterTypeDecoratorAddition() throws Exception {
        verifyResponse("/afterTypeDiscovery/", "New msg: decorated");
    }

    @Test
    public void testAfterTypeInterceptorAddition() throws Exception {
        verifyResponse("/afterTypeDiscovery/", "intercepted");
    }

    @Test
    public void testAfterTypeBeanAddition() throws Exception {
        verifyResponse("/afterTypeDiscovery/", "hello world");
    }

    @Test
    public void testAfterTypeAlternativeAddition() throws Exception {
        verifyResponse("/afterTypeDiscovery/", new String[] { "expecting one: alternative one", "expecting two: alternative two" });
    }

}
