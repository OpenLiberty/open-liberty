/*******************************************************************************
 * Copyright (c) 2010, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants.VerifyServer;

import test.common.SharedOutputManager;
import test.shared.Constants;
import test.shared.TestUtils;

public class BootstrapConfigTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    static final File defaultServer = new File(Constants.TEST_TMP_ROOT, "defaultServer");

    @Rule
    public TestName testName = new TestName();

    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() {
        TestUtils.cleanTempFiles();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        TestUtils.cleanTempFiles();
    }

    Map<String, String> initProps = new HashMap<String, String>();
    BootstrapConfig bc = new TestBootstrapConfig(initProps);

    @Before
    public void setUp() {
        Constants.TEST_TMP_ROOT_FILE.mkdirs();

        // Create server/workarea directories...
        defaultServer.mkdirs();
    }

    @After
    public void tearDown() throws Exception {
        initProps.clear();
        TestUtils.cleanTempFiles();
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.boot.BootstrapConfig#mergeProperties(java.util.Map, java.net.URL, java.lang.String)} .
     */
    @Test
    public void testMergeProperties() {

        try {
            File f1 = new File(Constants.TEST_PLATFORM_DIR + "include.properties");

            // Test system property value override: compare with testProcessIncludes,
            // which just looks for original value un-influenced by system properties
            System.setProperty("override", "systemProperty");
            bc.mergeProperties(initProps, null, f1.toURI().toString());
            assertEquals("System property value should win", "systemProperty", bc.get("override"));
            assertEquals("Peer file should be found", "found", bc.get("peer"));
            assertEquals("Relative file should be found", "found", bc.get("relative"));
        } finally {
            System.clearProperty("override");
        }
    }

    @Test
    public void testMergePropertiesBadURL() {
        Map<String, String> map = new HashMap<String, String>();

        try {
            bc.mergeProperties(map, null, "unknown:junk");
            fail("Expected location exception was not thrown");
        } catch (LocationException e) {
            System.out.println(BootstrapConstants.messages.getString("error.badLocation"));
            System.out.println(e.getTranslatedMessage());

            // unable to resolve locations
            assertTrue(outputMgr.checkForStandardOut("CWWKE0004E"));
            // malformed URI for bootstrap properties
            assertTrue(outputMgr.checkForStandardOut("CWWKE0008E"));
        }
    }

    @Test
    public void testMergePropertiesBadFile() {
        Map<String, String> map = new HashMap<String, String>();

        try {
            bc.mergeProperties(map, null, new File(Constants.TEST_PLATFORM_DIR + "notexist").toURI().toString());
        } catch (LocationException ex) {
            assertTrue(ex.getTranslatedMessage().contains("CWWKE0014E"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.boot.BootstrapConfig#addMissingProperties(java.util.Properties, java.util.Map)} .
     */
    @Test
    public void testAddMissingProperties() {
        final String key1 = "existingKey", value1 = "existingValue", key2 = "newKey", value2 = "newValue", value3 = "replaceValue";

        initProps.put(key1, value1);

        Properties source = new Properties();
        // add replacement value; should be ignored
        source.setProperty(key1, value3);
        source.setProperty(key2, value2);

        bc.addMissingProperties(source, initProps);

        assertEquals("Original value should be preserved", value1, initProps.get(key1));
        assertEquals("New key should be set", value2, initProps.get(key2));

        // Should not blow up w/ null/empty source or target
        bc.addMissingProperties(null, initProps);
        source.clear();
        bc.addMissingProperties(source, initProps);
        bc.addMissingProperties(source, null);

        // Should not blow up w/ zero-length key
        source.setProperty("", "blah");
        bc.addMissingProperties(source, initProps);
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.boot.BootstrapConfig#processIncludes(java.util.Map, java.net.URL, java.lang.String)} . Most valid paths are tested via
     * mergeProperties
     */
    @Test
    public void testProcessIncludes() {
        bc.processIncludes(initProps, null, null);

        File f1 = new File(Constants.TEST_PLATFORM_DIR + "include.properties");
        String fname = f1.toURI().toString();
        bc.processIncludes(initProps, null, fname + " ");
        // Compare with testMergeProperties
        assertEquals("Original value should be present", "original", bc.get("override"));
        assertEquals("Peer file should be found", "found", bc.get("peer"));
        assertEquals("Relative file should be found", "found", bc.get("relative"));

        bc.processIncludes(initProps, null, fname + " ,\t" + fname);
    }

    /**
     * Expect an IllegalArgumentException when null is passed to configure Test
     * method for {@link com.ibm.ws.kernel.boot.BootstrapConfig#configure(java.util.Map)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConfigureNull() {
        TestBootstrapConfig bc = new TestBootstrapConfig();

        // configure(map)
        bc.configure(null);
    }

    /**
     * Expect a LocationException when an unresolvable file is used as install dir
     *
     * Test method for {@link com.ibm.ws.kernel.boot.BootstrapConfig#configure(java.util.Map)}.
     *
     * @throws IOException
     */
    @Test(expected = com.ibm.ws.kernel.boot.LocationException.class)
    public void testConfigureBadLocation() throws IOException {
        String fName = "InstallDirAsFile";
        File file = new File(Constants.TEST_TMP_ROOT, fName);
        file.createNewFile();
        file.deleteOnExit();

        TestBootstrapConfig bc = new TestBootstrapConfig();
        bc.findLocations(testName.getMethodName(), file.getAbsolutePath(), null, null, null);
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.boot.BootstrapConfig#configure(java.util.Map)}.
     */
    @Test
    public void testFindLocations() throws Exception {
        File test1 = new File(Constants.TEST_TMP_ROOT, "test1");

        try {

            bc = new TestBootstrapConfig();
            bc.findLocations(null, null, null, null, null);

            bc.printLocations(false); // print locations: no formatting
            assertTrue("Bootstrap lib dir should be a directory (not a jar)", bc.bootstrapLib.isDirectory());

            checkDirs("A", bc);
            assertEquals("A: Default server name", BootstrapConstants.DEFAULT_SERVER_NAME, bc.getProcessName());
            assertEquals("A: installRoot should be parent of bootstrap lib", bc.installRoot, bc.bootstrapLib.getParentFile());
            assertEquals("A: userRoot should be child of installRoot", bc.installRoot, bc.userRoot.getParentFile());
            assertEquals("A: processesRoot should be a child of the userRoot", bc.userRoot, bc.processesRoot.getParentFile());
            assertEquals("A: configDir should be a child of the processesRoot", bc.processesRoot, bc.configDir.getParentFile());
            assertSame("A: outputRoot should be same as processesRoot", bc.processesRoot, bc.outputRoot);
            assertSame("A: outputDir should be same as configDir", bc.configDir, bc.outputDir);

            bc = new TestBootstrapConfig();
            bc.findLocations(testName.getMethodName(), test1.getAbsolutePath(), null, null, null);

            checkDirs("B", bc);
            assertEquals("B: userRoot should match userDir parameter", test1.getCanonicalFile(), bc.userRoot.getCanonicalFile());
            assertEquals("B: processesRoot should be a child of the userRoot", bc.userRoot, bc.processesRoot.getParentFile());
            assertEquals("B: configDir should be a child of the processesRoot", bc.processesRoot, bc.configDir.getParentFile());
            assertEquals("B: getServerFile(null) should return configDir", bc.configDir, bc.getConfigFile(null));

            assertSame("B: outputRoot should be same as processesRoot", bc.processesRoot, bc.outputRoot);
            assertSame("B: outputDir should be same as configDir", bc.configDir, bc.outputDir);
            assertEquals("B: getServerOutputFile(null) should return outputDir", bc.outputDir, bc.getOutputFile(null));

            // Now test for the output dir split: we now have two trees... (one shorter
            // than the other.. )
            bc = new TestBootstrapConfig();
            bc.findLocations(testName.getMethodName(), null, test1.getAbsolutePath(), null, null);

            checkDirs("C", bc);
            assertEquals("C: userRoot should be child of installRoot", bc.installRoot, bc.userRoot.getParentFile());
            assertEquals("C: processesRoot should be a child of the userRoot", bc.userRoot, bc.processesRoot.getParentFile());
            assertEquals("C: configDir should be a child of the processesRoot", bc.processesRoot, bc.configDir.getParentFile());
            assertEquals("C: getServerFile(null) should return configDir", bc.configDir, bc.getConfigFile(null));

            assertEquals("C: outputRoot should match outputDir parameter", test1.getCanonicalFile(), bc.outputRoot.getCanonicalFile());
            assertEquals("C: outputDir should be a child of the outputRoot", bc.outputRoot, bc.outputDir.getParentFile());
            assertEquals("C: getServerOutputFile(null) should return outputDir", bc.outputDir, bc.getOutputFile(null));

            assertEquals("C: getLogDiretory() should be a child of outputDir", bc.getOutputFile("logs"), bc.getLogDirectory());

            // Now test for a separate log directory
            bc = new TestBootstrapConfig();
            bc.findLocations(testName.getMethodName(), null, null, test1.getAbsolutePath(), null);

            checkDirs("D", bc);
            assertEquals("D: userRoot should be child of installRoot", bc.installRoot, bc.userRoot.getParentFile());
            assertEquals("D: processesRoot should be a child of the userRoot", bc.userRoot, bc.processesRoot.getParentFile());
            assertEquals("D: configDir should be a child of the processesRoot", bc.processesRoot, bc.configDir.getParentFile());
            assertEquals("D: getServerFile(null) should return configDir", bc.configDir, bc.getConfigFile(null));

            assertSame("D: outputRoot should be same as processesRoot", bc.processesRoot, bc.outputRoot);
            assertSame("D: outputDir should be same as configDir", bc.configDir, bc.outputDir);
            assertEquals("D: getServerOutputFile(null) should return outputDir", bc.outputDir, bc.getOutputFile(null));

            assertEquals("D: getLogDiretory() should match logDir parameter", test1.getCanonicalFile(), bc.getLogDirectory().getCanonicalFile());

            initProps.clear();

            // Make sure system properties are ignored
            System.setProperty(BootstrapConstants.LOC_PROPERTY_INSTANCE_DIR, test1.getAbsolutePath());
            bc = new TestBootstrapConfig();
            // configure(map, userDir, outputDir, logDir)
            bc.findLocations(testName.getMethodName(), null, null, null, null);

            // This set should be identical to the conditions used in A (i.e. the defaults,
            //  as null is passed in as a parameters)
            checkDirs("E", bc);
            assertEquals("E: userRoot should be child of installRoot", bc.installRoot, bc.userRoot.getParentFile());
            assertEquals("E: processesRoot should be a child of the userRoot", bc.userRoot, bc.processesRoot.getParentFile());
            assertEquals("E: configDir should be a child of the processesRoot", bc.processesRoot, bc.configDir.getParentFile());
            assertSame("E: outputRoot should be same as processesRoot", bc.processesRoot, bc.outputRoot);
            assertSame("E: outputDir should be same as configDir", bc.configDir, bc.outputDir);

            // Embedded workarea for utilities
            bc = new TestBootstrapConfig();
            bc.findLocations(testName.getMethodName(), null, null, null, null, BootstrapConstants.LOC_AREA_NAME_WORKING_UTILS);
            assertTrue("F: workarea should be child of outputDir/workarea: ", new File(bc.outputDir, "workarea").equals(bc.workarea.getParentFile()));

        } finally {
            TestUtils.cleanTempFiles(test1);
            System.clearProperty(BootstrapConstants.LOC_PROPERTY_INSTANCE_DIR);
            System.clearProperty(BootstrapConstants.LOC_PROPERTY_INSTALL_DIR);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.boot.BootstrapConfig#setSystemProperties()}.
     */
    @Test
    public void testSetSystemProperty() {
        final String key1 = "existingKey", value1 = "existingValue", key2 = "newKey", value2 = "newValue", nonExistentKey = "nonExistentKey";

        String javaSecurityProp = "websphere.java.security";

        bc.setSystemProperties();
        assertNull("System property test value did not return null as expected.", System.getProperty(key1));
        assertNull("System property test value did not return null as expected.", System.getProperty(key2));
        assertNull("Non existent system property did not return null as expected.", System.getProperty(nonExistentKey));
        assertNull("Websphere Java security property did not return null as expected.", System.getProperty(javaSecurityProp));

        initProps.put(key1, value1);
        initProps.put(key2, value2);
        bc.setSystemProperties();
        assertEquals("System property test value did not equal expected value.", value1, System.getProperty(key1));
        assertEquals("System property test value did not equal expected value.", value2, System.getProperty(key2));
        assertNull("Non existent system property did not return null as expected.", System.getProperty(nonExistentKey));
        assertNull("System property " + javaSecurityProp + " did not return null as expected.", System.getProperty(javaSecurityProp));

        initProps.put(javaSecurityProp, "true");
        bc.setSystemProperties();
        assertEquals("System property test value did not equal expected value.", value1, System.getProperty(key1));
        assertEquals("System property test value did not equal expected value.", value2, System.getProperty(key2));
        assertNull("Non existent system property did not return null as expected.", System.getProperty(nonExistentKey));
        assertNull("System property " + javaSecurityProp + " did not return null as expected.", System.getProperty(javaSecurityProp));
    }

    /**
     * Prepare a server directory and BootstrapConfig for verification.
     */
    private BootstrapConfig prepareServer(String serverName) {
        File usrDir = new File(Constants.TEST_TMP_ROOT);
        File serverDir = new File(usrDir, "servers" + File.separatorChar + serverName);
        TestUtils.cleanTempFiles(serverDir);
        bc = new BootstrapConfig();
        bc.findLocations(serverName, Constants.TEST_TMP_ROOT, null, null, null);
        return bc;
    }

    private static final String[] SERVER_NAMES = new String[] { "defaultServer", "newServer" };

    private BootstrapConfig prepareDefaultServer() {
        return prepareServer("defaultServer");
    }

    private BootstrapConfig prepareNewServer() {
        return prepareServer("newServer");
    }

    /**
     * The type of templates that can be used when creating a server.
     */
    private enum ServerTemplateType {
        /** Liberty templates (wlp/templates/servers/defaultServer/) */
        LIBERTY,
        /** --template=test (files created by {@link #runTemplateTest}) */
        TEST,
        /** Kernel template (boot JAR, used when no other templates found) */
        KERNEL,
    }

    /**
     * Prepare bootstrap statics to allow templates to be found, and then run
     * the specified action.
     */
    private void withMockTemplatesDirectory(ServerTemplateType templateType, PrivilegedExceptionAction<Void> action) throws Exception {
        File installDir;
        switch (templateType) {
            case TEST:
                installDir = new File(Constants.TEST_TMP_ROOT_FILE, "install");
                break;
            case LIBERTY:
                installDir = new File(Constants.BOOTSTRAP_PUBLISH_DIR);
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(templateType));
        }

        TestUtils.setKernelUtilsBootstrapLibDir(new File(installDir, "lib"));
        try {
            if (templateType == ServerTemplateType.TEST) {
                File templateDir = new File(installDir, "templates/servers/test");
                assertTrue("created " + templateDir, templateDir.mkdirs());
                PrintWriter pw = new PrintWriter(new File(templateDir, "server.xml"), "UTF-8");
                pw.println("<server description='test'/>");
                pw.close();
            }

            action.run();
        } finally {
            TestUtils.setKernelUtilsBootstrapLibDir(null);
        }
    }

    /**
     * Analyze server.xml to determine which server template was used.
     */
    private ServerTemplateType getServerTemplateType(File serverConfig) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(serverConfig));
        Element element = doc.getDocumentElement();
        assertEquals("server", element.getNodeName());

        if ("test".equals(element.getAttribute("description"))) {
            // A test server.xml create by runTemplateTest above.
            return ServerTemplateType.TEST;
        }

        if (element.getElementsByTagName("featureManager").getLength() > 0) {
            // /com.ibm.ws.kernel.boot/publish/servers/defaultServer/server.xml
            // aka wlp/templates/servers/defaultServer/server.xml.
            return ServerTemplateType.LIBERTY;
        }

        // Otherwise, assume it was the "kernel default" server.xml, which
        // (currently?) has no features enabled.
        // /com.ibm.ws.kernel.boot/resources/OSGI-OPT/websphere/server/server.xml
        // aka wlp/lib/com.ibm.ws.kernel_*.jar!/OSGI-OPT/websphere/server/server.xml
        return ServerTemplateType.KERNEL;
    }

    /**
     * VerifyServer.EXISTS with a missing server = SERVER_NOT_EXIST_STATUS
     */
    @Test
    public void testVerifyServerExistsError() {
        for (String serverName : SERVER_NAMES) {
            try {
                prepareServer(serverName).verifyProcess(VerifyServer.EXISTS, null);
            } catch (LaunchException e) {
                assertEquals(ReturnCode.SERVER_NOT_EXIST_STATUS, e.getReturnCode());
            }
        }
    }

    /**
     * VerifyServer.EXISTS after creating should succeed.
     */
    @Test
    public void testVerifyServerExists() throws Exception {
        for (String serverName : SERVER_NAMES) {
            prepareServer(serverName).verifyProcess(VerifyServer.CREATE, null);
            bc.verifyProcess(VerifyServer.EXISTS, null);
        }
    }

    /**
     * VerifyServer.CREATE should use the default kernel template when creating.
     */
    @Test
    public void testVerifyServerCreate() throws Exception {
        for (String serverName : SERVER_NAMES) {
            prepareServer(serverName).verifyProcess(VerifyServer.CREATE, null);
            assertEquals(ServerTemplateType.KERNEL, getServerTemplateType(bc.getConfigFile(BootstrapConstants.SERVER_XML)));
        }
    }

    /**
     * VerifyServer.CREATE should use wlp/templates if present when creating.
     */
    @Test
    public void testVerifyServerCreateKernelTemplate() throws Exception {
        withMockTemplatesDirectory(ServerTemplateType.LIBERTY, new PrivilegedExceptionAction<Void>() {
            @Override
            public Void run() throws Exception {
                for (String serverName : SERVER_NAMES) {
                    prepareServer(serverName).verifyProcess(VerifyServer.CREATE, null);
                    assertEquals(ServerTemplateType.LIBERTY, getServerTemplateType(bc.getConfigFile(BootstrapConstants.SERVER_XML)));
                }
                return null;
            }
        });
    }

    /**
     * VerifyServer.CREATE with invalid --template = LAUNCH_EXCEPTION + error.fileNotFound.
     */
    @Test
    public void testVerifyServerCreateWithInvalidTemplate() throws Exception {
        try {
            String[] args = new String[] { "--template=invalid" };
            List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
            prepareDefaultServer().verifyProcess(VerifyServer.CREATE, new LaunchArguments(cmdArgs, null));
        } catch (LaunchException e) {
            assertEquals(ReturnCode.LAUNCH_EXCEPTION, e.getReturnCode());
            assertTrue(e.getTranslatedMessage(), e.getTranslatedMessage().startsWith("CWWKE0054E:"));
        }
    }

    /**
     * VerifyServer.CREATE with --template should use that template.
     */
    @Test
    public void testVerifyServerCreateWithTemplate() throws Exception {
        withMockTemplatesDirectory(ServerTemplateType.TEST, new PrivilegedExceptionAction<Void>() {
            @Override
            public Void run() throws Exception {
                for (String serverName : SERVER_NAMES) {
                    String[] args = new String[] { "--template=test" };
                    List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
                    prepareServer(serverName).verifyProcess(VerifyServer.CREATE, new LaunchArguments(cmdArgs, null));
                    assertEquals(ServerTemplateType.TEST, getServerTemplateType(bc.getConfigFile(BootstrapConstants.SERVER_XML)));
                }
                return null;
            }
        });
    }

    /**
     * VerifyServer.CREATE after creating a server = REDUNDANT_ACTION_STATUS.
     */
    @Test
    public void testVerifyServerCreateAlreadyExists() {
        for (String serverName : SERVER_NAMES) {
            try {
                prepareServer(serverName).verifyProcess(VerifyServer.CREATE, null);
                bc.verifyProcess(VerifyServer.CREATE, null);
            } catch (LaunchException e) {
                assertEquals(ReturnCode.REDUNDANT_ACTION_STATUS, e.getReturnCode());
            }
        }
    }

    /**
     * VerifyServer.CREATE_DEFAULT should use the default kernel template when
     * creating defaultServer.
     */
    @Test
    public void testVerifyServerCreateDefaultKernelTemplate() throws Exception {
        prepareDefaultServer().verifyProcess(VerifyServer.CREATE_DEFAULT, null);
        assertEquals(ServerTemplateType.KERNEL, getServerTemplateType(bc.getConfigFile(BootstrapConstants.SERVER_XML)));
    }

    /**
     * VerifyServer.CREATE_DEFAULT should use wlp/templates if present when
     * creating defaultServer.
     */
    @Test
    public void testVerifyServerCreateDefaultLibertyTemplate() throws Exception {
        withMockTemplatesDirectory(ServerTemplateType.LIBERTY, new PrivilegedExceptionAction<Void>() {
            @Override
            public Void run() throws Exception {
                prepareDefaultServer().verifyProcess(VerifyServer.CREATE_DEFAULT, null);
                assertEquals(ServerTemplateType.LIBERTY, getServerTemplateType(bc.getConfigFile(BootstrapConstants.SERVER_XML)));
                return null;
            }
        });
    }

    /**
     * VerifyServer.CREATE_DEFAULT should ignore --template when creating
     * defaultServer.
     */
    @Test
    public void testVerifyServerCreateDefaultWithTemplate() throws Exception {
        // This will fail if CREATE_DEFAULT erroneously checks --template.
        String[] args = new String[] { "--template=invalid" };
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
        prepareDefaultServer().verifyProcess(VerifyServer.CREATE_DEFAULT, new LaunchArguments(cmdArgs, null));
        assertEquals(ServerTemplateType.KERNEL, getServerTemplateType(bc.getConfigFile(BootstrapConstants.SERVER_XML)));
    }

    /**
     * VerifyServer.CREATE_DEFAULT should ignore --template when creating
     * defaultServer, even if an empty directory already exists.
     */
    @Test
    public void testVerifyServerCreateDefaultForEmptyDirWithTemplate() throws Exception {
        assertTrue("created server directory", prepareDefaultServer().getConfigFile(null).mkdirs());
        // This will fail if CREATE_DEFAULT erroneously checks --template.
        String[] args = new String[] { "--template=invalid" };
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
        bc.verifyProcess(VerifyServer.CREATE_DEFAULT, new LaunchArguments(cmdArgs, null));
        assertEquals(ServerTemplateType.KERNEL, getServerTemplateType(bc.getConfigFile(BootstrapConstants.SERVER_XML)));
    }

    @Test
    public void testVerifyServerEnvAppend() throws Exception {

    }

    /**
     * VerifyServer.CREATE_DEFAULT is like EXISTS for non-defaultServer:
     * non-existent server = SERVER_NOT_EXIST_STATUS
     */
    @Test
    public void testVerifyServerCreateDefaultError() {
        try {
            prepareNewServer().verifyProcess(VerifyServer.CREATE_DEFAULT, null);
        } catch (LaunchException e) {
            assertEquals(ReturnCode.SERVER_NOT_EXIST_STATUS, e.getReturnCode());
        }
    }

    /**
     * VerifyServer.CREATE_DEFAULT is a no-op for an existing server.
     */
    @Test
    public void testVerifyServerCreateDefaultAlreadyExists() {
        for (String serverName : SERVER_NAMES) {
            prepareServer(serverName).verifyProcess(VerifyServer.CREATE, null);
            bc.verifyProcess(VerifyServer.CREATE_DEFAULT, null);
        }
    }

    /**
     * VerifyServer.SKIP (or null) is a no-op regardless of whether or not
     * the server exists.
     */
    @Test
    public void testVerifyServerSkip() throws Exception {
        for (String serverName : SERVER_NAMES) {
            prepareServer(serverName).verifyProcess(null, null);
            bc.verifyProcess(VerifyServer.SKIP, null);

            // Create the server and try again.
            bc.verifyProcess(VerifyServer.CREATE, null);
            bc.verifyProcess(null, null);
            bc.verifyProcess(VerifyServer.SKIP, null);
        }
    }

    @Test
    public void testNewServer() throws Exception {
        File commonFile = new File(Constants.TEST_TMP_ROOT);
        File newServerDir = new File(commonFile, "servers/newServer");

        try {
            TestUtils.cleanTempFiles(newServerDir);

            // Find the new server (includes mapping to canonical name, etc.)
            bc = new BootstrapConfig();
            bc.findLocations("newServer", Constants.TEST_TMP_ROOT, null, null, null);
            System.out.println(newServerDir.toURI().toString());

            // Invoke configure with property indicating that the server should be created
            bc.verifyProcess(VerifyServer.CREATE, null);

            assertTrue("I: new server should have been created", newServerDir.exists() && newServerDir.isDirectory());
            assertEquals("I: intended server should be created", newServerDir.getCanonicalFile(), bc.configDir.getCanonicalFile());

            File sFile = new File(bc.configDir, "server.xml");
            assertTrue("I: new server should have server.xml file created", sFile.exists() && sFile.isFile());
        } finally {
            TestUtils.cleanTempFiles(newServerDir.getParentFile()); // servers dir
        }
    }

    @Test
    public void testNewServerExistingKeystorePassword() throws Exception {
        File commonFile = new File(Constants.TEST_TMP_ROOT);
        File newServerDir = new File(commonFile, "servers/newEnvServer");

        try {
            TestUtils.cleanTempFiles(newServerDir);

            // Simulate a java8 JDK
            initProps.put("java.specification.version", "1.8");
            File serverEnv = TestUtils.createTempFile("server", "env");

            // Simulate creation of a server.env template file
            BufferedWriter bw = new BufferedWriter(new FileWriter(serverEnv));
            bw.write("WLP_MY_ENV_VAR=true\n");
            bw.write("keystore_password=liberty");
            bw.close();

            //Create bootstrap props to make a new server from.
            bc = new ServerEnvTestBootstrapConfig(serverEnv, initProps);
            bc.findLocations("newEnvServer", Constants.TEST_TMP_ROOT, null, null, null);
            System.out.println(newServerDir.toURI().toString());

            // Invoke configure with property indicating that the server should be created
            bc.verifyProcess(VerifyServer.CREATE, null);

            //Sanity check that a server was created
            assertTrue("new server should have been created", newServerDir.exists() && newServerDir.isDirectory());
            assertEquals("intended server should be created", newServerDir.getCanonicalFile(), bc.configDir.getCanonicalFile());

            //Real testing lines. Verify server.env was appended to instead of overwritten, and keystore_password was preserved
            BufferedReader br = new BufferedReader(new FileReader(serverEnv));
            String customAdditionLine = br.readLine();
            String keystorePasswordLine = br.readLine();
            String maxPermSizeLine = br.readLine();
            String extraLine = br.readLine();
            br.close();
            assertNull("server.env should only have 3 lines, but found an extra line: " + extraLine, extraLine);

            System.out.println(customAdditionLine);
            System.out.println(keystorePasswordLine);
            System.out.println(maxPermSizeLine);

            assertTrue("Server env file should contain WLP_MY_ENV_VAR=true as the first line", "WLP_MY_ENV_VAR=true".equals(customAdditionLine));
            assertTrue("Server env file should contain keystore_password=liberty as the second line but was: " + keystorePasswordLine,
                       "keystore_password=liberty".equals(keystorePasswordLine));
        } finally {
            TestUtils.cleanTempFiles(newServerDir.getParentFile()); // servers dir
        }
    }

    @Test
    public void testNewServerCustomServerEnvAppend() throws Exception {
        File commonFile = new File(Constants.TEST_TMP_ROOT);
        File newServerDir = new File(commonFile, "servers/newEnvServer");

        try {
            TestUtils.cleanTempFiles(newServerDir);

            // Simulate a java8 JDK
            initProps.put("java.specification.version", "1.8");
            File serverEnv = TestUtils.createTempFile("server", "env");

            //Simulate creation of a server.env template file
            BufferedWriter bw = new BufferedWriter(new FileWriter(serverEnv));
            bw.write("WLP_MY_ENV_VAR=true");
            bw.close();

            //Create bootstrap props to make a new server from.
            bc = new ServerEnvTestBootstrapConfig(serverEnv, initProps);
            bc.findLocations("newEnvServer", Constants.TEST_TMP_ROOT, null, null, null);
            System.out.println(newServerDir.toURI().toString());

            // Invoke configure with property indicating that the server should be created
            bc.verifyProcess(VerifyServer.CREATE, null);

            //Sanity check that a server was created
            assertTrue("new server should have been created", newServerDir.exists() && newServerDir.isDirectory());
            assertEquals("intended server should be created", newServerDir.getCanonicalFile(), bc.configDir.getCanonicalFile());

            //Real testing lines. Verify server.env was appended to instead of overwritten.
            BufferedReader br = new BufferedReader(new FileReader(serverEnv));
            String customAdditionLine = br.readLine();
            String keystorePasswordLine = br.readLine();
            String maxPermSizeLine = br.readLine();
            String extraLine = br.readLine();
            br.close();
            assertNull("server.env should only have 3 lines, but found an extra line: " + extraLine, extraLine);

            System.out.println(customAdditionLine);
            System.out.println(keystorePasswordLine);
            System.out.println(maxPermSizeLine);

            assertTrue("Server env file should contain WLP_MY_ENV_VAR=true as the first line", "WLP_MY_ENV_VAR=true".equals(customAdditionLine));
            assertTrue("Server env file should contain keystore_password=... as the second line but was: " + keystorePasswordLine,
                       keystorePasswordLine.startsWith("keystore_password="));
            assertEquals("Generated keystore password should be 23 chars long: " + keystorePasswordLine, 23,
                         keystorePasswordLine.substring("keystore_password=".length()).length());
        } finally {
            TestUtils.cleanTempFiles(newServerDir.getParentFile()); // servers dir
        }
    }

    @Test
    public void testServerNameCaseSensitivity() {

        File serversDir = new File(Constants.TEST_TMP_ROOT_FILE, "servers");
        File newServerDir = new File(serversDir, "newServer");
        File newserverDir = new File(serversDir, "newserver");

        try {
            TestUtils.cleanTempFiles(newServerDir);
            TestUtils.cleanTempFiles(newserverDir);

            // Create a server named "newServer"

            // find locations first
            bc = new BootstrapConfig();
            bc.findLocations("newServer", Constants.TEST_TMP_ROOT, null, null, null);

            // configure / create the server
            initProps.clear();
            bc.verifyProcess(VerifyServer.CREATE, null);

            // This test cannot proceed unless the file system is case-insensitive.
            Assume.assumeTrue(newserverDir.exists());

            // Configure a server named "newserver".
            bc = new BootstrapConfig();
            bc.findLocations("newserver", Constants.TEST_TMP_ROOT, null, null, null);
            bc.verifyProcess(VerifyServer.CREATE_DEFAULT, null);

            initProps.clear();
            bc.configure(initProps);

            // Verify that the server is named "newServer".
            assertEquals("newServer", bc.getProcessName());
            assertEquals("newServer", bc.getConfigFile(null).getName());
            assertEquals("newServer", bc.getOutputFile(null).getName());

            // Verify that no framework property contains "newserver" since
            // these are all set as system properties.
            for (Map.Entry<String, String> entry : bc.getFrameworkProperties().entrySet()) {
                assertFalse("framework property " + entry.getKey() + '=' + entry.getValue(),
                            entry.getValue().contains("newserver"));
            }
        } finally {
            TestUtils.cleanTempFiles(serversDir);
        }
    }

    @Test
    public void testSetServerName() {
        bc.setProcessName("abcdefghijklmnopqrstuvwxyz");
        bc.setProcessName("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        bc.setProcessName("12345567890");
        bc.setProcessName("_-.+");
        bc.setProcessName("+-._");
    }

    @Test(expected = LocationException.class)
    public void testSetServerDirBad1() {
        bc.setProcessName("bad!");
    }

    @Test(expected = LocationException.class)
    public void testSetServerDirBad2() {
        bc.setProcessName("bad  ");
    }

    @Test(expected = LocationException.class)
    public void testSetServerDirBad3() {
        bc.setProcessName("bad\b");
    }

    @Test(expected = LocationException.class)
    public void testSetServerDirBad4() {
        bc.setProcessName(".bad");
    }

    @Test(expected = LocationException.class)
    public void testSetServerDirBad5() {
        bc.setProcessName("-bad");
    }

    public void checkDirs(String m, BootstrapConfig bc) throws IllegalArgumentException, IllegalAccessException {
        // make sure all dirs are set.. use reflection so that we catch if we
        // missed
        // one..
        Field fields[] = BootstrapConfig.class.getDeclaredFields();
        for (Field f : fields) {
            if (f.getType().equals(File.class)) {
                f.setAccessible(true);
                String name = f.getName();
                File file = (File) f.get(bc);
                assertNotNull(m + ": File location should be set for " + name, file);
                System.out.printf("%18s %s\n", name, file.getAbsolutePath());
            }
        }

        // sanity check the calculated directories
        assertEquals(bc.outputDir, bc.workarea.getParentFile());
    }

    //A class to override the server.env file only. A bit ugly but works fine.
    protected class ServerEnvTestBootstrapConfig extends BootstrapConfig {
        ServerEnvTestBootstrapConfig(File tempEnvFile, Map<String, String> initProps) {
            super();
            this.tempEnvFile = tempEnvFile;
            super.initProps = initProps;
        }

        private final File tempEnvFile;

        @Override
        public File getConfigFile(String relativeServerPath) {
            if (relativeServerPath == null)
                return configDir;
            else if ((tempEnvFile != null) && "server.env".equals(relativeServerPath))
                return this.tempEnvFile;
            else
                return new File(configDir, relativeServerPath);
        }
    }

    protected class TestBootstrapConfig extends BootstrapConfig {
        TestBootstrapConfig() {
        }

        TestBootstrapConfig(Map<String, String> initProps) {
            super.initProps = initProps;
        }

        @Override
        protected void verifyProcess(VerifyServer verify, LaunchArguments args) throws LaunchException {
        }
    }
}
