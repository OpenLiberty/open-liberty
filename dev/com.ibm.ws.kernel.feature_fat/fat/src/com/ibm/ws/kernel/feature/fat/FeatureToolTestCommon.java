/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.AfterClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public abstract class FeatureToolTestCommon {
    public static final Class<?> c = FeatureToolTestCommon.class;
    public static LibertyServer server;
    public static String javaExc;
    public static String installRoot;

    // ETC product extension related variables.
    public static final String PRODUCT_FEATURE_PATH = "producttest/lib/features/";
    public static final String PRODUCT_BUNDLE_PATH = "producttest/lib/";
    public static final String PRODUCT_EXTENSIONS_PATH = "etc/extensions/";
    public static final String PRODUCT_FEATURE_PROPERTIES_FILE = "testproduct.properties";
    public static final String PRODUCT_FEATURE_EMPTY_PATH_PROPERTIES_FILE = "testproductbadpath.properties";
    public static final String PRODUCT_FEATURE_PRODTEST_MF = "prodtest-1.0.mf";
    public static final String PRODUCT_FEATURE_PRODTEST_JAR = "com.ibm.ws.prodtest.internal_1.0.jar";
    public static final String PRODUCT_EXT_NAME = "testproduct";

    // USR product extension related properties:
    public static final String USR_PRODUCT_FEATURE_NAME = "usertest";

    // Product pre-set return codes as set in:
    // com.ibm.ws.kernel.feature.internal.cmdline.ReturnCode,
    // com.ibm.ws.kernel.feature.internal.generator.FeatureListOptions.
    public static final int PRODUCT_EXT_NOT_FOUND = 26;
    public static final int PRODUCT_EXT_NOT_DEFINED = 27;
    public static final int PRODUCT_EXT_NO_FEATURES_FOUND = 28;

    // Other variables.
    public static final String CORE_PRODUCT_NAME = "core";
    public static final String USR_PRODUCT_NAME = "usr";
    public static final int SETUP_PROD_EXT = 1;
    public static final int SETUP_USR_PROD_EXT = 2;
    public static final int SETUP_ALL_PROD_EXTS = 3;

    /**
     * Setup the environment.
     * 
     * @param svr The server instance.
     * 
     * @throws Exception
     */
    public static void setupEnv(LibertyServer svr) throws Exception {
        final String METHOD_NAME = "setup";
        server = svr;
        installRoot = server.getInstallRoot();
        javaExc = System.getProperty("java.home") + "/bin/java";
        Log.entering(c, METHOD_NAME);
        Log.info(c, METHOD_NAME, "java: " + javaExc);
        Log.info(c, METHOD_NAME, "installRoot: " + installRoot);

        // Create a directory to store the output files.
        File toolsOutputDir = new File(installRoot + "/tool.output.dir");
        toolsOutputDir.mkdir();
    }

    /**
     * Setup product extensions.
     * 
     * @param setupOption The option that determines what preset product extension will be installed.
     * 
     * @throws Exception
     */
    public static void setupProductExtensions(int setupOption) throws Exception {
        final String METHOD_NAME = "setupProductExtensions";
        Log.exiting(c, METHOD_NAME);
        boolean setupAll = false;
        switch (setupOption) {
            case SETUP_ALL_PROD_EXTS:
                setupAll = true;
            case SETUP_PROD_EXT:
                // Install a product extension manually. Product name: testproduct. Install location: wlp/productTest (as specified in testproduct.properties).
                server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_PRODTEST_MF);
                assertTrue("product feature: " + PRODUCT_FEATURE_PRODTEST_MF + " should have been copied to: " + PRODUCT_FEATURE_PATH,
                           server.fileExistsInLibertyInstallRoot(PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_PRODTEST_MF));
                server.copyFileToLibertyInstallRoot(PRODUCT_BUNDLE_PATH, PRODUCT_FEATURE_PRODTEST_JAR);
                assertTrue("product bundle: " + PRODUCT_FEATURE_PRODTEST_JAR + " should have been copied to: " + PRODUCT_BUNDLE_PATH,
                           server.fileExistsInLibertyInstallRoot(PRODUCT_BUNDLE_PATH + PRODUCT_FEATURE_PRODTEST_JAR));
                server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_PROPERTIES_FILE);
                assertTrue("product extension props file: " + PRODUCT_FEATURE_PROPERTIES_FILE + " should have been copied to: " + PRODUCT_EXTENSIONS_PATH,
                           server.fileExistsInLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE));
                Log.info(c, METHOD_NAME, "Product extension: " + PRODUCT_EXT_NAME + " has been installed.");
                if (!setupAll) {
                    break;
                }
            case SETUP_USR_PROD_EXT:
                // install a (usr) product extension.
                ProgramOutput po = server.installFeature(null, USR_PRODUCT_FEATURE_NAME);
                String stdout = po.getStdout();
                if (!stdout.contains("CWWKF1000I")) {
                    assertEquals("The feature: " + USR_PRODUCT_FEATURE_NAME + " should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0,
                                 po.getReturnCode());
                }
                assertTrue("The " + USR_PRODUCT_FEATURE_NAME + " feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.mf"));
                assertTrue("The " + USR_PRODUCT_FEATURE_NAME + " bundle should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/usertest_1.0.0.jar"));
                Log.info(c, METHOD_NAME, "Product extension: " + USR_PRODUCT_FEATURE_NAME + " has been installed in usr");

                break;
            default:
                throw new Exception("Invalid setupOption: " + setupOption);

        }

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Cleans up the installation from any files that may have been left around.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void AfterClassCleanup() throws Exception {
        final String METHOD_NAME = "cleanup";

        Log.entering(c, METHOD_NAME);

        if (server == null) {
            return;
        }

        if (server.isStarted())
            server.stopServer();

        server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");
        server.deleteDirectoryFromLibertyInstallRoot("producttest");
        server.deleteDirectoryFromLibertyInstallRoot("etc/extensions");
        server.deleteDirectoryFromLibertyInstallRoot("tool.output.dir");

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test that the --productExtension help option is displayed when help is requested.
     * 
     * @param cmd The command to execute.
     * @param parms The parameters for the command.
     * @param workDir The working directory where the command is to be issued.
     * 
     * @throws Exception
     */
    public void testFeatureToolProductExtensionParmHelpDisplay(String cmd, String[] parms, String workDir) throws Exception {
        final String METHOD_NAME = "testFeatureToolProductExtensionParmHelpDisplay";
        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.getMachine().execute(cmd, parms, workDir);

        String stdout = po.getStdout();
        int returnCode = po.getReturnCode();
        Log.info(c, METHOD_NAME, "Return Code: " + returnCode + ". STDOUT:" + stdout);
        assertTrue("The output should contain the option --productExtension. ", stdout.contains("--productExtension"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the command to list features where the features folder for the product extension is empty.
     * The request is expected to fail and issue a message.
     * 
     * @param cmd The command to execute.
     * @param parms The parameters for the command.
     * @param workDir The working directory where the command is to be issued.
     * @param expectedMsg The expected message to use for validation.
     * 
     * @throws Exception
     */
    public void testFeatureToolUsingProductWithNoFeatureMfs(String cmd, String[] parms, String workDir, String expectedMsg) throws Exception {
        final String METHOD_NAME = "testFeatureToolUsingProductWithNoFeatureMfs";
        Log.entering(c, METHOD_NAME);

        server.deleteFileFromLibertyInstallRoot(PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_PRODTEST_MF);
        assertFalse("Manifest file: " + PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_PRODTEST_MF + "should not exist.",
                    server.fileExistsInLibertyInstallRoot(PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_PRODTEST_MF));
        try {
            ProgramOutput po = server.getMachine().execute(cmd, parms, workDir);
            String stdout = po.getStdout();
            int returnCode = po.getReturnCode();
            Log.info(c, METHOD_NAME, "Return Code: " + returnCode + ". STDOUT: " + stdout);

            assertTrue("The return code should have been: " + PRODUCT_EXT_NO_FEATURES_FOUND + ". Found: " + returnCode, (returnCode == PRODUCT_EXT_NO_FEATURES_FOUND));
            assertTrue("The features list request should have failed and message " + expectedMsg + " should have been issued :\r\n" + stdout, stdout.contains(expectedMsg));
            assertFalse("The prodExtFeaturelistNoFeatures.xml should not exist. The request should have failed.",
                        server.fileExistsInLibertyInstallRoot("tool.output.dir/prodExtFeaturelistNoFeatures.xml"));
        } finally {
            server.copyFileToLibertyInstallRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_PRODTEST_MF);
            assertTrue("product feature: " + PRODUCT_FEATURE_PRODTEST_MF + " should have been copied to: " + PRODUCT_FEATURE_PATH,
                       server.fileExistsInLibertyInstallRoot(PRODUCT_FEATURE_PATH + PRODUCT_FEATURE_PRODTEST_MF));
        }
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * 
     * Tests the command to list features where the productExtension argument value points to a product that does not exist: --productExtension=testproductbadName.
     * The request is expected to fail and issue a message.
     * 
     * @param cmd The command to execute.
     * @param parms The parameters for the command.
     * @param workDir The working directory where the command is to be issued.
     * @param expectedMsg The expected message to use for validation.
     * 
     * @throws Exception
     */
    public void testFeatureToolUsingBadProductExtNameArgument(String cmd, String[] parms, String workDir, String expectedMsg) throws Exception {
        final String METHOD_NAME = "testFeatureToolUsingBadProductExtNameArgument";
        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.getMachine().execute(cmd, parms, workDir);
        String stdout = po.getStdout();
        int returnCode = po.getReturnCode();
        Log.info(c, METHOD_NAME, "Return Code: " + returnCode + ". STDOUT: " + stdout);

        assertTrue("The return code should have been: " + PRODUCT_EXT_NOT_DEFINED + ". Found: " + returnCode, (returnCode == PRODUCT_EXT_NOT_DEFINED));
        assertTrue("The features list request should have failed and message " + expectedMsg + " should have been issued :\r\n" + stdout, stdout.contains(expectedMsg));
        assertFalse("The prodExtFeaturelistBadNameArg.xml should not exist. The request should have failed.",
                    server.fileExistsInLibertyInstallRoot("tool.output.dir/prodExtFeaturelistBadNameArg.xml"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * 
     * Tests the command to list features where the com.ibm.websphere.productInstall in the product extension's properties file points to "".
     * The request is expected to fail and issue a message.
     * 
     * @param cmd The command to execute.
     * @param parms The parameters for the command.
     * @param workDir The working directory where the command is to be issued.
     * @param expectedMsg The expected message to use for validation.
     * 
     * @throws Exception
     */
    public void testFeatureToolUsingEmptyInstallLocationInProdExtPropsFile(String cmd, String[] parms, String workDir, String expectedMsg) throws Exception {
        final String METHOD_NAME = "testFeatureToolUsingEmptyInstallLocationInProdExtPropsFile";
        Log.entering(c, METHOD_NAME);

        server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE);
        assertFalse(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE + " should not exist.",
                    server.fileExistsInLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE));
        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_EMPTY_PATH_PROPERTIES_FILE);
        assertTrue("product extension props file: " + PRODUCT_FEATURE_EMPTY_PATH_PROPERTIES_FILE + " should have been copied to: " + PRODUCT_EXTENSIONS_PATH,
                   server.fileExistsInLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_EMPTY_PATH_PROPERTIES_FILE));
        server.renameLibertyInstallRootFile(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_EMPTY_PATH_PROPERTIES_FILE, PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE);
        assertTrue(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE + " should not exist.",
                   server.fileExistsInLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE));
        try {
            ProgramOutput po = server.getMachine().execute(cmd, parms, workDir);
            String stdout = po.getStdout();
            int returnCode = po.getReturnCode();
            Log.info(c, METHOD_NAME, "Return Code: " + returnCode + ". STDOUT: " + stdout);

            assertTrue("The return code should have been: " + PRODUCT_EXT_NOT_FOUND + ". Found: " + returnCode, (returnCode == PRODUCT_EXT_NOT_FOUND));
            assertTrue("The features list request should have failed and message " + expectedMsg + " should have been issued :\r\n" + stdout, stdout.contains(expectedMsg));
            assertFalse("The prodExtFeaturelistWithInvalidInstLocInPropsFile.xml should not exist. The request should have failed.",
                        server.fileExistsInLibertyInstallRoot("tool.output.dir/prodExtFeaturelistWithInvalidInstLocInPropsFile.xml"));
        } finally {
            server.deleteFileFromLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE);
            assertFalse(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE + " should not exist.",
                        server.fileExistsInLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE));

            server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_PROPERTIES_FILE);
            assertTrue("product extension props file: " + PRODUCT_FEATURE_PROPERTIES_FILE + " should have been copied to: " + PRODUCT_EXTENSIONS_PATH,
                       server.fileExistsInLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH + PRODUCT_FEATURE_PROPERTIES_FILE));
        }
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the command to list features where the command does not contain the --productExtension argument.
     * The request is expected complete.
     * 
     * @param cmd The command to execute.
     * @param parms The parameters for the command.
     * @param workDir The working directory where the command is to be issued.
     * 
     * @throws Exception
     */
    public void testFeatureToolWithNoProdExtArgument(String cmd, String[] parms, String workDir) throws Exception {
        final String METHOD_NAME = "testFeatureToolWithNoProdExtArgument";
        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.getMachine().execute(cmd, parms, workDir);
        logInfo(po, "tool.output.dir/coreFeaturelist.xml");

        // Open the resulting xml.
        RemoteFile rf = server.getFileFromLibertyInstallRoot("tool.output.dir/coreFeaturelist.xml");
        InputStream in = rf.openForReading();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(in);

        // Read the featureInfo header. There should only be one node tagged as such.
        NodeList nl = doc.getElementsByTagName("featureInfo");
        assertTrue("There should only be one featureInfo node in feature list xml", nl.getLength() == 1);
        Element e = (Element) nl.item(0);

        // Validate the featureInfo attributes: name, location, and productId.
        String productName = e.getAttribute("name");
        assertTrue("The product name should be core. Found: " + productName, CORE_PRODUCT_NAME.equals(productName));
        String location = e.getAttribute("location");
        assertTrue("The location should be the install root. Found: " + location, location.endsWith("wlp"));
        String productId = e.getAttribute("productId");
        assertTrue("Product extensions in usr location should not have a product id. Found: " + productId, productId.isEmpty());

        // check for includes with tolerates
        Element jspFeatureElement = getFeatureBySymbolicName(e, "com.ibm.websphere.appserver.jsp-2.2");
        NodeList includes = jspFeatureElement.getElementsByTagName("include");
        assertTrue("There should be at least one include node for jsp", includes.getLength() >= 1);
        Element includeServlet = null;
        for (int i = 0; i < includes.getLength(); i++) {
            Element el = (Element) includes.item(i);
            if ("com.ibm.websphere.appserver.servlet-3.0".equals(el.getAttribute("symbolicName"))) {
                includeServlet = el;
                break;
            }
        }
        assertNotNull("Expected jsp-2.2 to include servlet-3.0 feature, but it was not found", includeServlet);
        assertEquals("Wrong tolerates: " + includeServlet, "3.1", includeServlet.getAttribute("tolerates"));

        // Make sure short name is correct 
        assertEquals("Wrong short name: " + includeServlet, "servlet-3.0", includeServlet.getAttribute("shortName"));

        // check for private feature with singleton
        Element servletApiFeatureElement = getFeatureBySymbolicName(e, "com.ibm.websphere.appserver.javax.servlet-3.0");
        assertEquals("Wrong singleton value: " + servletApiFeatureElement, "true", getSingletonElement(servletApiFeatureElement, "singleton").getTextContent());

        // check servlet feature to make sure it has no process type
        Set<String> servletProcessTypes = getTextContents(servletApiFeatureElement.getElementsByTagName("processType"));
        assertEquals("Wrong number of processTypes: " + servletProcessTypes, 0, servletProcessTypes.size());

        // check that the javaeeClient feature has the client process type
        Element artifactFeatureElement = getFeatureBySymbolicName(e, "com.ibm.websphere.appserver.javaeeClient-7.0");
        Set<String> processTypes = getTextContents(artifactFeatureElement.getElementsByTagName("processType"));
        assertEquals("Wrong number of processTypes: " + processTypes, 1, processTypes.size());
        assertTrue("Wrong process types: " + processTypes, processTypes.contains("CLIENT"));

        NodeList defaultConfigList = e.getElementsByTagName("defaultConfiguration");
        assertEquals("There should only be one defaultConfiguration node in the feature list", defaultConfigList.getLength(), 1);
        List<Element> elements = findDefaultConfigurationByProvidingFeature(defaultConfigList, "com.ibm.websphere.appserver.channelfw-1.0");
        assertTrue("There should be three default instances provided by channelfw-1.0, found:" + elements.size(), elements.size() == 3);
        for (Element instance : elements) {
            NodeList configs = instance.getElementsByTagName("*");
            assertEquals(1, configs.getLength());
            Element config = (Element) configs.item(0);
            assertNotNull(config);

            String name = config.getNodeName();
            if ("tcpOptions".equals(name)) {
                assertEquals("defaultTCPOptions", config.getAttribute("id"));
                assertEquals("-1", config.getAttribute("service.ranking"));
            } else if ("variable".equals(name)) {
                assertEquals("defaultHostName", config.getAttribute("name"));
                assertEquals("localhost", config.getAttribute("value"));
            } else if ("udpOptions".equals(name)) {
                assertEquals("defaultUDPOptions", config.getAttribute("id"));
                assertEquals("-1", config.getAttribute("service.ranking"));
            } else {
                org.junit.Assert.fail("Invalid default configuration found for channelfw-1.0: " + name);
            }

        }
        Log.exiting(c, METHOD_NAME);
    }

    private static Element getSingletonElement(Element parent, String childName) {
        NodeList nl = parent.getElementsByTagName(childName);
        assertEquals(parent.getNodeName() + '/' + childName + ": " + nl, 1, nl.getLength());
        return (Element) nl.item(0);
    }

    private static Element getSingletonElementByAttribute(Element parent, String childName, String attributeName, String value) {
        NodeList nl = parent.getElementsByTagName(childName);
        List<Element> results = new ArrayList<Element>();
        for (int i = 0, length = nl.getLength(); i < length; i++) {
            Element element = (Element) nl.item(i);
            if (value.equals(element.getAttribute(attributeName))) {
                results.add(element);
            }
        }

        assertEquals(parent.getNodeName() + '/' + childName + "[@" + attributeName + "='" + value + "': " + results, 1, results.size());
        return results.get(0);
    }

    private static Element getFeatureBySymbolicName(Element parent, String symbolicName) {
        List<Element> results = new ArrayList<Element>();
        NodeList nl = parent.getElementsByTagName("feature");
        findFeatureBySymbolicName(nl, symbolicName, results);
        nl = parent.getElementsByTagName("protectedFeature");
        findFeatureBySymbolicName(nl, symbolicName, results);
        nl = parent.getElementsByTagName("autoFeature");
        findFeatureBySymbolicName(nl, symbolicName, results);
        nl = parent.getElementsByTagName("privateFeature");
        findFeatureBySymbolicName(nl, symbolicName, results);

        assertEquals(parent.getNodeName() + '/' + symbolicName + "': " + results, 1, results.size());
        return results.get(0);
    }

    private static void findFeatureBySymbolicName(NodeList nl, String symboliName, List<Element> results) {
        for (int i = 0, length = nl.getLength(); i < length; i++) {
            Element featureElement = (Element) nl.item(i);
            if (symboliName.equals(getSingletonElement(featureElement, "symbolicName").getTextContent())) {
                results.add(featureElement);
            }
        }
    }

    private List<Element> findDefaultConfigurationByProvidingFeature(NodeList nl, String featureName) {
        List<Element> elements = new ArrayList<Element>();

        Element root = (Element) nl.item(0);
        NodeList instances = root.getElementsByTagName("defaultInstance");
        for (int i = 0, length = instances.getLength(); i < length; i++) {
            Element defaultElement = (Element) instances.item(i);
            if (featureName.equals(defaultElement.getAttribute("providingFeatures"))) {
                elements.add(defaultElement);
            }
        }
        return elements;
    }

    private static Set<String> getTextContents(NodeList nl) {
        Set<String> result = new LinkedHashSet<String>();
        for (int i = 0, length = nl.getLength(); i < length; i++) {
            result.add(nl.item(i).getTextContent());
        }
        return result;
    }

    private static Set<String> set(String... values) {
        return new LinkedHashSet<String>(Arrays.asList(values));
    }

    /**
     * Tests the command to list features with --productExtension=usr argument.
     * The request is expected complete.
     * 
     * @param cmd The command to execute.
     * @param parms The parameters for the command.
     * @param workDir The working directory where the command is to be issued.
     * 
     * @throws Exception
     */
    public void testFeatureToolWithUsrProdExtArgument(String cmd, String[] parms, String workDir) throws Exception {
        final String METHOD_NAME = "testFeatureToolWithUsrProdExtArgument";
        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.getMachine().execute(cmd, parms, workDir);
        logInfo(po, "tool.output.dir/usrFeaturelist.xml");

        RemoteFile rf = server.getFileFromLibertyInstallRoot("tool.output.dir/usrFeaturelist.xml");
        InputStream in = rf.openForReading();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(in);
        NodeList nl = doc.getElementsByTagName("featureInfo");
        assertTrue("There should only be one featureInfo node in feature list xml", nl.getLength() == 1);
        Element e = (Element) nl.item(0);
        String productName = e.getAttribute("name");
        assertTrue("The product name should be usr. Found: " + productName, USR_PRODUCT_NAME.equals(productName));
        String location = e.getAttribute("location");
        assertTrue("The location should end in usr. Found: " + location, location.endsWith("usr"));
        String productId = e.getAttribute("productId");
        assertTrue("Product extensions in usr location should not have a product id. Found: " + productId, productId.isEmpty());

        Element featureElement = getSingletonElementByAttribute(e, "feature", "name", "usertest");
        assertEquals("feature/symbolicName", "usertest", getSingletonElement(featureElement, "symbolicName").getTextContent());
        assertEquals("feature/enables", set("ssl-1.0"), getTextContents(featureElement.getElementsByTagName("enables")));

        NodeList defaultConfigList = e.getElementsByTagName("defaultConfiguration");
        assertTrue("There should only be one defaultConfiguration node in the feature list", defaultConfigList.getLength() == 1);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the command to list features with --productExtension=productx argument.
     * The request is expected complete.
     * 
     * @param cmd The command to execute.
     * @param parms The parameters for the command.
     * @param workDir The working directory where the command is to be issued.
     * 
     * @throws Exception
     */
    public void testFeatureToolWithProdExtArgument(String cmd, String[] parms, String workDir) throws Exception {
        final String METHOD_NAME = "testFeatureToolWithProdExtArgument";
        Log.entering(c, METHOD_NAME);

        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_PROPERTIES_FILE);
        ProgramOutput po = server.getMachine().execute(cmd, parms, workDir);
        logInfo(po, "tool.output.dir/prodExtFeaturelist.xml");

        RemoteFile rf = server.getFileFromLibertyInstallRoot("tool.output.dir/prodExtFeaturelist.xml");
        InputStream in = rf.openForReading();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(in);
        NodeList nl = doc.getElementsByTagName("featureInfo");
        assertTrue("There should only be one featureInfo node in feature list xml", nl.getLength() == 1);
        Element e = (Element) nl.item(0);
        String productName = e.getAttribute("name");
        assertTrue("The product name should be usr. Found: " + productName, PRODUCT_EXT_NAME.equals(productName));
        String location = e.getAttribute("location");
        assertTrue("The location should be: wlp/producttest/. Found: " + location, location.equals("wlp/producttest/"));
        String productId = e.getAttribute("productId");
        assertTrue("Product extensions should have a product id with the name of: bigProduct. Found: " + productId, productId.equals("bigProduct"));

        NodeList defaultConfigList = e.getElementsByTagName("defaultConfiguration");
        assertTrue("There should only be one defaultConfiguration node in the feature list", defaultConfigList.getLength() == 1);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Prints an extended debug output.
     * 
     * @param po The programOutput
     * @param fileName
     * @throws Exception
     */
    public void logInfo(ProgramOutput po, String fileName) throws Exception {
        String methodName = "logInfo";
        Log.info(c, methodName, "Return Code: " + po.getReturnCode() + ". STDOUT: " + po.getStdout());

        if (po.getReturnCode() != 0) {
            Log.info(c, methodName, "STDERR: " + po.getStderr());
            RemoteFile rf = server.getFileFromLibertyInstallRoot(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(rf.openForReading()));
            StringBuffer sb = new StringBuffer();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            Log.info(c, methodName, "File " + fileName + " content:\n" + sb.toString());
            br.close();
        }
    }
}
