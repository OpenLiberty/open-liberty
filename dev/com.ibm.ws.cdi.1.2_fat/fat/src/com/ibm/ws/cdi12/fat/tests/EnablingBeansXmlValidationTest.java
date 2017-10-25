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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class EnablingBeansXmlValidationTest extends LoggingTest {

    private static LibertyServer server;

    @Test
    @ExpectedFFDC({ "org.jboss.weld.exceptions.IllegalStateException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testEnablingBeansXmlValidation() throws Exception {
        server = LibertyServerFactory.getLibertyServer("cdi12BeansXmlValidationServer");

        try {

            server.startServer(true);

            if (!server.findStringsInLogs("WELD-001210").isEmpty()) {
                /*
                 * WELD-001210 means that the server could not get the schema document from java.sun.com.
                 * In this case the server defaults to saying the xml is valid.
                 */
                System.out.println("Due to the network issue we could not download the schema file for beans.xml; we will supress the test failure");
            } else {
                fail("The application should not start successfully.");
            }
        } catch (Exception e) {
            assertNotNull("WELD-001208 Warning message not found", server.waitForStringInLog("WELD-001208: Error when validating wsjar:file:.*"));
            assertNotNull("CWWKZ0002E: An exception occurred while starting the application",
                          server.waitForStringInLog("CWWKZ0002E: An exception occurred while starting the application.*"));
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {

        return null;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            /*
             * Ignore following exception as those are expected:
             * 0000003f com.ibm.ws.app.manager.AppMessageHelper E CWWKZ0002E: An exception occurred while starting the application disablingBeansXmlValidation.
             * The exception message was: com.ibm.ws.container.service.state.StateChangeException:
             * org.jboss.weld.exceptions.IllegalStateException:
             * WELD-001202: Error parsing wsjar:file:...cdi12BeansXmlValidationServer/dropins/disablingBeansXmlValidation.war!/WEB-INF/beans.xml
             */
            server.stopServer("CWWKZ0002E");
        }
    }
}
