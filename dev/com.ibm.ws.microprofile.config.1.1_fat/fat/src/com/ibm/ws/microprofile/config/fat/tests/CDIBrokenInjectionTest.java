/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CDIBrokenInjectionTest extends FATServletClient {

    public static final String APP_NAME = "brokenCDIConfig";

    // Don't repeat against mpConfig > 1.4 since the error messages changed. New similar tests are in io.openliberty.microprofile.config.2.0.internal_fat bucket
    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat("brokenCDIConfigServer", MicroProfileActions.MP12, MicroProfileActions.MP33);

    @Server("brokenCDIConfigServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackages(true, "com.ibm.ws.microprofile.appConfig.cdi.broken")
                                   .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                                   .addAsManifestResource(new File("test-applications/" + APP_NAME
                                                                   + ".war/resources/META-INF/services/org.eclipse.microprofile.config.spi.Converter"),
                                                          "services/org.eclipse.microprofile.config.spi.Converter");

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        /*
         * The application will fail to start due to the brokenness of the Config under test.
         *
         * Use the LibertyServer startServerAndValidate() method to allow the server to start
         * without the necessity to validate that the application has started successfully.
         *
         * The startServerAndValidate parameters are set, in order, as follows,
         *
         * DEFAULT_PRE_CLEAN=true, DEFAULT_CLEANSTART=true, validateApps=false
         */
        server.startServerAndValidate(true, true, false);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            /*
             * Ignore following exception as those are expected:
             * CWMCG5003E: The {0} InjectionPoint dependency was not resolved. Error: {1}
             *
             * CWWKZ0002E: An exception occurred while starting the application brokenCDIConffig. The exception message was:
             * com.ibm.ws.container.service.state.StateChangeException: org.jboss.weld.exceptions.DeploymentException:
             */
            server.stopServer("CWWKZ0002E", "CWMCG5003E");
        }
    }

    @Test
    public void testMethodUnnamed() throws Exception {
        List<String> errors = server.findStringsInLogs("ConfigUnnamedMethodInjectionBean.*setSimpleKey6.*The property name must be specified for Constructor and Method configuration property injection");
        assertNotNull("error not found", errors);
        assertTrue("error not found: " + errors.size(), errors.size() > 0);
    }

    @Test
    public void testConstructorUnnamed() throws Exception {
        List<String> errors = server.findStringsInLogs("ConfigUnnamedConstructorInjectionBean.*The property name must be specified for Constructor and Method configuration property injection");
        assertNotNull("error not found", errors);
        assertTrue("error not found: " + errors.size(), errors.size() > 0);
    }

    @Test
    public void testNonExistantKey() throws Exception {
        List<String> errors = server.findStringsInLogs("CWMCG5003E.*nonExistantKey.*CWMCG0015E: The property com.ibm.ws.microprofile.appConfig.cdi.broken.beans.MissingConfigPropertyBean.nonExistantKey was not found in the configuration.");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @Test
    public void testNonExistantKeyWithCustomConverter() throws Exception {
        List<String> errors = server.findStringsInLogs("CWMCG5003E.*undefinedKeyWithConverter.*CWMCG0015E: The property com.ibm.ws.microprofile.appConfig.cdi.broken.beans.MissingConfigPropertyBean.undefinedKeyWithConverter was not found in the configuration.");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @Test
    public void testConverterMissing() throws Exception {
        List<String> errors = server.findStringsInLogs("CWMCG5003E.*noConverterProp.*CWMCG0014E: A Converter could not be found for type com.ibm.ws.microprofile.appConfig.cdi.broken.test.TypeWithNoConverter.");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

}
