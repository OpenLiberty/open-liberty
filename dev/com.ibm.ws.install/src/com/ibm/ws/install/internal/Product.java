/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.asset.ESAAsset;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.feature.internal.generator.ManifestFileProcessor;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.provisioning.ProductExtension;
import com.ibm.ws.kernel.provisioning.ProductExtensionInfo;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

public class Product {

    private static final String PRODUCT_ID = "com.ibm.websphere.productId";
    private static final String PRODUCT_VERSION = "com.ibm.websphere.productVersion";
    private static final String PRODUCT_EDITION = "com.ibm.websphere.productEdition";
    private static final String PRODUCT_PROPERTIES_PATH = "lib/versions/WebSphereApplicationServer.properties";
    private static final String PRODUCT_INSTALL_TYPE = "com.ibm.websphere.productInstallType";
    private File installDir;
    private File installTempDir;
    private File userDir;
    private File userExtensionDir;
    private Properties productProperties;

    private Map<String, ProvisioningFeatureDefinition> featureDefs;
    private Map<String, ProvisioningFeatureDefinition> installFeatureDefs;
    private ManifestFileProcessor mfp;

    public Product(File installDir) {
        if (installDir != null) {
            this.installDir = installDir;
            Utils.setInstallDir(this.installDir);
        } else {
            this.installDir = Utils.getInstallDir();
        }
        refresh();
    }

    /**
     * Resets productProperties, featureDefs, installFeatureDefs, and mfp.
     */
    public void refresh() {
        this.productProperties = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(this.installDir, PRODUCT_PROPERTIES_PATH));
            this.productProperties.load(fis);
        } catch (Exception e) {
        } finally {
            InstallUtils.close(fis);
        }
        featureDefs = null;
        installFeatureDefs = null;
        mfp = null;
    }

    public String getProductId() {
        return productProperties.getProperty(PRODUCT_ID);
    }

    public String getProductVersion() {
        return productProperties.getProperty(PRODUCT_VERSION);
    }

    public String getProductEdition() {
        return productProperties.getProperty(PRODUCT_EDITION);
    }

    public String getProductInstallType() {
        return productProperties.getProperty(PRODUCT_INSTALL_TYPE);
    }

    ManifestFileProcessor getManifestFileProcessor() {
        if (mfp == null)
            mfp = new ManifestFileProcessor();
        return mfp;
    }

    public Map<String, ProvisioningFeatureDefinition> getFeatureDefinitions() {
        if (featureDefs == null) {
            featureDefs = getManifestFileProcessor().getFeatureDefinitions();
        }
        return featureDefs;
    }

    public Map<String, ProvisioningFeatureDefinition> getCoreFeatureDefinitions() {
        return getManifestFileProcessor().getFeatureDefinitions(InstallConstants.TO_CORE);
    }

    public Map<String, ProvisioningFeatureDefinition> getCoreFeatureDefinitionsExceptPlatform() {
        return getManifestFileProcessor().getCoreFeatureDefinitionsExceptPlatform();
    }

    public Map<String, ProvisioningFeatureDefinition> getFeatureCollectionDefinitions() {
        if (installFeatureDefs == null) {
            installFeatureDefs = getManifestFileProcessor().getInstallFeatureDefinitions();
        }
        return installFeatureDefs;
    }

    public Map<String, ProvisioningFeatureDefinition> getAllFeatureDefinitions() {
        Map<String, ProvisioningFeatureDefinition> map = new TreeMap<String, ProvisioningFeatureDefinition>();
        map.putAll(getFeatureDefinitions());
        map.putAll(getFeatureCollectionDefinitions());
        return map;
    }

    public Map<String, ProvisioningFeatureDefinition> getAllCoreFeatureDefinitions() {
        Map<String, ProvisioningFeatureDefinition> map = new TreeMap<String, ProvisioningFeatureDefinition>();
        map.putAll(getCoreFeatureDefinitions());
        map.putAll(getFeatureCollectionDefinitions());
        return map;
    }

    public Map<String, ProvisioningFeatureDefinition> getAllCoreFeatureDefinitionsExceptPlatform() {
        Map<String, ProvisioningFeatureDefinition> map = new TreeMap<String, ProvisioningFeatureDefinition>();
        map.putAll(getCoreFeatureDefinitionsExceptPlatform());
        map.putAll(getFeatureCollectionDefinitions());
        return map;
    }

    public File getInstallTempDir() {
        if (installTempDir == null)
            installTempDir = new File(getInstallDir(), "installTmp");
        return installTempDir;
    }

    public File getInstallDir() {
        return installDir;
    }

    public File getUserDir() {
        if (userDir == null)
            userDir = Utils.getUserDir();
        return userDir;
    }

    public File getUserExtensionDir() {
        if (userExtensionDir == null)
            userExtensionDir = new File(getUserDir(), "extension");
        return userExtensionDir;
    }

    public File getUserDirExternal(String repoType) throws InstallException {
        File baseDir;
        try {
            ProductExtensionInfo productExtension = ProductExtension.getProductExtension(repoType);
            if (productExtension == null) {
                throw new InstallException(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.bad.extension", repoType), InstallException.BAD_ARGUMENT);
            }

            String featurePath = productExtension.getLocation();
            if (featurePath != null) {
                String normalizedPath = PathUtils.normalize(featurePath);
                if (!!!PathUtils.pathIsAbsolute(normalizedPath)) {
                    File parentfile = Utils.getInstallDir().getParentFile();
                    baseDir = new File(parentfile, featurePath);
                } else {
                    baseDir = new File(normalizedPath);
                }
            } else {
                throw new InstallException(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.invalid.extension", repoType));
            }
        } catch (IOException e) {
            throw new InstallException(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.ioexception.extension", repoType, e.getMessage()));
        }

        return baseDir;
    }

    public boolean containsFeature(String featureName) {
        return getFeatureDefinitions().containsKey(featureName);
    }

    public void addFeature(String name, ProvisioningFeatureDefinition fd) {
        getFeatureDefinitions().put(name, fd);
    }

    public boolean containsFeatureCollection(String featureCollectionName) {
        return getFeatureCollectionDefinitions().containsKey(featureCollectionName);
    }

    public void addFeatureCollection(String name, ProvisioningFeatureDefinition fd) {
        getFeatureCollectionDefinitions().put(name, fd);
    }

    public Set<String> getAcceptedLicenses() {
        Set<String> acceptedLicenses = new HashSet<String>();
        for (ProvisioningFeatureDefinition fd : getAllFeatureDefinitions().values()) {
            String subsystemLicenseType = fd.getHeader("Subsystem-License");
            if (subsystemLicenseType != null && !subsystemLicenseType.isEmpty()) {
                acceptedLicenses.add(subsystemLicenseType);
            }
        }
        return acceptedLicenses;
    }

    public List<String> getExtensionNames() {
        List<String> extensionNames = new ArrayList<String>(0);
        List<ProductExtensionInfo> productExtensionInfoList = ProductExtension.getProductExtensions(getInstallDir());
        for (ProductExtensionInfo productExtensionInfo : productExtensionInfoList) {
            extensionNames.add(productExtensionInfo.getName());
        }
        return extensionNames;
    }

    public Set<String> getInstalledFeatures(String installedBy) {
        Map<String, ProvisioningFeatureDefinition> map = getManifestFileProcessor().getFeatureDefinitions(installedBy);
        return map == null ? new HashSet<String>(0) : map.keySet();
    }

    public Set<String> getInstalledFeatures() {
        return new HashSet<String>(getFeatureDefinitions().keySet());
    }

    public boolean isInstalled(RepositoryResource installResource) {
        if (installResource instanceof EsaResource) {
            EsaResource esa = (EsaResource) installResource;
            String symName = esa.getProvideFeature();
            return containsFeature(symName) || containsFeatureCollection(symName);
        }
        return false;
    }

    public boolean isInstalled(ESAAsset esa) {
        String symName = esa.getFeatureName();
        return containsFeature(symName) || containsFeatureCollection(symName);
    }

}
