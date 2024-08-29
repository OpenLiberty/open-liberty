/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.DISABLE_VALIDATION;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * Servlet mapping tests.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ServletMappingTests {
    private static final Logger LOG = Logger.getLogger(ServletMappingTests.class.getName());

    private static final String TEST_SERVLET_MAPPING_APP_NAME = "TestServletMapping";
    private static final String TEST_SERVLET_MAPPING_ANNO_APP_NAME = "TestServletMappingAnno";

    @Server("servlet31_servletMappingServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive TestServletMappingApp = ShrinkHelper.buildDefaultApp(TEST_SERVLET_MAPPING_APP_NAME + ".war",
                                                                        "com.ibm.ws.webcontainer.servlet_31_fat.testservletmapping.war.servlets");
        TestServletMappingApp = (WebArchive) ShrinkHelper.addDirectory(TestServletMappingApp, "test-applications/TestServletMapping.war/resources");
        WebArchive TestServletMappingAnnoApp = ShrinkHelper.buildDefaultApp(TEST_SERVLET_MAPPING_ANNO_APP_NAME + ".war",
                                                                            "com.ibm.ws.webcontainer.servlet_31_fat.testservletmappinganno.war.servlets");
        TestServletMappingAnnoApp = (WebArchive) ShrinkHelper.addDirectory(TestServletMappingAnnoApp, "test-applications/TestServletMappingAnno.war/resources");

        ShrinkHelper.exportAppToServer(server, TestServletMappingApp, DISABLE_VALIDATION);
        ShrinkHelper.exportAppToServer(server, TestServletMappingAnnoApp, DISABLE_VALIDATION);

        server.startServer(ServletMappingTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            // SRVE9016E: Unable to insert mapping [/TestMap] for servlet named [Test2].
            // The URL pattern is already defined for servlet named [Test1].
            server.stopServer("SRVE9016E:.*");
        }
    }

    /**
     * Verify that a duplicate <servlet-mapping> element results in a deployment error. Servlet 3.1 spec, section 12.2
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "com.ibm.wsspi.adaptable.module.UnableToAdaptException", "com.ibm.ws.container.service.metadata.MetaDataException" })
    public void test_ServletMapping() throws Exception {
        server.setMarkToEndOfLog();

        server.saveServerConfiguration();
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);
        // copy server.xml for TestServletMapping.war
        // should use updateServerConfiguration(wlp.getServerRoot() +
        server.setServerConfigurationFile("TestServletMapping/server.xml");
        server.waitForConfigUpdateInLogUsingMark(null);
        // check for error message
        String logmsg = server.waitForStringInLogUsingMark("CWWKZ0002E:.*TestServletMapping");
        Assert.assertNotNull("TestServletMapping application should have failed to start ", logmsg);

        // application failed to start, verify that it is because of a duplicate servlet-mapping
        logmsg = server.waitForStringInLogUsingMark("SRVE9016E:");
        Assert.assertNotNull("TestServletMapping application deployment did not result in  message SRVE9016E: ", logmsg);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    /**
     * Verify that a duplicate <servlet-mapping> element results in a deployment error. Servlet 3.1 spec, section 12.2
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "com.ibm.wsspi.adaptable.module.UnableToAdaptException", "com.ibm.ws.container.service.metadata.MetaDataException" })
    public void test_ServletMappingAnno() throws Exception {
        server.setMarkToEndOfLog();

        server.saveServerConfiguration();
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);
        // copy server.xml for TestServletMappingAnno.war
        server.setServerConfigurationFile("TestServletMappingAnno/server.xml");
        server.waitForConfigUpdateInLogUsingMark(null);
        // check for error message
        String logmsg = server.waitForStringInLogUsingMark("CWWKZ0002E:.*TestServletMappingAnno");
        Assert.assertNotNull("TestServletMappingAnno application should have failed to start ", logmsg);

        // application failed to start, verify that it is because of a duplicate servlet-mapping
        logmsg = server.waitForStringInLogUsingMark("SRVE9016E:");
        Assert.assertNotNull("TestServletMappingAnno application deployment did not result in  message SRVE9016E ", logmsg);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);
    }
}
