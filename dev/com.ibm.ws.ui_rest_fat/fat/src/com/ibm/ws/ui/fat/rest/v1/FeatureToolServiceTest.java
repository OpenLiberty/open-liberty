/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package com.ibm.ws.ui.fat.rest.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ui.fat.APIConstants;
import com.ibm.ws.ui.fat.FATSuite;
import com.ibm.ws.ui.fat.rest.CommonRESTTest;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class FeatureToolServiceTest extends CommonRESTTest implements APIConstants {
    private static final Class<?> c = FeatureToolServiceTest.class;

    private static final String BASIC_AUTOFEATURE = "basicAutoFeatureTool";
    private static final String BASIC_AUTOFEATURE_L10N = "com.ibm.websphere.appserver.basicautofeaturetool-1.0";
    private static final String PUBLIC_BASIC_AUTOFEATURE = "basicPublicAutoFeatureTool";
    private static final String PROTECTED_BASIC_AUTOFEATURE = "basicProtectedAutoFeatureTool";
    private static final String USR = "usr";
    private static final String USR_L10N = "com.ibm.websphere.appserver.usrtool-1.0";
    private static final String PRODUCT_EXTN_1 = "prodextn1";
    private static final String PRODUCT_EXTN_1_L10N = "com.ibm.websphere.appserver.prodextn1-1.0";
    private static final String PRODUCT_EXTN_2 = "prodextn2";
    private static final String PRODUCT_EXTN_3 = "prodextn3";
    private static final String SCOPED_PRODUCT_EXTN = "scopedprodextn";

    private static final String SINGLE_UI_ENDPOINT_NON_ENDPOINT_WAB_MANIFEST_HEADER = "singleUIEndpointAndNonEndpointWABManifestHeader";
    private static final String SINGLE_UI_ENDPOINT_MANIFEST_HEADER = "singleUIEndpointManifestHeader";
    private static final String MULTI_UI_ENDPOINT_MANIFEST_HEADER = "multiUIEndpointManifestHeader";
    private static final String URL_SINGLE_WAB = "urlInSingleWab";
    private static final String URL_MULTIPLE_WAB = "urlInMultipleWabs";

    private static final String ORIGINAL_UPDATE_MANIFEST = "originalUpdateAutoFeatureTool";
    private static final String UPDATED_UPDATE_MANIFEST = "updatedUpdateAutoFeatureTool";

    private static final String PRODEXTN1_ORIGINAL_UPDATE_MANIFEST = "prodextn1originalUpdateAutoFeatureTool";
    private static final String PRODEXTN1_UPDATED_UPDATE_MANIFEST = "prodextn1updatedUpdateAutoFeatureTool";

    private static final String IBM_SHORTNAME_MANIFEST = "IBMShortNameManifest";
    private static final String SUBSYSTEM_NAME_MANIFEST = "SubsystemNameManifest";
    private static final String SUBSYSTEM_SYMBOLICNAME_MANIFEST = "SubsystemSymbolicNameManifest";

    private static final String PUBLIC_PROD_EXTN = "publicprodextn";
    private static final String PROTECTED_PROD_EXTN = "protectedprodextn";

    private static final String BUNDLE_NO_CONTEXT_PATH = "bundleWithoutWebContextPath";
    private static final String BUNDLE_CONTEXT_PATH1 = "bundleWithWebContextPath1";
    private static final String BUNDLE_CONTEXT_PATH2 = "bundleWithWebContextPath2";

    private static final String UI_ENDPOINT_BUNDLE1 = "adminCenterEndpointBundle1";
    private static final String UI_ENDPOINT_BUNDLE2 = "adminCenterEndpointBundle2";
    private static final String BASIC_BUNDLE1 = "basicBundle1";
    private static final String BASIC_BUNDLE1_V2 = "basicBundle1v2";
    private static final String TEST_BUNDLE1 = "testBundle1";

    private static final String PRODEXTN1_BASIC_BUNDLE1 = "prodextn1BasicBundle1";
    private static final String PRODEXTN1_TEST_BUNDLE1 = "prodextn1TestBundle1";

    private static final String[] coreFeatures = { BASIC_AUTOFEATURE, SINGLE_UI_ENDPOINT_NON_ENDPOINT_WAB_MANIFEST_HEADER,
                                                  SINGLE_UI_ENDPOINT_MANIFEST_HEADER, MULTI_UI_ENDPOINT_MANIFEST_HEADER, URL_SINGLE_WAB,
                                                  URL_MULTIPLE_WAB, PUBLIC_BASIC_AUTOFEATURE, PROTECTED_BASIC_AUTOFEATURE, SUBSYSTEM_NAME_MANIFEST,
                                                  SUBSYSTEM_SYMBOLICNAME_MANIFEST, IBM_SHORTNAME_MANIFEST, ORIGINAL_UPDATE_MANIFEST,
                                                  UPDATED_UPDATE_MANIFEST };
    private static final String[] coreBundles = { BASIC_AUTOFEATURE, BUNDLE_NO_CONTEXT_PATH, BUNDLE_CONTEXT_PATH1,
                                                 BUNDLE_CONTEXT_PATH2, UI_ENDPOINT_BUNDLE1, UI_ENDPOINT_BUNDLE2, BASIC_BUNDLE1,
                                                 TEST_BUNDLE1, BASIC_BUNDLE1_V2 };
    private static final String[] usrFeatures = { USR };
    private static final String[] usrBundles = { USR };

    private static final String[] productExtns = { PRODUCT_EXTN_1, PRODUCT_EXTN_2, PRODUCT_EXTN_3, SCOPED_PRODUCT_EXTN,
                                                  PRODEXTN1_ORIGINAL_UPDATE_MANIFEST, PRODEXTN1_UPDATED_UPDATE_MANIFEST };

    public FeatureToolServiceTest() {
        super(c);
        url = API_V1_CATALOG;
    }

    /**
     * At the end of all of the testing, reset to default.
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
        Log.info(c, "tearDownClass", "restarting server");
        FATSuite.server.restartServer();
    }

    /**
     * Ensure that the catalog starts from scratch before each test.
     */
    @Before
    @After
    public void cleanup() throws Exception {
        ignoreErrorAndStopServerWithValidate();

        if (FATSuite.server.fileExistsInLibertyServerRoot(CATALOG_JSON)) {
            FATSuite.server.deleteFileFromLibertyServerRoot(CATALOG_JSON);
            if (FATSuite.server.fileExistsInLibertyServerRoot(CATALOG_JSON)) {
                fail("The persisted catalog (file system) could not be deleted. Tests will fail if it exists. Aborting.");
            }
        }

        if (FATSuite.server.fileExistsInLibertyServerRoot("server-original.xml")){
            FATSuite.server.deleteFileFromLibertyServerRoot("server.xml");
            FATSuite.server.renameLibertyServerRootFile("server-original.xml", "server.xml");
            if (FATSuite.server.fileExistsInLibertyServerRoot("server-original.xml")) {
                Log.info(c, method.getMethodName(), "WARNING: The original server.xml file couldn't be restored. It would cause other testsuites to fail.");
            } else {
                Log.info(c, method.getMethodName(), "The original server.xml was successfully restored.");
            }
        }

        removeCoreFeatureL10N(BASIC_AUTOFEATURE_L10N);
        for (String feature : coreFeatures)
            removeCoreFeature(feature);

        for (String bundle : coreBundles)
            removeCoreBundle(bundle);

        removeUsrFeatureL10N(USR_L10N);
        for (String usrFeature : usrFeatures)
            removeUsrFeature(usrFeature);

        for (String usrBundle : usrBundles)
            removeUsrBundle(usrBundle);

        for (String prodExtn : productExtns)
            removeProductExtn(prodExtn);
    }

    private void ignoreErrorAndStopServerWithValidate() throws Exception {
        FATSuite.server.stopServer("CWWKF0002E: A bundle could not be found for com.ibm.ws.appserver.*",
                            "CWWKE0702E: Could not resolve module: com.ibm.ws.appserver.*",
                            "CWWKF0029E: Could not resolve module: com.ibm.ws.appserver.*",
                            "CWWKF0001E: A feature definition could not be found for scopedprodextn.*",
                            "CWWKX1009E:.*");
        assertFalse("FAIL: Server is not stopped.",
                    FATSuite.server.isStarted());
    }

    private void installCoreFeatureAndBundle(String name) throws Exception {
        installCoreBundle(name);
        installCoreFeature(name);
    }

    private void installCoreFeature(String name) throws Exception {
        FATSuite.server.installSystemFeature(name);
        assertTrue("Feature " + FEATURE_PATH + name + MANIFEST_SUFFIX + " was not installed successfully.",
                   FATSuite.server.fileExistsInLibertyInstallRoot(FEATURE_PATH + name + MANIFEST_SUFFIX));
    }

    private void installCoreFeatureL10N(String name) throws Exception {
        FATSuite.server.installSystemFeatureL10N(name);
        assertTrue("Feature translation " + FEATURE_L10N_PATH + name + PROPERTIES_SUFFIX + " was not installed successfully.",
                   FATSuite.server.fileExistsInLibertyInstallRoot(FEATURE_L10N_PATH + name + PROPERTIES_SUFFIX));
    }

    private void installCoreBundle(String name) throws Exception {
        FATSuite.server.installSystemBundle(name);
        assertTrue("Bundle " + LIB_PATH + name + JAR_SUFFIX + " was not installed successfully.",
                   FATSuite.server.fileExistsInLibertyInstallRoot(LIB_PATH + name + JAR_SUFFIX));
    }

    private void removeCoreFeatureAndBundle(String name) throws Exception {
        removeCoreFeature(name);
        removeCoreBundle(name);
    }

    private void removeCoreFeature(String name) throws Exception {
        FATSuite.server.uninstallSystemFeature(name);
        assertFalse("Feature " + FEATURE_PATH + name + MANIFEST_SUFFIX + " was not deleted successfully.",
                    FATSuite.server.fileExistsInLibertyInstallRoot(FEATURE_PATH + name + MANIFEST_SUFFIX));
    }

    private void removeCoreFeatureL10N(String name) throws Exception {
        FATSuite.server.uninstallSystemFeatureL10N(name);
        assertFalse("Feature translation " + FEATURE_L10N_PATH + name + PROPERTIES_SUFFIX + " was not deleted successfully.",
                    FATSuite.server.fileExistsInLibertyInstallRoot(FEATURE_L10N_PATH + name + MANIFEST_SUFFIX));
    }

    private void removeCoreBundle(String name) throws Exception {
        FATSuite.server.uninstallSystemBundle(name);
        assertFalse("Bundle " + LIB_PATH + name + JAR_SUFFIX + " was not deleted successfully.",
                    FATSuite.server.fileExistsInLibertyInstallRoot(LIB_PATH + name + JAR_SUFFIX));
    }

    private void installUsrFeatureAndBundle(String name) throws Exception {
        installUsrBundle(name);
        installUsrFeature(name);
    }

    private void installUsrFeature(String name) throws Exception {
        FATSuite.server.installUserFeature(name);
        assertTrue("Feature " + USR_FEATURE_PATH + name + MANIFEST_SUFFIX + " was not installed successfully.",
                   FATSuite.server.fileExistsInLibertyInstallRoot(USR_FEATURE_PATH + name + MANIFEST_SUFFIX));
    }

    private void installUsrFeatureL10N(String name) throws Exception {
        FATSuite.server.installUserFeatureL10N(name);
        assertTrue("Feature translation " + USR_FEATURE_L10N_PATH + name + PROPERTIES_SUFFIX + " was not installed successfully.",
                   FATSuite.server.fileExistsInLibertyInstallRoot(USR_FEATURE_L10N_PATH + name + PROPERTIES_SUFFIX));
    }

    private void installUsrBundle(String name) throws Exception {
        FATSuite.server.installUserBundle(name);
        assertTrue("Bundle " + USR_LIB_PATH + name + JAR_SUFFIX + " was not installed successfully.",
                   FATSuite.server.fileExistsInLibertyInstallRoot(USR_LIB_PATH + name + JAR_SUFFIX));

    }

    private void removeUsrFeature(String name) throws Exception {
        FATSuite.server.uninstallUserFeature(name);
        assertFalse("Feature " + USR_FEATURE_PATH + name + MANIFEST_SUFFIX + " was not deleted successfully.",
                    FATSuite.server.fileExistsInLibertyInstallRoot(USR_FEATURE_PATH + name + MANIFEST_SUFFIX));
    }

    private void removeUsrFeatureL10N(String name) throws Exception {
        FATSuite.server.uninstallUserFeatureL10N(name);
        assertFalse("Feature " + USR_FEATURE_L10N_PATH + name + PROPERTIES_SUFFIX + " was not deleted successfully.",
                    FATSuite.server.fileExistsInLibertyInstallRoot(USR_FEATURE_L10N_PATH + name + PROPERTIES_SUFFIX));
    }

    private void removeUsrBundle(String name) throws Exception {
        FATSuite.server.uninstallUserBundle(name);
        assertFalse("Bundle " + USR_LIB_PATH + name + JAR_SUFFIX + " was not deleted successfully.",
                    FATSuite.server.fileExistsInLibertyInstallRoot(USR_LIB_PATH + name + JAR_SUFFIX));
    }

    private void installProductExtn(String productExtnName, String productExtnNameL10N) throws Exception {
        FATSuite.server.installProductExtension(productExtnName);
        FATSuite.server.installProductBundle(productExtnName, productExtnName);
        FATSuite.server.installProductFeature(productExtnName, productExtnName);
        if (productExtnNameL10N != null) {
            FATSuite.server.installProductFeatureL10N(productExtnName, productExtnNameL10N);
        }
        assertTrue("Properties " + ETC_EXTNS_PATH + productExtnName + ".properties was not installed successfully.",
                   FATSuite.server.fileExistsInLibertyInstallRoot(ETC_EXTNS_PATH + productExtnName + ".properties"));
    }

    private void removeProductExtn(String productExtnName) throws Exception {
        FATSuite.server.uninstallProductExtension(productExtnName);
        assertFalse("Properties " + ETC_EXTNS_PATH + productExtnName + ".properties was not deleted successfully.",
                    FATSuite.server.fileExistsInLibertyInstallRoot(ETC_EXTNS_PATH + productExtnName + ".properties"));
    }

    /**
     * Asserts the attribute exists and equals the expected value
     * 
     * @param featureTools - the complete map of all tools used when we hit a problem
     * @param tool - The map of attributes for a particular tool
     * @param attributeName - A String containing the attribute name to check
     * @param expectedValue - A String containing the expected value of the attr.
     */
    private void assertToolAttribute(JsonArray featureTools, JsonObject tool, String attributeName, String expectedValue) {
        assertNotNull("Tool " + tool + " did not have the expected attribute '" + attributeName + "': Feature Tools: " + featureTools,
                      tool.getString(attributeName));
        assertEquals("Unexpected " + attributeName + " found: " + tool.getString(attributeName) + ". ExpectedValue: " + expectedValue,
                     tool.getString(attributeName), expectedValue);
    }

    /**
     * Finds a featureTool with the specified attribute values.
     * 
     * @param featureTools
     * @param id
     * @param featureName
     * @param featureVersion
     * @param name
     * @param url
     * @param description
     * @param icon
     */
    private void findCompleteFeatureTool(JsonArray featureTools, String id,
                                         String featureName, String featureVersion,
                                         String name, String url, String description, String icon) {
        boolean foundTool = false;
        // for (Map<?, ?> tool : featureTools) {
        for (int i = 0; i < featureTools.size(); i++) {
            JsonObject tool = featureTools.getJsonObject(i);
            if (id.equals(tool.getString("id"))) {
                assertToolAttribute(featureTools, tool, "featureName", featureName);
                assertToolAttribute(featureTools, tool, "featureVersion", featureVersion);
                assertToolAttribute(featureTools, tool, "name", name);
                assertToolAttribute(featureTools, tool, "url", url);
                assertToolAttribute(featureTools, tool, "description", description);
                assertToolAttribute(featureTools, tool, "icon", icon);
                foundTool = true;
            }
        }
        assertTrue("FAIL: No feature tool was found in the featureTools list for id " + id,
                   foundTool);
    }

    /**
     * Finds a featureTool with the specified attribute values.
     * 
     * @param featureTools
     * @param id
     * @param featureName
     * @param featureVersion
     * @param name
     * @param url
     */
    private void findFeatureTool(JsonArray featureTools, String id,
                                 String featureName, String featureVersion,
                                 String name, String url) {
        boolean foundTool = false;
        for (int i = 0; i < featureTools.size(); i++) {
            JsonObject tool = featureTools.getJsonObject(i);
            if (id.equals(tool.getString("id"))) {
                assertToolAttribute(featureTools, tool, "featureName", featureName);
                assertToolAttribute(featureTools, tool, "featureVersion", featureVersion);
                assertToolAttribute(featureTools, tool, "name", name);
                if (url != null) {
                    assertToolAttribute(featureTools, tool, "url", url);
                }
                foundTool = true;
            }
        }
        assertTrue("FAIL: No feature tool was found in the featureTools list for id " + id,
                   foundTool);
    }

    /**
     * Finds a featureTool with the specified attribute values.
     * 
     * @param featureTools
     * @param id
     * @param featureName
     * @param featureVersion
     * @param name
     * @param url
     */
    private void findFeatureTool(JsonArray featureTools, String id) {
        boolean foundTool = false;
        for (int i = 0; i < featureTools.size(); i++) {
            JsonObject tool = featureTools.getJsonObject(i);
            if (id.equals(tool.getString("id"))) {
                foundTool = true;
            }
        }
        assertTrue("FAIL: No feature tool was found in the featureTools list for id " + id,
                   foundTool);
    }

    /**
     * Check to see if the id does NOT exist in the list of featureTools.
     * 
     * @param featureTools
     * @param id
     */
    private void confirmRemoved(JsonArray featureTools, String id) {
        boolean foundTool = false;
        for (int i = 0; i < featureTools.size(); i++) {
            JsonObject tool = featureTools.getJsonObject(i);
            if (id.equals(tool.getString("id"))) {
                foundTool = true;
            }
        }
        assertFalse("FAIL: The feature tool was found (but should not have been) in the featureTools list for id " + id,
                    foundTool);
    }

    /**
     * This is the basic test to ensure that when a server starts we process the feature and store the
     * tool in the catalog.
     */
    @Test
    public void testCatalogUpdatedWithFeature() throws Exception {
        installCoreFeatureAndBundle(BASIC_AUTOFEATURE);
        installCoreFeatureL10N(BASIC_AUTOFEATURE_L10N);
        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the featureTools are correct
        assertContains(response, "featureTools");

        JsonArray featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools: " + featureTools);

        findCompleteFeatureTool(featureTools, "com.ibm.websphere.appserver.basicautofeaturetool-1.0-1.0.0",
                                "com.ibm.websphere.appserver.basicautofeaturetool-1.0", "1.0.0",
                                "BasicAutoFeatureTool Display Name", "/basicAutoFeatureTool",
                                "This is the description for BasicAutoFeatureTool. It is used to test that we can install a tool feature into the liberty runtime.",
                                "/ibm/api/adminCenter/v1/icons/com.ibm.websphere.appserver.basicautofeaturetool-1.0");
    }

    /**
     * This test ensures that tool features in product extensions, including the usr extension are read in correctly.
     */
    @Test
    public void testCatalogUpdatedWithFeatureFromProductExtensions() throws Exception {
        installProductExtn(PRODUCT_EXTN_1, PRODUCT_EXTN_1_L10N);
        installProductExtn(PRODUCT_EXTN_2, null);
        installUsrFeatureAndBundle(USR);
        installUsrFeatureL10N(USR_L10N);

        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the featureTools are correct
        assertContains(response, "featureTools");

        JsonArray featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools: " + featureTools);

        findCompleteFeatureTool(featureTools, "usr%3Acom.ibm.websphere.appserver.usrtool-1.0-1.0.0",
                                "usr:com.ibm.websphere.appserver.usrtool-1.0", "1.0.0",
                                "Usr Tool Display Name", "/usrtool/welcome",
                                "This is the description for usrtool. It is used to test that we can read from usr Extensions feature directories.",
                                "/ibm/api/adminCenter/v1/icons/com.ibm.websphere.appserver.usrtool-1.0");

        findCompleteFeatureTool(featureTools, "prodextn1%3Acom.ibm.websphere.appserver.prodextn1-1.0-1.0.0",
                                "prodextn1:com.ibm.websphere.appserver.prodextn1-1.0", "1.0.0",
                                "Prod Extn 1 Tool Display Name", "/prodextn1/welcome",
                                "This is the description for prodextn1. It is used to test that we can read from product Extensions feature directories.",
                                "/ibm/api/adminCenter/v1/icons/com.ibm.websphere.appserver.prodextn1-1.0");

        findCompleteFeatureTool(featureTools, "prodextn2%3Acom.ibm.websphere.appserver.prodextn2-2.0-2.0.0",
                                "prodextn2:com.ibm.websphere.appserver.prodextn2-2.0", "2.0.0",
                                "Prod Extn 2 Tool Display Name", "/prodextn2/welcome",
                                "This is the description for prodextn2. It is used to test that we can read from product Extensions feature directories.",
                                "/ibm/api/adminCenter/v1/icons/com.ibm.websphere.appserver.prodextn2-2.0");
    }

    /**
     * This test ensures that we use the URL header if set, or otherwise picks the Web_ContentPath from the
     * 1st alphabetically sorted bundle in the list. We also check that if we have only 1 bundle set, that is used.
     */
    @Test
    public void testCatalogUpdatedWithURLHeaderOrWebBundleRootContext() throws Exception {
        FATSuite.server.installSystemFeature(SINGLE_UI_ENDPOINT_NON_ENDPOINT_WAB_MANIFEST_HEADER);
        FATSuite.server.installSystemFeature(URL_SINGLE_WAB);
        FATSuite.server.installSystemFeature(URL_MULTIPLE_WAB);
        FATSuite.server.installSystemFeature(MULTI_UI_ENDPOINT_MANIFEST_HEADER);
        FATSuite.server.installSystemFeature(SINGLE_UI_ENDPOINT_MANIFEST_HEADER);

        FATSuite.server.installSystemBundle(BUNDLE_NO_CONTEXT_PATH);
        FATSuite.server.installSystemBundle(BUNDLE_CONTEXT_PATH1);
        FATSuite.server.installSystemBundle(BUNDLE_CONTEXT_PATH2);
        FATSuite.server.installSystemBundle(BASIC_BUNDLE1);
        FATSuite.server.installSystemBundle(TEST_BUNDLE1);
        FATSuite.server.installSystemBundle(UI_ENDPOINT_BUNDLE1);
        FATSuite.server.installSystemBundle(UI_ENDPOINT_BUNDLE2);
        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the featureTools are correct
        assertContains(response, "featureTools");

        JsonArray featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools: " + featureTools);

        findFeatureTool(featureTools, "com.ibm.websphere.appserver.singleuiendpointmanifestHeader-1.0-1.0.0",
                        "com.ibm.websphere.appserver.singleuiendpointmanifestHeader-1.0", "1.0.0",
                        "Single UI Endpoint Manifest Header Tool Display Name", "/AdminCenterEndpoint1/welcome");

        findFeatureTool(featureTools, "com.ibm.websphere.appserver.multiuiendpointmanifestHeader-1.0-1.0.0",
                        "com.ibm.websphere.appserver.multiuiendpointmanifestHeader-1.0", "1.0.0",
                        "Multi UI Endpoint Manifest Header Tool Display Name", "/AdminCenterEndpoint2/welcome");

        findFeatureTool(featureTools, "com.ibm.websphere.appserver.singleUIEndpointAndNonEndpointWABManifestHeader-1.0-1.0.0",
                        "com.ibm.websphere.appserver.singleUIEndpointAndNonEndpointWABManifestHeader-1.0", "1.0.0",
                        "Single UIEndpoint And Non Endpoint WAB Manifest Header Tool Display Name", "/AdminCenterEndpoint1/welcome");

        findFeatureTool(featureTools, "com.ibm.websphere.appserver.urlinsinglewab-1.0-1.0.0",
                        "com.ibm.websphere.appserver.urlinsinglewab-1.0", "1.0.0",
                        "URL in Single Wab Tool Display Name", "/TestBundle1/welcome");

        findFeatureTool(featureTools, "com.ibm.websphere.appserver.urlinmultiplewab-1.0-1.0.0",
                        "com.ibm.websphere.appserver.urlinmultiplewab-1.0", "1.0.0",
                        "URL in Multiple Wab Tool Display Name", "/BasicBundle1/welcome");
    }

    /**
     * This test ensures that we get the correct URL from a bundle when the feature is from a productExtn.
     */
    @Test
    public void testCatalogUpdatedWithURLFromProductExtensionFeature() throws Exception {
        installProductExtn(PRODUCT_EXTN_3, null);

        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the featureTools are correct
        assertContains(response, "featureTools");

        JsonArray featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools: " + featureTools);

        findCompleteFeatureTool(featureTools, "prodextn3%3Acom.ibm.websphere.appserver.prodextn3-1.0-1.0.0",
                                "prodextn3:com.ibm.websphere.appserver.prodextn3-1.0", "1.0.0",
                                "Prod Extn 3 Tool Display Name", "/prodextn3/welcome",
                                "This is the description for prodextn3. It is used to test that we can read from product Extensions feature directories.",
                                "/ibm/api/adminCenter/v1/icons/com.ibm.websphere.appserver.prodextn3-1.0");
    }

    /**
     * This test ensures that we get the correct URL from a bundle when there are different versioned
     * bundles in the runtime.
     */
    @Test
    public void testCatalogUpdatedWithURLFromCorrectVersionedBundle() throws Exception {
        installCoreFeature(ORIGINAL_UPDATE_MANIFEST);
        installCoreBundle(BASIC_BUNDLE1);
        installCoreBundle(BASIC_BUNDLE1_V2);
        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the tools are correct
        assertContains(response, "featureTools");

        JsonArray featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools: " + featureTools);

        findCompleteFeatureTool(featureTools, "com.ibm.websphere.appserver.updateautofeaturetool-1.0-1.0.0",
                                "com.ibm.websphere.appserver.updateautofeaturetool-1.0", "1.0.0",
                                "Original AutoFeatureTool Display Name", "/BasicBundle1Version2/welcome",
                                "This is the description for UpdateAutoFeatureTool. It is the original tool in the update test.",
                                "/ibm/api/adminCenter/v1/icons/com.ibm.websphere.appserver.updateautofeaturetool-1.0");
    }

    /**
     * This test ensures that we reread manifest files if their timestamps have changed. Because we cache
     * which manifests we've read, we check that an existing manifest hasn't been updated, perhaps via an uninstall and
     * reinstall since we last read the manifests.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCatalogUpdatedWithUpdatedManifests() throws Exception {
        installCoreFeature(ORIGINAL_UPDATE_MANIFEST);
        installCoreBundle(BASIC_BUNDLE1);
        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the tools are correct
        assertContains(response, "featureTools");

        JsonArray featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools initial GET: " + featureTools);

        findCompleteFeatureTool(featureTools, "com.ibm.websphere.appserver.updateautofeaturetool-1.0-1.0.0",
                                "com.ibm.websphere.appserver.updateautofeaturetool-1.0", "1.0.0",
                                "Original AutoFeatureTool Display Name", "/BasicBundle1/welcome",
                                "This is the description for UpdateAutoFeatureTool. It is the original tool in the update test.",
                                "/ibm/api/adminCenter/v1/icons/com.ibm.websphere.appserver.updateautofeaturetool-1.0");

        // we have to do a delete here, because the uninstallFeature has a guard against do it in a running server. 
        FATSuite.server.deleteFileFromLibertyInstallRoot(FEATURE_PATH + ORIGINAL_UPDATE_MANIFEST + MANIFEST_SUFFIX);
        LibertyFileManager.copyFileIntoLiberty(FATSuite.server.getMachine(), FATSuite.server.getInstallRoot() + "/" + FEATURE_PATH, "publish/features/" + UPDATED_UPDATE_MANIFEST
                                                                                                                                    + MANIFEST_SUFFIX);
        LibertyFileManager.copyFileIntoLiberty(FATSuite.server.getMachine(), FATSuite.server.getInstallRoot() + "/" + LIB_PATH, "publish/bundles/" + TEST_BUNDLE1 + ".jar");

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the tools are correct
        assertContains(response, "featureTools");

        featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools after update: " + featureTools);

        findCompleteFeatureTool(featureTools, "com.ibm.websphere.appserver.updateautofeaturetool-1.0-1.0.0",
                                "com.ibm.websphere.appserver.updateautofeaturetool-1.0", "1.0.0",
                                "Updated AutoFeatureTool Display Name", "/TestBundle1/welcome",
                                "This is the description for UpdateAutoFeatureTool. It is the updated tool in the update test.",
                                "/ibm/api/adminCenter/v1/icons/com.ibm.websphere.appserver.updateautofeaturetool-1.0");
    }

    /**
     * This test ensures that we reread manifest files if their timestamps have changed. Because we cache
     * which manifests we've read, we check that an existing manifest hasn't been updated, perhaps via an uninstall and
     * reinstall since we last read the manifests. This test checks the product extension directories.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCatalogUpdatedWithUpdatedManifestsInProductExtension() throws Exception {
        FATSuite.server.installProductExtension(PRODUCT_EXTN_1);
        FATSuite.server.installProductFeature(PRODUCT_EXTN_1, PRODEXTN1_ORIGINAL_UPDATE_MANIFEST);
        FATSuite.server.installProductBundle(PRODUCT_EXTN_1, PRODEXTN1_BASIC_BUNDLE1);
        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the tools are correct
        assertContains(response, "featureTools");

        JsonArray featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools initial GET: " + featureTools);

        findCompleteFeatureTool(featureTools, "prodextn1%3Acom.ibm.websphere.appserver.prodextn1updateautofeaturetool-1.0-1.0.0",
                                "prodextn1:com.ibm.websphere.appserver.prodextn1updateautofeaturetool-1.0", "1.0.0",
                                "ProdExtn1 Original AutoFeatureTool Display Name", "/ProdExtn1BasicBundle1/welcome",
                                "This is the description for ProdExtn1 Update AutoFeatureTool. It is the original tool in the update test.",
                                "/ibm/api/adminCenter/v1/icons/com.ibm.websphere.appserver.prodextn1updateautofeaturetool-1.0");

        // We have to do a delete here, because the uninstallProductExtn has a guard against do it in a running server.
        File installRoot = new File(FATSuite.server.getInstallRoot());
        File prodExtnManifest = new File(installRoot.getParent(), PRODUCT_EXTN_1 + "/" + FEATURE_PATH + PRODEXTN1_ORIGINAL_UPDATE_MANIFEST + MANIFEST_SUFFIX);
        prodExtnManifest.delete();
        assertFalse("Product Extension manifest failed to delete.", prodExtnManifest.exists());

        // Manually install the feature and bundle into the productExtension.
        File prodExtnDir = new File(installRoot.getParent(), PRODUCT_EXTN_1);
        LibertyFileManager.copyFileIntoLiberty(FATSuite.server.getMachine(), prodExtnDir.getAbsolutePath() + "/" + FEATURE_PATH, "publish/productfeatures/"
                                                                                                                                 + PRODEXTN1_UPDATED_UPDATE_MANIFEST
                                                                                                                                 + MANIFEST_SUFFIX);
        LibertyFileManager.copyFileIntoLiberty(FATSuite.server.getMachine(), prodExtnDir.getAbsolutePath() + "/" + LIB_PATH, "publish/productbundles/" + PRODEXTN1_TEST_BUNDLE1
                                                                                                                             + ".jar");

        response = get(url, adminUser, adminPassword, 200);

        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the tools are correct
        assertContains(response, "featureTools");
        featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools after update: " + featureTools);

        findCompleteFeatureTool(featureTools, "prodextn1%3Acom.ibm.websphere.appserver.prodextn1updateautofeaturetool-1.0-1.0.0",
                                "prodextn1:com.ibm.websphere.appserver.prodextn1updateautofeaturetool-1.0", "1.0.0",
                                "ProdExtn1 Updated AutoFeatureTool Display Name", "/ProdExtn1TestBundle1/welcome",
                                "This is the description for ProdExtn1 Update AutoFeatureTool. It is the updated tool in the update test.",
                                "/ibm/api/adminCenter/v1/icons/com.ibm.websphere.appserver.prodextn1updateautofeaturetool-1.0");
    }

    /**
     * This test ensures that if a feature is removed, the catalog is updated.
     */
    @Test
    public void testCatalogUpdatedWithRemovedFeature() throws Exception {
        installCoreFeatureAndBundle(BASIC_AUTOFEATURE);
        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the featureTools are correct
        assertContains(response, "featureTools");

        JsonArray featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools post-add: " + featureTools);
        findFeatureTool(featureTools, "com.ibm.websphere.appserver.basicautofeaturetool-1.0-1.0.0");

        ignoreErrorAndStopServerWithValidate();

        removeCoreFeatureAndBundle(BASIC_AUTOFEATURE);

        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the featureTools are correct
        assertContains(response, "featureTools");

        JsonArray refreshedfeatureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools post-remove: " + refreshedfeatureTools);
        confirmRemoved(refreshedfeatureTools, "com.ibm.websphere.appserver.basicautofeaturetool-1.0-1.0.0");
    }

    /**
     * This test ensures that if a Product Extension feature is removed, the catalog is updated.
     */
    @Test
    public void testCatalogUpdatedWithRemovedProductExtensionFeature() throws Exception {
        installProductExtn(PRODUCT_EXTN_1, PRODUCT_EXTN_1_L10N);
        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the featureTools are correct
        assertContains(response, "featureTools");

        JsonArray featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools post-add: " + featureTools);
        findFeatureTool(featureTools, "prodextn1%3Acom.ibm.websphere.appserver.prodextn1-1.0-1.0.0");

        ignoreErrorAndStopServerWithValidate();

        removeProductExtn(PRODUCT_EXTN_1);

        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the featureTools are correct
        assertContains(response, "featureTools");

        JsonArray refreshedfeatureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools post-remove: " + refreshedfeatureTools);
        confirmRemoved(refreshedfeatureTools, "prodextn1%3Acom.ibm.websphere.appserver.prodextn1-1.0-1.0.0");
    }

    /**
     * This test ensures if we have a feature that is provisioned in a running server that we still add it to the catalog
     * correctly. It also tests that the catalog is updated when the feature is removed.
     */
    @Test
    public void testCatalogUpdatedWithRefreshedFeature() throws Exception {
        installCoreFeatureAndBundle(BASIC_AUTOFEATURE);
        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the featureTools are correct
        assertContains(response, "featureTools");

        JsonArray featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools post-add: " + featureTools);
        findFeatureTool(featureTools, "com.ibm.websphere.appserver.basicautofeaturetool-1.0-1.0.0");

        // we have to do a delete here, because the uninstallFeature has a guard against do it in a running server. 
        FATSuite.server.deleteFileFromLibertyInstallRoot(FEATURE_PATH + BASIC_AUTOFEATURE + MANIFEST_SUFFIX);
        // Install a new feature into the runtime
        LibertyFileManager.copyFileIntoLiberty(FATSuite.server.getMachine(), FATSuite.server.getInstallRoot() + "/" + FEATURE_PATH, "publish/features/" + URL_SINGLE_WAB
                                                                                                                                    + MANIFEST_SUFFIX);
        LibertyFileManager.copyFileIntoLiberty(FATSuite.server.getMachine(), FATSuite.server.getInstallRoot() + "/" + LIB_PATH, "publish/bundles/" + TEST_BUNDLE1 + ".jar");

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the featureTools are correct
        assertContains(response, "featureTools");

        JsonArray refreshedfeatureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools post-refresh: " + refreshedfeatureTools);
        findFeatureTool(refreshedfeatureTools, "com.ibm.websphere.appserver.urlinsinglewab-1.0-1.0.0");
    }

    /**
     * This test ensures if we have a product extension feature that is provisioned in a running server,
     * that we still add it to the catalog correctly. It also tests that the catalog is updated when the feature is removed.
     */
    @Test
    public void testCatalogUpdatedWithRefreshedProductExtensionFeature() throws Exception {
        installProductExtn(PRODUCT_EXTN_1, PRODUCT_EXTN_1_L10N);
        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the featureTools are correct
        assertContains(response, "featureTools");

        JsonArray featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools post-add: " + featureTools);
        findFeatureTool(featureTools, "prodextn1%3Acom.ibm.websphere.appserver.prodextn1-1.0-1.0.0");

        // We have to do a delete here, because the uninstallProductExtn has a guard against do it in a running server.
        File installRoot = new File(FATSuite.server.getInstallRoot());
        File prodExtnManifest = new File(installRoot.getParent(), PRODUCT_EXTN_1 + "/" + FEATURE_PATH + PRODUCT_EXTN_1 + MANIFEST_SUFFIX);
        prodExtnManifest.delete();
        assertFalse("Product Extension manifest failed to delete.", prodExtnManifest.exists());

        // Also add a new Usr feature into the runtime
        LibertyFileManager.copyFileIntoLiberty(FATSuite.server.getMachine(), FATSuite.server.getInstallRoot() + "/usr/extension/lib/features", "publish/features/" + USR
                                                                                                                                               + MANIFEST_SUFFIX);
        LibertyFileManager.copyFileIntoLiberty(FATSuite.server.getMachine(), FATSuite.server.getInstallRoot() + "/usr/extension/lib", "publish/bundles/" + USR + ".jar");

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the featureTools are correct
        assertContains(response, "featureTools");

        JsonArray refreshedfeatureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools post-refresh: " + refreshedfeatureTools);
        findFeatureTool(refreshedfeatureTools, "usr%3Acom.ibm.websphere.appserver.usrtool-1.0-1.0.0");
    }

    /**
     * This test ensures if we have a public feature and a private feature that the feature name is stored correctly.
     * Public features use short names as their installed name whereas protected and private use symbolicname as their installed name.
     */
    @Test
    public void testScopedFeatureNamesAreStoredCorrectly() throws Exception {
        installCoreBundle(BASIC_AUTOFEATURE);
        installCoreFeature(BASIC_AUTOFEATURE);
        installCoreFeatureL10N(BASIC_AUTOFEATURE_L10N);
        installCoreFeature(PUBLIC_BASIC_AUTOFEATURE);
        installCoreFeature(PROTECTED_BASIC_AUTOFEATURE);

        FATSuite.server.installProductExtension(SCOPED_PRODUCT_EXTN);
        FATSuite.server.installProductBundle(SCOPED_PRODUCT_EXTN, PRODUCT_EXTN_1);
        FATSuite.server.installProductFeature(SCOPED_PRODUCT_EXTN, PRODUCT_EXTN_1);
        FATSuite.server.installProductFeatureL10N(SCOPED_PRODUCT_EXTN, PRODUCT_EXTN_1_L10N);
        FATSuite.server.installProductFeature(SCOPED_PRODUCT_EXTN, PROTECTED_PROD_EXTN);
        FATSuite.server.installProductFeature(SCOPED_PRODUCT_EXTN, PUBLIC_PROD_EXTN);

        // use server.xml with scopedprodextn:publicprodextntool-1.0 feature
        FATSuite.server.renameLibertyServerRootFile("server.xml", "server-original.xml");
        FATSuite.server.copyFileToLibertyServerRoot("server-prodextn.xml");
        FATSuite.server.renameLibertyServerRootFile("server-prodextn.xml", "server.xml");
        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the featureTools are correct
        assertContains(response, "featureTools");

        JsonArray featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools: " + featureTools);

        findFeatureTool(featureTools, "BasicPublicAutoFeatureTool-1.0-1.0.0",
                        "BasicPublicAutoFeatureTool-1.0", "1.0.0",
                        "BasicPublicAutoFeatureTool Display Name", null);

        findFeatureTool(featureTools, "com.ibm.websphere.appserver.basicprotectedautofeaturetool-1.0-1.0.0",
                        "com.ibm.websphere.appserver.basicprotectedautofeaturetool-1.0", "1.0.0",
                        "BasicProtectedAutoFeatureTool Display Name", null);

        findFeatureTool(featureTools, "com.ibm.websphere.appserver.basicautofeaturetool-1.0-1.0.0",
                        "com.ibm.websphere.appserver.basicautofeaturetool-1.0", "1.0.0",
                        "BasicAutoFeatureTool Display Name", null);

        findFeatureTool(featureTools, "scopedprodextn%3Apublicprodextntool-1.0-1.0.0",
                        "scopedprodextn:publicprodextntool-1.0", "1.0.0",
                        "Public Prod Extn Tool Display Name", null);

        findFeatureTool(featureTools, "scopedprodextn%3Acom.ibm.websphere.appserver.protectedprodextn-1.0-1.0.0",
                        "scopedprodextn:com.ibm.websphere.appserver.protectedprodextn-1.0", "1.0.0",
                        "Protected Prod Extn Tool Display Name", null);

        findFeatureTool(featureTools, "scopedprodextn%3Acom.ibm.websphere.appserver.prodextn1-1.0-1.0.0",
                        "scopedprodextn:com.ibm.websphere.appserver.prodextn1-1.0", "1.0.0",
                        "Prod Extn 1 Tool Display Name", null);
    }

    /**
     * This test ensures if we have a have a Subsystem-Name header we use that, otherwise we use the shortname of the feature, and
     * failing that we use the symbolicName.
     */
    @Test
    public void testToolNameDefaults() throws Exception {
        installCoreBundle(BASIC_AUTOFEATURE);
        installCoreFeature(SUBSYSTEM_NAME_MANIFEST);
        installCoreFeature(IBM_SHORTNAME_MANIFEST);
        installCoreFeature(SUBSYSTEM_SYMBOLICNAME_MANIFEST);

        startServerAndValidate(FATSuite.server);

        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the featureTools are correct
        assertContains(response, "featureTools");

        JsonArray featureTools = response.getJsonArray("featureTools");
        Log.info(c, method.getMethodName(), "featureTools: " + featureTools);

        findFeatureTool(featureTools, "com.ibm.websphere.appserver.subsystemname-1.0-1.0.0",
                        "com.ibm.websphere.appserver.subsystemname-1.0", "1.0.0",
                        "SubsystemName Display Name", null);

        // The public tool will use the IBM ShortName as its feature name.
        findFeatureTool(featureTools, "com.ibm.websphere.appserver.ibmshortname-1.0-1.0.0",
                        "com.ibm.websphere.appserver.ibmshortname-1.0", "1.0.0",
                        "IBMShortName-1.0", null);

        findFeatureTool(featureTools, "com.ibm.websphere.appserver.subsystemsymbolicname-1.0-1.0.0",
                        "com.ibm.websphere.appserver.subsystemsymbolicname-1.0", "1.0.0",
                        "com.ibm.websphere.appserver.subsystemsymbolicname-1.0", null);
    }

}
