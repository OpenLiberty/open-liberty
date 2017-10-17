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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

public class SimpleJSFWithSharedLibTest extends LoggingTest {

    public static LibertyServer server;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getStartedLibertyServer("cdi12JSFWithSharedLibServer");
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application simpleJSFWithSharedLib started");
    }

    @Test
    public void testSimpleJSFWithSharedLib() throws Exception {
        HttpUtils.findStringInUrl(server, "/simpleJSFWithSharedLib/faces/testBasicJsf.xhtml",
                                  "SimpleJsfBean injected with: Hello from an InjectedHello, I am here: SimpleJsfBean");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }
}
