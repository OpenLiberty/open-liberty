package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;

/**
 * All CDI tests with all applicable server features enabled.
 */
public class MultiModuleAppTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapServer SHARED_SERVER = new ShrinkWrapServer("cdi12BasicServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testMultiModuleApps() throws Exception {
        //part of multiModuleApp1
        this.verifyResponse(
                            "/multiModuleAppWeb1/",
                            "Test Sucessful!");
        //part of multiModuleApp1
        this.verifyResponse(
                            "/multiModuleAppWeb2/",
                            "Test Sucessful!");
        //part of multiModuleApp2
        this.verifyResponse(
                            "/multiModuleAppWeb3/",
                            "Test Sucessful!");
        //part of multiModuleApp2
        this.verifyResponse(
                            "/multiModuleAppWeb4/",
                            "Test Sucessful!");
    }
}
