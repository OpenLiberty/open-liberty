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

import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

public class PassivationBeanTests extends LoggingTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("cdi12PassivationServer");

    @Test
    public void testTransientReference() throws Exception {
        //Enable when 158940 is complete.
        this.verifyResponse("/passivaiton/", new String[] { "destroyed-one", "doNothing2destroyed-two" });
        this.verifyResponse("/passivaiton/", new String[] { "destroyed-one" });
    }

    /**
     * Passivation Capability FVT Test Group
     *
     * @throws Exception
     *             if validation fails, or if an unexpected error occurs
     */
    @Test
    public void testTransientReferenceInPassivation() throws Exception {

        //I don't know why adding a shared web browser is nessacary here. It wasn't in the CDI1.0 test I'm copying.
        WebBrowser wb = createWebBrowserForTestCase();

        this.verifyResponse(wb, "/transientReferenceInSessionPersist/PassivationCapability.jsp",
                            new String[] { "Initialized", "PASSED", "MyStatefulSessionBean was destroyed", "injected into PassivatationBean and it has been destroyed" });

        SHARED_SERVER.getApplicationMBean("transientReferenceInSessionPersist").restart();

        this.verifyResponse(wb, "/transientReferenceInSessionPersist/PassivationCapability.jsp",
                            new String[] { "Reused", "PASSED", "destroy" });
    }

    @BeforeClass
    public static void setUp() throws Exception {
        for (Archive archive : ShrinkWrapServer.getAppsForServer("cdi12PassivationServer")) {
            ShrinkHelper.exportDropinAppToServer(SHARED_SERVER.getLibertyServer(), archive);
        }

    }

    @Override
    protected SharedServer getSharedServer() {

        return SHARED_SERVER;
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (SHARED_SERVER.getLibertyServer() != null) {
            SHARED_SERVER.getLibertyServer().stopServer();
        }
    }

}
