/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.suite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.Assert; 

import com.ibm.websphere.simplicity.log.Log;

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

     //

    private static final LibertyServer server =
        LibertyServerFactory.getLibertyServer("ddmodel_fat");

    protected static LibertyServer getServer() {
        return server;
    }

    //

    public static final String RESOURCES_FRAGMENT = "resources";

    public static final String RESOURCES_DEFAULT_SUFFIX = "";    
    public static final String RESOURCES_PARTIAL_SUFFIX = "-partial";
    public static final String RESOURCES_MINIMAL_SUFFIX = "-minimal";

    protected static FileAsset addResource(
            Archive<?> archive,
            String resourcePath,
            String resourcesSuffix) {
        
        String sourcePath =
            "test-applications" + '/' + archive.getName() + '/' +
            RESOURCES_FRAGMENT + resourcesSuffix + '/' + resourcePath;

        String targetPath = "/" + resourcePath;

        FileAsset asset = new FileAsset( new File(sourcePath) );
        archive.add(asset, targetPath);

        return asset;
    }

    // ServletTest.war
    // ServletTestNoBnd.war
    // EJBTest.jar
    // EJBTestNoBnd.jar
    // Test.ear

    protected static WebArchive createTestWar(String resourcesSuffix) {
        WebArchive servletTest = ShrinkWrap.create(WebArchive.class, "ServletTest.war");
        servletTest.addPackage("servlettest.web");
        addResource(servletTest, "WEB-INF/web.xml", resourcesSuffix);
        addResource(servletTest, "WEB-INF/ibm-web-bnd.xml", resourcesSuffix);
        addResource(servletTest, "WEB-INF/ibm-web-ext.xml", resourcesSuffix);
        addResource(servletTest, "WEB-INF/ibm-ws-bnd.xml", resourcesSuffix);
        addResource(servletTest, "META-INF/permissions.xml", resourcesSuffix);
        return servletTest;
    }

    protected static WebArchive createTestNoBndWar(String resourcesSuffix) {
        WebArchive servletTestNoBnd = ShrinkWrap.create(WebArchive.class, "ServletTestNoBnd.war");
        servletTestNoBnd.addPackage("servlettestnobnd.web");
        addResource(servletTestNoBnd, "WEB-INF/web.xml", resourcesSuffix);
        return servletTestNoBnd;
    }

    protected static JavaArchive createTestJar(String resourcesSuffix) {
        JavaArchive ejbTest = ShrinkWrap.create(JavaArchive.class, "EJBTest.jar");
        ejbTest.addPackage("ejbtest.ejb");
        addResource(ejbTest, "META-INF/MANIFEST.MF", resourcesSuffix);
        addResource(ejbTest, "META-INF/ejb-jar.xml", resourcesSuffix);
        addResource(ejbTest, "META-INF/ibm-ejb-jar-bnd.xmi", resourcesSuffix);
        addResource(ejbTest, "META-INF/ibm-ejb-jar-ext.xmi", resourcesSuffix);
        addResource(ejbTest, "META-INF/ibm-managed-bean-bnd.xml", resourcesSuffix);
        addResource(ejbTest, "META-INF/ibm_ejbext.properties", resourcesSuffix);
        return ejbTest;
    }

    protected static JavaArchive createTestNoBndJar(String resourcesSuffix) {
        JavaArchive ejbTestNoBnd = ShrinkWrap.create(JavaArchive.class, "EJBTestNoBnd.jar");
        ejbTestNoBnd.addPackage("ejbtestnobnd.ejb");
        return ejbTestNoBnd;
    }

    protected static EnterpriseArchive createTestEar() {
        return createTestEar(RESOURCES_DEFAULT_SUFFIX);
    }
    
    protected static EnterpriseArchive createTestEarPartialHeaders() {
        return createTestEar(RESOURCES_PARTIAL_SUFFIX);
    }    
    
    protected static EnterpriseArchive createTestEarMinimalHeaders() {
        return createTestEar(RESOURCES_MINIMAL_SUFFIX);
    }    

    protected static EnterpriseArchive createTestEar(String resourcesSuffix) {
        EnterpriseArchive testEar =
            ShrinkWrap.create(EnterpriseArchive.class, "Test.ear");

        testEar.addAsModules(
            createTestWar(resourcesSuffix),
            createTestNoBndWar(resourcesSuffix),
            createTestJar(resourcesSuffix),
            createTestNoBndJar(resourcesSuffix) );

        addResource(testEar, "META-INF/application.xml", resourcesSuffix);
        addResource(testEar, "META-INF/ibm-application-bnd.xml", resourcesSuffix);
        addResource(testEar, "META-INF/ibm-application-ext.xml", resourcesSuffix);
        addResource(testEar, "META-INF/permissions.xml", resourcesSuffix);

        return testEar;
    };

    protected static FailableConsumer<LibertyServer, Exception> setUpTestModules =
        (LibertyServer server) -> {
            setUpTestModules(RESOURCES_DEFAULT_SUFFIX);
        };
            
    protected static FailableConsumer<LibertyServer, Exception> setUpTestModulesPartialHeaders =
        (LibertyServer server) -> {
            setUpTestModules(RESOURCES_PARTIAL_SUFFIX);
        };            

    protected static FailableConsumer<LibertyServer, Exception> setUpTestModulesMinimalHeaders =
        (LibertyServer server) -> {
            setUpTestModules(RESOURCES_MINIMAL_SUFFIX);
        };            
        
    protected static void setUpTestModules(String resourcesSuffix) throws Exception {
        ShrinkHelper.exportAppToServer( server, createTestWar(resourcesSuffix), DeployOptions.SERVER_ONLY );
        ShrinkHelper.exportAppToServer( server, createTestNoBndWar(resourcesSuffix), DeployOptions.SERVER_ONLY );

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

        Archive<?> testJar = createTestJar(resourcesSuffix);
        ShrinkHelper.exportAppToServer( server, testJar, DeployOptions.SERVER_ONLY );
        String testJarName = testJar.getName();
        server.removeInstalledAppForValidation(testJarName);
        server.addInstalledAppForValidation( testJarName.substring(0, testJarName.length() - ".jar".length()) );

        Archive<?> testJarNoBnd = createTestNoBndJar(resourcesSuffix);
        ShrinkHelper.exportAppToServer( server, testJarNoBnd, DeployOptions.SERVER_ONLY );
        String testJarNoBndName = testJarNoBnd.getName();
        server.removeInstalledAppForValidation(testJarNoBndName);
        server.addInstalledAppForValidation( testJarNoBndName.substring(0, testJarNoBndName.length() - ".jar".length()) );
    };

    protected static FailableConsumer<LibertyServer, Exception> tearDownTestModules =
        (LibertyServer server) -> {
            ShrinkHelper.cleanAllExportedArchives();
            server.removeAllInstalledAppsForValidation();
        };

    protected static FailableConsumer<LibertyServer, Exception> setUpTestApp =
        (LibertyServer server) -> {
            setUpTestApp(RESOURCES_DEFAULT_SUFFIX);
        };

    protected static FailableConsumer<LibertyServer, Exception> setUpTestAppPartialHeaders =
        (LibertyServer server) -> {
            setUpTestApp(RESOURCES_PARTIAL_SUFFIX);
        };

    protected static FailableConsumer<LibertyServer, Exception> setUpTestAppMinimalHeaders =
        (LibertyServer server) -> {
            setUpTestApp(RESOURCES_MINIMAL_SUFFIX);
        };
        
    protected static void setUpTestApp(String resourcesSuffix) throws Exception {
        ShrinkHelper.exportAppToServer( server, createTestEar(resourcesSuffix), DeployOptions.SERVER_ONLY );
    }
        
    protected static FailableConsumer<LibertyServer, Exception> tearDownTestApp =
        (LibertyServer server) -> {
            ShrinkHelper.cleanAllExportedArchives();
            server.removeAllInstalledAppsForValidation();
        };

    protected static void commonSetUp(
        Class<?> testClass,
        String serverConfig,
        FailableConsumer<LibertyServer, Exception> appSetUp) throws Exception {

        Log.info( testClass, "commonSetup", "Server configuration [ " + serverConfig + " ]" );

        LibertyServer useServer = getServer();

        appSetUp.accept(useServer); // throws Exception

        useServer.listAllInstalledAppsForValidation();

        useServer.setServerConfigurationFile(serverConfig);

        useServer.copyFileToLibertyInstallRoot("lib/features", "features/libertyinternals-1.0.mf");
        useServer.copyFileToLibertyInstallRoot("lib", "bundles/ddmodel.jar");

        useServer.startServer( testClass.getSimpleName() + ".log" );
    }

    protected static void commonTearDown(
        Class<?> testClass,
        FailableConsumer<LibertyServer, Exception> appTearDown,
        String[] expectedErrors) throws Exception {

        Log.info( testClass, "commonTearDown", "Expected errors [ " + expectedErrors.length + " ]" );

        LibertyServer useServer = getServer();
        
        useServer.stopServer(expectedErrors);

        useServer.deleteFileFromLibertyInstallRoot("lib/ddmodel_1.0.0.jar");
        useServer.deleteFileFromLibertyInstallRoot("lib/features/libertyinternals-1.0.mf");

        appTearDown.accept(useServer); // throws Exception
    }

    //

    protected static void test(Class<?> testClass, String testName) throws Exception {
        String methodName = "test";
        String description = "[ " + testName + " ]";

        URL url = new URL( "http://" +
                           server.getHostname() + ":" +
                           server.getHttpDefaultPort() + "/" +
                           "autoctx?testName=" + testName );
        Log.info(testClass, description + ": URL", "[ " + url + " ]" );

        HttpURLConnection con = getHttpConnection(url);
        try {
            try ( BufferedReader br = getConnectionStream(con) ) {
                String line = br.readLine();
                if ( !"OK".equals(line) ) {
                    Log.info( testClass, methodName, description + ": FAILED" );
                    Assert.fail("Unexpected response [ " + line + " ] expected [ OK ]");
                } else {
                    Log.info( testClass, methodName, description + ": PASSED" );
                }
                while ( (line = br.readLine()) != null ) {
                    Log.info( testClass, methodName, "[ " + line + " ]" );
                }
            }

        } finally {
            con.disconnect();
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
