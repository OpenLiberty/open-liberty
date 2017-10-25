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

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
public class ClassLoadPrereqLogger extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12BasicServer");

    /** {@inheritDoc} */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        // TODO Auto-generated method stub
        return SHARED_SERVER;
    }

    @Test
    @Mode(TestMode.FULL)
    public void testAfterTypeDecoratorAddition() throws Exception {
        verifyResponse("/TestClassLoadPrereqLogger/testservlet", "Test passed");
    }

}
