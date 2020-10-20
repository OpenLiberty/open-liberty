/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

import junit.framework.Assert;

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

    private static LibertyServer server =
        LibertyServerFactory.getLibertyServer("ddmodel_fat");

    protected static LibertyServer getServer() {
        return server;
    }

    //

    protected static FileAsset addResource(Archive archive, String resourcePath) {
        String sourcePath = "test-applications/" + archive.getName() + "/resources/" + resourcePath;
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

    protected static WebArchive createTestWar() {
        WebArchive servletTest = ShrinkWrap.create(WebArchive.class, "ServletTest.war");
        servletTest.addPackage("servlettest.web");
        addResource(servletTest, "WEB-INF/web.xml");
        addResource(servletTest, "WEB-INF/ibm-web-bnd.xml");
        addResource(servletTest, "WEB-INF/ibm-web-ext.xml");
        addResource(servletTest, "WEB-INF/ibm-ws-bnd.xml");
        addResource(servletTest, "META-INF/permissions.xml");
        return servletTest;
    }

    protected static WebArchive createTestNoBndWar() {
        WebArchive servletTestNoBnd = ShrinkWrap.create(WebArchive.class, "ServletTestNoBnd.war");
        servletTestNoBnd.addPackage("servlettestnobnd.web");
        addResource(servletTestNoBnd, "WEB-INF/web.xml");
        return servletTestNoBnd;
    }

    protected static JavaArchive createTestJar() {
        JavaArchive ejbTest = ShrinkWrap.create(JavaArchive.class, "EJBTest.jar");
        ejbTest.addPackage("ejbtest.ejb");
        addResource(ejbTest, "META-INF/MANIFEST.MF");
        addResource(ejbTest, "META-INF/ejb-jar.xml");
        addResource(ejbTest, "META-INF/ibm-ejb-jar-bnd.xmi");
        addResource(ejbTest, "META-INF/ibm-ejb-jar-ext.xmi");
        addResource(ejbTest, "META-INF/ibm-managed-bean-bnd.xml");
        addResource(ejbTest, "META-INF/ibm_ejbext.properties");
        return ejbTest;
    }

    protected static JavaArchive createTestNoBndJar() {
        JavaArchive ejbTestNoBnd = ShrinkWrap.create(JavaArchive.class, "EJBTestNoBnd.jar");
        ejbTestNoBnd.addPackage("ejbtestnobnd.ejb");
        return ejbTestNoBnd;
    }

    protected static EnterpriseArchive createTestEar() {
        EnterpriseArchive testEar =
            ShrinkWrap.create(EnterpriseArchive.class, "Test.ear");

        testEar.addAsModules(
            createTestWar(),
            createTestNoBndWar(),
            createTestJar(),
            createTestNoBndJar() );

        addResource(testEar, "META-INF/application.xml");
        addResource(testEar, "META-INF/ibm-application-bnd.xml");
        addResource(testEar, "META-INF/ibm-application-ext.xml");
        addResource(testEar, "META-INF/permissions.xml");

        return testEar;
    };

    protected static FailableConsumer<LibertyServer, Exception> setUpTestModules =
        (LibertyServer server) -> {
            ShrinkHelper.exportAppToServer( server, CommonTests.createTestWar(), DeployOptions.SERVER_ONLY );
            ShrinkHelper.exportAppToServer( server, CommonTests.createTestNoBndWar(), DeployOptions.SERVER_ONLY );
            ShrinkHelper.exportAppToServer( server, CommonTests.createTestJar(), DeployOptions.SERVER_ONLY );
            ShrinkHelper.exportAppToServer( server, CommonTests.createTestNoBndJar(), DeployOptions.SERVER_ONLY );
        };

    protected static FailableConsumer<LibertyServer, Exception> tearDownTestModules =
        (LibertyServer server) -> {
            ShrinkHelper.cleanAllExportedArchives();
        };

    protected static FailableConsumer<LibertyServer, Exception> setUpTestApp =
        (LibertyServer server) -> {
            ShrinkHelper.exportAppToServer( server, CommonTests.createTestEar(), DeployOptions.SERVER_ONLY );
        };

    protected static FailableConsumer<LibertyServer, Exception> tearDownTestApp =
        (LibertyServer server) -> {
            ShrinkHelper.cleanAllExportedArchives();
        };

    protected static void commonSetUp(
        Class<?> testClass,
        String serverConfig,
        FailableConsumer<LibertyServer, Exception> appSetUp) throws Exception {

        appSetUp.accept(server); // throws Exception

        server.setServerConfigurationFile(serverConfig);

        server.copyFileToLibertyInstallRoot("lib/features", "features/libertyinternals-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib", "bundles/ddmodel.jar");

        server.startServer( testClass.getClass().getSimpleName() + ".log" );
    }

    protected static void commonTearDown(
        Class<?> testClass,
        FailableConsumer<LibertyServer, Exception> appTearDown,
        String[] expectedErrors) throws Exception {

        server.stopServer(expectedErrors);

        server.deleteFileFromLibertyInstallRoot("lib/ddmodel_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot("lib/features/libertyinternals-1.0.mf");

        appTearDown.accept(server); // throws Exception
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
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();
        if ( !"OK".equals(line) ) {
            Log.info( testClass, methodName, description + ": FAILED" );
            Assert.fail("Unexpected response: " + line);
        } else {
            Log.info( testClass, methodName, description + ": PASSED" );
        }
    }

    public static class ErrorTest {
        public final String initialLine;
        public final String finalLine;
        public final String configSuffix;
        public final String description;
        public final String expectedErrors;

        public ErrorTest(String initialLine, String finalLine, String configSuffix,
                         String description,
                         String expectedErrors) {

            this.initialLine = initialLine;
            this.finalLine = finalLine;
            this.configSuffix = configSuffix;
            this.description = description;
            this.expectedErrors = expectedErrors;
        }
    }

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
            Assert.assertEquals(description + ": Incorrect count of replacements", replacements, 1);
        }

        String errorMessage;

        server.saveServerConfiguration(); // throws Exception
        server.setMarkToEndOfLog(); // throws Exception
        try {
            server.setServerConfigurationFromFilePath(newConfigPath); // throws Exception
            errorMessage = server.waitForStringInLogUsingMark(errorTest.expectedErrors);
        } finally {
            server.restoreServerConfiguration(); // throws Exception
        }

        if ( errorMessage == null ) {
            Log.info(testClass, methodName, description + "FAILED");
            Assert.assertNotNull(errorTest.description, errorMessage);
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
