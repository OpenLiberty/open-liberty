/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class EnablingBeansXmlValidationTest extends LoggingTest {

    @Server("cdi12BeansXmlValidationServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive invalidBeansXml = ShrinkWrap.create(WebArchive.class, "invalidBeansXml.war")
                        .addClass("com.ibm.ws.cdi12.test.TestServlet")
                        .addClass("com.ibm.ws.cdi12.test.TestBean")
                        .add(new FileAsset(new File("test-applications/invalidBeansXml.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/invalidBeansXml.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        ShrinkHelper.exportDropinAppToServer(server, invalidBeansXml);
    }

    /**
     * Test to ensure that an old beans.xml which was parsed by OWB is rejected as long as validation is enabled.
     */
    @Test
    @SkipForRepeat({SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES})
    @AllowedFFDC({ "org.jboss.weld.exceptions.IllegalStateException", "com.ibm.ws.container.service.state.StateChangeException" }) //We are expecting these errors, but we're using Allowed because the schema download occasionally fails.
    public void testEnablingBeansXmlValidation() throws Exception {
        boolean foundNetworkError = false;
        try {
            server.startServer(true); // Expect exception thrown here because the app does not start
            if (server.waitForStringInLog("WELD-001210") != null) {
                /*
                 * WELD-001210 means that the server could not get the schema document from java.sun.com.
                 * In this case the server defaults to saying the xml is valid.
                 */
                System.out.println("Due to the network issue we could not download the schema file for beans.xml; we will supress the test failure");
                foundNetworkError = true;
            } else {
                fail("The application should not start successfully.");
            }
        } catch (Exception e) {
            //I saw a failure with WELD-001208 in the logs, but not CWWKZ0002E, so I'm adding a fallback WELD-001210 check.
            //If we saw WELD-001210 before or we see it now skip the asserts.
            if (foundNetworkError == false && server.waitForStringInLog("WELD-001210") == null) {
                // We don't care about what exception weld threw as long as the app failed to start
                assertNotNull("CWWKZ0002E: An exception occurred while starting the application",
                              server.waitForStringInLog("CWWKZ0002E: An exception occurred while starting the application.*"));
            }
        }

    }

    //In CDI 2.0 with weld 3.0.4 or later WELD-001210 is a warning not an error. 
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testEnablingBeansXmlValidationCDITwo() throws Exception {
        boolean foundNetworkError = false;
        try {
            server.startServer(true);

            if (server.waitForStringInLog("WELD-001210") != null) {
                /*
                 * WELD-001210 means that the server could not get the schema document from java.sun.com.
                 * In this case the server defaults to saying the xml is valid.
                 */
                 foundNetworkError = true;
            } else { 
                if (server.waitForStringInLog("WELD-001210") == null) {
                    assertNotNull("WELD-001208 Warning message not found", server.waitForStringInLog("WELD-001208"));
                }
            }
        } catch (Exception e) {
            //I saw a failure with WELD-001208 in the logs, but not CWWKZ0002E, so I'm adding a fallback WELD-001210 check.
            //If we saw WELD-001210 before or we see it now skip the asserts. 
            if (foundNetworkError == false && server.waitForStringInLog("WELD-001210") == null) {
                assertNotNull("WELD-001208 Warning message not found", server.waitForStringInLog("WELD-001208"));
            }
        }
    }

    @Override
    protected SharedServer getSharedServer() {
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
