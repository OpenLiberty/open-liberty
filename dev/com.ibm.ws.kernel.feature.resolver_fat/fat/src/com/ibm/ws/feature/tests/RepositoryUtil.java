/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.feature.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.KernelUtils;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureRepository;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Selector;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;

import componenttest.common.apiservices.Bootstrap;

public class RepositoryUtil {
    public static final String INSTALL_PATH_PROPERTY_NAME = "libertyInstallPath";

    public static String IMAGE_PATH;
    public static String BOOTSTRAP_LIB_PATH;

    public static File IMAGE_DIR;
    public static File BOOTSTRAP_LIB_DIR;

    public static File getImageDir() {
        return IMAGE_DIR;
    }

    public static File getBootstrapLibDir() {
        return BOOTSTRAP_LIB_DIR;
    }

    public static void setupLocations() throws Exception {
        Bootstrap bootstrap = Bootstrap.getInstance(); // throws Exception

        IMAGE_PATH = bootstrap.getValue(INSTALL_PATH_PROPERTY_NAME);
        BOOTSTRAP_LIB_PATH = IMAGE_PATH + "/lib";

        IMAGE_DIR = new File(IMAGE_PATH);
        BOOTSTRAP_LIB_DIR = new File(BOOTSTRAP_LIB_PATH);
    }

    //

    public static void setupRepo(String serverName) throws Exception {
        System.out.println("Image   [ " + IMAGE_DIR.getAbsolutePath() + " ]");
        System.out.println("BootLib [ " + BOOTSTRAP_LIB_DIR.getAbsolutePath() + " ]");
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
            public List<ProvisioningFeatureDefinition> select(Selector<ProvisioningFeatureDefinition> selector) {
                return baseRepo.select(selector);
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

    public static boolean isPublic(ProvisioningFeatureDefinition featureDef) {
        return (featureDef.getVisibility() == Visibility.PUBLIC);
    }

    public static FeatureResolver.Repository repository;

    public static FeatureResolver.Repository getRepository() {
        return repository;
    }

    public static ProvisioningFeatureDefinition getFeature(String featureName) {
        return getRepository().getFeature(featureName);
    }

    public static List<ProvisioningFeatureDefinition> getFeatures() {
        return getRepository().getFeatures();
    }

    // Use this to decide whether to run in WAS liberty mode or in open liberty
    // mode.
    //
    // WAS liberty has a different set of features.  This perturbs the
    // feature resolution.

    public static final String WAS_LIBERTY_FEATURE_NAME = "apiDiscovery-1.0";

    public static boolean isWASLiberty() {
        return (getFeature(WAS_LIBERTY_FEATURE_NAME) != null);
    }

    private static List<String> publicFeatures;

    public static List<String> getPublicFeatures() {
        if (publicFeatures == null) {
            List<String> features = new ArrayList<>();
            for (ProvisioningFeatureDefinition featureDef : getFeatures()) {
                if (isPublic(featureDef)) {
                    String symbolicName = featureDef.getSymbolicName();
                    if (!isTest(symbolicName)) {
                        features.add(symbolicName);
                    }
                }
            }
            publicFeatures = features;
        }
        return publicFeatures;
    }

    public static boolean isVersionless(String featureName) {
        return (featureName.startsWith("io.openliberty.versionless"));
    }

    private static List<String> versionlessFeatures;

    public static List<String> getVersionlessFeatures() {
        if (versionlessFeatures == null) {
            List<String> features = new ArrayList<>();
            for (String publicFeature : getPublicFeatures()) {
                if (isVersionless(publicFeature)) {
                    features.add(publicFeature);
                }
            }
            versionlessFeatures = features;
        }
        return versionlessFeatures;
    }

    public static boolean isServlet(String featureName) {
        return (featureName.startsWith("servlet-"));
    }

    private static List<String> servletFeatures;

    public static List<String> getServletFeatures() {
        if (servletFeatures == null) {
            List<String> features = new ArrayList<>();
            for (String publicFeature : getPublicFeatures()) {
                if (isServlet(publicFeature)) {
                    features.add(publicFeature);
                }
            }
            servletFeatures = features;
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
            ProvisioningFeatureDefinition featureDef = getFeature(featureName);
            if (featureDef == null) {
                throw new IllegalArgumentException("Feature not found [ " + featureName + " ]");
            }
            featureDefs.add(featureDef);
        }
        return featureDefs;
    }

    /**
     * Tests are placing several test related features in the server features
     * repository. Ignore these for now:
     *
     * <ul>
     * <li>test.InterimFixManagerTest-1.0.mf</li>
     * <li>test.InterimFixesManagerTest-1.0.mf</li>
     * <li>test.TestFixManagerTest-1.0.mf</li>
     * <li>test.featurefixmanager-1.0.mf</li>
     * <li>txtest-1.0.mf</li>
     * <li>txtest-2.0.mf</li>
     * </ul>
     *
     * @param featureName A feature name.
     *
     * @return True or false telling if the named feature is a test feature.
     */
    public static boolean isTest(String featureName) {
        return featureName.startsWith("test.") || featureName.startsWith("txtest-");
    }
}
