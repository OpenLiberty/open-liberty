/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class EnablingBeansXmlValidationTest extends LoggingTest {

    //For reasons I do not know this test requires the app to be in the dropins folder before the test starts. Thus the app is built and exported in FATSuit.java
    private static LibertyServer server;

    private static boolean hasSetUp = false;

    @BeforeClass
    public static void setUp() throws Exception {
        if (hasSetUp) {
            return;
        }
        WebArchive invalidBeansXml = ShrinkWrap.create(WebArchive.class, "invalidBeansXml.war")
                        .addClass("com.ibm.ws.cdi12.test.TestServlet")
                        .addClass("com.ibm.ws.cdi12.test.TestBean")
                        .add(new FileAsset(new File("test-applications/invalidBeansXml.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/invalidBeansXml.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        LibertyServer server = LibertyServerFactory.getLibertyServer("cdi12BeansXmlValidationServer");
        ShrinkHelper.exportDropinAppToServer(server, invalidBeansXml);
        hasSetUp = true;
    }        

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
