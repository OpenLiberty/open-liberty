/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.embedded;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Locale;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * General tests that don't involve updating configuration while the server is running.
 */
@RunWith(FATRunner.class)
public class EmbeddedJCATest extends FATServletClient {

    private static final String fvtapp = "fvtapp";
    private static final String standaloneapp = "standaloneapp";
    private static final String fvtweb = "fvtweb";
    private static final String fvtweb1 = "fvtweb1";
    private static final String standardWAB = "standardWAB";

    @Server("com.ibm.ws.jca.fat.embeddedrar")
    public static LibertyServer server;

    private static String activationError;

    @BeforeClass
    public static void setUp() throws Exception {

        //Build jars that will be in the rar
        JavaArchive j1 = ShrinkWrap.create(JavaArchive.class, "JCAFAT1.jar").addPackage("fat.jca.embeddedresourceadapter.jar1");
        JavaArchive j2 = ShrinkWrap.create(JavaArchive.class, "JCAFAT2.jar").addPackage("fat.jca.embeddedresourceadapter.jar2");

        //Build rars
        ResourceAdapterArchive r1 = ShrinkWrap.create(ResourceAdapterArchive.class, "tra1.rar")
                        .addAsManifestResource(new File("test-resourceadapters/test-resourceadapter/resources/META-INF/ra.xml"))
                        .addAsDirectories("/test-resourceadapters/test-resourceadapter/lib")
                        .addAsLibraries(j1, j2);
        r1.as(JavaArchive.class)
                        .addPackage("fat.jca.testra")
                        .addPackage("fat.jca.dll");

        ResourceAdapterArchive r2 = ShrinkWrap.create(ResourceAdapterArchive.class, "tra2.rar")
                        .addAsManifestResource(new File("test-resourceadapters/test-resourceadapter/resources/META-INF/ra.xml"))
                        .addAsLibraries(j1, j2);
        r2.as(JavaArchive.class)
                        .addPackage("fat.jca.testra")
                        .addPackage("fat.jca.dll");

        ResourceAdapterArchive r3 = ShrinkWrap.create(ResourceAdapterArchive.class, "fvt10ra.rar")
                        .addAsManifestResource(new File("test-resourceadapters/test-resourceadapter/resources/META-INF/ra10.xml"));
        r3.as(JavaArchive.class)
                        .addPackage("fat.jca.test10ra");

        ShrinkHelper.exportToServer(server, "connectors", r1);
        ShrinkHelper.exportToServer(server, "connectors", r2);
        ShrinkHelper.exportToServer(server, "connectors", r3);

        WebArchive fvtwebWAR = ShrinkWrap.create(WebArchive.class, fvtweb + ".war");
        WebArchive fvtweb1WAR = ShrinkWrap.create(WebArchive.class, fvtweb1 + ".war");

        server.addInstalledAppForValidation(fvtapp);
        server.addInstalledAppForValidation(standaloneapp);
        server.startServer();
        server.waitForStringInLog("CWWKE0002I");
        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Server should report it has started",
                      server.waitForStringInLog("CWWKF0011I"));

        activationError = server.waitForStringInLog("J2CA8810E");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CNTR401(5|6)W: .* (queue1|FVTMessageDrivenBeanOverride)", // EXPECTED: MDB is not in the server.xml for all beans on application
                          "J2CA8811E: .*(topic1|queue1).*fvtapp.tra1", // EXPECTED
                          "J2CA8811E: Resource ims/cf1 from embedded resource adapter fvtapp.ims is available only to application fvtapp", // EXPECTED
                          "J2CA8810E: The endpoint FVTMessageDrivenBeanOverride from embedded resource adapter fvtapp.tra1 can be activated only from application fvtapp.", // EXPECTED
                          "J2CA8809E: .*(topic1|queue1).*fvtapp.tra1", // EXPECTED
                          "J2CA8809E: Resource ims/cf1 from embedded resource adapter fvtapp.ims is available only to application fvtapp.", // EXPECTED
                          "CWWKN0008E: An object could not be obtained for name (ims/cf1|(jms/(queue1|topic1)))", // EXPECTED
                          "CWWKE0701E" // EXPECTED due to access restrictions of embedded resources
        );
    }

    @Test
    public void testNativeLibs() throws Exception {
        final String nativeLib = "Test";

        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        Log.info(getClass(), "testNativeLibs", "os.name: " + os);
        Log.info(getClass(), "testNativeLibs", "os.arch: " + arch);

        if (os.contains("win") || os.contains("linux")) {
            String libName;
            if (arch.contains("x86") || arch.contains("amd")) {
                libName = arch.contains("64") ? nativeLib : nativeLib + "32";
            } else {
                libName = null;
            }

            if (libName != null) {
                // - tra1 (standalone + fvtapp) include native libraries in .rar
                // - tra2 (standalone) uses privateLibraryRef
                int num = server.waitForMultipleStringsInLog(2, "Loaded Native Library " + libName);
                Assert.assertEquals("Did not find expected number of \"Loaded Native Library " + libName + "\"", 2, num);
            }
        }
    }

    @Test
    public void testConfigurationOfEmbeddedResourceAdapter() throws Exception {
        runTest(server, fvtweb, getTestMethodSimpleName());
    }

    @Test
    public void testEmbeddedConnectionFactory() throws Exception {
        runTest(server, fvtweb, getTestMethodSimpleName());
    }

    @Test
    public void testEmbeddedConnectionFactory10() throws Exception {
        runTest(server, fvtweb, getTestMethodSimpleName());
    }

    @Test
    public void testEmbeddedActivationSpec() throws Exception {
        runTest(server, fvtweb, getTestMethodSimpleName());
        assertNotNull("the MDB should have recieved this message and sent it out to the logs",
                      server.waitForStringInLog("FVTMessageDrivenBean:message:testActivationSpec"));
    }

    @Test
    public void testEmbeddedDestinations() throws Exception {
        runTest(server, fvtweb, getTestMethodSimpleName());
    }

    @ExpectedFFDC({ "javax.resource.ResourceException" })
    @Test
    public void testEmbeddedConnectionFactoryFromDifferentEAR() throws Exception {
        runTest(server, fvtweb1, getTestMethodSimpleName());
    }

    @ExpectedFFDC({ "javax.resource.ResourceException" })
    @Test
    public void testEmbeddedAOFromDifferentEAR() throws Exception {
        runTest(server, fvtweb1, getTestMethodSimpleName());
    }

    @Test
    public void testEmbeddedConnectionFactoryFromDifferentEARIndirectLookup() throws Exception {
        runTest(server, fvtweb1, getTestMethodSimpleName());
    }

    @ExpectedFFDC({ "javax.resource.ResourceException" })
    @Test
    public void testEmbeddedAOFromDifferentEARIndirectLookup() throws Exception {
        runTest(server, fvtweb1, getTestMethodSimpleName());
    }

    @ExpectedFFDC({ "javax.resource.ResourceException" })
    @Test
    public void testEmbeddedAOFromDifferentWAB() throws Exception {
        runTest(server, standardWAB, getTestMethodSimpleName());
    }

    @Test
    public void testEmbeddedEndpointActivationFromDifferentEAR() throws Exception {
        assertNotNull("Endpoint Activation should fail with J2CA8810E",
                      activationError);
    }

    @ExpectedFFDC({ "javax.resource.ResourceException" })
    @Test
    public void testEmbeddedObjectFromDifferentEBA() throws Exception {
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot("dropins", "jcaOSGi.eba");
        assertNotNull("the jcaOSGi.eba application should have started successfully",
                      server.waitForStringInLogUsingMark("CWWKZ000[13]I"));

        assertNotNull("test failed for ims/cf1", server.waitForStringInLogUsingMark("Lookup failed for ims/cf1"));
        assertNotNull("test failed for jms/queue1", server.waitForStringInLogUsingMark("Lookup failed for jms/queue1"));
        assertNotNull("test failed for jms/topic1", server.waitForStringInLogUsingMark("Lookup failed for jms/topic1"));
        assertNotNull("test failed for javax.resource.cci.ConnectionFactory", server.waitForStringInLogUsingMark("Lookup failed for javax.resource.cci.ConnectionFactory"));

        server.setMarkToEndOfLog();
        server.deleteFileFromLibertyServerRoot("dropins/jcaOSGi.eba");
        assertNotNull("the jcaOSGi.eba application should have stopped successfully",
                      server.waitForStringInLogUsingMark("CWWKZ0009I"));
    }

    @Test
    public void testReauthentication() throws Exception {
        runTest(server, fvtweb, getTestMethodSimpleName());
    }

    @Test
    public void testConnectionPoolStats() throws Exception {
        runTest(server, fvtweb, getTestMethodSimpleName());
    }
}
