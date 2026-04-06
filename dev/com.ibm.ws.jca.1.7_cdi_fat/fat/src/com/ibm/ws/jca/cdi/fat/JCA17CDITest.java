/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.cdi.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jakarta.enterprise.inject.spi.Extension;
import lib.cdi.CDIExtension;

/**
 * This test class starts a single server and then drops in multiple applications
 * to verify that each application installs without error.
 *
 * This tests different scenarios using Connector and JMS resource definitions with
 * and without CDI to verify that the startup sequence can always find the
 * resource adapter bundle.
 */
@RunWith(FATRunner.class)
public class JCA17CDITest extends FATServletClient {

    private static final String WEB_MODULE_NAME = "web";

    private static final String BEANS_XML = "test-applications/web/resources/";

    @Server("com.ibm.ws.jca.cdi.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer("JCA17CDITest");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWNEN0047W" // Expected warning from test testMissingBundle
        );
    }

    private static WebArchive createWAR(String... directories) throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_MODULE_NAME + ".war");
        for (String directory : directories) {
            ShrinkHelper.addDirectory(war, directory);
        }
        return war;
    }

    /**
     * Verify that if the RAR bundle is actually missing, we still get an error
     * in a reasonable amount of time.
     */
    @Test
    @ExpectedFFDC({ "java.lang.NoClassDefFoundError",
                    "org.jboss.weld.resources.spi.ResourceLoadingException" })
    public void testMissingBundle() throws Exception {
        // Create web archive
        WebArchive war = createWAR(BEANS_XML);
        war.addClass(web.AODServlet.class);

        // Create cdi library
        JavaArchive jar = ShrinkHelper.buildJavaArchive("cdiextension", "lib.cdi");
        jar.addAsServiceProvider(Extension.class, CDIExtension.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, getTestMethodSimpleName() + ".ear");
        ear.addAsModule(war);
        ear.addAsLibraries(jar);

        try {
            server.setMarkToEndOfLog();
            ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.DISABLE_VALIDATION);
            // Wait for application to install
            server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + getTestMethodSimpleName());
        } finally {
            server.setMarkToEndOfLog();
            server.deleteAllDropinApplications();
        }

        // Wait for application to uninstall
        server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + getTestMethodSimpleName());
    }

    /**
     * Test @AdministeredObjectDefinition resource is available on install.
     * Without CDI Bean discovery.
     */
    @Test
    public void testAODResource() throws Exception {
        // Create web archive
        WebArchive war = createWAR();
        war.addClass(web.AODServlet.class);

        // Create embedded resource adapter
        ResourceAdapterArchive rar = ShrinkHelper.buildDefaultRar("adminobject", "ra.ao");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, getTestMethodSimpleName() + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(rar);

        try {
            server.setMarkToEndOfLog();
            ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.DISABLE_VALIDATION);
            // Wait for application to install
            server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + getTestMethodSimpleName());

            runTest(server, WEB_MODULE_NAME, getTestMethodSimpleName());
        } finally {
            server.setMarkToEndOfLog();
            server.deleteAllDropinApplications();
        }

        // Wait for application to uninstall
        server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + getTestMethodSimpleName());
    }

    /**
     * Test @AdministeredObjectDefinition resource is available on install.
     * With CDI Bean discovery.
     */
    @Test
    public void testAODResourceWithCDI() throws Exception {
        // Create web archive
        WebArchive war = createWAR(BEANS_XML);
        war.addClass(web.cdi.AODServlet.class);

        // Create embedded resource adapter
        ResourceAdapterArchive rar = ShrinkHelper.buildDefaultRar("adminobject", "ra.ao");

        // Create cdi library
        JavaArchive jar = ShrinkHelper.buildJavaArchive("cdiextension", "lib.cdi");
        jar.addAsServiceProvider(Extension.class, CDIExtension.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, getTestMethodSimpleName() + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(rar);
        ear.addAsLibraries(jar);

        try {
            server.setMarkToEndOfLog();
            ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.DISABLE_VALIDATION);
            // Wait for application to install
            server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + getTestMethodSimpleName());

            runTest(server, WEB_MODULE_NAME, getTestMethodSimpleName());
        } finally {
            server.setMarkToEndOfLog();
            server.deleteAllDropinApplications();
        }

        // Wait for application to uninstall
        server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + getTestMethodSimpleName());
    }

    /**
     * Test @ConnectionFactoryDefinition resource is available on install.
     * Without CDI Bean discovery.
     */
    @Test
    public void testCFDResource() throws Exception {
        // Create web archive
        WebArchive war = createWAR();
        war.addClass(web.CFDServlet.class);

        // Create embedded resource adapter
        ResourceAdapterArchive rar = ShrinkHelper.buildDefaultRar("connectionfactory", "ra.cf");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, getTestMethodSimpleName() + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(rar);

        try {
            server.setMarkToEndOfLog();
            ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.DISABLE_VALIDATION);
            // Wait for application to install
            server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + getTestMethodSimpleName());

            runTest(server, WEB_MODULE_NAME, getTestMethodSimpleName());
        } finally {
            server.setMarkToEndOfLog();
            server.deleteAllDropinApplications();
        }

        // Wait for application to uninstall
        server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + getTestMethodSimpleName());
    }

    /**
     * Test @ConnectionFactoryDefinition resource is available on install.
     * With CDI Bean discovery.
     */
    @Test
    public void testCFDResourceWithCDI() throws Exception {
        // Create web archive
        WebArchive war = createWAR(BEANS_XML);
        war.addClass(web.cdi.CFDServlet.class);

        // Create embedded resource adapter
        ResourceAdapterArchive rar = ShrinkHelper.buildDefaultRar("connectionfactory", "ra.cf");

        // Create cdi library
        JavaArchive jar = ShrinkHelper.buildJavaArchive("cdiextension", "lib.cdi");
        jar.addAsServiceProvider(Extension.class, CDIExtension.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, getTestMethodSimpleName() + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(rar);
        ear.addAsLibraries(jar);

        try {
            server.setMarkToEndOfLog();
            ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.DISABLE_VALIDATION);
            // Wait for application to install
            server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + getTestMethodSimpleName());

            runTest(server, WEB_MODULE_NAME, getTestMethodSimpleName());
        } finally {
            server.setMarkToEndOfLog();
            server.deleteAllDropinApplications();
        }

        // Wait for application to uninstall
        server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + getTestMethodSimpleName());
    }

    /**
     * Test @JMSConnectionFactoryDefinition resource is available on install.
     * Without CDI Bean discovery.
     */
    @Test
    public void testJMSCFDResource() throws Exception {
        // Create web archive
        WebArchive war = createWAR();
        war.addClass(web.JMSCFDServlet.class);

        // Create embedded resource adapter
        ResourceAdapterArchive rar = ShrinkHelper.buildDefaultRar("jmsconnectionfactory", "ra.jms.cf");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, getTestMethodSimpleName() + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(rar);

        try {
            server.setMarkToEndOfLog();
            ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.DISABLE_VALIDATION);
            // Wait for application to install
            server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + getTestMethodSimpleName());

            runTest(server, WEB_MODULE_NAME, getTestMethodSimpleName());
        } finally {
            server.setMarkToEndOfLog();
            server.deleteAllDropinApplications();
        }

        // Wait for application to uninstall
        server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + getTestMethodSimpleName());
    }

    /**
     * Test @JMSConnectionFactoryDefinition resource is available on install.
     * with CDI Bean discovery.
     */
    @Test
    public void testJMSCFDResourceWithCDI() throws Exception {
        // Create web archive
        WebArchive war = createWAR(BEANS_XML);
        war.addClass(web.cdi.JMSCFDServlet.class);

        // Create embedded resource adapter
        ResourceAdapterArchive rar = ShrinkHelper.buildDefaultRar("jmsconnectionfactory", "ra.jms.cf");

        // Create cdi library
        JavaArchive jar = ShrinkHelper.buildJavaArchive("cdiextension", "lib.cdi");
        jar.addAsServiceProvider(Extension.class, CDIExtension.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, getTestMethodSimpleName() + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(rar);
        ear.addAsLibrary(jar);

        try {
            server.setMarkToEndOfLog();
            ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.DISABLE_VALIDATION);
            // Wait for application to install
            server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + getTestMethodSimpleName());

            runTest(server, WEB_MODULE_NAME, getTestMethodSimpleName());
        } finally {
            server.setMarkToEndOfLog();
            server.deleteAllDropinApplications();
        }

        // Wait for application to uninstall
        server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + getTestMethodSimpleName());
    }

    /**
     * Test @JMSDestinationDefinition resource is available on install.
     * Without CDI Bean discovery.
     */
    @Test
    public void testJMSDDResource() throws Exception {
        // Create web archive
        WebArchive war = createWAR();
        war.addClass(web.JMSDDServlet.class);

        // Create embedded resource adapter
        ResourceAdapterArchive rar = ShrinkHelper.buildDefaultRar("jmsdestination", "ra.jms.dest");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, getTestMethodSimpleName() + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(rar);

        try {
            server.setMarkToEndOfLog();
            ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.DISABLE_VALIDATION);
            // Wait for application to install
            server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + getTestMethodSimpleName());

            runTest(server, WEB_MODULE_NAME, getTestMethodSimpleName());
        } finally {
            server.setMarkToEndOfLog();
            server.deleteAllDropinApplications();
        }

        // Wait for application to uninstall
        server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + getTestMethodSimpleName());
    }

    /**
     * Test @JMSDestinationDefinition resource is available on install.
     * with CDI Bean discovery.
     */
    @Test
    public void testJMSDDResourceWithCDI() throws Exception {
        // Create web archive
        WebArchive war = createWAR();
        war.addClass(web.cdi.JMSDDServlet.class);

        // Create embedded resource adapter
        ResourceAdapterArchive rar = ShrinkHelper.buildDefaultRar("jmsdestination", "ra.jms.dest");

        // Create cdi library
        JavaArchive jar = ShrinkHelper.buildJavaArchive("cdiextension", "lib.cdi");
        jar.addAsServiceProvider(Extension.class, CDIExtension.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, getTestMethodSimpleName() + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(rar);
        ear.addAsLibrary(jar);

        try {
            server.setMarkToEndOfLog();
            ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.DISABLE_VALIDATION);
            // Wait for application to install
            server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + getTestMethodSimpleName());

            runTest(server, WEB_MODULE_NAME, getTestMethodSimpleName());
        } finally {
            server.setMarkToEndOfLog();
            server.deleteAllDropinApplications();
        }

        // Wait for application to uninstall
        server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + getTestMethodSimpleName());
    }
}
