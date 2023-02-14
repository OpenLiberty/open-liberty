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

import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.jaxws.fat.util.ExplodedShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
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

    private static Machine machine;
    private static String installRoot;

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
        if (JakartaEE9Action.isActive()) {
            JakartaEE9Action.transformApp(explodedFile.toPath());
        }
        ExplodedShrinkHelper.copyFileToDirectory(server, outputFile, "dropins");

        installRoot = server.getInstallRoot();
        machine = server.getMachine();
        
        server.startServer();

        TEST_WSDL_LOCATION = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/PeopleService/PeopleService?wsdl").toString();

        TEST_PACKAGE_DIR = new StringBuilder().append("com").append(File.separator).append("ibm").append(File.separator).append("ws").append(File.separator).append("jaxws").append(File.separator).append("test").append(File.separator).append("wsr").append(File.separator).append("server").toString();        
    }

    @Before
    public void start() throws Exception {
        if (JakartaEE9Action.isActive()) {
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

        String[] wsgenArgs = new String[] { "-cp", 
                                            classpathFile.getAbsolutePath(),
                                            "-wsdl",
                                            "-s",
                                            wsgenSrcDir.getAbsolutePath(),
                                            "-d",
                                            wsgenClassesDir.getAbsolutePath(),
                                            "-r",
                                            outputResourceDir.getAbsolutePath(),
                                            "com.ibm.ws.jaxws.test.wsr.server.impl.Bill" };

        assertTrue("The file bin/wsgen does not exist.", wsgen.exists());
        assertTrue("The file bin/wsgen.bat does not exist.", wsgenBat.exists());
        
        machine.execute(wsgen.getAbsolutePath(), wsgenArgs, installRoot);

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

        String[] wsgenArgs = new String[] { "-cp",
                                            classpathFile.getAbsolutePath(),
                                            "-wsdl",
                                            "-inlineSchemas",
                                            "-s",
                                            wsgenSrcDir.getAbsolutePath(),
                                            "-d",
                                            wsgenClassesDir.getAbsolutePath(),
                                            "-r",
                                            outputResourceDir.getAbsolutePath(),
                                            "com.ibm.ws.jaxws.test.wsr.server.impl.Bill" };

        assertTrue("The file bin/wsgen does not exist.", wsgen.exists());
        assertTrue("The file bin/wsgen.bat does not exist.", wsgenBat.exists());
        
        machine.execute(wsgen.getAbsolutePath(), wsgenArgs, installRoot);

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

        ProgramOutput po = machine.execute(wsgen.getAbsolutePath(), installRoot);
        
        // No args should give exit code 1.
        assertTrue("No args wsgen call should return 1", po.getReturnCode() == 1);
    }

    @Test
    public void testWsImportTool() throws Exception {

        server.waitForStringInLog("CWWKZ0001I.*PeopleService");

        String[] wsimportArgs = new String[] { "-s",
                                               wsimportSrcDir.getAbsolutePath(),
                                               "-d",
                                               wsimportClassesDir.getAbsolutePath(),
                                               JakartaEE9Action.isActive() ? "-target 3.0" : "-target 2.2",
                                               TEST_WSDL_LOCATION };

        assertTrue("The file bin/wsimport does not exist.", wsimport.exists());
        assertTrue("The file bin/wsimport.bat does not exist.", wsimportBat.exists());
        
        machine.execute(wsimport.getAbsolutePath(), wsimportArgs, installRoot);

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

        ProgramOutput po = machine.execute(wsimport.getAbsolutePath(), installRoot);
        
        // No args should give exit code 1.
        assertTrue("No args wsgen call should return 1", po.getReturnCode() == 1);
    }

    // Not required for xmlWS-3.0
    @Test
    @SkipForRepeat(JakartaEE9Action.ID)
    public void testWsImportToolWithoutTarget() throws Exception {

        server.waitForStringInLog("CWWKZ0001I.*PeopleService");

        String[] wsimportArgs = new String[] { "-s",
                                               wsimportSrcDir.getAbsolutePath(),
                                               "-d",
                                               wsimportClassesDir.getAbsolutePath(),
                                               TEST_WSDL_LOCATION };

        assertTrue("The file bin/wsimport does not exist.", wsimport.exists());
        assertTrue("The file bin/wsimport.bat does not exist.", wsimportBat.exists());
        
        ProgramOutput po = machine.execute(wsimport.getAbsolutePath(), wsimportArgs, installRoot);
        assertTrue("The wsimport call should return 1", po.getReturnCode() == 1);
        assertTrue("The output should contain the error id 'CWWKW0800E', but the actual is not.", po.getStdout().indexOf("CWWKW0800E") >= 0);
    }
}
