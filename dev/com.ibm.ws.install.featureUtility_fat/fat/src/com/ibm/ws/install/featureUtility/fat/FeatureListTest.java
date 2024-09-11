/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.ws.install.featureUtility.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.install.InstallException;

import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import test.utils.TestUtils;

public class FeatureListTest extends FeatureUtilityToolTest {

	private static final Class<?> c = FeatureListTest.class;
    // ETC product extension related variables.
    public static final String PRODUCT_FEATURE_PATH = "producttest/lib/features";
    public static final String PRODUCT_BUNDLE_PATH = "producttest/lib";
    public static final String PRODUCT_EXTENSIONS_PATH = "etc/extensions";
    public static final String PRODUCT_FEATURE_PROPERTIES_FILE = "publish/tmp/testproduct.properties";
    public static final String PRODUCT_FEATURE_EMPTY_PATH_PROPERTIES_FILE = "publish/tmp/testproductbadpath.properties";
    public static final String PRODUCT_FEATURE_BAD_WLP_PROPERTIES_FILE = "publish/tmp/testproductbadwlp.properties";
    public static final String PRODUCT_FEATURE_PRODTEST_MF = "publish/tmp/prodtest-1.0.mf";
    public static final String PRODUCT_FEATURE_PRODTEST_JAR = "publish/tmp/com.ibm.ws.prodtest.internal_1.0.jar";
    public static final String PRODUCT_EXT_NAME = "testproduct";

    // USR product extension related properties:
    public static final String USR_PRODUCT_FEATURE_NAME = "usertest.esa";
    public static final String USR_PRODUCT_NAME = "usr";
    public static final String CORE_PRODUCT_NAME = "core";

    // Product pre-set return codes as set in:
    // com.ibm.ws.kernel.feature.internal.cmdline.ReturnCode,
    // com.ibm.ws.kernel.feature.internal.generator.FeatureListOptions.
    public static final int PRODUCT_EXT_NOT_FOUND = 26;
    public static final int PRODUCT_EXT_NOT_DEFINED = 27;
    public static final int PRODUCT_EXT_NO_FEATURES_FOUND = 28;
    private static boolean checkReturnCode = true;


	@BeforeClass
	public static void beforeClassSetup() throws Exception {
		final String methodName = "beforeClassSetup";
        /* Enable tests only if running on a zOS machine, otherwise skip class */
		Assume.assumeTrue(!isZos);
		Log.entering(c, methodName);
		deleteFeaturesAndLafilesFolders("beforeClassSetup");
		replaceWlpProperties(libertyVersion);
		setUpProdExt(methodName);
		// Create a directory to store the output files.
        File toolsOutputDir = new File(minifiedRoot + "/tool.output.dir");
        toolsOutputDir.mkdir();
        
		//Check if wlp folder is corrupted and log stdout
        String cmd = minifiedRoot + "/bin/" + "productInfo";
        String parameters[] = { "validate" };
        ProgramOutput po = server.getMachine().execute(cmd, parameters);
        if (po.getStdout().contains("Product validation completed successfully.")) {
            Log.info(c, methodName, "productInfo validate passed");
        } else {
            checkReturnCode = false;
            Log.info(c, methodName, "WLP folder corrupted");
            Log.info(c, methodName, po.getStdout());
        }
        
        //Install couple core features for testing
        replaceWlpProperties("24.0.0.8");
		copyFileToMinifiedRoot("etc",
			    "publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");
		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", mavenLocalRepo2);
		//need to skip signature verification because our test artifactory doesn't have signatures
		po = runFeatureUtility(methodName, new String[] {"installFeature", "jpa-2.1", "servlet-5.0", "javaeeClient-7.0", "--verify=skip"});
		assertTrue(po.getStdout().contains("All features were successfully installed"));
		Log.exiting(c, methodName);
	}

	@Before
	public void beforeSetUp() throws Exception {
	    copyFileToMinifiedRoot("etc",
		    "publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");
	    writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", mavenLocalRepo1);
	}

	@After
	public void afterCleanUp() throws Exception {		
		deleteUsrExtFolder("afterCleanUp");
		
	}

	@AfterClass
	public static void cleanUp() throws Exception {
		if (!isZos) {
			resetOriginalWlpProps();
		}
		deleteFeaturesAndLafilesFolders("afterCleanUp");
		deleteEtcFolder("afterCleanUp");
		deleteTmpFolder("afterCleanUp");
		deleteFolder("afterCleanUp", "tool.output.dir");
		deleteFolder("afterCleanUp", "badwlp/lib/features");
		
	}

	 
	 private static void setUpProdExt(String METHOD_NAME) throws Exception {
	        // Install a product extension manually. Product name: testproduct. Install location: wlp/productTest (as specified in testproduct.properties).
		 	copyFileToMinifiedRoot(PRODUCT_FEATURE_PATH, PRODUCT_FEATURE_PRODTEST_MF);
	        
	        copyFileToMinifiedRoot(PRODUCT_BUNDLE_PATH, PRODUCT_FEATURE_PRODTEST_JAR);
	       
	        //Copy product extensions
	        copyFileToMinifiedRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_PROPERTIES_FILE);


	        copyFileToMinifiedRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_EMPTY_PATH_PROPERTIES_FILE);
	       

	        copyFileToMinifiedRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_BAD_WLP_PROPERTIES_FILE);
	        
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

	    private static void findFeatureBySymbolicName(NodeList nl, String symboliName, List<Element> results) {
	        for (int i = 0, length = nl.getLength(); i < length; i++) {
	            Element featureElement = (Element) nl.item(i);
	            if (symboliName.equals(getSingletonElement(featureElement, "symbolicName").getTextContent())) {
	                results.add(featureElement);
	            }
	        }
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

	    private static List<String> getTextContents(NodeList nl) {
	        ArrayList<String> result = new ArrayList<String>();
	        for (int i = 0, length = nl.getLength(); i < length; i++) {
	            result.add(nl.item(i).getTextContent());
	        }
	        return result;
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

	    /**
	     * TestDescription:
	     * This test ensures that the featureList action generates a feature list.
	     *
	     * @throws Exception
	     */
	    @Mode(TestMode.LITE)
	    @Test
	    public void testFeatureList() throws Exception {
	        final String METHOD_NAME = "testFeatureList";
	        Log.entering(c, METHOD_NAME);

	        copyFileToMinifiedRoot("tmp", "publish/features/usertest.with.api-1.0.esa");
	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "installFeature", minifiedRoot + "/tmp/usertest.with.api-1.0.esa" });

	        if (checkReturnCode) {
	            assertEquals("The feature should have been installed.\r\n", 0, po.getReturnCode());
	        }

	        po = runFeatureUtility(METHOD_NAME, new String[] { "featureList", "--productExtension=usr", minifiedRoot + "/tool.output.dir/featurelist.xml" });

	        RemoteFile rf = server.getMachine().getFile(minifiedRoot + "/tool.output.dir/featurelist.xml");
	        InputStream in = rf.openForReading();
	        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	        Document doc = builder.parse(in);
	        NodeList nl = doc.getElementsByTagName("feature");

	        boolean found = false;
	        boolean foundApiJar = false;
	        boolean foundDisplayName = false;
	        boolean foundDescription = false;
	        boolean foundCategory1 = false;
	        boolean foundCategory2 = false;

	        for (int i = 0; i < nl.getLength(); i++) {
	            Element e = (Element) nl.item(i);
	            if ("usertest.with.api".equals(e.getAttribute("name"))) {
	                found = true;
	                NodeList nl2 = e.getChildNodes();
	                for (int j = 0; j < nl2.getLength(); j++) {
	                    if (nl2.item(j) instanceof Element) {
	                        e = (Element) nl2.item(j);
	                        if ("apiJar".equals(e.getNodeName())) {
	                            foundApiJar = true;
	                            assertEquals("The api jar name is incorrect", "usr/extension/dev/api/usertest_1.0.0.jar", e.getTextContent().trim());
	                        } else if ("description".equals(e.getNodeName())) {
	                            foundDescription = true;
	                            assertEquals("The feature description is incorrect", "Test feature description", e.getTextContent().trim());
	                        } else if ("category".equals(e.getNodeName())) {
	                            String catName = e.getTextContent();
	                            if ("category1".equals(catName)) {
	                                foundCategory1 = true;
	                            } else if ("category2".equals(catName)) {
	                                foundCategory2 = true;
	                            }
	                        }
	                    }
	                }
	            }
	        }

	        assertTrue("The user feature should be in the feature list", found);
	        assertTrue("The feature's api jar was not found", foundApiJar);
	        assertTrue("The feature's category (category1) was not found", foundCategory1);
	        assertTrue("The feature's category (category2) was not found", foundCategory2);

	        Log.exiting(c, METHOD_NAME);
	    }

	    /**
	     * Tests that the installUtility featureList action help displays the --productExtension option.
	     *
	     * @throws Exception
	     */
	    @Test
	    public void testFeatureListProductExtensionParmHelpDisplay() throws Exception {
	        String METHOD_NAME = "testFeatureListProductExtensionParmHelpDisplay";
	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "help", "featureList" });

	        String stdout = po.getStdout();
	        int returnCode = po.getReturnCode();
	        Log.info(c, METHOD_NAME, "Return Code: " + returnCode);
	        assertTrue("The output should contain the option --productExtension. ", stdout.contains("--productExtension"));

	        Log.exiting(c, METHOD_NAME);
	    }

	    /**
	     * Test installUtility featureList with --productExtension=testproductbadwlp where the features folder for the product extension is empty.
	     * The request is expected to fail and issue message: CWWKF1019E.
	     *
	     * @throws Exception
	     */
	    @Test
	    public void testFeatureListUsingProductWithNoFeatureMfs() throws Exception {
	        final String METHOD_NAME = "testFeatureListUsingProductWithNoFeatureMfs";

	        Log.entering(c, METHOD_NAME);
	        // Create a wlp directory for product extension
	        File toolsOutputDir = new File(minifiedRoot + "/badwlp/lib/features");
	        toolsOutputDir.mkdirs();
	        assertTrue("Should have been made: " , toolsOutputDir.exists());
	        Log.info(c, METHOD_NAME, "toolsOutputDir: " + toolsOutputDir.getAbsolutePath());

	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "featureList", "--productExtension=testproductbadwlp",
	                                                                         minifiedRoot + "/tool.output.dir/prodExtFeaturelistNoFeatures.xml" });

	        String stdout = po.getStdout();
	        int returnCode = po.getReturnCode();
	        Log.info(c, METHOD_NAME, "Return Code: " + returnCode);

	        assertTrue("The return code should have been: " + PRODUCT_EXT_NO_FEATURES_FOUND + ". Found: " + returnCode, (returnCode == PRODUCT_EXT_NO_FEATURES_FOUND));
	        assertTrue("The features list request should have failed and message " + "CWWKF1019E" + " should have been issued :\r\n", stdout.contains("CWWKF1019E"));
	        assertFalse("The prodExtFeaturelistNoFeatures.xml should not exist. The request should have failed.",
	                    server.fileExistsInLibertyInstallRoot("tool.output.dir/prodExtFeaturelistNoFeatures.xml"));

	        Log.exiting(c, METHOD_NAME);
	    }

	    /**
	     * Test installUtility featureList with a product extension name argument pointing to a product that does not exist: --productExtension=testproductbadName.
	     * The request is expected to fail and issue message: CWWKF1021E.
	     *
	     * @throws Exception
	     */
	    @Test
	    public void testFeatureListUsingBadProductExtNameArgument() throws Exception {
	        final String METHOD_NAME = "testFeatureListUsingBadProductExtNameArgument";
	        Log.entering(c, METHOD_NAME);
	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "featureList", "--productExtension=testproductbadName",
	                                                                         minifiedRoot + "/tool.output.dir/prodExtFeaturelistBadNameArg.xml" });
	        String stdout = po.getStdout();
	        int returnCode = po.getReturnCode();
	        Log.info(c, METHOD_NAME, "Return Code: " + returnCode);

	        assertTrue("The return code should have been: " + PRODUCT_EXT_NOT_DEFINED + ". Found: " + returnCode, (returnCode == PRODUCT_EXT_NOT_DEFINED));
	        assertTrue("The features list request should have failed and message " + "CWWKF1021E" + " should have been issued :\r\n", stdout.contains("CWWKF1021E"));
	        assertFalse("The prodExtFeaturelistBadNameArg.xml should not exist. The request should have failed.",
	                    server.fileExistsInLibertyInstallRoot("tool.output.dir/prodExtFeaturelistBadNameArg.xml"));

	        Log.exiting(c, METHOD_NAME);
	    }

	    /**
	     * Test installUtility featureList with --productExtension=testproduct where the com.ibm.websphere.productInstall in the
	     * product extension's properties file points to "".
	     * The request is expected to fail and issue message: CWWKF1020E.
	     *
	     * @throws Exception
	     */
	    @Test
	    public void testFeatureListUsingEmptyInstallLocationInProdExtPropsFile() throws Exception {
	        final String METHOD_NAME = "testFeatureListUsingEmptyInstallLocationInProdExtPropsFile";
	        Log.entering(c, METHOD_NAME);

	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "featureList", "--productExtension=testproductbadpath",
	                                                                         minifiedRoot + "/tool.output.dir/prodExtFeaturelistWithInvalidInstLocInPropsFile.xml" });
	        String stdout = po.getStdout();
	        int returnCode = po.getReturnCode();
	        Log.info(c, METHOD_NAME, "Return Code: " + returnCode);

	        assertTrue("The return code should have been: " + PRODUCT_EXT_NOT_FOUND + ". Found: " + returnCode, (returnCode == PRODUCT_EXT_NOT_FOUND));
	        assertTrue("The features list request should have failed and message " + "CWWKF1020E" + " should have been issued :\r\n", stdout.contains("CWWKF1020E"));
	        assertFalse("The prodExtFeaturelistWithInvalidInstLocInPropsFile.xml should not exist. The request should have failed.",
	                    server.fileExistsInLibertyInstallRoot("tool.output.dir/prodExtFeaturelistWithInvalidInstLocInPropsFile.xml"));

	        Log.exiting(c, METHOD_NAME);
	    }

	    /**
	     * Test installUtility featureList without the --productExtension argument.
	     * Only Core features expected in the output list.
	     *
	     * @throws Exception
	     */
	    @Test
	    public void testFeatureListWithNoProdExtArgument() throws Exception {
	        final String METHOD_NAME = "testFeatureListWithNoProdExtArgument";
	        Log.entering(c, METHOD_NAME);

	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "featureList", minifiedRoot + "/tool.output.dir/coreFeaturelist.xml" });

	        // Open the resulting xml.
	        RemoteFile rf = server.getMachine().getFile(minifiedRoot + "/tool.output.dir/coreFeaturelist.xml");
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
	        Element jpaFeatureElement = getFeatureBySymbolicName(e, "com.ibm.websphere.appserver.jpa-2.1");
	        NodeList includes = jpaFeatureElement.getElementsByTagName("include");
	        assertTrue("There should be at least one include node for ejbLite", includes.getLength() >= 1);
	        Element includeServlet = null;
	        for (int i = 0; i < includes.getLength(); i++) {
	            Element el = (Element) includes.item(i);
	            if ("com.ibm.websphere.appserver.jdbc-4.1".equals(el.getAttribute("symbolicName"))) {
	                includeServlet = el;
	                break;
	            }
	        }
	        assertNotNull("Expected jpa-2.1 to include jdbc-4.1 feature, but it was not found", includeServlet);
	        assertEquals("Wrong tolerates: " + includeServlet, "4.2,4.3", includeServlet.getAttribute("tolerates"));

	        // Make sure short name is correct
	        assertEquals("Wrong short name: " + includeServlet, "jdbc-4.1", includeServlet.getAttribute("shortName"));

	        // check for private feature with singleton
	        Element servletApiFeatureElement = getFeatureBySymbolicName(e, "io.openliberty.servlet.api-5.0");
	        assertEquals("Wrong singleton value: " + servletApiFeatureElement, "true", getSingletonElement(servletApiFeatureElement, "singleton").getTextContent());

	        // check servlet feature to make sure it has no process type
	        List<String> servletProcessTypes = getTextContents(servletApiFeatureElement.getElementsByTagName("processType"));
	        assertEquals("Wrong number of processTypes: " + servletProcessTypes, 0, servletProcessTypes.size());

	        // check that the javaeeClient feature has the client process type
	        Element artifactFeatureElement = getFeatureBySymbolicName(e, "com.ibm.websphere.appserver.javaeeClient-7.0");
	        List<String> processTypes = getTextContents(artifactFeatureElement.getElementsByTagName("processType"));
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
	                assertEquals("localhost", config.getAttribute("defaultValue"));
	            } else if ("udpOptions".equals(name)) {
	                assertEquals("defaultUDPOptions", config.getAttribute("id"));
	                assertEquals("-1", config.getAttribute("service.ranking"));
	            } else {
	                org.junit.Assert.fail("Invalid default configuration found for channelfw-1.0: " + name);
	            }

	        }
	        Log.exiting(c, METHOD_NAME);
	    }

	    /**
	     * Test installUtility featureList with the --productExtension=usr argument.
	     * Only features in the default user product extension location are expected in the output list.
	     *
	     * @throws Exception
	     */
	    @Test
	    public void testFeatureListWithUsrProdExtArgument() throws Exception {

	        final String METHOD_NAME = "testFeatureListWithUsrProdExtArgument";
	        Log.entering(c, METHOD_NAME);
	        
	        copyFileToMinifiedRoot("tmp", "publish/features/usertest-1.0.esa");
	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "installFeature", minifiedRoot + "/tmp/usertest-1.0.esa" });
	        
	        
	        po = runFeatureUtility(METHOD_NAME, new String[] { "featureList", "--productExtension=usr", minifiedRoot + "/tool.output.dir/usrFeaturelist.xml" });

	        RemoteFile rf = server.getMachine().getFile(minifiedRoot + "/tool.output.dir/usrFeaturelist.xml");
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
	        assertEquals("feature/enables", Arrays.asList("ssl-1.0"), getTextContents(featureElement.getElementsByTagName("enables")));

	        NodeList defaultConfigList = e.getElementsByTagName("defaultConfiguration");
	        assertTrue("There should only be one defaultConfiguration node in the feature list", defaultConfigList.getLength() == 1);

	        Log.exiting(c, METHOD_NAME);
	    }

	    /**
	     * Test installUtility featureList with the --productExtension=testproduct argument.
	     * Only features in the default user product extension testproduct are expected in the output list.
	     *
	     * @throws Exception
	     */
	    @Test
	    public void testFeatureListWithProdExtArgument() throws Exception {
	        final String METHOD_NAME = "testFeatureListWithProdExtArgument";
	        Log.entering(c, METHOD_NAME);

//	        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_PROPERTIES_FILE);
	        ProgramOutput po = runFeatureUtility(METHOD_NAME,
	                                             new String[] { "featureList", "--productExtension=testproduct", minifiedRoot + "/tool.output.dir/prodExtFeaturelist.xml" , "--verbose"});

	        RemoteFile rf = server.getMachine().getFile(minifiedRoot + "/tool.output.dir/prodExtFeaturelist.xml");
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
}
