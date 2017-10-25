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

import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * These tests verify that inspecting event meta data works correctly as per http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#event_metadata
 */

@Mode(TestMode.FULL)
public class EventMetaDataTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12EventMetadataServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testDefaultMetaData() throws Exception {
        WebResponse response = SHARED_SERVER.getResponse(createWebBrowserForTestCase(), "/MetaDataTest/");
        String[] splitResponse = response.getResponseBody().split("]");
        String qualifiers = splitResponse[0];
        assertTrue(qualifiers.contains("Default event qualifiers")
                   && qualifiers.contains("@javax.enterprise.inject.Any()")
                   && qualifiers.contains("@javax.enterprise.context.Initialized(value=javax.enterprise.context.RequestScoped.class)"));
        this.verifyResponse("/MetaDataTest/",
                            new String[] { "Default event injection points: null",
                                           "Default event type: interface javax.servlet.http.HttpServletRequest" });
    }

    @Test
    public void testFiredMetaData() throws Exception {
        this.verifyResponse("/MetaDataTest/",
                            new String[] { "Non-default event qualifiers: [@javax.enterprise.inject.Any(), @com.ibm.ws.cdi12.test.MetaQualifier()]",
                                           "Non-default event injection points: [BackedAnnotatedField] @Inject @MetaQualifier",
                                           "Non-default event type: class com.ibm.ws.cdi12.test.MyEvent" });
    }
}
