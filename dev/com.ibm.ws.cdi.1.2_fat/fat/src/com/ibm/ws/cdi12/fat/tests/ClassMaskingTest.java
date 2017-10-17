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

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This test ensures that if a class in one module is masked by a class in another module, CDI doesn't break.
 * <p>
 * The test was introduced because CDI was assuming that all classes present in a module should be loaded by that modules classloader. However, they could be loaded from another
 * module which is on the application classpath.
 * <p>
 * We also test that beans in an App Client jar are not visible to other modules.
 */

public class ClassMaskingTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapServer server = new ShrinkWrapServer("cdi12ClassMasking");

    @Override
    protected ShrinkWrapServer getSharedServer() {
        return server;
    }

    //@Mode(TestMode.FULL)
    @Test
    public void testClassMasking() throws Exception {
        LibertyServer lServer = server.getLibertyServer();
        lServer.useSecondaryHTTPPort();

        HttpUtils.findStringInUrl(lServer, "/maskedClassWeb/TestServlet",
                                  "This is Type3, a managed bean in the war",
                                  "This is TestBean in the war");
    }
}
