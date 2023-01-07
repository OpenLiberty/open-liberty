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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Version;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ui.fat.APIConstants;
import com.ibm.ws.ui.fat.FATSuite;
import com.ibm.ws.ui.fat.rest.CommonRESTTest;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.utils.FileUtils;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class IconRestHandlerTest extends CommonRESTTest implements APIConstants {
    private static final Class<?> c = IconRestHandlerTest.class;

    private static final FeatureInfo BASIC_AUTOFEATURE = new FeatureInfo("basicAutoFeatureTool", "com.ibm.websphere.appserver.basicautofeaturetool-1.0");
    private static final FeatureInfo NO_ICON_HEADER_AUTOFEATURE = new FeatureInfo("noIconHeaderManifest", "com.ibm.websphere.appserver.noiconheadermanifest-1.0");
    private static final FeatureInfo MIXED_SIZED_ICON_HEADER_AUTOFEATURE = new FeatureInfo("mixedSizedIconHeaderManifest", "com.ibm.websphere.appserver.mixedsizediconheadermanifest-1.0");
    private static final FeatureInfo MIXED_SIZED_ICON_WITH_UNSIZED_ICON_HEADER_AUTOFEATURE = new FeatureInfo("mixedSizedWithUnsizedIconHeaderManifest", "com.ibm.websphere.appserver.mixedsizedwithunsizediconheadermanifest-1.0");
    private static final FeatureInfo MULTI_UNSIZED_ICON_HEADER_AUTOFEATURE = new FeatureInfo("multiUnsizedIconHeaderManifest", "com.ibm.websphere.appserver.multiunsizediconheadermanifest-1.0");
    private static final FeatureInfo NO_SIZED_ICON_HEADER_AUTOFEATURE = new FeatureInfo("noSizedIconHeaderManifest", "com.ibm.websphere.appserver.nosizediconheadermanifest-1.0");
    private static final FeatureInfo EMPTY_ICON_HEADER_AUTOFEATURE = new FeatureInfo("emptyIconHeaderManifest", "com.ibm.websphere.appserver.emptyiconheadermanifest-1.0");
    private static final FeatureInfo GIF_ICON_HEADER_AUTOFEATURE = new FeatureInfo("gifIconHeaderManifest", "com.ibm.websphere.appserver.gificonheadermanifest-1.0");
    private static final FeatureInfo JPG_ICON_HEADER_AUTOFEATURE = new FeatureInfo("jpegIconHeaderManifest", "com.ibm.websphere.appserver.jpgiconheadermanifest-1.0");
    private static final FeatureInfo INVALID_RELATIVE_ICON_URI_HEADER_AUTOFEATURE = new FeatureInfo("invalidRelativeIconURIHeaderManifest", "com.ibm.websphere.appserver.invalidrelativeiconuriheadermanifest-1.0");
    private static final FeatureInfo USR = new FeatureInfo("usr", "com.ibm.websphere.appserver.usrtool-1.0");
    private static final FeatureInfo PRODUCT_EXTN_1 = new FeatureInfo("prodextn1", "com.ibm.websphere.appserver.prodextn1-1.0");
    private static final FeatureInfo PRODUCT_EXTN_2 = new FeatureInfo("prodextn2", "com.ibm.websphere.appserver.prodextn2-2.0");

    private static final FeatureInfo[] coreFeatures = { NO_ICON_HEADER_AUTOFEATURE, MIXED_SIZED_ICON_HEADER_AUTOFEATURE,
                                                        MIXED_SIZED_ICON_WITH_UNSIZED_ICON_HEADER_AUTOFEATURE, MULTI_UNSIZED_ICON_HEADER_AUTOFEATURE,
                                                        NO_SIZED_ICON_HEADER_AUTOFEATURE, EMPTY_ICON_HEADER_AUTOFEATURE, GIF_ICON_HEADER_AUTOFEATURE,
                                                        JPG_ICON_HEADER_AUTOFEATURE, INVALID_RELATIVE_ICON_URI_HEADER_AUTOFEATURE };
    private static final FeatureInfo[] coreBundles = { BASIC_AUTOFEATURE };
    private static final FeatureInfo[] usrFeatures = { USR };
    private static final FeatureInfo[] usrBundles = { USR };

    private static final FeatureInfo[] productExtns = { PRODUCT_EXTN_1, PRODUCT_EXTN_2 };

    private static final String localTestDir = "lib/LibertyFATTestFiles";

    public IconRestHandlerTest() {
        super(c);
        url = API_V1_ICONS;
    }

    /**
     * At the end of all of the testing, reset to default.
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
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

        for (FeatureInfo feature : coreFeatures) {
            removeCoreFeature(feature.featureName);
            File iconsDir = new File(FATSuite.server.getInstallRoot(), FEATURE_PATH + "icons/" + feature.featureSymbolicName);
            if (iconsDir.exists())
                FileUtils.recursiveDelete(iconsDir);
        }

        for (FeatureInfo bundle : coreBundles)
            removeCoreBundle(bundle.featureName);

        for (FeatureInfo usrFeature : usrFeatures) {
            removeUsrFeature(usrFeature.featureName);
            File iconsDir = new File(FATSuite.server.getUserDir(), "extensions/" + FEATURE_PATH + "icons/" + usrFeature.featureSymbolicName);
            if (iconsDir.exists())
                FileUtils.recursiveDelete(iconsDir);
        }

        for (FeatureInfo usrBundle : usrBundles)
            removeUsrBundle(usrBundle.featureName);

        for (FeatureInfo prodExtn : productExtns)
            removeProductExtn(prodExtn.featureName);
    }

    private void ignoreErrorAndStopServerWithValidate() throws Exception {
        FATSuite.server.stopServer("CWWKF0002E: A bundle could not be found for com.ibm.ws.appserver.*", 
                            "CWWKE0702E: Could not resolve module: com.ibm.ws.appserver.*",
                            "CWWKF0029E: Could not resolve module: com.ibm.ws.appserver.*",
                            "CWWKX1009E:.*");
        assertFalse("FAIL: Server is not stopped.",
                    FATSuite.server.isStarted());
    }

    private void installCoreFeature(String name) throws Exception {
        FATSuite.server.installSystemFeature(name);
        assertTrue("Feature " + FEATURE_PATH + name + MANIFEST_SUFFIX + " was not installed successfully.",
                   FATSuite.server.fileExistsInLibertyInstallRoot(FEATURE_PATH + name + MANIFEST_SUFFIX));
    }

    private void installCoreBundle(String name) throws Exception {
        FATSuite.server.installSystemBundle(name);
        assertTrue("Bundle " + LIB_PATH + name + JAR_SUFFIX + " was not installed successfully.", FATSuite.server.fileExistsInLibertyInstallRoot(LIB_PATH + name + JAR_SUFFIX));
    }

    private void removeCoreFeature(String name) throws Exception {
        FATSuite.server.uninstallSystemFeature(name);
        assertFalse("Feature " + FEATURE_PATH + name + MANIFEST_SUFFIX + " was not deleted successfully.",
                    FATSuite.server.fileExistsInLibertyInstallRoot(FEATURE_PATH + name + MANIFEST_SUFFIX));
    }

    private void removeCoreBundle(String name) throws Exception {
        FATSuite.server.uninstallSystemBundle(name);
        assertFalse("Bundle " + LIB_PATH + name + JAR_SUFFIX + " was not deleted successfully.", FATSuite.server.fileExistsInLibertyInstallRoot(LIB_PATH + name + JAR_SUFFIX));
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

    private void removeUsrBundle(String name) throws Exception {
        FATSuite.server.uninstallUserBundle(name);
        assertFalse("Bundle " + USR_LIB_PATH + name + JAR_SUFFIX + " was not deleted successfully.",
                    FATSuite.server.fileExistsInLibertyInstallRoot(USR_LIB_PATH + name + JAR_SUFFIX));
    }

    private void installProductExtn(String productExtnName) throws Exception {
        FATSuite.server.installProductExtension(productExtnName);
        FATSuite.server.installProductBundle(productExtnName, productExtnName);
        FATSuite.server.installProductFeature(productExtnName, productExtnName);
        assertTrue("Properties " + ETC_EXTNS_PATH + productExtnName + ".properties was not installed successfully.",
                   FATSuite.server.fileExistsInLibertyInstallRoot(ETC_EXTNS_PATH + productExtnName + ".properties"));
    }

    private void removeProductExtn(String productExtnName) throws Exception {
        FATSuite.server.uninstallProductExtension(productExtnName);
        assertFalse("Properties " + ETC_EXTNS_PATH + productExtnName + ".properties was not deleted successfully.",
                    FATSuite.server.fileExistsInLibertyInstallRoot(ETC_EXTNS_PATH + productExtnName + ".properties"));
    }

    // private byte[] convertStringToBytes(String stringToConvert) throws Exception {
    //     return convertStringToBytes.getBytes(StandardCharsets.UTF_8);
    // }

    /**
     * This basic test ensures we can get the default sized default icon.
     */
    @Test
    public void testGetDefaultSizedDefaultIcon() throws Exception {
        startServerAndValidate(FATSuite.server);

        byte[] responseBytes = getImage(url + "/default", adminUser, adminPassword, 200);

        assertTrue("Image not the expected size", Arrays.equals(responseBytes, getDefaultIcon(142)));

    }

    /**
     * This basic test ensures we can get the default sized default icon as reader.
     */
    @Test
    public void testGetDefaultSizedDefaultIcon_reader() throws Exception {
        startServerAndValidate(FATSuite.server);

        byte[] responseBytes = getImage(url + "/default", readerUser, readerPassword, 200);

        assertTrue("Image does not have the expected size", Arrays.equals(responseBytes, getDefaultIcon(142)));

    }

    /**
     * This basic test ensures we cannot get the default sized default icon without a role.
     */
    @Test
    public void testGetDefaultSizedDefaultIcon_nonadmin() throws Exception {
        startServerAndValidate(FATSuite.server);

        getImage(url + "/default", nonadminUser, nonadminPassword, 403);
    }

    /**
     * This basic test ensures we can get the specified sized default icon.
     */
    @Test
    public void testGetSpecificSizedDefaultIcon() throws Exception {
        startServerAndValidate(FATSuite.server);

        Integer size = 78;
        byte[] responseBytes = getImage(url + "/default?size=" + size, adminUser, adminPassword, 200);

        assertTrue("Image does not have the expected size", Arrays.equals(responseBytes, getDefaultIcon(size)));

    }

    /**
     * This basic test ensures we can list all the default icon sizes
     */
    @Test
    public void testGetDefaultIconSizes() throws Exception {
        startServerAndValidate(FATSuite.server);

        Set<String> response = getImages(url + "/default?sizes", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "response from getImages: " + response);

        assertTrue("There are more than the expected number of default icons: " + response.size(), response.size() == 4);
        assertTrue("Expected Default Icon size of 142 not found", response.contains("142"));
        assertTrue("Expected Default Icon size of 78 not found", response.contains("78"));
        assertTrue("Expected Default Icon size of 52 not found", response.contains("52"));
        assertTrue("Expected Default Icon size of 28 not found", response.contains("28"));
    }

    /**
     * This basic test ensures we can get the unsized icon from the feature manifest. If all icons are sized, we
     * should get the default sized icon from the feature. Failing that we will get the default icon.
     * We test with manifests that have a mixture of sizes and unsized icon headers.
     */
    @Test
    public void testGetSizedAndUnSizedDefaultSizedFeatureIcon() throws Exception {
        installCoreBundle(BASIC_AUTOFEATURE.featureName);
        installCoreFeature(MIXED_SIZED_ICON_WITH_UNSIZED_ICON_HEADER_AUTOFEATURE.featureName);
        String relativeIconDir = "lib/features/icons/" + MIXED_SIZED_ICON_WITH_UNSIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "/icons";
        String iconFile = "icons/toolicon1.png";
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir, iconFile);
        String iconFile2 = "icons/toolicon2.png";
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir, iconFile2);

        startServerAndValidate(FATSuite.server);

        byte[] responseBytes = getImage(url + "/" + MIXED_SIZED_ICON_WITH_UNSIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName, adminUser, adminPassword, 200);
        byte[] expectedBytes = getIcon(new File(localTestDir, iconFile));
        assertTrue("The response bytes does not equal the expected icon bytes. responseBytes.length=" + responseBytes.length + " expectedBytes.length=" + expectedBytes.length,
                   Arrays.equals(responseBytes, expectedBytes));

        responseBytes = getImage(url + "/" + MIXED_SIZED_ICON_WITH_UNSIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "?size=142", adminUser, adminPassword, 200);
        expectedBytes = getIcon(new File(localTestDir, iconFile2));
        assertTrue("The response bytes does not equal the expected icon bytes. responseBytes2.length=" + responseBytes.length + " expectedBytes2.length=" + expectedBytes.length,
                   Arrays.equals(responseBytes, expectedBytes));

        Set<String> response = getImages(url + "/" + MIXED_SIZED_ICON_WITH_UNSIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "?sizes", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "response from getImages: " + response);
        assertTrue("Expected Default Icon size of 142 not found", response.contains("142"));
        assertTrue("Expected Default Icon size of 0 not found", response.contains("0"));
    }

    /**
     * This basic test ensures we cope with both unsized and sized icons in the manifest.
     */
    @Test
    public void testGetDefaultSizedFeatureIcon() throws Exception {
        installCoreBundle(BASIC_AUTOFEATURE.featureName);
        installCoreFeature(NO_SIZED_ICON_HEADER_AUTOFEATURE.featureName);
        String relativeIconDir = "lib/features/icons/" + NO_SIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "/icons";
        String iconFile = "icons/toolicon1.png";
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir, iconFile);

        startServerAndValidate(FATSuite.server);

        byte[] responseBytes = getImage(url + "/" + NO_SIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName, adminUser, adminPassword, 200);
        byte[] expectedBytes = getIcon(new File(localTestDir, iconFile));
        assertTrue("The response bytes does not equal the expected icon bytes. responseBytes.length=" + responseBytes.length + " expectedBytes.length=" + expectedBytes.length,
                   Arrays.equals(responseBytes, expectedBytes));
    }

    /**
     * This basic test ensures we can get the specified sized feature icon.
     */
    @Test
    public void testGetSpecifiedSizedFeatureIcon() throws Exception {
        installCoreBundle(BASIC_AUTOFEATURE.featureName);
        installCoreFeature(MIXED_SIZED_ICON_HEADER_AUTOFEATURE.featureName);
        String relativeIconDir = "lib/features/icons/" + MIXED_SIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "/icons";
        String iconFile = "icons/toolicon1.png";
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir, iconFile);
        String iconFile2 = "icons/toolicon2.png";
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir, iconFile2);

        startServerAndValidate(FATSuite.server);

        byte[] responseBytes = getImage(url + "/" + MIXED_SIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "?size=77", adminUser, adminPassword, 200);
        byte[] expectedBytes = getIcon(new File(localTestDir, iconFile2));
        assertTrue("The response bytes does not equal the expected icon bytes. responseBytes.length=" + responseBytes.length + " expectedBytes.length=" + expectedBytes.length,
                   Arrays.equals(responseBytes, expectedBytes));
    }

    /**
     * This basic test ensures we can get a default sized feature icon when no default icon
     * is configured in the manifest header but we have a sized icon that matches the default
     * size.
     */
    @Test
    public void testGetDefaultSizedFeatureIconFromSizedIconHeader() throws Exception {
        installCoreBundle(BASIC_AUTOFEATURE.featureName);
        installCoreFeature(MIXED_SIZED_ICON_HEADER_AUTOFEATURE.featureName);
        String relativeIconDir = "lib/features/icons/" + MIXED_SIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "/icons";
        String iconFile = "icons/toolicon1.png";
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir, iconFile);
        String iconFile2 = "icons/toolicon2.png";
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir, iconFile2);

        startServerAndValidate(FATSuite.server);

        byte[] responseBytes = getImage(url + "/" + MIXED_SIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName, adminUser, adminPassword, 200);
        byte[] expectedBytes = getIcon(new File(localTestDir, iconFile));
        assertTrue("The response bytes does not equal the expected icon bytes. responseBytes.length=" + responseBytes.length + " expectedBytes.length=" + expectedBytes.length,
                   Arrays.equals(responseBytes, expectedBytes));
    }

    /**
     * This basic test ensures we can list the icon sizes, including unsized icon.
     */
    @Test
    public void testGetFeatureIconSizes() throws Exception {
        installCoreBundle(BASIC_AUTOFEATURE.featureName);
        installCoreFeature(MIXED_SIZED_ICON_HEADER_AUTOFEATURE.featureName);
        String relativeIconDir = "lib/features/icons/" + MIXED_SIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "/icons";
        String iconFile = "icons/toolicon1.png";
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir, iconFile);
        String iconFile2 = "icons/toolicon2.png";
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir, iconFile2);
        startServerAndValidate(FATSuite.server);

        Set<String> response = getImages(url + "/" + MIXED_SIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "?sizes", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "response from getImages: " + response);
        assertTrue("Expected Default Icon size of 142 not found. Sizes: " + response, response.contains("142"));
        assertTrue("Expected Default Icon size of 77 not found. Sizes: " + response, response.contains("77"));
    }

    /**
     * This basic test ensures we can get the default sized default icon.
     */
    @Test
    public void testGetIconFromNoIconHeaderManifest() throws Exception {
        installCoreBundle(BASIC_AUTOFEATURE.featureName);
        installCoreFeature(NO_ICON_HEADER_AUTOFEATURE.featureName);
        startServerAndValidate(FATSuite.server);

        byte[] responseBytes = getImage(url + "/" + NO_ICON_HEADER_AUTOFEATURE.featureSymbolicName, adminUser, adminPassword, 200);
        assertTrue("Image not the expected size", Arrays.equals(responseBytes, getDefaultIcon(142)));
    }

    /**
     * This basic test ensures we can cope with multiple unsized icons. We should only have 1 icon listed
     * and this will be the last icon URL processed.
     */
    @Test
    public void testGetIconFromMultiUnsizedIconHeaderManifest() throws Exception {
        installCoreBundle(BASIC_AUTOFEATURE.featureName);
        installCoreFeature(MULTI_UNSIZED_ICON_HEADER_AUTOFEATURE.featureName);
        String relativeIconDir = "lib/features/icons/" + MULTI_UNSIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "/icons";
        String iconFile = "icons/toolicon1.png";
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir, iconFile);
        String iconFile2 = "icons/toolicon2.png";
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir, iconFile2);
        startServerAndValidate(FATSuite.server);

        Set<String> response = getImages(url + "/" + MULTI_UNSIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "?sizes", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "response from getImages: " + response);
        assertTrue("There were multiple entries in the list of sizes when it was expected to be one:" + response, response.size() == 1);
    }

    /**
     * This Test ensures we can cope with a Subsystem-Icon header with no icons listed. It should return the default icon in this case.
     */
    @Test
    public void testGetIconFromEmptyIconHeaderManifest() throws Exception {
        installCoreBundle(BASIC_AUTOFEATURE.featureName);
        installCoreFeature(EMPTY_ICON_HEADER_AUTOFEATURE.featureName);
        startServerAndValidate(FATSuite.server);

        byte[] responseBytes = getImage(url + "/" + EMPTY_ICON_HEADER_AUTOFEATURE.featureSymbolicName, adminUser, adminPassword, 200);
        assertTrue("Image does not have the expected size", Arrays.equals(responseBytes, getDefaultIcon(142)));

        Set<String> response = getImages(url + "/" + EMPTY_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "?sizes", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "response from getImages: " + response);
        Log.info(c, method.getMethodName(), "response empty: " + response.isEmpty());
        Log.info(c, method.getMethodName(), "response size: " + response.size());
        assertTrue("There were entries in the list of sizes when it was expected to be empty", response.size() == 0);
    }

    /**
     * This Test ensures we that a request for an icon of size that can't be found in either the applications list of icons
     * nor the default list of icons, returns the default icon at the defaultSize.
     */
    @Test
    public void testDefaultIconDefaultSizeWhenNoIconsExistForRequestedSize() throws Exception {
        installCoreBundle(BASIC_AUTOFEATURE.featureName);
        installCoreFeature(MIXED_SIZED_ICON_HEADER_AUTOFEATURE.featureName);
        String relativeIconDir = "lib/features/icons/" + MIXED_SIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName;
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir, "icons/toolicon1.png");
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir, "icons/toolicon2.png");
        startServerAndValidate(FATSuite.server);

        byte[] responseBytes = getImage(url + "/com.ibm.websphere.appserver." + MIXED_SIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "&size=999", adminUser, adminPassword, 200);
        assertTrue("Image does not have the expected size", Arrays.equals(responseBytes, getDefaultIcon(142)));
    }

    /**
     * This Test ensures we can download different image types. Valid types are png, jpg and gif.
     * We've tested PNG in previous tests, so this is gif and jpg.
     */
    @Test
    public void testGetIconTypes() throws Exception {
        installCoreBundle(BASIC_AUTOFEATURE.featureName);
        installCoreFeature(JPG_ICON_HEADER_AUTOFEATURE.featureName);
        installCoreFeature(GIF_ICON_HEADER_AUTOFEATURE.featureName);
        String relativeIconDir1 = "lib/features/icons/" + GIF_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "/icons";
        String iconFile = "icons/toolicon9.gif";
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir1, iconFile);
        String relativeIconDir2 = "lib/features/icons/" + JPG_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "/icons";
        String iconFile2 = "icons/toolicon9.jpg";
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir2, iconFile2);

        startServerAndValidate(FATSuite.server);

        byte[] responseBytes = getImage(url + "/" + GIF_ICON_HEADER_AUTOFEATURE.featureSymbolicName, adminUser, adminPassword, 200, "image/gif");
        byte[] expectedBytes = getIcon(new File(localTestDir, iconFile));
        assertTrue("The response bytes does not equal the expected icon bytes. responseBytes.length=" + responseBytes.length + " expectedBytes.length=" + expectedBytes.length,
                   Arrays.equals(responseBytes, expectedBytes));

        responseBytes = getImage(url + "/" + JPG_ICON_HEADER_AUTOFEATURE.featureSymbolicName, adminUser, adminPassword, 200, "image/jpeg");
        expectedBytes = getIcon(new File(localTestDir, iconFile2));
        assertTrue("The response bytes does not equal the expected icon bytes. responseBytes.length=" + responseBytes.length + " expectedBytes.length=" + expectedBytes.length,
                   Arrays.equals(responseBytes, expectedBytes));

    }

    /**
     * This Test that an icon URL that has ../../.. doesn't get to directories outside of the icons dir.
     */
    @Test
    public void testSecuringRelativeIconURIs() throws Exception {
        installCoreBundle(BASIC_AUTOFEATURE.featureName);
        installCoreFeature(INVALID_RELATIVE_ICON_URI_HEADER_AUTOFEATURE.featureName);
        String iconFile = "icons/toolicon1.png";
        FATSuite.server.copyFileToLibertyInstallRoot(iconFile);

        startServerAndValidate(FATSuite.server);

        byte[] responseBytes = getImage(url + "/" + INVALID_RELATIVE_ICON_URI_HEADER_AUTOFEATURE.featureSymbolicName, adminUser, adminPassword, 200);
        assertTrue("Image does not have the expected size", Arrays.equals(responseBytes, getDefaultIcon(142)));
    }

    /**
     * This Test that a call to the IconRestHandler with an invalid size is presented with the default icon at the
     * default size.
     */
    @Test
    public void testInvalidIconSizeURIs() throws Exception {
        installCoreBundle(BASIC_AUTOFEATURE.featureName);
        installCoreFeature(NO_SIZED_ICON_HEADER_AUTOFEATURE.featureName);
        String relativeIconDir = "lib/features/icons/" + NO_SIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "/icons";
        String iconFile = "icons/toolicon1.png";
        FATSuite.server.copyFileToLibertyInstallRoot(relativeIconDir, iconFile);

        startServerAndValidate(FATSuite.server);

        byte[] responseBytes = getImage(url + "/" + NO_SIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "?size=-1", adminUser, adminPassword, 200);
        byte[] expectedBytes = getIcon(new File(localTestDir, iconFile));
        assertTrue("The response bytes does not equal the expected icon bytes. responseBytes.length=" + responseBytes.length + " expectedBytes.length=" + expectedBytes.length,
                   Arrays.equals(responseBytes, expectedBytes));

        responseBytes = getImage(url + "/" + NO_SIZED_ICON_HEADER_AUTOFEATURE.featureSymbolicName + "?size=bob", adminUser, adminPassword, 200);
        assertTrue("The response bytes does not equal the expected icon bytes. responseBytes2.length=" + responseBytes.length + " expectedBytes.length=" + expectedBytes.length,
                   Arrays.equals(responseBytes, expectedBytes));
    }

    /**
     * This Test ensures we can serve icons from product extension libraries, including the usr extension
     */
    @Test
    public void testGetIconfromProductExtensions() throws Exception {
        installProductExtn(PRODUCT_EXTN_1.featureName);
        File installRoot = new File(FATSuite.server.getInstallRoot());
        File prodExtnIconDir = new File(installRoot.getParent(), PRODUCT_EXTN_1.featureName + "/" + FEATURE_PATH +
                                                                 "icons/" + PRODUCT_EXTN_1.featureSymbolicName + "/icons");
        LibertyFileManager.copyFileIntoLiberty(FATSuite.server.getMachine(), prodExtnIconDir.getAbsolutePath(), localTestDir + "/icons/toolicon3.png");
        LibertyFileManager.copyFileIntoLiberty(FATSuite.server.getMachine(), prodExtnIconDir.getAbsolutePath(), localTestDir + "/icons/toolicon4.png");

        installProductExtn(PRODUCT_EXTN_2.featureName);
        File installRoot2 = new File(FATSuite.server.getInstallRoot());
        File prodExtn2IconDir = new File(installRoot2.getParent(), PRODUCT_EXTN_2.featureName + "/" + FEATURE_PATH +
                                                                   "icons/" + PRODUCT_EXTN_2.featureSymbolicName + "/icons");
        LibertyFileManager.copyFileIntoLiberty(FATSuite.server.getMachine(), prodExtn2IconDir.getAbsolutePath(), localTestDir + "/icons/toolicon5.png");
        LibertyFileManager.copyFileIntoLiberty(FATSuite.server.getMachine(), prodExtn2IconDir.getAbsolutePath(), localTestDir + "/icons/toolicon6.png");

        installUsrFeatureAndBundle(USR.featureName);
        File usrIconDir = new File(FATSuite.server.getInstallRoot(), "usr/extension/" + FEATURE_PATH +
                                                                     "icons/" + USR.featureSymbolicName + "/icons");
        LibertyFileManager.copyFileIntoLiberty(FATSuite.server.getMachine(), usrIconDir.getAbsolutePath(), localTestDir + "/icons/toolicon7.png");
        LibertyFileManager.copyFileIntoLiberty(FATSuite.server.getMachine(), usrIconDir.getAbsolutePath(), localTestDir + "/icons/toolicon8.png");

        startServerAndValidate(FATSuite.server);

        byte[] responseBytes = getImage(url + "/" + PRODUCT_EXTN_1.featureSymbolicName, adminUser, adminPassword, 200);
        assertTrue("Image does not have the expected size", Arrays.equals(responseBytes, getIcon(new File(localTestDir, "icons/toolicon3.png"))));

        responseBytes = getImage(url + "/" + PRODUCT_EXTN_1.featureSymbolicName + "?size=58", adminUser, adminPassword, 200);
        assertTrue("Image does not have the expected size", Arrays.equals(responseBytes, getIcon(new File(localTestDir, "icons/toolicon4.png"))));

        responseBytes = getImage(url + "/" + PRODUCT_EXTN_2.featureSymbolicName, adminUser, adminPassword, 200);
        assertTrue("Image does not have the expected size", Arrays.equals(responseBytes, getIcon(new File(localTestDir, "icons/toolicon5.png"))));

        responseBytes = getImage(url + "/" + PRODUCT_EXTN_2.featureSymbolicName + "?size=58", adminUser, adminPassword, 200);
        assertTrue("Image does not have the expected size", Arrays.equals(responseBytes, getIcon(new File(localTestDir, "icons/toolicon6.png"))));

        responseBytes = getImage(url + "/" + USR.featureSymbolicName, adminUser, adminPassword, 200);
        assertTrue("Image does not have the expected size", Arrays.equals(responseBytes, getIcon(new File(localTestDir, "icons/toolicon7.png"))));

        responseBytes = getImage(url + "/" + USR.featureSymbolicName + "?size=58", adminUser, adminPassword, 200);
        assertTrue("Image does not have the expected size", Arrays.equals(responseBytes, getIcon(new File(localTestDir, "icons/toolicon8.png"))));

    }

    /**
     * This Test ensures we can get the default icon if we attempt to call the icon url for a non-existant feature
     */
    @Test
    public void testGetIconfromUninstalledFeature() throws Exception {
        startServerAndValidate(FATSuite.server);

        byte[] responseBytes = getImage(url + "/non.existant.feature-1.0", adminUser, adminPassword, 200);
        assertTrue("Image does not have the expected size", Arrays.equals(responseBytes, getDefaultIcon(142)));

        responseBytes = getImage(url + "/non.existant.feature-1.0?size=78", adminUser, adminPassword, 200);
        assertTrue("Image does not have the expected size", Arrays.equals(responseBytes, getDefaultIcon(78)));
    }

    /**
     * Returns a String of all of the file names in the given File array.
     * 
     * @param files Array of files to get the file name from.
     * @return A comma delimited String of file basenames.
     */
    private String getNames(File[] files) {
        StringBuilder sb = new StringBuilder();
        for (File file : files) {
            sb.append(file.getName());
            sb.append(", ");
        }
        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - 2);
        } else {
            return sb.toString();
        }
    }

    /**
     * This method loads the default Icon from the UI Bundle so that we can compare the bytes against the one
     * served from the IconServing Handler.
     * 
     * @param size - the size of the default icon to return.
     * @return - the byte array of the required icon.
     */
    private byte[] getDefaultIcon(int size) throws Exception {
        JarFile jarFile = null;
        byte[] iconBytes = new byte[] {};
        try {
            // Need to find the core UI bundle, but need to find the right one.
            // This is looking for all versions of the jar.
            final String MATCH_NAME = "com.ibm.ws.ui_";
            File libDir = new File(FATSuite.server.getInstallRoot(), "lib");
            File[] uiJars = libDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (name.startsWith(MATCH_NAME)) {
                        return true;
                    }
                    return false;
                }
            });
            assertNotNull("The list of UI JARs was null", uiJars);
            assertFalse("The list of UI JARs was empty", uiJars.length == 0);
            // Pick the latest jar version so that in the case of multiple jars (like for iFixes), we run the tests against the newest one
            File newestUiJarFile = null;
            Version newestVersion = null;
            for (File uiJar : uiJars) {
                JarFile uiJarFile = new JarFile(uiJar);
                Manifest manifest = uiJarFile.getManifest();
                uiJarFile.close();
                if (manifest != null) {
                    Attributes attributes = manifest.getMainAttributes();
                    Version version = Version.parseVersion(attributes.getValue("Bundle-Version"));
                    if (newestVersion == null || version.compareTo(newestVersion) > 0) {
                        newestVersion = version;
                        newestUiJarFile = uiJar;
                    }
                } else {
                    Log.info(c, "getDefaultIcon", "Could not find manifest for jar '" + uiJar.getName() + "' so it will not be used for test.");
                }
            }
            if (newestUiJarFile != null) {
                jarFile = new JarFile(newestUiJarFile);
            } else {
                Log.warning(c, "No UI JARs with Bundle-Versions were found.  Choosing the first bundle from the list.");
                jarFile = new JarFile(uiJars[0]);
            }
            Log.info(c, "getDefaultIcon", "Found " + uiJars.length + " JARs matching '" + MATCH_NAME + "'. Matches are: " + getNames(uiJars) + ".  Using JAR " + jarFile.getName());
            ZipEntry iconFile = jarFile.getEntry("images/tools/defaultTool_" + size + "x" + size + ".png");
            iconBytes = getIconBytes(jarFile.getInputStream(iconFile));
            Log.info(c, "getDefaultIcon", "icon size is " + iconBytes.length);
            Log.info(c, "getDefaultIcon", "icon content is " + iconBytes);
        } finally {
            if (jarFile != null)
                jarFile.close();
        }
        return iconBytes;
    }

    private byte[] getIcon(File iconFile) throws Exception {
        byte[] iconBytes = getIconBytes(new FileInputStream(iconFile));
        return iconBytes;
    }

    private byte[] getIconBytes(InputStream iconIS) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            // Read the inputstream into the outputStream.
            byte[] bytes = new byte[4096];
            int bytesRead = 0;
            while ((bytesRead = iconIS.read(bytes)) >= 0) {
                bos.write(bytes, 0, bytesRead);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (iconIS != null)
                iconIS.close();

            bos.flush();
            bos.close();
        }
        return bos.toByteArray();
    }

    private static class FeatureInfo {
        public String featureName = null;
        public String featureSymbolicName = null;

        public FeatureInfo(String featureName, String featureSymbolicName) {
            this.featureName = featureName;
            this.featureSymbolicName = featureSymbolicName;
        }
    }
}
