/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.suite;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.Assert; 

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.javaee.ddmodel.suite.util.FailableBiConsumer;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public abstract class CommonTests {
    private static final LibertyServer server =
        LibertyServerFactory.getLibertyServer("ddmodel_fat");

    protected static LibertyServer getServer() {
        return server;
    }

    // Unified: Used for package selection, resource selection, and application naming.

    public static final String DEFAULT_SUFFIX = "";

    public static final String PARTIAL_SUFFIX = "_partial"; // resource
    public static final String MINIMAL_SUFFIX = "_minimal"; // resource

    public static final String JAKARTA_SUFFIX = "_j"; // application
    public static final String JAKARTA9_SUFFIX = "_j9"; // package, resource
    public static final String JAKARTA10_SUFFIX = "_j10"; // package, resource
    public static final String JAKARTA11_SUFFIX = "_j11"; // package, resource

    protected static FileAsset addResource(
            Archive<?> archive,
            String resourcePath,
            String resourcesSuffix) {

        return addResource(archive, archive.getName(), resourcePath, resourcesSuffix);
    }

    protected static final String UNUSED_TARGET_PATH = null;

    protected static FileAsset addResource(
            Archive<?> archive, String archiveResourceName,
            String resourcePath, String resourcesSuffix) {
        
        return addResource(
                archive, archiveResourceName,
                resourcePath, resourcesSuffix,
                UNUSED_TARGET_PATH);
    }
    
    protected static FileAsset addResource(
            Archive<?> archive, String archiveResourceName,
            String resourcePath, String resourcesSuffix,
            String altTargetPath) {

        String sourcePath =
            "test-applications" + '/' + archiveResourceName + '/' +
            "resources" + resourcesSuffix + '/' + resourcePath;

        FileAsset asset = new FileAsset( new File(sourcePath) );

        String targetPath = "/" + ((altTargetPath == null) ? resourcePath : altTargetPath);

        archive.add(asset, targetPath);

        return asset;
    }

    // ServletTest.war
    // ServletTestNoBnd.war
    // EJBTest.jar
    // EJBTestNoBnd.jar
    // Test.ear

    protected static WebArchive createTestWar(String packageSuffix, String resourcesSuffix) {
        WebArchive servletTest = ShrinkWrap.create(WebArchive.class, "ServletTest.war");
        servletTest.addPackage("servlettest" + packageSuffix + ".web");
        addResource(servletTest, "WEB-INF/web.xml", resourcesSuffix);
        addResource(servletTest, "WEB-INF/ibm-web-bnd.xml", resourcesSuffix);
        addResource(servletTest, "WEB-INF/ibm-web-ext.xml", resourcesSuffix);
        addResource(servletTest, "WEB-INF/ibm-ws-bnd.xml", resourcesSuffix);
        addResource(servletTest, "META-INF/permissions.xml", resourcesSuffix);
        return servletTest;
    }

    protected static WebArchive createTestNoBndWar(String packageSuffix, String resourcesSuffix) {
        WebArchive servletTestNoBnd = ShrinkWrap.create(WebArchive.class, "ServletTestNoBnd.war");
        servletTestNoBnd.addPackage("servlettestnobnd" + packageSuffix + ".web");
        addResource(servletTestNoBnd, "WEB-INF/web.xml", resourcesSuffix);
        return servletTestNoBnd;
    }

    protected static JavaArchive createTestJar(String packageSuffix, String resourcesSuffix, boolean useXmi) {
        JavaArchive ejbTest = ShrinkWrap.create(JavaArchive.class, "EJBTest.jar");
        ejbTest.addPackage("ejbtest" + packageSuffix + ".ejb");
        addResource(ejbTest, "META-INF/MANIFEST.MF", resourcesSuffix);
        addResource(ejbTest, "META-INF/ejb-jar.xml", resourcesSuffix);
        if ( useXmi ) {
            addResource(ejbTest, "META-INF/ibm-ejb-jar-bnd.xmi", resourcesSuffix);
            addResource(ejbTest, "META-INF/ibm-ejb-jar-ext.xmi", resourcesSuffix);
        } else {
            addResource(ejbTest, "META-INF/ibm-ejb-jar-bnd.xml", resourcesSuffix);
            addResource(ejbTest, "META-INF/ibm-ejb-jar-ext.xml", resourcesSuffix);
        }
        addResource(ejbTest, "META-INF/ibm-managed-bean-bnd.xml", resourcesSuffix);
        addResource(ejbTest, "META-INF/ibm_ejbext.properties", resourcesSuffix);
        return ejbTest;
    }

    protected static JavaArchive createTestNoBndJar(String packageSuffix, String resourcesSuffix) {
        JavaArchive ejbTestNoBnd = ShrinkWrap.create(JavaArchive.class, "EJBTestNoBnd.jar");
        ejbTestNoBnd.addPackage("ejbtestnobnd" + packageSuffix + ".ejb");
        return ejbTestNoBnd;
    }

    protected static EnterpriseArchive createTestEar(
        Class<?> testClass,
        String packageSuffix, String resourcesSuffix,
        String packagingSuffix, boolean includeNoBnd, boolean useXmiEjb) {

        String methodName = "createTestEar";

        String earResourceName = "Test.ear";
        String earName = "Test" + packagingSuffix + ".ear";

        Log.info(testClass, methodName, "Application resource [ " + earResourceName + " ]");
        Log.info(testClass, methodName, "Application name [ " + earName + " ]");
        Log.info(testClass, methodName, "Package suffix [ " + packageSuffix + " ]");
        Log.info(testClass, methodName, "Include No-Bnd [ " + includeNoBnd + " ]");
        Log.info(testClass, methodName, "Use XMI EJB [ " + useXmiEjb + " ]");

        EnterpriseArchive testEar = ShrinkWrap.create(EnterpriseArchive.class, earName);

        if ( includeNoBnd ) {
            testEar.addAsModules( createTestWar(packageSuffix, resourcesSuffix),
                                  createTestNoBndWar(packageSuffix, resourcesSuffix),
                                  createTestJar(packageSuffix, resourcesSuffix, useXmiEjb),
                                  createTestNoBndJar(packageSuffix, resourcesSuffix) );
        } else {
            testEar.addAsModules( createTestWar(packageSuffix, resourcesSuffix),
                                  createTestJar(packageSuffix, resourcesSuffix, useXmiEjb) );
        }

        addResource(testEar, earResourceName, "META-INF/application.xml", resourcesSuffix);
        addResource(testEar, earResourceName, "META-INF/ibm-application-bnd.xml", resourcesSuffix);
        addResource(testEar, earResourceName, "META-INF/ibm-application-ext.xml", resourcesSuffix);
        addResource(testEar, earResourceName, "META-INF/permissions.xml", resourcesSuffix);

        return testEar;
    };

    protected static final boolean INCLUDE_NO_BND = true;
    protected static final boolean USE_XMI_EJB = true;

    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpTestModules =
        (Class<?> testClass, LibertyServer server) -> {
            setUpTestModules(testClass, server,
                    DEFAULT_SUFFIX, DEFAULT_SUFFIX,
                    INCLUDE_NO_BND, USE_XMI_EJB);
        };

    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpTestModules_J9 =
        (Class<?> testClass, LibertyServer server) -> {
            setUpTestModules(testClass, server,
                    JAKARTA_SUFFIX, JAKARTA9_SUFFIX,
                    INCLUDE_NO_BND, !USE_XMI_EJB);
        };

    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpTestModules_J10 =
        (Class<?> testClass, LibertyServer server) -> {
            setUpTestModules(testClass, server,
                    JAKARTA_SUFFIX, JAKARTA10_SUFFIX,
                    INCLUDE_NO_BND, !USE_XMI_EJB);
        };
        
    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpTestModules_J11 =
            (Class<?> testClass, LibertyServer server) -> {
                setUpTestModules(testClass, server,
                        JAKARTA_SUFFIX, JAKARTA11_SUFFIX,
                        INCLUDE_NO_BND, !USE_XMI_EJB);
            };
  
    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpTestModulesPartialHeaders =
        (Class<?> testClass, LibertyServer server) -> {
            setUpTestModules(testClass, server,
                    DEFAULT_SUFFIX, PARTIAL_SUFFIX,
                    INCLUDE_NO_BND, USE_XMI_EJB);
        };            

    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpTestModulesMinimalHeaders =
        (Class<?> testClass, LibertyServer server) -> {
            setUpTestModules(testClass, server,
                    DEFAULT_SUFFIX, MINIMAL_SUFFIX,
                    INCLUDE_NO_BND, USE_XMI_EJB);
        };            

    protected static void setUpTestModules(
        Class<?> testClass, LibertyServer server,
        String packageSuffix, String resourcesSuffix,
        boolean includeNoBnd, boolean useXmiEjb) throws Exception {

        ShrinkHelper.exportAppToServer( server,
                                        createTestWar(packageSuffix, resourcesSuffix),
                                        DeployOptions.SERVER_ONLY );

        if ( includeNoBnd ) {
            ShrinkHelper.exportAppToServer( server,
                                            createTestNoBndWar(packageSuffix, resourcesSuffix),
                                            DeployOptions.SERVER_ONLY );
        }

        // The installation message uses the jar name without the ".jar" extension.
        // Current ShrinkHelper and LibertyServer code does not take this into account,
        // and look for the jar name with the ".jar" extension.  That causes application
        // startup verification to fail, leading to a test failure.
        //
        // Patches are made to the list of installed application names to work-around this
        // naming problem.
        //
        // See java source file
        // "open-liberty/dev/fattest.simplicity/src/com/ibm/websphere/simplicity/ShrinkHelper.java"
        // and method
        // "exportAppToServer".

        Archive<?> testJar = createTestJar(packageSuffix, resourcesSuffix, useXmiEjb);
        ShrinkHelper.exportAppToServer( server, testJar, DeployOptions.SERVER_ONLY );
        String testJarName = testJar.getName();
        server.removeInstalledAppForValidation(testJarName);
        server.addInstalledAppForValidation( testJarName.substring(0, testJarName.length() - ".jar".length()) );

        if ( includeNoBnd ) {
            Archive<?> testJarNoBnd = createTestNoBndJar(packageSuffix, resourcesSuffix);
            ShrinkHelper.exportAppToServer( server, testJarNoBnd, DeployOptions.SERVER_ONLY );
            String testJarNoBndName = testJarNoBnd.getName();
            server.removeInstalledAppForValidation(testJarNoBndName);
            server.addInstalledAppForValidation( testJarNoBndName.substring(0, testJarNoBndName.length() - ".jar".length()) );
        }
    };

    protected static FailableBiConsumer<Class<?>, LibertyServer> tearDownTestModules =
        (Class<?> testClass, LibertyServer server) -> {
            ShrinkHelper.cleanAllExportedArchives();
            server.removeAllInstalledAppsForValidation();
        };

    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpTestApp =
        (Class<?> testClass, LibertyServer server) -> {
            setUpTestApp(testClass, server,
                         DEFAULT_SUFFIX, DEFAULT_SUFFIX, DEFAULT_SUFFIX,
                         INCLUDE_NO_BND, USE_XMI_EJB);
        };

    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpTestApp_J9 =
        (Class<?> testClass, LibertyServer server) -> {
            setUpTestApp(testClass, server,
                         JAKARTA_SUFFIX, JAKARTA9_SUFFIX, JAKARTA_SUFFIX,
                         INCLUDE_NO_BND, !USE_XMI_EJB);
        };

    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpTestApp_J10 =
        (Class<?> testClass, LibertyServer server) -> {
            setUpTestApp(testClass, server,
                         JAKARTA_SUFFIX, JAKARTA10_SUFFIX, JAKARTA_SUFFIX,
                         INCLUDE_NO_BND, !USE_XMI_EJB);
        };
        
    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpTestApp_J11 =
            (Class<?> testClass, LibertyServer server) -> {
                setUpTestApp(testClass, server,
                             JAKARTA_SUFFIX, JAKARTA11_SUFFIX, JAKARTA_SUFFIX,
                             INCLUDE_NO_BND, !USE_XMI_EJB);
            };

    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpTestAppPartialHeaders =
        (Class<?> testClass, LibertyServer server) -> {
            setUpTestApp(testClass, server,
                         DEFAULT_SUFFIX, PARTIAL_SUFFIX, DEFAULT_SUFFIX,
                         INCLUDE_NO_BND, USE_XMI_EJB);
        };

    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpTestAppMinimalHeaders =
        (Class<?> testClass, LibertyServer server) -> {
            setUpTestApp(testClass, server,
                         DEFAULT_SUFFIX, MINIMAL_SUFFIX, DEFAULT_SUFFIX,
                         INCLUDE_NO_BND, USE_XMI_EJB);
        };

    protected static void setUpTestApp(
        Class<?> testClass, LibertyServer server,
        String packageSuffix, String resourcesSuffix, String packagingSuffix,
        boolean includeNoBnd, boolean useXmiEjb) throws Exception {

        ShrinkHelper.exportAppToServer( server,
                                        createTestEar(testClass,
                                                      packageSuffix, resourcesSuffix, packagingSuffix,
                                                      includeNoBnd, useXmiEjb),
                                        DeployOptions.SERVER_ONLY );
    }

    protected static FailableBiConsumer<Class<?>, LibertyServer> tearDownTestApp =
        (Class<?> testClass, LibertyServer server) -> {
            ShrinkHelper.cleanAllExportedArchives();
            server.removeAllInstalledAppsForValidation();
        };

    protected static void commonSetUp(
        Class<?> testClass,
        String serverConfig,
        FailableBiConsumer<Class<?>, LibertyServer> appSetUp) throws Exception {

        Log.info( testClass, "commonSetUp", "Server configuration [ " + serverConfig + " ]" );

        LibertyServer useServer = getServer();

        appSetUp.accept(testClass, useServer); // throws Exception

        useServer.listAllInstalledAppsForValidation();

        useServer.setServerConfigurationFile(serverConfig);

        useServer.copyFileToLibertyInstallRoot("lib/features", "features/libertyinternals-1.0.mf");
        useServer.copyFileToLibertyInstallRoot("lib", "bundles/ddmodel.jar");

        useServer.startServer( testClass.getSimpleName() + ".log" );
    }

    public static final String[] NO_ALLOWED_ERRORS = new String[] {
        // EMPTY
    };
    
    protected static void commonTearDown(
        Class<?> testClass,
        FailableBiConsumer<Class<?>, LibertyServer> appTearDown,
        String[] expectedErrors) throws Exception {

        Log.info(testClass, "commonTearDown",
                 "Expected errors [ " + expectedErrors.length + " ]");

        LibertyServer useServer = getServer();

        useServer.stopServer(expectedErrors);

        useServer.deleteFileFromLibertyInstallRoot("lib/ddmodel_1.0.0.jar");
        useServer.deleteFileFromLibertyInstallRoot("lib/features/libertyinternals-1.0.mf");

        appTearDown.accept(testClass, useServer); // throws Exception
    }

    //

    public static final String TEST_CONTEXT_ROOT = "autoctx";
    public static final String OK_LINE = "OK";

    protected static void test(Class<?> testClass, String testName) throws Exception {
        test(testClass, testName, TEST_CONTEXT_ROOT, OK_LINE);
    }

    protected static void test(
            Class<?> testClass, String testName,
            String contextRoot, String expectedLine) throws MalformedURLException {
        test(testClass, testName, contextRoot, expectedLine, null);
    }
    
    protected static void test(
        Class<?> testClass, String testName,
        String contextRoot,
        String expectedLine,
        Class<? extends Exception> expectedException) throws MalformedURLException {

        String methodName = "test";
        String description = "[ " + testName + " ]";

        URL url = new URL( "http://" +
                           server.getHostname() + ":" +
                           server.getHttpDefaultPort() + "/" +
                           contextRoot + "?testName=" + testName );

        Log.info(testClass, description + ": URL", "[ " + url + " ]" );

        if ( expectedLine != null ) {
            Log.info(testClass, description + ": Expected", "[ " + expectedLine + " ]");
        }
        if ( expectedException != null ) {
            Log.info(testClass, description + ": Expected", "[ " + expectedException + " ] (exception)");
        }

        if ( (expectedLine == null) && (expectedException == null) ) {
            Assert.fail("Neither expected line nor expected exception were specified.");                
        }

        String capturedLine = null;
        Exception capturedException = null;

        try ( CloseableConnection con = getConnection(url);
              BufferedReader br = getConnectionStream(con) ) {

            String line;

            while ( (line = br.readLine()) != null ) {
                Log.info( testClass, methodName, "[ " + line + " ]" );

                if ( capturedLine == null ) {
                    capturedLine = line;
                }
            } 

        } catch ( Exception e ) {
            capturedException = e;
        }

        // The captured exception must match the expected exception,
        // regardless of the captured and expected lines.

        String exceptionMessage;
        boolean failedException;

        if ( expectedException != null ) {
            if ( capturedException == null ) {
                exceptionMessage = "Failed to capture [ " + expectedException + " ]";
                failedException = true;
            } else if ( !expectedException.isInstance(capturedException) ) {
                exceptionMessage = "Captured [ " + capturedException + " ] expecting [ " + expectedException + " ]";                
                failedException = true;
            } else {
                exceptionMessage = "Captured [ " + capturedException + " ]";
                failedException = false;                
            }
        } else {
            if ( capturedException != null ) {
                exceptionMessage = "Unexpectedly captured [ " + capturedException + " ]";
                failedException = true;
            } else {
                exceptionMessage = null;
                failedException = false;
            }
        }

        // The captured line must match the expected line only
        // if there is no expected exception.

        // When an expected line is provided with an expected exception,
        // the expected line is what is expected if the exception does not
        // occur.

        String lineMessage;
        boolean failedLine;

        if ( expectedLine != null ) {
            if ( capturedLine == null ) {
                lineMessage = "Failed to obtain [ " + expectedLine + " ]";
                failedLine = true; // But see below: This might be ignored.
            } else if ( !expectedLine.equals(capturedLine) ) {
                lineMessage = "Obtained [ " + capturedLine + " ] expecting [ " + expectedLine + " ]";                
                failedLine = true; // But see below: This might be ignored.
            } else {
                lineMessage = "Obtained [ " + capturedLine + " ]";
                failedLine = false;
            }
        } else {
            if ( capturedLine != null ) {
                lineMessage = "Unexpected obtained [ " + capturedLine + " ]";
                failedLine = false;
            } else {
                lineMessage = null;
                failedLine = false;
            }
        }

        if ( exceptionMessage != null ) {
            Log.info( testClass, methodName, description + ": " + exceptionMessage);
        }

        if ( lineMessage != null ) {
            Log.info( testClass, methodName, description + ": " + lineMessage);
        }        

        // Note that a line failure only causes a failure of the test
        // if there is no expected exception.

        String failedMessage;
        boolean failed;

        if ( failedException ) {
            failedMessage = exceptionMessage;
            failed = true;
        } else if ( failedLine && (expectedException == null) ) {
            failedMessage = lineMessage;
            failed = true;
        } else {
            failedMessage= null;
            failed = false;
        }

        Log.info( testClass, methodName, description + ": " + ( failed ? "FAILED" : "PASSED") );

        if ( failed ) {
            Assert.fail(failedMessage);
        }
    }

    public static class ErrorTest {
        public final String initialLine;
        public final String finalLine;
        public final String configSuffix;
        public final String description;
        public final String expectedErrors;

        public ErrorTest(String initialLine, String finalLine,
                         String configSuffix,
                         String description,
                         String expectedErrors) {

            this.initialLine = initialLine;
            this.finalLine = finalLine;

            this.configSuffix = configSuffix;
            this.description = description;
            this.expectedErrors = expectedErrors;
        }
    }
    
//    Expected output:
//
//    First update (introduce error):
//        CWWKG0016I, CWWKG0017I, CWWKZ0009I, ERROR, CWWKZ0003I,
//    Second update (restore to good):
//        CWWKG0016I, CWWKG0017I, CWWKZ0009I, CWWKZ0003I
//
//    Usual first update:
//
//    [AUDIT   ] CWWKG0016I: Starting server configuration update.
//    [AUDIT   ] CWWKG0028A: Processing included configuration resource: /home/jazz_build/Build/jbe/build/dev/image/output/wlp/usr/servers/fatTestPorts.xml
//    [AUDIT   ] CWWKG0028A: Processing included configuration resource: /home/jazz_build/Build/jbe/build/dev/image/output/wlp/usr/servers/fatTestCommon.xml
//    [AUDIT   ] CWWKT0017I: Web application removed (default_host): http://ebcprh01433494-n.fyre.ibm.com:8010/nobindings/
//    [AUDIT   ] CWWKT0017I: Web application removed (default_host): http://ebcprh01433494-n.fyre.ibm.com:8010/autoctx/
//    [AUDIT   ] CWWKZ0009I: The application Test has stopped successfully.
//    [AUDIT   ] CWWKG0017I: The server configuration was successfully updated in 0.286 seconds.
//    [ERROR   ] CWWKC2276E: The 'moduleName' attribute is missing from one or more 'web-bnd' bindings and extension configuration elements of the Test.ear application.
//    [AUDIT   ] CWWKT0016I: Web application available (fromApp): http://localhost:8030/autoctx/
//    [AUDIT   ] CWWKT0016I: Web application available (default_host): http://ebcprh01433494-n.fyre.ibm.com:8010/nobindings/
//    [AUDIT   ] CWWKZ0003I: The application Test updated in 0.369 seconds.
//
//    Usual second update:
//
//    [AUDIT   ] CWWKG0016I: Starting server configuration update.
//    [AUDIT   ] CWWKG0028A: Processing included configuration resource: /home/jazz_build/Build/jbe/build/dev/image/output/wlp/usr/servers/fatTestPorts.xml
//    [AUDIT   ] CWWKG0028A: Processing included configuration resource: /home/jazz_build/Build/jbe/build/dev/image/output/wlp/usr/servers/fatTestCommon.xml
//    [AUDIT   ] CWWKT0017I: Web application removed (default_host): http://ebcprh01433494-n.fyre.ibm.com:8010/nobindings/
//    [AUDIT   ] CWWKT0017I: Web application removed (fromApp): http://localhost:8030/autoctx/
//    [AUDIT   ] CWWKZ0009I: The application Test has stopped successfully.
//    [AUDIT   ] CWWKG0017I: The server configuration was successfully updated in 0.074 seconds.
//    [AUDIT   ] CWWKT0016I: Web application available (default_host): http://ebcprh01433494-n.fyre.ibm.com:8010/autoctx/
//    [AUDIT   ] CWWKT0016I: Web application available (default_host): http://ebcprh01433494-n.fyre.ibm.com:8010/nobindings/
//    [AUDIT   ] CWWKZ0003I: The application Test updated in 0.467 seconds.
//
//    Failed second update:
//    [AUDIT   ] CWWKG0016I: Starting server configuration update.
//    [AUDIT   ] CWWKG0028A: Processing included configuration resource: /home/jazz_build/Build/jbe/build/dev/image/output/wlp/usr/servers/fatTestPorts.xml
//    [AUDIT   ] CWWKG0028A: Processing included configuration resource: /home/jazz_build/Build/jbe/build/dev/image/output/wlp/usr/servers/fatTestCommon.xml
//    [AUDIT   ] CWWKT0017I: Web application removed (default_host): http://ebcprh01433494-n.fyre.ibm.com:8010/nobindings/
//    [AUDIT   ] CWWKT0017I: Web application removed (default_host): http://ebcprh01433494-n.fyre.ibm.com:8010/autoctx/
//    [AUDIT   ] CWWKZ0009I: The application Test has stopped successfully.
//    [AUDIT   ] CWWKG0017I: The server configuration was successfully updated in 0.126 seconds.
//    [AUDIT   ] CWWKE0055I: Server shutdown requested on Wednesday, December 8, 2021 at 2:51 AM. The server ddmodel_fat is shutting down.
//    [AUDIT   ] CWWKE1100I: Waiting for up to 30 seconds for the server to quiesce.
//    [ERROR   ] CWWKZ0004E: An exception occurred while starting the application Test. The exception message was: java.lang.NullPointerException
//    [AUDIT   ] CWWKI0002I: The CORBA name server is no longer available at corbaloc:iiop:localhost:2809/NameService.
//    [AUDIT   ] CWWKE0036I: The server ddmodel_fat stopped after 30.636 seconds.

    
    protected static void errorTest(Class<?> testClass, ErrorTest errorTest) throws Exception {
        String methodName = "errorTest";
        String description = "[ " + errorTest.description + " ]";
        Log.info(testClass, methodName, description);

        LibertyServer server = getServer();

        String configPath = server.getServerConfigurationPath();
        String newConfigPath = configPath + errorTest.configSuffix;
        int replacements = replaceLines(
            testClass,
            configPath, newConfigPath,
            errorTest.initialLine, errorTest.finalLine); // throws Exception
        if ( replacements != 1 ) {
            Log.info( testClass, methodName, description +
                      ": Replacement failure: Expected [ 1 ]; Actual [ " + replacements + " ]" );
            Assert.assertEquals(description + ": Incorrect count of replacements", 1, replacements);
        }

        String errorMessage;

        // Issue 19577: Wait for the CWWKZ0003I message.  Otherwise,
        // after running the last test, the application restart which is
        // triggered by restoring the server configuration can overlap with the
        // server shutdown which is performed after all tests have completed.
        
        server.saveServerConfiguration(); // throws Exception
        server.setMarkToEndOfLog(); // throws Exception
        try {
            // CWWKG0016I, CWWKG0017I, CWWKZ0009I, ERROR, CWWKZ0003I,            
            server.setServerConfigurationFromFilePath(newConfigPath); // throws Exception
            errorMessage = server.waitForStringInLogUsingMark(errorTest.expectedErrors);
            server.waitForStringInLogUsingMark("CWWKZ0003I");
        } finally {
            // CWWKG0016I, CWWKG0017I, CWWKZ0009I, CWWKZ0003I            
            server.setMarkToEndOfLog(); // throws Exception
            server.restoreServerConfiguration(); // throws Exception
            server.waitForStringInLogUsingMark("CWWKZ0003I");            
        }

        if ( errorMessage == null ) {
            Log.info(testClass, methodName, description + ": FAILED");
            Assert.assertNotNull(errorMessage);
        } else {
            Log.info(testClass, methodName, description + "PASSED");
        }
    }

    private static HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }
    
    private static BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        return br;
    }

    private static CloseableConnection getConnection(URL url) throws IOException, ProtocolException {
        return new CloseableConnection( getHttpConnection(url) );
    }

    private static BufferedReader getConnectionStream(CloseableConnection con) throws IOException {
        return getConnectionStream( con.getConnection() );
    }

    private static class CloseableConnection implements Closeable {
        public CloseableConnection(HttpURLConnection connection) {
            this.connection = connection;
        }

        private final HttpURLConnection connection;

        public HttpURLConnection getConnection() {
            return connection;
        }

        @SuppressWarnings("unused")
        @Override
        public void close() throws IOException {
            getConnection().disconnect();
        }
    }

    public static int replaceLines(
        Class<?> testClass,
        String inputPath, String outputPath,
        String initialText, String finalText) throws IOException {

        String methodName = "replaceLines";

        Log.info(testClass, methodName, "Initial [ " + initialText + " ]");
        Log.info(testClass, methodName, "Final [ " + finalText + " ]");

        int replacements = 0;

        try ( BufferedReader input = new BufferedReader( new FileReader(inputPath) ) ) { // throws FileNotFoundException
            try ( PrintWriter output = new PrintWriter( new FileWriter(outputPath, false) ) ) { // throws IOException
                String nextLine;
                while ( ( nextLine = input.readLine()) != null ) { // throws IOException
                    if ( nextLine.contains(initialText) ) {
                        nextLine = nextLine.replace(initialText, finalText);
                        replacements++;
                    }
                    output.println(nextLine); // throws IOException
                }
            }
        }

        Log.info(testClass, methodName, "Replacements [ " + replacements + " ]");
        return replacements;
    }
}
