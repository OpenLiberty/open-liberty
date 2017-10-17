package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;

/**
 * Tests for CDI from shared libraries
 */

public class SharedLibraryTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapServer SHARED_SERVER = new ShrinkWrapServer("cdi12SharedLibraryServer");

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
    public void testSharedLibraryNoInjection() throws Exception {
        // Sanity check test, tests that the shared library exists and is available without CDI being involved
        this.verifyResponse("/sharedLibraryNoInjectionApp/noinjection",
                            "Hello from shared library class? :Hello from a non injected class name: Iain");

    }

    @Test
    public void testSharedLibraryWithCDI() throws Exception {

        // Now with CDI
        this.verifyResponse("/sharedLibraryAppWeb1/",
                            "Can i get to HelloC? :Hello from an InjectedHello, I am here: Iain");

    }

}
