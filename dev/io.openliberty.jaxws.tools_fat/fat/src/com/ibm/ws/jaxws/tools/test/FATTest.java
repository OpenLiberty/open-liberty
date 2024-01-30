/*******************************************************************************
 * Copyright (c) 2012, 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxws.tools.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jaxws.fat.util.ExplodedShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class FATTest {
    private static Class<?> c = FATTest.class;

    @Server("com.ibm.ws.jaxws.tools.TestServer")
    public static LibertyServer server;

    //private static final String TEST_WSDL_LOCATION = server.//FATTest.class.getResource("PeopleService.wsdl").getPath();
    private static String TEST_WSDL_LOCATION;

    private static String TEST_PACKAGE_DIR;

    private RemoteFile wsimportSrcDir;
    private RemoteFile wsimportClassesDir;
    private RemoteFile outputResourceDir;
    private RemoteFile wsgenSrcDir;
    private RemoteFile wsgenClassesDir;
    private String toolsDirectory;

    private RemoteFile wsgen;
    private RemoteFile wsgenBat;

    private RemoteFile wsimport;
    private RemoteFile wsimportBat;

    /**
     * True if running on Windows and the .bat file should be used.
     */
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
    /**
     * True if running on IBM i and a different shell should be used.
     */
    private static final boolean isIBMi = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("os/400");
    /**
     * Environment variable that can be set to test the UNIX script on Windows.
     */
    private static final String WLP_CYGWIN_HOME = System.getenv("WLP_CYGWIN_HOME");

    @BeforeClass
    public static void setup() throws Exception {

        WebArchive war = ExplodedShrinkHelper.explodedDropinApp(server, "PeopleService", "com.ibm.ws.jaxws.test.wsr.server",
                                                                "com.ibm.ws.jaxws.test.wsr.server.impl");
        war.setWebXML(new File("test-applications/PeopleService/resources/WEB-INF/web.xml"));
        // copy httpConduitProperties and make httpConduitProperties2
        String localLocation = "publish/servers/" + server.getServerName() + "/dropins/";
        File outputFile = new File(localLocation);
        outputFile.mkdirs();
        File explodedFile = war.as(ExplodedExporter.class).exportExploded(outputFile, "PeopleService.war");
        if (JakartaEEAction.isEE9OrLaterActive()) {
            JakartaEEAction.transformApp(explodedFile.toPath());
        }
        ExplodedShrinkHelper.copyFileToDirectory(server, outputFile, "dropins");

        server.startServer();

        TEST_WSDL_LOCATION = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/PeopleService/PeopleService?wsdl").toString();

        TEST_PACKAGE_DIR = new StringBuilder().append("com").append(File.separator).append("ibm").append(File.separator).append("ws").append(File.separator).append("jaxws").append(File.separator).append("test").append(File.separator).append("wsr").append(File.separator).append("server").toString();
    }

    @Before
    public void start() throws Exception {
        if (JakartaEEAction.isEE9OrLaterActive()) {
            toolsDirectory = "xmlWS";
        } else {

            toolsDirectory = "jaxws";
        }
        wsimportSrcDir = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsimportSrc");
        wsimportClassesDir = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsimportClasses");

        wsgenSrcDir = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsgenSrc");
        wsgenClassesDir = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsgenClasses");

        outputResourceDir = server.getFileFromLibertyServerRoot("temp" + File.separator + "resource");

        String wsgenPath = new StringBuilder().append("bin").append(File.separator).append(toolsDirectory).append(File.separator).append("wsgen").toString();
        String wsgenBatPath = new StringBuilder().append("bin").append(File.separator).append(toolsDirectory).append(File.separator).append("wsgen.bat").toString();

        wsgen = server.getFileFromLibertyInstallRoot(wsgenPath);
        wsgenBat = server.getFileFromLibertyInstallRoot(wsgenBatPath);

        String wsimportPath = new StringBuilder().append("bin").append(File.separator).append(toolsDirectory).append(File.separator).append("wsimport").toString();
        String wsimportBatPath = new StringBuilder().append("bin").append(File.separator).append(toolsDirectory).append(File.separator).append("wsimport.bat").toString();

        wsimport = server.getFileFromLibertyInstallRoot(wsimportPath);
        wsimportBat = server.getFileFromLibertyInstallRoot(wsimportBatPath);
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testWsGenTool() throws Exception {
        String path = FATTest.class.getResource("").getPath();
        int packageIndex = path.lastIndexOf("com");
        assertTrue("The path does not contain the package com.ibm.ws.jaxws.tools.test", packageIndex > 0);
        String packagePath = new StringBuilder().append("dropins").append(File.separator).append("PeopleService.war").append(File.separator).append("WEB-INF").append(File.separator).append("classes").toString();

        RemoteFile classpathFile = server.getFileFromLibertyServerRoot(packagePath);

        String wsgenArgs = new StringBuilder().append("-cp ").append(classpathFile.getAbsolutePath()).append(" -wsdl ").append("-s ").append(wsgenSrcDir).append(" -d ").append(wsgenClassesDir).append(" -r ").append(outputResourceDir).append(" com.ibm.ws.jaxws.test.wsr.server.impl.Bill").toString();

        assertTrue("The file bin/wsgen does not exist.", wsgen.exists());
        assertTrue("The file bin/wsgen.bat does not exist.", wsgenBat.exists());

        StringBuilder commandBuilder = new StringBuilder();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            commandBuilder.append(wsgenBat);
        } else {
            if (WLP_CYGWIN_HOME == null) {
                if (isIBMi) {
                    commandBuilder.append("/QOpenSys/usr/bin/sh"); // IBM i
                } else {
                    commandBuilder.append("/bin/sh");
                }
            } else {
                commandBuilder.append(WLP_CYGWIN_HOME + "/bin/sh");
            }
            commandBuilder.append(" -x ");
            commandBuilder.append(wsgen);
        }
        commandBuilder.append(" ").append(wsgenArgs);

        execute(commandBuilder.toString());

        RemoteFile wsdlFile = server.getFileFromLibertyServerRoot("temp" + File.separator + "resource" + File.separator + "PeopleService.wsdl");
        RemoteFile schemaFile = server.getFileFromLibertyServerRoot("temp" + File.separator + "resource" + File.separator + "PeopleService_schema1.xsd");

        RemoteFile helloSrc = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsgenSrc" + File.separator + TEST_PACKAGE_DIR + File.separator + "jaxws"
                                                                  + File.separator + "Hello.java");
        RemoteFile helloResponseSrc = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsgenSrc" + File.separator + TEST_PACKAGE_DIR + File.separator + "jaxws"
                                                                          + File.separator + "HelloResponse.java");

        RemoteFile helloClass = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsgenClasses" + File.separator + TEST_PACKAGE_DIR + File.separator + "jaxws"
                                                                    + File.separator + "Hello.class");
        RemoteFile helloResponseClass = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsgenClasses" + File.separator + TEST_PACKAGE_DIR + File.separator
                                                                            + "jaxws"
                                                                            + File.separator + "HelloResponse.class");

        assertTrue("Hello.java does not exist.", helloSrc.exists());
        assertTrue("HelloResponse.java does not exist.", helloResponseSrc.exists());

        assertTrue("Hello.class does not exist.", helloClass.exists());
        assertTrue("HelloResponse.class does not exist.", helloResponseClass.exists());

        assertTrue("PeopleService.wsdl does not exist.", wsdlFile.exists());
        assertTrue("PeopleService_schema1.xsd does not exist.", schemaFile.exists());

    }

    @Test
    public void testWsGenToolwithinlineSchemas() throws Exception {
        String path = FATTest.class.getResource("").getPath();
        int packageIndex = path.lastIndexOf("com");
        assertTrue("The path does not contain the package com.ibm.ws.jaxws.tools.test", packageIndex > 0);
        String packagePath = new StringBuilder().append("dropins").append(File.separator).append("PeopleService.war").append(File.separator).append("WEB-INF").append(File.separator).append("classes").toString();

        RemoteFile classpathFile = server.getFileFromLibertyServerRoot(packagePath);

        String wsgenArgs = new StringBuilder().append("-cp ").append(classpathFile.getAbsolutePath()).append(" -wsdl ").append("-inlineSchemas ").append("-s ").append(wsgenSrcDir).append(" -d ").append(wsgenClassesDir).append(" -r ").append(outputResourceDir).append(" com.ibm.ws.jaxws.test.wsr.server.impl.Bill").toString();

        assertTrue("The file bin/wsgen does not exist.", wsgen.exists());
        assertTrue("The file bin/wsgen.bat does not exist.", wsgenBat.exists());

        StringBuilder commandBuilder = new StringBuilder();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            commandBuilder.append(wsgenBat);
        } else {
            if (WLP_CYGWIN_HOME == null) {
                if (isIBMi) {
                    commandBuilder.append("/QOpenSys/usr/bin/sh"); // IBM i
                } else {
                    commandBuilder.append("/bin/sh");
                }
            } else {
                commandBuilder.append(WLP_CYGWIN_HOME + "/bin/sh");
            }
            commandBuilder.append(" -x ");
            commandBuilder.append(wsgen);
        }
        commandBuilder.append(" ").append(wsgenArgs);

        execute(commandBuilder.toString());

        RemoteFile wsdlFile = server.getFileFromLibertyServerRoot("temp" + File.separator + "resource" + File.separator + "PeopleService.wsdl");

        RemoteFile helloSrc = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsgenSrc" + File.separator + TEST_PACKAGE_DIR + File.separator + "jaxws"
                                                                  + File.separator + "Hello.java");
        RemoteFile helloResponseSrc = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsgenSrc" + File.separator + TEST_PACKAGE_DIR + File.separator + "jaxws"
                                                                          + File.separator + "HelloResponse.java");

        RemoteFile helloClass = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsgenClasses" + File.separator + TEST_PACKAGE_DIR + File.separator + "jaxws"
                                                                    + File.separator + "Hello.class");
        RemoteFile helloResponseClass = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsgenClasses" + File.separator + TEST_PACKAGE_DIR + File.separator
                                                                            + "jaxws"
                                                                            + File.separator + "HelloResponse.class");

        assertTrue("Hello.java does not exist.", helloSrc.exists());
        assertTrue("HelloResponse.java does not exist.", helloResponseSrc.exists());

        assertTrue("Hello.class does not exist.", helloClass.exists());
        assertTrue("HelloResponse.class does not exist.", helloResponseClass.exists());

        assertTrue("PeopleService.wsdl does not exist.", wsdlFile.exists());

    }

    @Test
    public void testWsGenToolError() throws Exception {

        assertTrue("The file bin/wsgen does not exist.", wsgen.exists());
        assertTrue("The file bin/wsgen.bat does not exist.", wsgenBat.exists());

        StringBuilder commandBuilder = new StringBuilder();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            commandBuilder.append(wsgenBat);
        } else {
            if (WLP_CYGWIN_HOME == null) {
                if (isIBMi) {
                    commandBuilder.append("/QOpenSys/usr/bin/sh"); // IBM i
                } else {
                    commandBuilder.append("/bin/sh");
                }
            } else {
                commandBuilder.append(WLP_CYGWIN_HOME + "/bin/sh");
            }
            commandBuilder.append(" -x ");
            commandBuilder.append(wsgen);
        }

        // No args should give exit code 1.
        execute(commandBuilder.toString(), 1);
    }

    @Test
    public void testWsImportTool() throws Exception {

        server.waitForStringInLog("CWWKZ0001I.*PeopleService");

        String wsimportArgs = new StringBuilder().append("-s ").append(wsimportSrcDir.getAbsolutePath()).append(" -d ").append(wsimportClassesDir.getAbsolutePath()).append(JakartaEEAction.isEE9OrLaterActive() ? " -target 3.0 " : " -target 2.2 ").append(TEST_WSDL_LOCATION).toString();

        assertTrue("The file bin/wsimport does not exist.", wsimport.exists());
        assertTrue("The file bin/wsimport.bat does not exist.", wsimportBat.exists());

        StringBuilder commandBuilder = new StringBuilder();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            commandBuilder.append(wsimportBat.getAbsolutePath());
        } else {
            if (WLP_CYGWIN_HOME == null) {
                if (isIBMi) {
                    commandBuilder.append("/QOpenSys/usr/bin/sh"); // IBM i
                } else {
                    commandBuilder.append("/bin/sh");
                }
            } else {
                commandBuilder.append(WLP_CYGWIN_HOME + "/bin/sh");
            }
            commandBuilder.append(" -x ");
            commandBuilder.append(wsimport.getAbsolutePath());
        }
        commandBuilder.append(" ").append(wsimportArgs);

        execute(commandBuilder.toString());

        RemoteFile helloSrc = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsimportSrc" + File.separator + TEST_PACKAGE_DIR + File.separator + "Hello.java");
        RemoteFile helloResponseSrc = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsimportSrc" + File.separator + TEST_PACKAGE_DIR + File.separator
                                                                          + "HelloResponse.java");
        RemoteFile objectFactorySrc = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsimportSrc" + File.separator + TEST_PACKAGE_DIR + File.separator
                                                                          + "ObjectFactory.java");
        RemoteFile packageInfoSrc = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsimportSrc" + File.separator + TEST_PACKAGE_DIR + File.separator
                                                                        + "package-info.java");
        RemoteFile peopleSrc = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsimportSrc" + File.separator + TEST_PACKAGE_DIR + File.separator + "People.java");
        RemoteFile peopleServiceSrc = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsimportSrc" + File.separator + TEST_PACKAGE_DIR + File.separator
                                                                          + "PeopleService.java");

        RemoteFile helloClass = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsimportClasses" + File.separator + TEST_PACKAGE_DIR + File.separator
                                                                    + "Hello.class");
        RemoteFile helloResponseClass = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsimportClasses" + File.separator + TEST_PACKAGE_DIR + File.separator
                                                                            + "HelloResponse.class");
        RemoteFile objectFactoryClass = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsimportClasses" + File.separator + TEST_PACKAGE_DIR + File.separator
                                                                            + "ObjectFactory.class");
        RemoteFile packageInfoClass = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsimportClasses" + File.separator + TEST_PACKAGE_DIR + File.separator
                                                                          + "package-info.class");
        RemoteFile peopleClass = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsimportClasses" + File.separator + TEST_PACKAGE_DIR + File.separator
                                                                     + "People.class");
        RemoteFile peopleServiceClass = server.getFileFromLibertyServerRoot("temp" + File.separator + "wsimportClasses" + File.separator + TEST_PACKAGE_DIR + File.separator
                                                                            + "PeopleService.class");

        assertTrue("Hello.java does not exist.", helloSrc.exists());
        assertTrue("HelloResponse.java does not exist.", helloResponseSrc.exists());
        assertTrue("ObjectFactory.java does not exist.", objectFactorySrc.exists());
        assertTrue("package-info.java does not exist.", packageInfoSrc.exists());
        assertTrue("People.java does not exist.", peopleSrc.exists());
        assertTrue("PeopleService.java does not exist.", peopleServiceSrc.exists());

        assertTrue("Hello.class does not exist.", helloClass.exists());
        assertTrue("HelloResponse.class does not exist.", helloResponseClass.exists());
        assertTrue("ObjectFactory.class does not exist.", objectFactoryClass.exists());
        assertTrue("package-info.class does not exist.", packageInfoClass.exists());
        assertTrue("People.class does not exist.", peopleClass.exists());
        assertTrue("PeopleService.class does not exist.", peopleServiceClass.exists());
    }

    @Test
    public void testWsImportToolError() throws Exception {

        assertTrue("The file bin/wsimport does not exist.", wsimport.exists());
        assertTrue("The file bin/wsimport.bat does not exist.", wsimportBat.exists());

        StringBuilder commandBuilder = new StringBuilder();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            commandBuilder.append(wsimportBat.getAbsolutePath());
        } else {
            if (WLP_CYGWIN_HOME == null) {
                if (isIBMi) {
                    commandBuilder.append("/QOpenSys/usr/bin/sh"); // IBM i
                } else {
                    commandBuilder.append("/bin/sh");
                }
            } else {
                commandBuilder.append(WLP_CYGWIN_HOME + "/bin/sh");
            }
            commandBuilder.append(" -x ");
            commandBuilder.append(wsimport.getAbsolutePath());
        }

        // No args should give exit code 1.
        execute(commandBuilder.toString(), 1);
    }

    // Not required for xmlWS-3.0
    @Test
    @SkipForRepeat({JakartaEEAction.EE9_ACTION_ID, JakartaEEAction.EE10_ACTION_ID})
    public void testWsImportToolWithoutTarget() throws Exception {

        server.waitForStringInLog("CWWKZ0001I.*PeopleService");

        String wsimportArgs = new StringBuilder().append("-s ").append(wsimportSrcDir.getAbsolutePath()).append(" -d ").append(wsimportClassesDir.getAbsolutePath()).append(" ").append(TEST_WSDL_LOCATION).toString();

        assertTrue("The file bin/wsimport does not exist.", wsimport.exists());
        assertTrue("The file bin/wsimport.bat does not exist.", wsimportBat.exists());

        StringBuilder commandBuilder = new StringBuilder();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            commandBuilder.append(wsimportBat.getAbsolutePath());
        } else {
            if (WLP_CYGWIN_HOME == null) {
                if (isIBMi) {
                    commandBuilder.append("/QOpenSys/usr/bin/sh"); // IBM i
                } else {
                    commandBuilder.append("/bin/sh");
                }
            } else {
                commandBuilder.append(WLP_CYGWIN_HOME + "/bin/sh");
            }
            commandBuilder.append(" -x ");
            commandBuilder.append(wsimport.getAbsolutePath());
        }
        commandBuilder.append(" ").append(wsimportArgs);
        String output = execute(commandBuilder.toString(), 1);
        assertTrue("The output should contain the error id 'CWWKW0800E', but the actual is not.", output.indexOf("CWWKW0800E") >= 0);
    }

    private String execute(String commandLine) throws IOException, InterruptedException {
        return execute(commandLine, 0);
    }

    private String execute(String commandLine, int expectedExitValue) throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        for (String arg : commandLine.split(" ")) {
            command.add(arg);
        }
        Log.info(c, "execute", "Run command: " + commandLine);

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(command);
        String javaHome = JavaInfo.forServer(server).javaHome();
        builder.environment().put("JAVA_HOME", javaHome);
        Log.info(c, "execute", "Using JAVA_HOME=" + javaHome);

        final Process p = builder.start();
        List<String> stdout = new ArrayList<String>();
        List<String> stderr = new ArrayList<String>();
        Thread outThread = inheritIO(p.getInputStream(), stdout);
        Thread errThread = inheritIO(p.getErrorStream(), stderr);

        outThread.join(60 * 1000);
        errThread.join(60 * 1000);
        p.waitFor();

        StringBuilder sb = new StringBuilder();
        Log.info(c, "execute", "Stdout:");
        for (String line : stdout) {
            sb.append(line).append('\n');
            Log.info(c, "execute", line);
        }
        Log.info(c, "execute", "Stderr:");
        for (String line : stderr) {
            sb.append(line).append('\n');
            Log.info(c, "execute", line);
        }

        int exitValue = p.exitValue();

        p.destroy();

        if (exitValue != expectedExitValue)
            throw new IOException(command.get(0) + " failed (exit=" + exitValue + ", expected " + expectedExitValue + "): " + sb.toString());

        return sb.toString();
    }

    private static Thread inheritIO(final InputStream src, final List<String> lines) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try (Scanner sc = new Scanner(src)) {
                    while (sc.hasNextLine()) {
                        lines.add(sc.nextLine());
                    }
                }
            }
        });
        t.start();
        return t;
    }
}
