/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
import java.io.FileNotFoundException;
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
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;

import componenttest.common.apiservices.Bootstrap;
import junit.framework.Assert;

public class RepositoryUtil {
    public static final String INSTALL_PATH_PROPERTY_NAME = "libertyInstallPath";

    private static String IMAGE_PATH;
    private static File IMAGE_DIR;

    private static String BOOTSTRAP_LIB_PATH;
    private static File BOOTSTRAP_LIB_DIR;

    public static String getImagePath() {
        requireImageLocations();
        return IMAGE_PATH;
    }

    public static File getImageDir() {
        requireImageLocations();
        return IMAGE_DIR;
    }

    public static String getImageLibPath() {
        requireImageLocations();
        return BOOTSTRAP_LIB_PATH;
    }

    public static File getImageLibDir() {
        requireImageLocations();
        return BOOTSTRAP_LIB_DIR;
    }

    //

    private static boolean didSetupImageLocations;
    private static boolean didSetupImageFailure;

    private static void requireImageLocations() {
        if (!didSetupImageLocations) {
            if (didSetupImageFailure) {
                throw new IllegalStateException("Prior RepositoryUtil.setupImageLocations failure");
            } else {
                throw new IllegalStateException("RepositoryUtil.setupImageLocations has not been run.");
            }
        }
    }

    public static void setupImageLocations() throws Exception {
        if (didSetupImageFailure) {
            throw new Exception("Prior image locations failure");
        } else if (didSetupImageLocations) {
            return;
        } else {
            try {
                basicSetupImageLocations();
                didSetupImageLocations = true;
            } catch (Exception e) {
                didSetupImageFailure = true;
                throw e;
            }
        }
    }

    protected static void basicSetupImageLocations() throws Exception {
        Bootstrap bootstrap = Bootstrap.getInstance(); // throws Exception

        IMAGE_PATH = bootstrap.getValue(INSTALL_PATH_PROPERTY_NAME);
        IMAGE_DIR = new File(IMAGE_PATH);

        BOOTSTRAP_LIB_PATH = IMAGE_PATH + "/lib";
        BOOTSTRAP_LIB_DIR = new File(BOOTSTRAP_LIB_PATH);
    }

    //

    public static final String FEATURES_PATH = "./lib/features";
    public static final String ALT_FEATURES_PATH = "../com.ibm.websphere.appserver.features/visibility";

    private static String FEATURES_ABS_PATH;
    private static File FEATURES_FILE;

    public static String getFeaturesPath() {
        requireFeatureLocations();
        return FEATURES_ABS_PATH;
    }

    public static File getFeaturesFile() {
        requireFeatureLocations();
        return FEATURES_FILE;
    }

    //

    private static boolean didSetupFeatureLocations;
    private static boolean didSetupFeatureLocationsFailure;

    private static void requireFeatureLocations() {
        if (!didSetupFeatureLocations) {
            if (didSetupFeatureLocationsFailure) {
                throw new IllegalStateException("Prior RepositoryUtil.setupFeatureLocations failure");
            } else {
                throw new IllegalStateException("RepositoryUtil.setupFeatureLocations has not been run.");
            }
        }
    }

    public static void setupFeatureLocations() throws Exception {
        if (didSetupFeatureLocationsFailure) {
            throw new Exception("Prior feature locations failure");
        } else if (didSetupFeatureLocations) {
            return;
        } else {
            try {
                basicSetupFeatureLocations();
                didSetupFeatureLocations = true;
            } catch (Exception e) {
                didSetupFeatureLocationsFailure = true;
                throw e;
            }
        }
    }

    protected static void basicSetupFeatureLocations() throws Exception {
        File featuresFile = new File(FEATURES_PATH);
        String absPath = featuresFile.getCanonicalPath();
        if (!featuresFile.exists()) {
            System.out.println("Features file [ " + absPath + " ] does not exist; trying alternate");

            featuresFile = new File(ALT_FEATURES_PATH);
            absPath = featuresFile.getCanonicalPath();

            if (!featuresFile.exists()) {
                System.out.println("Alternate features file [ " + absPath + " ] does not exist");
                throw new FileNotFoundException("Neither [ " + FEATURES_PATH + " ] nor [ " + ALT_FEATURES_PATH + " ] exists");
            }
        }

        FEATURES_ABS_PATH = absPath;
        FEATURES_FILE = new File(FEATURES_ABS_PATH);
    }

    //

    public static final String PROFILES_PROJECT_PATH = "..";
    public static final String PROFILES_PATH = PROFILES_PROJECT_PATH + "/" + "profiles";

    private static String PROFILES_ABS_PATH;
    private static File PROFILES_FILE;

    public static String getProfilesPath() {
        requireProfileLocations();
        return PROFILES_ABS_PATH;
    }

    public static File getProfilesFile() {
        requireProfileLocations();
        return PROFILES_FILE;
    }

    //

    private static boolean didSetupProfileLocations;
    private static boolean didSetupProfileLocationsFailure;

    private static void requireProfileLocations() {
        if (!didSetupProfileLocations) {
            if (didSetupProfileLocationsFailure) {
                throw new IllegalStateException("Prior RepositoryUtil.setupProfileLocations failure");
            } else {
                throw new IllegalStateException("RepositoryUtil.setupProfileLocations has not been run.");
            }
        }
    }

    public static void setupProfileLocations() throws Exception {
        if (didSetupProfileLocationsFailure) {
            throw new Exception("Prior profile locations failure");
        } else if (didSetupProfileLocations) {
            return;
        } else {
            try {
                basicSetupProfileLocations();
                didSetupProfileLocations = true;
            } catch (Exception e) {
                didSetupProfileLocationsFailure = true;
                throw e;
            }
        }
    }

    protected static void basicSetupProfileLocations() throws Exception {
        setupImageLocations();

        File profilesFile = new File(getImageDir(), PROFILES_PATH);
        PROFILES_ABS_PATH = profilesFile.getCanonicalPath();
        PROFILES_FILE = new File(PROFILES_ABS_PATH);
    }

    //

    public static FeatureResolver.Repository repository;
    public static FeatureRepositorySupplier featureSupplier;

    public static FeatureResolver.Repository getRepository() {
        requireRepo();
        return repository;
    }

    public static ProvisioningFeatureDefinition getFeatureDef(String featureName) {
        return getRepository().getFeature(featureName);
    }

    public static List<ProvisioningFeatureDefinition> getFeatureDefs() {
        return getRepository().getFeatures();
    }

    public static FeatureRepositorySupplier getSupplier() {
        requireRepo();
        return featureSupplier;
    }

    //

    private static boolean didSetupRepo;
    private static boolean didSetupRepoFailure;

    private static void requireRepo() {
        if (!didSetupRepo) {
            if (didSetupRepoFailure) {
                throw new IllegalStateException("Prior RepositoryUtil.setupRepo failure");
            } else {
                throw new IllegalStateException("RepositoryUtil.setupRepo has not been run.");
            }
        }
    }

    public static void setupRepo(String serverName) throws Exception {
        if (didSetupRepoFailure) {
            throw new Exception("Prior setup repo failure");
        } else if (didSetupRepo) {
            return;
        } else {
            try {
                basicSetupRepo(serverName);
                didSetupRepo = true;
            } catch (Exception e) {
                didSetupRepoFailure = true;
                throw e;
            }
        }
    }

    protected static void basicSetupRepo(String serverName) throws Exception {
        setupImageLocations();

        File useImageDir = getImageDir();
        File useLibDir = getImageLibDir();
        System.out.println("Image   [ " + useImageDir.getCanonicalPath() + " ]");
        System.out.println("BootLib [ " + useLibDir.getCanonicalPath() + " ]");
        System.out.println("Server  [ " + serverName + " ]");

        Utils.setInstallDir(useImageDir);
        KernelUtils.setBootStrapLibDir(useLibDir);
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

        featureSupplier = new FeatureRepositorySupplier(repository);
    }

    //

    public static List<FeatureInfo> features;
    public static Map<String, FeatureInfo> featuresMap;

    public static List<FeatureInfo> getFeaturesList() {
        requireFeatures();
        return features;
    }

    public static Map<String, FeatureInfo> getFeaturesMap() {
        requireFeatures();
        return featuresMap;
    }

    public static FeatureInfo getFeatureInfo(String name) {
        requireFeatures();
        return featuresMap.get(name);
    }

    //

    private static boolean didSetupFeatures;
    private static boolean didSetupFeaturesFailure;

    private static void requireFeatures() {
        if (!didSetupFeatures) {
            if (didSetupFeaturesFailure) {
                throw new IllegalStateException("Prior RepositoryUtil.setupFeatures failure");
            } else {
                throw new IllegalStateException("RepositoryUtil.setupFeatures has not been run.");
            }
        }
    }

    public static void setupFeatures() throws Exception {
        if (didSetupFeaturesFailure) {
            throw new Exception("Prior features failure");
        } else if (didSetupFeatures) {
            return;
        } else {
            try {
                basicSetupFeatures();
                didSetupFeatures = true;
            } catch (Exception e) {
                didSetupFeaturesFailure = true;
                throw e;
            }
        }
    }

    protected static void basicSetupFeatures() throws Exception {
        setupFeatureLocations();

        File rootFeaturesInputFile = getFeaturesFile();
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

    //

    public static Images images;

    public static Images getImages() {
        requireProfiles();
        return images;
    }

    //

    private static boolean didSetupProfiles;
    private static boolean didSetupProfilesFailure;

    private static void requireProfiles() {
        if (!didSetupProfiles) {
            if (didSetupProfilesFailure) {
                throw new IllegalStateException("Prior RepositoryUtil.setupProfiles failure");
            } else {
                throw new IllegalStateException("RepositoryUtil.setupProfiles has not been run.");
            }
        }
    }

    public static void setupProfiles() throws Exception {
        return;
    }

    public static void disabled_setupProfiles() throws Exception {
        if (didSetupProfilesFailure) {
            throw new Exception("Prior profiles failure");
        } else if (didSetupProfiles) {
            return;
        } else {
            try {
                basicSetupProfiles();
                didSetupProfiles = true;
            } catch (Exception e) {
                didSetupProfilesFailure = true;
                throw e;
            }
        }
    }

    protected static void basicSetupProfiles() throws Exception {
        setupProfileLocations();

        File rootInputFile = getProfilesFile();
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

    //

    public static Map<String, ProvisioningFeatureDefinition> versionlessFeatureDefs;

    /**
     * If not already done, it first saves a map of versionless feature names to feature definitions.
     * Then it looks up the feature def using the given feature name.
     *
     * @param featureName Name of a versionless feature. This is NOT simply a versioned feature name
     *            minus the version. The "package" name should be "io.openliberty.versionless."
     *            or "com.ibm.websphere.appserver.versionless."
     *            <br>Example: io.openliberty.versionless.appClientSupport
     * @return feature definition of the versionless feature associated with the input featureName
     */
    public static ProvisioningFeatureDefinition getVersionlessFeatureDef(String featureName) {
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
        String shortName = asShortName(featureName);
        // wmqJmsClient and wmqMessagingClient versionless features are WebSphere Liberty versionless features and have a different prefix
        return (shortName.startsWith("wmq") ? "com.ibm.websphere.appserver.versionless." : "io.openliberty.versionless.") + shortName;
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
        String shortNameWithVersion = asShortNameWithVersion(featureName);
        // wmqJmsClient and wmqMessagingClient versionless features are WebSphere Liberty versionless features and have a different prefix
        return (shortNameWithVersion.startsWith("wmq") ? "com.ibm.ws.internal.versionless." : "io.openliberty.internal.versionless.")
               + shortNameWithVersion;
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
     * Answer the platform of a feature.
     *
     * Answer null if the feature cannot be found, or if the
     * feature is not public.
     *
     * @param symName The symbolic name of a feature.
     *
     * @return The platform of the feature. Null if the
     *         feature is not found, or is not public.
     */
    public static String getPlatformOf(String symName) {
        return getPlatformOf(getFeatureDef(symName));
    }

    public static String getPlatformOf(ProvisioningFeatureDefinition featureDef) {
        return ((featureDef.getVisibility() == Visibility.PUBLIC) ? featureDef.getPlatformName() : null);
    }

    public static final String NO_SHIP_10 = "io.openliberty.noShip-1.0";

    /**
     * Tell if feature is no-ship.
     *
     * No ship features have "io.openliberty.noShip-1.0" as a constituent feature.
     *
     * @param featureDef A feature definition
     *
     * @return True or false telling if the feature is no-ship.
     */
    public static boolean isNoShip(ProvisioningFeatureDefinition featureDef) {
        for (FeatureResource fr : featureDef.getConstituents(SubsystemContentType.FEATURE_TYPE)) {
            String subSymName = fr.getSymbolicName();
            if ((subSymName != null) && subSymName.equals(NO_SHIP_10)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tell if a combination of symbolic name and platform are to be
     * skipped based on JDBC exceptions.
     *
     * The exception occurs because of the version priorities in the JDBC feature dependency,
     * and because of the JDBC 4.2 platform setting.
     *
     * From "open-liberty/dev/build.image/wlp/lib/features/com.ibm.websphere.appserver.jdbc-4.3.mf":
     *
     * <code>
     * Subsystem-Content: ...
     * io.openliberty.jdbc4.3.internal.ee-6.0; ibm.tolerates:="9.0"; type="osgi.subsystem.feature"
     * WLP-Platform: jakartaee-11.0
     * </code>
     *
     * This combination causes the resolution of "jdbc-4.3" as a singleton versioned feature to
     * change when modified to "jdbc" (versionless) with assigned platform "jakartaee-11.0".
     *
     * @param symName A feature symbolic name.
     * @param platform A platform.
     *
     * @return True or false telling if the symbolic name and platform should be skipped
     *         by versionless singleton tests because of the JDBC 4.3 exception.
     */
    public static boolean isJDBCVersionlessException(String symName, String platform) {
        return ((symName.equals("com.ibm.websphere.appserver.jdbc-4.3") && platform.equals("jakartaee-11.0")) ||
                (symName.equals("io.openliberty.persistence-3.2") && platform.equals("jakartaee-11.0")));
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
        return (featureName.startsWith("io.openliberty.versionless") || featureName.startsWith("com.ibm.websphere.appserver.versionless"));
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

    //

    //@formatter:off
    private static final String[][] FEATURE_RENAMES = {
        { "io.openliberty.appAuthentication-2.0", "jaspic-2.0" },
        { "io.openliberty.appAuthentication-3.0", "jaspic-3.0" },
        { "io.openliberty.appAuthentication-3.1", "jaspic-3.1" },
        { "io.openliberty.appAuthorization-2.0", "jacc-2.0" },
        { "io.openliberty.appAuthorization-2.1", "jacc-2.1" },
        { "io.openliberty.appAuthorization-3.0", "jacc-3.0" },
        { "io.openliberty.beanValidation-3.1", "beanValidation-3.1" },
        { "io.openliberty.connectors-2.0", "jca-2.0" },
        { "io.openliberty.connectors-2.1", "jca-2.1" },
        { "io.openliberty.connectorsInboundSecurity-2.0", "jcaInboundSecurity-2.0" },
        { "io.openliberty.enterpriseBeans-4.0", "ejb-4.0" },
        { "io.openliberty.enterpriseBeansHome-4.0", "ejbHome-4.0" },
        { "io.openliberty.enterpriseBeansLite-4.0", "ejbLite-4.0" },
        { "io.openliberty.enterpriseBeansPersistentTimer-4.0", "ejbPersistentTimer-4.0" },
        { "io.openliberty.enterpriseBeansRemote-4.0", "ejbRemote-4.0" },
        { "io.openliberty.expressionLanguage-4.0", "el-4.0" },
        { "io.openliberty.expressionLanguage-5.0", "el-5.0" },
        { "io.openliberty.expressionLanguage-6.0", "el-6.0" },
        { "io.openliberty.faces-3.0", "jsf-3.0" },
        { "io.openliberty.faces-4.0", "jsf-4.0" },
        { "io.openliberty.faces-4.1", "jsf-4.1" },
        { "io.openliberty.facesContainer-3.0", "jsfContainer-3.0" },
        { "io.openliberty.facesContainer-4.0", "jsfContainer-4.0" },
        { "io.openliberty.facesContainer-4.1", "jsfContainer-4.1" },
        { "io.openliberty.jakartaee-9.1", "jakartaee-9.0" },
        { "io.openliberty.jakartaeeClient-9.1", "jakartaeeClient-9.0" },
        { "io.openliberty.mail-2.0", "javaMail-2.0" },
        { "io.openliberty.mail-2.1", "javaMail-2.1" },
        { "io.openliberty.messaging-3.0", "jms-3.0" },
        { "io.openliberty.messaging-3.1", "jms-3.1" },
        { "io.openliberty.messagingClient-3.0", "wasJmsClient-3.0" },
        { "io.openliberty.messagingSecurity-3.0", "wasJmsSecurity-3.0" },
        { "io.openliberty.messagingServer-3.0", "wasJmsServer-3.0" },
        { "io.openliberty.pages-3.0", "jsp-3.0" },
        { "io.openliberty.pages-3.1", "jsp-3.1" },
        { "io.openliberty.pages-4.0", "jsp-4.0" },
        { "io.openliberty.persistence-3.0", "jpa-3.0" },
        { "io.openliberty.persistence-3.1", "jpa-3.1" },
        { "io.openliberty.persistence-3.2", "jpa-3.2" },
        { "io.openliberty.persistenceContainer-3.0", "jpaContainer-3.0" },
        { "io.openliberty.persistenceContainer-3.1", "jpaContainer-3.1" },
        { "io.openliberty.persistenceContainer-3.2", "jpaContainer-3.2" },
        { "io.openliberty.restfulWS-3.0", "jaxrs-3.0" },
        { "io.openliberty.restfulWS-3.1", "jaxrs-3.1" },
        { "io.openliberty.restfulWS-4.0", "jaxrs-4.0" },
        { "io.openliberty.restfulWSClient-3.0", "jaxrsClient-3.0" },
        { "io.openliberty.restfulWSClient-3.1", "jaxrsClient-3.1" },
        { "io.openliberty.restfulWSClient-4.0", "jaxrsClient-4.0" },
        { "io.openliberty.webProfile-9.1", "webProfile-9.0" },
        { "io.openliberty.xmlBinding-3.0", "jaxb-3.0" },
        { "io.openliberty.xmlBinding-4.0", "jaxb-4.0" },
        { "io.openliberty.xmlWS-3.0", "jaxws-3.0" },
        { "io.openliberty.xmlWS-4.0", "jaxws-4.0" }
    };
    //@formatter:on

    private static final Map<String, String> featureRenames;

    static {
        Map<String, String> renames = new HashMap<>(FEATURE_RENAMES.length);
        for (String[] names : FEATURE_RENAMES) {
            renames.put(names[0], names[1]);
        }
        featureRenames = renames;
    }

    public static String getRename(String featureName) {
        return featureRenames.get(featureName);
    }
}
