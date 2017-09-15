/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.generator;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.feature.internal.cmdline.FeatureToolException;
import com.ibm.ws.kernel.feature.internal.cmdline.NLS;
import com.ibm.ws.kernel.feature.internal.cmdline.ReturnCode;
import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry.BundleRepositoryHolder;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.ws.kernel.provisioning.ExtensionConstants;
import com.ibm.ws.kernel.provisioning.ProductExtension;
import com.ibm.ws.kernel.provisioning.ProductExtensionInfo;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 *
 */
public class ManifestFileProcessor {

    protected static final String FEATURE_DIR = "lib/features";
    protected static final String PLATFORM_DIR = "lib/platform";
    protected static final String ASSET_DIR = "lib/assets";

    protected static final String USER_FEATURE_DIR = "extension/lib/features";

    protected HashMap<String, ProductExtensionInfo> productExtNameInfoMap;

    public static final String MF_EXTENSION = ".mf";
    public static final String CORE_PRODUCT_NAME = "core";

    public static final String USR_PRODUCT_EXT_NAME = ExtensionConstants.USER_EXTENSION;

    private static FileFilter MFFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            if (file != null && file.isFile()) {
                String name = file.getName();

                int pos = name.lastIndexOf('.');
                if (pos < 0)
                    return false; // NEXT!

                // Look only at the file extension, case insensitively
                if (name.regionMatches(true, pos, MF_EXTENSION, 0, 3))
                    return true;
            }
            return false;
        }
    };

    /**
     * Retrieves a map of features definitions associated with the specified product name.
     * 
     * @param productName The product name whose feature definitions to return.
     * @return The feature definitions associated with the input product name.
     */
    public Map<String, ProvisioningFeatureDefinition> getFeatureDefinitions(String productName) {
        if (productName.equals(CORE_PRODUCT_NAME)) {
            return getCoreProductFeatureDefinitions();
        } else if (productName.equals(USR_PRODUCT_EXT_NAME)) {
            return getUsrProductFeatureDefinitions();
        } else {
            return getProductExtFeatureDefinitions(productName);
        }
    }

    // This API for install used only.
    public Map<String, ProvisioningFeatureDefinition> getCoreFeatureDefinitionsExceptPlatform() {
        Map<String, ProvisioningFeatureDefinition> features = new TreeMap<String, ProvisioningFeatureDefinition>();
        File featureDir = getCoreFeatureDir();
        //the feature directory may not exist if the packaged server had no features installed when minified
        if (!featureDir.isDirectory() && !featureDir.mkdir()) {
            throw new FeatureToolException("Unable to find or create feature directory: " + featureDir,
                                           MessageFormat.format(NLS.messages.getString("tool.feature.dir.not.found"), featureDir),
                                           null,
                                           ReturnCode.MISSING_CONTENT);
        }

        File[] manifestFiles = featureDir.listFiles(MFFilter);
        if (manifestFiles != null) {
            for (File file : manifestFiles) {
                try {
                    ProvisioningFeatureDefinition fd = new SubsystemFeatureDefinitionImpl(ExtensionConstants.CORE_EXTENSION, file);
                    if (fd.isSupportedFeatureVersion()) {
                        // using symbolic name because gets compared to FeatureResource symbolic name
                        features.put(fd.getSymbolicName(), fd);
                    }
                } catch (IOException e) {
                    // TODO: PROPER NLS MESSAGE
                    throw new FeatureToolException("Unable to read core feature manifest: " + file,
                                                   (String) null,
                                                   e,
                                                   ReturnCode.BAD_FEATURE_DEFINITION);
                }
            }
        }
        return features;
    }

    /**
     * Retrieves a Map of Liberty install features which locates at lib/assets.
     * 
     * @return A Map of Liberty install features.
     */
    public Map<String, ProvisioningFeatureDefinition> getInstallFeatureDefinitions() {
        Map<String, ProvisioningFeatureDefinition> features = new TreeMap<String, ProvisioningFeatureDefinition>();
        File assetDir = getCoreAssetDir();
        if (!assetDir.exists()) {
            return features;
        }
        if (!assetDir.isDirectory()) {
            throw new FeatureToolException("Unable to find or create asset directory: " + assetDir,
                                           MessageFormat.format(NLS.messages.getString("tool.feature.dir.not.found"), assetDir),
                                           null,
                                           ReturnCode.MISSING_CONTENT);
        }
        File[] manifestFiles = assetDir.listFiles(MFFilter);
        if (manifestFiles != null) {
            for (File file : manifestFiles) {
                try {
                    ProvisioningFeatureDefinition fd = new SubsystemFeatureDefinitionImpl(ExtensionConstants.CORE_EXTENSION, file);
                    if (fd.isSupportedFeatureVersion()) {
                        // using symbolic name because gets compared to FeatureResource symbolic name
                        features.put(fd.getSymbolicName(), fd);
                    }
                } catch (IOException e) {
                    // TODO: PROPER NLS MESSAGE
                    throw new FeatureToolException("Unable to read install feature manifest: " + file,
                                                   (String) null,
                                                   e,
                                                   ReturnCode.BAD_FEATURE_DEFINITION);
                }
            }
        }
        return features;
    }

    /**
     * Returns a map of all product feature definitions (lib/features, usr/extension, etc/extensions)
     * 
     * @return All product installed product features.
     */
    public Map<String, ProvisioningFeatureDefinition> getFeatureDefinitions() {
        Map<String, ProvisioningFeatureDefinition> featureDefs = new TreeMap<String, ProvisioningFeatureDefinition>();

        // Add feature definitions from Liberty core.
        featureDefs.putAll(getCoreProductFeatureDefinitions());

        // Add product extension feature definitions in the default usr location.
        Map<String, ProvisioningFeatureDefinition> userFeatureDefs = getUsrProductFeatureDefinitions();
        if (userFeatureDefs != null && !userFeatureDefs.isEmpty()) {
            featureDefs.putAll(userFeatureDefs);
        }

        // Add product extension feature definitions that are not in the default usr location.
        readProductExtFeatureLocations();
        if (!productExtNameInfoMap.isEmpty()) {
            for (String productExtName : BundleRepositoryRegistry.keys()) {
                if (productExtName.isEmpty() || USR_PRODUCT_EXT_NAME.equals(productExtName))
                    continue;

                Map<String, ProvisioningFeatureDefinition> prodExtFeatureDefs = getProductExtFeatureDefinitions(productExtName);
                if (prodExtFeatureDefs != null && !prodExtFeatureDefs.isEmpty())
                    featureDefs.putAll(prodExtFeatureDefs);
            }
        }

        return featureDefs;
    }

    /**
     * Retrieves a map of product feature definitions organized by product name (core, usr, prod1, prod2, ...)
     * 
     * @return
     */
    public Map<String, Map<String, ProvisioningFeatureDefinition>> getFeatureDefinitionsByProduct() {
        Map<String, Map<String, ProvisioningFeatureDefinition>> prodFeatureMap = new TreeMap<String, Map<String, ProvisioningFeatureDefinition>>();

        // Add feature definitions from Liberty core.
        prodFeatureMap.put(CORE_PRODUCT_NAME, getCoreProductFeatureDefinitions());

        // Add product extension feature definitions in the default usr location.
        Map<String, ProvisioningFeatureDefinition> userFeatureDefs = getUsrProductFeatureDefinitions();
        if (userFeatureDefs != null && !userFeatureDefs.isEmpty()) {
            prodFeatureMap.put(USR_PRODUCT_EXT_NAME, userFeatureDefs);
        }

        // Add product extension feature definitions that are not in the default usr location.
        readProductExtFeatureLocations();
        if (!BundleRepositoryRegistry.isEmpty()) {
            for (String productExtName : BundleRepositoryRegistry.keys()) {
                if (productExtName.isEmpty() || USR_PRODUCT_EXT_NAME.equals(productExtName))
                    continue;

                Map<String, ProvisioningFeatureDefinition> prodExtFeatureDefs = getProductExtFeatureDefinitions(productExtName);
                if (prodExtFeatureDefs != null && !prodExtFeatureDefs.isEmpty())
                    prodFeatureMap.put(productExtName, prodExtFeatureDefs);
            }
        }

        return prodFeatureMap;
    }

    /**
     * Retrieves a Map of Liberty core features.
     * 
     * @return A Map of LIberty core features.
     */
    private Map<String, ProvisioningFeatureDefinition> getCoreProductFeatureDefinitions() {
        Map<String, ProvisioningFeatureDefinition> features = new TreeMap<String, ProvisioningFeatureDefinition>();
        File featureDir = getCoreFeatureDir();
        //the feature directory may not exist if the packaged server had no features installed when minified
        if (!featureDir.isDirectory() && !featureDir.mkdir()) {
            throw new FeatureToolException("Unable to find or create feature directory: " + featureDir,
                                           MessageFormat.format(NLS.messages.getString("tool.feature.dir.not.found"), featureDir),
                                           null,
                                           ReturnCode.MISSING_CONTENT);
        }
        File platformDir = getCorePlatformDir();
        File[] manifestFiles = featureDir.listFiles(MFFilter);

        if (manifestFiles != null) {
            for (File file : manifestFiles) {
                try {
                    ProvisioningFeatureDefinition fd = new SubsystemFeatureDefinitionImpl(ExtensionConstants.CORE_EXTENSION, file);
                    if (fd.isSupportedFeatureVersion()) {
                        // using symbolic name because gets compared to FeatureResource symbolic name
                        features.put(fd.getSymbolicName(), fd);
                    }
                } catch (IOException e) {
                    // TODO: PROPER NLS MESSAGE
                    throw new FeatureToolException("Unable to read core feature manifest: " + file,
                                                   (String) null,
                                                   e,
                                                   ReturnCode.BAD_FEATURE_DEFINITION);
                }
            }
        }
        manifestFiles = platformDir.listFiles(MFFilter);

        if (manifestFiles != null) {
            for (File file : manifestFiles) {
                try {
                    ProvisioningFeatureDefinition fd = new KernelFeatureListDefinition(file);
                    // using symbolic name because gets compared to FeatureResource symbolic name
                    features.put(fd.getSymbolicName(), fd);
                } catch (IOException e) {
                    // TODO: PROPER NLS MESSAGE
                    throw new FeatureToolException("Unable to read core manifest: " + file,
                                                   (String) null,
                                                   e,
                                                   ReturnCode.BAD_FEATURE_DEFINITION);
                }
            }
        }
        return features;
    }

    /**
     * Retrieves a Map of feature definitions in default usr product extension location
     * 
     * @return Null if the user directory cannot be found. An empty map if the features manifests cannot
     *         be found. A Map of product extension features if all goes well.
     */
    private Map<String, ProvisioningFeatureDefinition> getUsrProductFeatureDefinitions() {
        Map<String, ProvisioningFeatureDefinition> features = null;
        File userDir = Utils.getUserDir();
        if (userDir != null && userDir.exists()) {
            File userFeatureDir = new File(userDir, USER_FEATURE_DIR);
            if (userFeatureDir.exists()) {
                features = new TreeMap<String, ProvisioningFeatureDefinition>();
                File[] userManifestFiles = userFeatureDir.listFiles(MFFilter);
                if (userManifestFiles != null) {
                    for (File file : userManifestFiles) {
                        try {
                            ProvisioningFeatureDefinition fd = new SubsystemFeatureDefinitionImpl(USR_PRODUCT_EXT_NAME, file);
                            features.put(fd.getSymbolicName(), fd);
                        } catch (IOException e) {
                            // TODO: PROPER NLS MESSAGE
                            throw new FeatureToolException("Unable to read feature manifest from user extension: " + file,
                                                           (String) null,
                                                           e,
                                                           ReturnCode.BAD_FEATURE_DEFINITION);
                        }
                    }
                }
            }
        }

        return features;
    }

    /**
     * Retrieves a Map of feature definitions in product extension locations defined in etc/extensions.
     * 
     * @return Null if the product extension cannot be found. An empty map if the features manifests cannot
     *         be found. A Map of product extension features if all goes well.
     */
    private Map<String, ProvisioningFeatureDefinition> getProductExtFeatureDefinitions(String productName) {
        readProductExtFeatureLocations();
        Map<String, ProvisioningFeatureDefinition> features = null;

        BundleRepositoryHolder featureData = BundleRepositoryRegistry.getRepositoryHolder(productName);
        if (featureData != null) {
            File productInstallDir = new File(featureData.getInstallDir());

            if (productInstallDir.exists()) {
                File featuresDir = new File(productInstallDir, FEATURE_DIR);
                if (featuresDir.exists()) {
                    features = new TreeMap<String, ProvisioningFeatureDefinition>();
                    File[] productManifestFiles = featuresDir.listFiles(MFFilter);
                    if (productManifestFiles != null) {
                        for (File productManifestFile : productManifestFiles) {
                            try {
                                ProvisioningFeatureDefinition fd = new SubsystemFeatureDefinitionImpl(productName, productManifestFile);
                                features.put(fd.getSymbolicName(), fd);
                            } catch (IOException e) {
                                // TODO: PROPER NLS MESSAGE
                                throw new FeatureToolException("Unable to read feature manifest from product extension: " + productManifestFile,
                                                               (String) null,
                                                               e,
                                                               ReturnCode.BAD_FEATURE_DEFINITION);
                            }
                        }
                    }
                }
            }
        }

        return features;
    }

    /**
     * Retrieves and saves the product extension locations defined in the etc/extensions.
     */
    private void readProductExtFeatureLocations() {
        if (productExtNameInfoMap != null) {
            return;
        }

        productExtNameInfoMap = new HashMap<String, ProductExtensionInfo>();
        File installDir = Utils.getInstallDir();
        Iterator<ProductExtensionInfo> productExtensions = ProductExtension.getProductExtensions(installDir).iterator();

        while (productExtensions.hasNext()) {
            ProductExtensionInfo prodExt = productExtensions.next();
            String prodName = prodExt.getName();
            String prodLocation = prodExt.getLocation();
            productExtNameInfoMap.put(prodName, prodExt);

            if (prodLocation != null && !prodLocation.isEmpty()) {
                String normalizedPath = PathUtils.normalize(prodLocation);

                if (PathUtils.pathIsAbsolute(normalizedPath) == false) {
                    File parentfile = installDir.getParentFile();
                    String parentPath = parentfile.getAbsolutePath();
                    normalizedPath = parentPath + "/" + prodLocation + "/";
                    normalizedPath = PathUtils.normalize(normalizedPath);
                }

                if (normalizedPath != null) {
                    File productInstallDir = new File(normalizedPath);

                    if (productInstallDir.exists()) {
                        BundleRepositoryRegistry.addBundleRepository(normalizedPath, prodName);
                    }
                }
            }
        }
    }

    /**
     * Retrieves the location of the specified product name.
     * 
     * @param productName The product name.
     * @return The location of the specified product installation.
     */
    public String getProdFeatureLocation(String productName) {
        String location = null;
        if (productName.equals(CORE_PRODUCT_NAME)) {
            location = Utils.getInstallDir().getAbsolutePath();
        } else if (productName.equals(USR_PRODUCT_EXT_NAME)) {
            location = Utils.getUserDir().getAbsolutePath();
        } else {
            readProductExtFeatureLocations();
            if (productExtNameInfoMap.containsKey(productName)) {
                location = productExtNameInfoMap.get(productName).getLocation();
            }
        }

        return location;
    }

    /**
     * Retrieves the ID of the specified product name.
     * 
     * @param productName The product name.
     * @return The Id of the specified product installation.
     */
    public String getProdFeatureId(String productName) {
        String productId = null;
        if (!productName.equals(CORE_PRODUCT_NAME) && !productName.equals(USR_PRODUCT_EXT_NAME)) {
            readProductExtFeatureLocations();
            if (productExtNameInfoMap.containsKey(productName)) {
                productId = productExtNameInfoMap.get(productName).getProductID();
            }
        }

        return productId;
    }

    /**
     * Retrieves the Liberty core features directory.
     * 
     * @return The Liberty core features directory
     */
    public File getCoreFeatureDir() {
        File featureDir = null;
        File installDir = Utils.getInstallDir();

        if (installDir != null) {
            featureDir = new File(installDir, FEATURE_DIR);
        }

        if (featureDir == null) {
            throw new RuntimeException("Feature Directory not found");
        }

        return featureDir;
    }

    /**
     * Retrieves the Liberty core platform directory.
     * 
     * @return The Liberty core platform directory
     */
    public File getCorePlatformDir() {
        File platformDir = null;
        File installDir = Utils.getInstallDir();

        if (installDir != null) {
            platformDir = new File(installDir, PLATFORM_DIR);
        }

        if (platformDir == null) {
            throw new RuntimeException("Platform Directory not found");
        }

        return platformDir;
    }

    /**
     * Retrieves the Liberty core assets directory.
     * 
     * @return The Liberty core assets directory
     */
    public File getCoreAssetDir() {
        File assetDir = null;
        File installDir = Utils.getInstallDir();
        if (installDir != null) {
            assetDir = new File(installDir, ASSET_DIR);
        }
        if (assetDir == null) {
            throw new RuntimeException("Asset Directory not found");
        }
        return assetDir;
    }

    /**
     * Get bundle repository
     * 
     * @param locService
     *            a location service
     * @param msgs
     *            true if messages should be output to the log, false otherwise.
     * 
     * @return a bundle repository
     * 
     */
    public ContentBasedLocalBundleRepository getBundleRepository(String featureName, WsLocationAdmin locService) {
        return BundleRepositoryRegistry.getRepositoryHolder(featureName).getBundleRepository();
    }
}
