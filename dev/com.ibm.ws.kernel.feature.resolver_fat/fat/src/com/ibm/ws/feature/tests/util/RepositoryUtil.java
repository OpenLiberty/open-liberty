/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.feature.tests.util;

import static com.ibm.ws.feature.tests.util.FeatureUtil.isPublic;
import static com.ibm.ws.feature.tests.util.FeatureUtil.isTest;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.KernelUtils;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureRepository;
import com.ibm.ws.kernel.feature.internal.util.ImageReader;
import com.ibm.ws.kernel.feature.internal.util.Images;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;

import componenttest.common.apiservices.Bootstrap;
import junit.framework.Assert;

public class RepositoryUtil {

    private static final String CLASS_NAME = "RepositoryUtil ";

    public static final String INSTALL_PATH_PROPERTY_NAME = "libertyInstallPath";

    public static String IMAGE_PATH;
    public static File IMAGE_DIR;

    public static String BOOTSTRAP_LIB_PATH;
    public static File BOOTSTRAP_LIB_DIR;

    private static boolean didSetupImageLocations;

    public static void setupImageLocations() throws Exception {
        if (didSetupImageLocations) {
            return;
        } else {
            didSetupImageLocations = true;
        }

        Bootstrap bootstrap = Bootstrap.getInstance(); // throws Exception

        IMAGE_PATH = bootstrap.getValue(INSTALL_PATH_PROPERTY_NAME);
        IMAGE_DIR = new File(IMAGE_PATH);

        BOOTSTRAP_LIB_PATH = IMAGE_PATH + "/lib";
        BOOTSTRAP_LIB_DIR = new File(BOOTSTRAP_LIB_PATH);
    }

    //

    public static final String FEATURES_PROJECT_PATH = "../../com.ibm.websphere.appserver.features";
    public static final String FEATURES_PATH = FEATURES_PROJECT_PATH + "/" + "visibility";

    public static String FEATURES_ABS_PATH;
    public static File FEATURES_FILE;

    private static boolean didSetupFeatureLocations;

    public static void setupFeatureLocations() throws Exception {
        if (didSetupFeatureLocations) {
            return;
        } else {
            didSetupFeatureLocations = true;
        }

        setupImageLocations();

        File featuresFile = new File(IMAGE_DIR, FEATURES_PATH);
        FEATURES_ABS_PATH = featuresFile.getCanonicalPath();
        FEATURES_FILE = new File(FEATURES_ABS_PATH);
    }

    //

    public static final String PROFILES_PROJECT_PATH = "..";
    public static final String PROFILES_PATH = PROFILES_PROJECT_PATH + "/" + "profiles";

    public static String PROFILES_ABS_PATH;
    public static File PROFILES_FILE;

    private static boolean didSetupProfileLocations;

    public static void setupProfileLocations() throws Exception {
        if (didSetupProfileLocations) {
            return;
        } else {
            didSetupProfileLocations = true;
        }

        setupImageLocations();

        File profilesFile = new File(IMAGE_DIR, PROFILES_PATH);
        PROFILES_ABS_PATH = profilesFile.getCanonicalPath();
        PROFILES_FILE = new File(PROFILES_ABS_PATH);
    }

    //

    private static boolean didSetupRepo;

    public static void setupRepo(String serverName) throws Exception {
        if (didSetupRepo) {
            return;
        } else {
            didSetupRepo = true;
        }

        setupImageLocations();

        System.out.println("Image   [ " + IMAGE_DIR.getCanonicalPath() + " ]");
        System.out.println("BootLib [ " + BOOTSTRAP_LIB_DIR.getCanonicalPath() + " ]");
        System.out.println("Server  [ " + serverName + " ]");

        Utils.setInstallDir(IMAGE_DIR);
        KernelUtils.setBootStrapLibDir(BOOTSTRAP_LIB_DIR);
        BundleRepositoryRegistry.initializeDefaults(serverName, true);

        FeatureRepository repoImpl = new FeatureRepository();
        repoImpl.init();
        System.out.println("Features [ " + repoImpl.getFeatures().size() + " ]");

        repository = new FeatureResolver.Repository() {
            private final FeatureRepository baseRepo = repoImpl;

            @Override
            public List<ProvisioningFeatureDefinition> getFeatures() {
                return baseRepo.getFeatures();
            }

            @Override
            public ProvisioningFeatureDefinition getFeature(String featureName) {
                return baseRepo.getFeature(featureName);
            }

            @Override
            public List<String> getConfiguredTolerates(String baseSymbolicName) {
                return baseRepo.getConfiguredTolerates(baseSymbolicName);
            }

            @Override
            public Collection<ProvisioningFeatureDefinition> getAutoFeatures() {
                return baseRepo.getAutoFeatures();
            }
        };
    }

    //

    private static boolean didSetupFeatures;

    public static void setupFeatures() throws Exception {
        if (didSetupFeatures) {
            return;
        } else {
            didSetupFeatures = true;
        }

        setupFeatureLocations();

        File rootFeaturesInputFile = FEATURES_FILE;
        String rootInputPath = rootFeaturesInputFile.getCanonicalPath();
        if (!rootFeaturesInputFile.exists()) {
            Assert.fail("Features input [ " + rootInputPath + " ] does not exist");
        } else {
            System.out.println("Features input [ " + rootInputPath + " ]");
        }

        List<File> featureFiles = FeatureReader.selectFeatureFiles(rootFeaturesInputFile);
        System.out.println("Selected [ " + featureFiles.size() + " ] feature files");

        features = FeatureReader.readFeatures(featureFiles);
        System.out.println("Read [ " + features.size() + " ] features from [ " + rootInputPath + " ]");

        Map<String, FeatureInfo> fMap = new HashMap<>();
        for (FeatureInfo fInfo : features) {
            fMap.put(fInfo.getName(), fInfo);
        }
        featuresMap = fMap;
    }

    public static List<FeatureInfo> features;
    public static Map<String, FeatureInfo> featuresMap;

    public static List<FeatureInfo> getFeaturesList() {
        return features;
    }

    public static Map<String, FeatureInfo> getFeaturesMap() {
        return featuresMap;
    }

    public static FeatureInfo getFeatureInfo(String name) {
        return featuresMap.get(name);
    }

    //

    private static boolean didSetupProfiles;

    public static void setupProfiles() throws Exception {
        if (didSetupProfiles) {
            return;
        } else {
            didSetupProfiles = true;
        }

        setupProfileLocations();

        File rootInputFile = PROFILES_FILE;
        String rootInputPath = rootInputFile.getCanonicalPath();
        if (!rootInputFile.exists()) {
            Assert.fail("Images input [ " + rootInputPath + " ] does not exist");
        } else {
            System.out.println("Images input [ " + rootInputPath + " ]");
        }

        List<File[]> imageDirs = ImageReader.selectImageDirs(rootInputFile);
        System.out.println("Selected [ " + imageDirs.size() + " ] feature files");

        images = ImageReader.readImages(imageDirs);
        System.out.println("Read [ " + images.getImages().size() + " ] images from [ " + rootInputPath + " ]");
    }

    public static Images images;

    public static Images getImages() {
        return images;
    }

    //

    public static FeatureResolver.Repository repository;
    public static Map<String, ProvisioningFeatureDefinition> versionlessFeatureDefs;

    public static FeatureResolver.Repository getRepository() {
        return repository;
    }

    public static ProvisioningFeatureDefinition getFeatureDef(String featureName) {
        return getRepository().getFeature(featureName);
    }

    /**
     * If not already done, it first saves a map of versionless feature names to feature definitions.
     * Then it looks up the feature def using the given feature name.
     *
     * @param featureName Name of a versionless feature. This is NOT simply a versioned feature name
     *            minus the version. The "package" name should be "io.openliberty.versionless."
     *            <br>Example: io.openliberty.versionless.appClientSupport
     * @return feature definition of the versionless feature associated with the input featureName
     */
    public static ProvisioningFeatureDefinition getVersionlessFeatureDef(String featureName) {
        final String METHOD_NAME = "getVersionlessFeatureDef ";
        if (versionlessFeatureDefs == null) {
            intializeVersionlessFeatureDefsMap();
        }

        return versionlessFeatureDefs.get(asVersionlessFeatureName(featureName));
    }

    /**
     * Initialize the map of versionless feature names to their feature definitions
     */
    public static void intializeVersionlessFeatureDefsMap() {

        Map<String, ProvisioningFeatureDefinition> featureDefs = new HashMap<>();
        for (ProvisioningFeatureDefinition featureDef : getRepository().getFeatures()) {
            if (featureDef.isVersionless()) {
                featureDefs.put(asVersionlessFeatureName(featureDef.getSymbolicName()), featureDef);
            }
        }
        versionlessFeatureDefs = featureDefs;
    }

    /**
     * Debug method for displaying the names of versionless features from the repository
     */
    public static void displayVersionlessFeatures() {
        if (versionlessFeatureDefs == null) {
            intializeVersionlessFeatureDefsMap();
        }
        System.out.println("\nDisplaying versionless feature names:\n[");
        for (String fn : versionlessFeatureDefs.keySet()) {
            System.out.println(fn);
        }
        System.out.println("]");
    }

    /**
     * Returns a versionless feature name for a given versioned feature name.
     * <br>Example:
     * <ul>
     * <li>input: com.ibm.websphere.appserver.appClientSupport-1.0
     * <li>returns: io.openliberty.versionless.appClientSupport
     * </ul>
     *
     * @param featureName the symbolic feature name
     */
    public static String asVersionlessFeatureName(String featureName) {

        return "io.openliberty.versionless." + asShortName(featureName);
    }

    /**
     * Returns the internal versionless "linking" feature name for a given versioned feature name.
     * <br>Example:
     * <ul>
     * <li>input: com.ibm.websphere.appserver.appClientSupport-1.0
     * <li>returns: io.openliberty.internal.versionless.appClientSupport-1.0
     * </ul>
     *
     * @param featureName the symbolic feature name
     */
    public static String asInternalVersionlessFeatureName(String featureName) {

        return "io.openliberty.internal.versionless." + asShortNameWithVersion(featureName);
    }

    /**
     * Returns just the short name without the version for a given versioned feature name.
     * <br>Example:
     * <ul>
     * <li>input: com.ibm.websphere.appserver.appClientSupport-1.0
     * <li>returns: appClientSupport
     * </ul>
     *
     * @param featureName the symbolic feature name
     */
    public static String asShortName(String featureName) {
        String nameWithoutVersion = removeVersion(featureName);

        int lastPeriodIndex = nameWithoutVersion.lastIndexOf(".");
        if (lastPeriodIndex == -1) {
            return nameWithoutVersion;
        }

        return nameWithoutVersion.substring(lastPeriodIndex + 1);
    }

    /**
     * Returns the short name including the version for a given versioned feature name.
     * <br>Example:
     * <ul>
     * <li>input: com.ibm.websphere.appserver.appClientSupport-1.0
     * <li>returns: appClientSupport-1.0
     * </ul>
     *
     * @param featureName the symbolic feature name
     */
    public static String asShortNameWithVersion(String featureName) {
        String shortName = asShortName(featureName);
        String version = asVersionOnly(featureName);

        return shortName + "-" + version;
    }

    /**
     * Returns just the version for a given versioned feature name
     * <br>Example:
     * <ul>
     * <li>input: com.ibm.websphere.appserver.appClientSupport-1.0
     * <li>returns: 1.0
     * </ul>
     *
     * @param featureName the symbolic feature name
     */
    public static String asVersionOnly(String featureName) {
        int lastDashIndex;
        if ((lastDashIndex = featureName.lastIndexOf('-')) >= 0) {
            return featureName.substring(lastDashIndex + 1);
        } else {
            return null;
        }
    }

    /**
     * Removes the version from the feature name. The result is not to be confused with
     * a versionless feature name which most likely has a different package name than the parameter
     * passed in.
     * <br>Example:
     * <ul>
     * <li>input: com.ibm.websphere.appserver.appClientSupport-1.0
     * <li>returns: com.ibm.websphere.appserver.appClientSupport
     * </ul>
     *
     * @param featureName symbolic name of feature
     */
    public static String removeVersion(String featureName) {
        int lastDashIndex;
        if ((lastDashIndex = featureName.lastIndexOf('-')) >= 0) {
            return featureName.substring(0, lastDashIndex);
        } else {
            return featureName;
        }
    }

    /**
     *
     * @param symName symbolic feature name
     * @return the platform name of the input feature
     */
    public static String getPlatformOf(String symName) {
        ProvisioningFeatureDefinition featureDef = getFeatureDef(symName);
        if (featureDef.getVisibility() != Visibility.PUBLIC) {
            return null;
        }

        return featureDef.getPlatformName();
    }
	
    public static List<ProvisioningFeatureDefinition> getFeatureDefs() {
        return getRepository().getFeatures();
    }

    // Use this to decide whether to run in WAS liberty mode or in open liberty
    // mode.
    //
    // WAS liberty has a different set of features.  This perturbs the
    // feature resolution.

    public static final String WAS_LIBERTY_FEATURE_NAME = "apiDiscovery-1.0";

    public static boolean isWASLiberty() {
        return (getFeatureDef(WAS_LIBERTY_FEATURE_NAME) != null);
    }

    private static List<String> publicFeatures;

    public static List<String> getPublicFeatures() {
        if (publicFeatures == null) {
            List<String> usePublicFeatures = new ArrayList<>();
            for (ProvisioningFeatureDefinition featureDef : getFeatureDefs()) {
                if (isPublic(featureDef)) {
                    String symbolicName = featureDef.getSymbolicName();
                    if (!isTest(symbolicName)) {
                        usePublicFeatures.add(symbolicName);
                    }
                }
            }
            publicFeatures = usePublicFeatures;
        }
        return publicFeatures;
    }

    public static boolean isVersionless(String featureName) {
        return (featureName.startsWith("io.openliberty.versionless"));
    }

    private static List<String> versionlessFeatures;

    public static List<String> getVersionlessFeatures() {
        if (versionlessFeatures == null) {
            List<String> useVersionlessFeatures = new ArrayList<>();
            for (String publicFeature : getPublicFeatures()) {
                if (isVersionless(publicFeature)) {
                    useVersionlessFeatures.add(publicFeature);
                }
            }
            versionlessFeatures = useVersionlessFeatures;
        }
        return versionlessFeatures;
    }

    public static boolean isServlet(String featureName) {
        return (featureName.startsWith("servlet-"));
    }

    private static List<String> servletFeatures;

    public static List<String> getServletFeatures() {
        if (servletFeatures == null) {
            List<String> useServletFeatures = new ArrayList<>();
            for (String publicFeature : getPublicFeatures()) {
                if (isServlet(publicFeature)) {
                    useServletFeatures.add(publicFeature);
                }
            }
            servletFeatures = useServletFeatures;
        }
        return servletFeatures;
    }

    /**
     * Note features which are being ignored.
     *
     * Answer an empty collection of feature definitions.
     *
     * @param description The description of the features.
     * @param featureNames The names of the features.
     *
     * @return An empty collection of feature definitions.
     */
    public static List<ProvisioningFeatureDefinition> ignoreFeatures(String description, List<String> featureNames) {
        System.out.println("Ignore [ " + description + " ] features:");
        for (String featureName : featureNames) {
            System.out.println("  " + featureName);
        }

        return Collections.emptyList();
    }

    public static List<ProvisioningFeatureDefinition> getFeatures(List<String> featureNames) {
        List<ProvisioningFeatureDefinition> featureDefs = new ArrayList<>(featureNames.size());
        for (String featureName : featureNames) {
            ProvisioningFeatureDefinition featureDef = getFeatureDef(featureName);
            if (featureDef == null) {
                throw new IllegalArgumentException("Feature not found [ " + featureName + " ]");
            }
            featureDefs.add(featureDef);
        }
        return featureDefs;
    }
}
