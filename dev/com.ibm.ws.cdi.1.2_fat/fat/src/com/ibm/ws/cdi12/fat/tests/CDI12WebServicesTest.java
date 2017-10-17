package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

public class CDI12WebServicesTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapServer SHARED_SERVER = new ShrinkWrapServer("cdi12WebServicesServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapServer getSharedServer() {
        return SHARED_SERVER;
    }

    /**
     * Verifies that a @Resource can be injected and used in a WebService that resides in a CDI implicit bean archive
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testResourceInjectionForWSinImplicitBDA() throws Exception {
        verifyResponse("/resourceWebServicesClient/TestWebServicesServlet", "Hello, Bobby from mySecondName in SayHelloPojoService");
    }

}
