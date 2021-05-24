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
package com.ibm.ws.cdi.beansxml.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE8;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.beansxml.fat.apps.invalidBeansXML.InvalidBeansXMLTestServlet;
import com.ibm.ws.cdi.beansxml.fat.apps.invalidBeansXML.TestBean;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class EnablingBeansXmlValidationTest {

    public static final String SERVER_NAME = "cdi12BeansXmlValidationServer";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE8);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive invalidBeansXml = ShrinkWrap.create(WebArchive.class, "invalidBeansXml.war");
        invalidBeansXml.addClass(InvalidBeansXMLTestServlet.class);
        invalidBeansXml.addClass(TestBean.class);
        CDIArchiveHelper.addBeansXML(invalidBeansXml, InvalidBeansXMLTestServlet.class);
        ShrinkHelper.exportDropinAppToServer(server, invalidBeansXml, DeployOptions.SERVER_ONLY);
    }

    //In CDI 2.0 with weld 3.0.4 or later WELD-001210 is a warning not an error.
    @Test
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
