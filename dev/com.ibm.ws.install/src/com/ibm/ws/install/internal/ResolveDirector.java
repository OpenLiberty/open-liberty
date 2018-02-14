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
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;

import com.ibm.websphere.crypto.InvalidPasswordDecodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallConstants.DownloadOption;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallProgressEvent;
import com.ibm.ws.install.RepositoryConfig;
import com.ibm.ws.install.RepositoryConfigUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.adaptor.FixAdaptor;
import com.ibm.ws.install.internal.asset.ESAAsset;
import com.ibm.ws.install.internal.asset.InstallAsset;
import com.ibm.ws.install.repository.download.RepositoryDownloadUtil;
import com.ibm.ws.install.repository.internal.RepositoryUtils;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.connections.DirectoryRepositoryConnection;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnectionProxy;
import com.ibm.ws.repository.connections.ZipRepositoryConnection;
import com.ibm.ws.repository.connections.liberty.MainRepository;
import com.ibm.ws.repository.connections.liberty.ProductInfoProductDefinition;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBackendIOException;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.exceptions.RepositoryHttpException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resolver.RepositoryResolutionException;
import com.ibm.ws.repository.resolver.RepositoryResolver;
import com.ibm.ws.repository.resources.AttachmentResource;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.SampleResource;

import wlp.lib.extract.SelfExtractor;

class ResolveDirector extends AbstractDirector {

    private RepositoryConnectionList repositoryConnectionList = null;
    private RestRepositoryConnectionProxy proxy = null;
    private Properties repoProperties;
    private String userAgent;
    private String repositoryUrl;

    private Map<String, File> downloadCach = null;
    private Map<String, ESAAsset> esaAssetCach = null;

    // For interactive
    Map<String, List<List<RepositoryResource>>> installResources;
    ArrayList<InstallAsset> localInstallAssets;

    private static class InstallInformation {
        private final File esaToInstall;
        private final String installRootFolder;
        private final boolean isTemporary;
        private final String feature;

        public InstallInformation(File esaToInstall, String feature, String installRootFolder, boolean isTemporary) {
            this.esaToInstall = esaToInstall;
            this.feature = feature;
            this.installRootFolder = installRootFolder;
            this.isTemporary = isTemporary;
        }
    }

    ResolveDirector(Product product, EventManager eventManager, Logger logger) {
        super(product, eventManager, logger);
        this.downloadCach = new HashMap<String, File>();
        this.esaAssetCach = new HashMap<String, ESAAsset>();
        setUserAgent();
    }

    void checkResources() throws InstallException {
        if (installResources != null) {
            for (List<List<RepositoryResource>> targetList : installResources.values()) {
                for (List<RepositoryResource> mrList : targetList) {
                    for (RepositoryResource installResource : mrList) {
                        checkResource(installResource);
                    }
                }
            }
        }
    }

    void cleanUp() {
        repositoryConnectionList = null;
        proxy = null;
        installResources = null;
        downloadCach.clear();
        esaAssetCach.clear();
    }

    boolean defaultRepo() {
        return repoProperties == null || isFeatureManager();
    }

    Collection<ESAAsset> getAutoFeature(File fromDir, String toExtension) {
        Collection<ESAAsset> autoFeatures = new ArrayList<ESAAsset>();
        FilenameFilter ff = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".esa");
            }
        };
        Set<String> installedFeatures = product.getInstalledFeatures();
        File[] esaes = fromDir.listFiles(ff);
        for (File esa : esaes) {
            if (esa.isFile()) {
                ESAAsset esaAsset = createESAAsset(esa, toExtension);
                if (esaAsset != null && esaAsset.isAutoFeature() && esaAsset.installWhenSatisfied() && !autoFeatures.contains(esaAsset)) {
                    boolean installed = installedFeatures.contains(esaAsset.getProvideFeature());
                    log(Level.FINEST, esaAsset.getProvideFeature() + " is auto feature and " + (installed ? "installed" : "not installed"));
                    if (!installed) {
                        autoFeatures.add(esaAsset);
                    }
                }
            }
        }
        return autoFeatures;
    }

    Map<String, List<List<RepositoryResource>>> getInstallResources() {
        return this.installResources;
    }

    int getInstallResourcesSize() {
        int t = 0;
        if (installResources != null) {
            for (List<List<RepositoryResource>> targetList : installResources.values()) {
                for (List<RepositoryResource> mrList : targetList) {
                    t += mrList.size();
                }
            }
        }
        return t;
    }

    long getInstallResourcesMainAttachmentSize() {
        long size = 0;
        if (installResources != null) {
            for (List<List<RepositoryResource>> targetList : installResources.values()) {
                for (List<RepositoryResource> mrList : targetList) {
                    for (RepositoryResource mr : mrList) {
                        AttachmentResource ar = null;
                        try {
                            ar = mr.getMainAttachment();
                        } catch (Exception e) {
                            log(Level.FINEST, e.getLocalizedMessage(), e);

                        }
                        if (ar != null) {
                            size += ar.getSize();
                        }
                    }
                }
            }
        }
        return size;
    }

    List<InstallAsset> getLocalInstallAssets() {
        return this.localInstallAssets;
    }

    int getLocalInstallAssetsSize() {
        if (localInstallAssets != null) {
            return localInstallAssets.size();
        }
        return 0;
    }

    private RepositoryConnectionList getRepositoryConnectionList(Collection<String> featureNames) throws InstallException {
        List<RepositoryConfig> repositoryConfigs = RepositoryConfigUtils.getRepositoryConfigs(repoProperties);
        proxy = RepositoryConfigUtils.getProxyInfo(repoProperties);
        List<RepositoryConnection> loginEntries = new ArrayList<RepositoryConnection>(repositoryConfigs.size());
        for (RepositoryConfig rc : repositoryConfigs) {
            RepositoryConnection lie = null;

            String url = rc.getUrl();
            if (url != null && url.toLowerCase().startsWith("file:")) {
                try {
                    URL urlProcessed = new URL(url);
                    File repoDir = new File(urlProcessed.getPath());
                    if (repoDir.exists()) {
                        if (repoDir.isDirectory()) {
                            lie = new DirectoryRepositoryConnection(repoDir);
                            loginEntries.add(lie);
                            continue;
                        } else {
                            lie = new ZipRepositoryConnection(repoDir);
                            loginEntries.add(lie);
                            continue;
                        }

                    } else {
                        throw new IOException();
                    }
                } catch (Exception e) {
                    throw ExceptionUtils.create(RepositoryUtils.getMessage("ERROR_FILEPATH_NOT_EXISTS", url));
                }

            }
            if (rc.isLibertyRepository()) {
                try {
                    lie = MainRepository.createConnection(proxy);
                } catch (RepositoryBackendIOException e) {
                    if (e instanceof RepositoryHttpException) {
                        if (((RepositoryHttpException) e).get_httpRespCode() == InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE) {
                            throw ExceptionUtils.createByKey(e, "ERROR_TOOL_INCORRECT_PROXY_CREDENTIALS");
                        }
                        throw ExceptionUtils.createByKey(e, "ERROR_FAILED_TO_CONNECT");
                    }
                    throw ExceptionUtils.create(e, featureNames, false, proxy, true);
                }
            } else {
                String decodedPwd = rc.getUserPwd();
                if (decodedPwd != null && !decodedPwd.isEmpty()) {
                    try {
                        decodedPwd = PasswordUtil.decode(rc.getUserPwd());
                    } catch (InvalidPasswordDecodingException ipde) {
                        decodedPwd = rc.getUserPwd();
                    } catch (UnsupportedCryptoAlgorithmException ucae) {
                        throw ExceptionUtils.createByKey(ucae, "ERROR_TOOL_PWD_CRYPTO_UNSUPPORTED");
                    }
                }
                lie = new RestRepositoryConnection(rc.getUser(), decodedPwd, rc.getApiKey(), rc.getUrl().toString());
                ((RestRepositoryConnection) lie).setProxy(proxy);
            }
            if (lie != null) {
                if (userAgent != null && !userAgent.isEmpty()) {
                    ((RestRepositoryConnection) lie).setUserAgent(userAgent);
                }
                loginEntries.add(lie);
            }
        }

        repositoryConnectionList = new RepositoryConnectionList(loginEntries);
        return repositoryConnectionList;
    }

    RepositoryConnectionList getRepositoryConnectionList(Collection<String> featureNames, String userId, String password, String logLable) throws InstallException {
        RestRepositoryConnection restConnection;
        if (repositoryConnectionList == null) {
            if (!isFeatureManager() && repoProperties != null && !repoProperties.isEmpty()) {
                getRepositoryConnectionList(featureNames);
                InstallLogUtils.logLoginInfo(repositoryConnectionList, logLable);
                return repositoryConnectionList;
            }
            try {
                //Retrieve and set proxy server settings
                proxy = RepositoryConfigUtils.getProxyInfo(repoProperties);
                restConnection = MainRepository.createConnection(proxy);
            } catch (RepositoryBackendIOException e) {
                if (e instanceof RepositoryHttpException) {
                    if (((RepositoryHttpException) e).get_httpRespCode() == InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE) {
                        throw ExceptionUtils.createByKey(e, "ERROR_TOOL_INCORRECT_PROXY_CREDENTIALS");
                    }
                    throw ExceptionUtils.createByKey(e, "ERROR_FAILED_TO_CONNECT");
                }
                throw ExceptionUtils.create(e, featureNames, false, proxy, true);
            }
            if (userId != null) {
                restConnection.setUserId(userId);
            }
            if (password != null) {
                restConnection.setPassword(password);
            }
            if (repositoryUrl != null && !repositoryUrl.trim().isEmpty()) {
                restConnection.setRepositoryUrl(repositoryUrl);
            }
            if (userAgent != null && !userAgent.isEmpty()) {
                restConnection.setUserAgent(userAgent);
            }

            repositoryConnectionList = new RepositoryConnectionList(restConnection);
            InstallLogUtils.logLoginInfo(repositoryConnectionList, logLable);
        }
        return repositoryConnectionList;
    }

    RestRepositoryConnectionProxy getProxy() {
        return this.proxy;
    }

    int getPublicInstallResourcesSize() {
        int t = 0;
        if (installResources != null) {
            for (List<List<RepositoryResource>> targetList : installResources.values()) {
                for (List<RepositoryResource> mrList : targetList) {
                    for (RepositoryResource mr : mrList) {
                        ResourceType type = mr.getType();
                        if (type.equals(ResourceType.FEATURE)) {
                            Visibility v = ((EsaResource) mr).getVisibility();
                            if (v.equals(Visibility.PUBLIC) || v.equals(Visibility.INSTALL)) {
                                t++;
                            }
                        } else if (type.equals(ResourceType.PRODUCTSAMPLE) ||
                                   type.equals(ResourceType.OPENSOURCE)) {
                            t++;
                        }
                    }
                }
            }
        }
        return t;
    }

    int getPublicLocalInstallAssetsSize() {
        int t = 0;
        if (localInstallAssets != null) {
            for (InstallAsset installAsset : localInstallAssets) {
                if (installAsset.isFeature()) {
                    ESAAsset esaa = ((ESAAsset) installAsset);
                    if (esaa.isPublic()) {
                        t++;
                    }
                }
            }
        }
        return t;
    }

    void resolve(Collection<String> assetIds, boolean download) throws InstallException {
        if (assetIds == null || assetIds.isEmpty()) {
            throw ExceptionUtils.createByKey("ERROR_ASSETS_LIST_INVALID");
        }

        RepositoryConnectionList loginInfo = getRepositoryConnectionList(null, null, null, this.getClass().getCanonicalName() + ".resolve");

        this.installResources = resolveMap(assetIds, loginInfo, download);
        if (isEmpty(this.installResources)) {
            throw ExceptionUtils.createByKey(InstallException.ALREADY_EXISTS, "ASSETS_ALREADY_INSTALLED",
                                             InstallUtils.getShortNames(product.getFeatureDefinitions(), assetIds).toString());
        }
    }

    void resolve(String feature, File esaFile, String toExtension) throws InstallException {
        Collection<String> featureIds = new HashSet<String>(1);
        featureIds.add(feature);
        createESAAsset(esaFile, toExtension);
        File fromDir = esaFile.getAbsoluteFile().getParentFile();
        ArrayList<InstallAsset> installAssets = new ArrayList<InstallAsset>();
        ArrayList<String> unresolvedFeatures = new ArrayList<String>();
        Collection<ESAAsset> autoFeatures = getAutoFeature(fromDir, toExtension);
        resolve(featureIds, fromDir, toExtension, false, installAssets, unresolvedFeatures);
        if (!unresolvedFeatures.isEmpty()) {
            log(Level.FINEST, "Determined unresolved features: " + unresolvedFeatures.toString() + " from " + fromDir.getAbsolutePath());
            RepositoryConnectionList loginInfo = getRepositoryConnectionList(null, null, null, this.getClass().getCanonicalName() + ".resolve");
            this.installResources = resolveMap(unresolvedFeatures, loginInfo, false);
        }
        if (!installAssets.isEmpty()) {
            resolveAutoFeatures(autoFeatures, installAssets);
            this.localInstallAssets = installAssets;
        }
        if (this.localInstallAssets == null || this.localInstallAssets.isEmpty()) {
            throw ExceptionUtils.createByKey(InstallException.ALREADY_EXISTS, "ALREADY_INSTALLED", InstallUtils.getFeatureListOutput(featureIds));
        }
    }

    List<List<RepositoryResource>> resolve(Collection<String> featureNames, DownloadOption downloadOption, String userId, String password) throws InstallException {
        Collection<String> featureNamesProcessed = new ArrayList<String>();
        for (String s : featureNames) {
            featureNamesProcessed.add(s.replaceAll("\\\\+$", ""));
        }
        Collection<ProductDefinition> productDefinitions = new HashSet<ProductDefinition>();
        try {
            for (ProductInfo productInfo : ProductInfo.getAllProductInfo().values()) {
                productDefinitions.add(new ProductInfoProductDefinition(productInfo));
            }
        } catch (Exception e) {
            throw ExceptionUtils.create(e);
        }

        RepositoryConnectionList loginInfo = getRepositoryConnectionList(featureNamesProcessed, userId, password, this.getClass().getCanonicalName() + ".resolve");

        RepositoryResolver resolver;
        Collection<List<RepositoryResource>> installResources;
        try {
            if (downloadOption == DownloadOption.all || downloadOption == DownloadOption.none) {
                resolver = new RepositoryResolver(productDefinitions, Collections.<ProvisioningFeatureDefinition> emptySet(), Collections.<IFixInfo> emptySet(), loginInfo);
                installResources = resolver.resolve(featureNamesProcessed);
            } else {
                Collection<String> featuresToInstall = getFeaturesToInstall(featureNamesProcessed, false);

                if (featuresToInstall.isEmpty()) {
                    return new ArrayList<List<RepositoryResource>>(0);
                }
                resolver = new RepositoryResolver(productDefinitions, product.getFeatureDefinitions().values(), FixAdaptor.getInstalledIFixes(product.getInstallDir()), loginInfo);
                installResources = resolver.resolve(featuresToInstall);
            }
        } catch (RepositoryResolutionException e) {
            throw ExceptionUtils.create(e, featureNamesProcessed, product.getInstallDir(), false);
        } catch (RepositoryException e) {
            throw ExceptionUtils.create(e, featureNamesProcessed, false, proxy, defaultRepo());
        }
        List<List<RepositoryResource>> installResourcesCollection = new ArrayList<List<RepositoryResource>>(installResources.size());
        List<RepositoryResource> installResourcesSingleList = new ArrayList<RepositoryResource>();
        if (downloadOption == DownloadOption.none) {
            for (List<RepositoryResource> installResourcesList : installResources) {
                for (RepositoryResource installResource : installResourcesList) {
                    if (installResource instanceof EsaResource || installResource instanceof SampleResource) {
                        for (String featureName : featureNamesProcessed) {
                            String provideFeature = null;
                            String shortName = null;
                            if (installResource instanceof EsaResource) {
                                provideFeature = ((EsaResource) installResource).getProvideFeature();
                                shortName = ((EsaResource) installResource).getShortName();
                            } else if (installResource instanceof SampleResource) {
                                provideFeature = ((SampleResource) installResource).getName();
                                shortName = ((SampleResource) installResource).getShortName();
                            }
                            if ((provideFeature != null && provideFeature.equals(featureName)) ||
                                (shortName != null && shortName.equalsIgnoreCase(featureName))) {
                                if (!installResourcesSingleList.contains(installResource)) {
                                    installResourcesSingleList.add(installResource);
                                }
                            }
                        }
                    }
                }
            }

            if (!installResourcesSingleList.isEmpty())
                installResourcesCollection.add(installResourcesSingleList);
        } else {
            for (List<RepositoryResource> installResourcesList : installResources) {
                ArrayList<RepositoryResource> mrList = new ArrayList<RepositoryResource>(installResourcesList.size());
                installResourcesCollection.add(mrList);
                for (RepositoryResource installResource : installResourcesList) {
                    if (!installResourcesSingleList.contains(installResource)) {
                        if (downloadOption == DownloadOption.all || !product.isInstalled(installResource)) {
                            mrList.add(installResource);
                            installResourcesSingleList.add(installResource);
                        }
                    }
                }
            }
        }
        return installResourcesCollection;
    }

    private boolean mapContains(Map<String, List<List<RepositoryResource>>> installResourcesMap, RepositoryResource mr) {
        for (List<List<RepositoryResource>> targetList : installResourcesMap.values()) {
            for (List<RepositoryResource> mrList : targetList) {
                for (RepositoryResource m : mrList) {
                    if (m.equals(mr))
                        return true;
                }
            }
        }
        return false;
    }

    private List<List<RepositoryResource>> removeDuplicated(Map<String, List<List<RepositoryResource>>> installResourcesMap, List<List<RepositoryResource>> resolved) {
        List<List<RepositoryResource>> newList = new ArrayList<List<RepositoryResource>>(resolved.size());
        for (List<RepositoryResource> mrList : resolved) {
            List<RepositoryResource> newMRList = new ArrayList<RepositoryResource>(mrList.size());
            for (RepositoryResource mr : mrList) {
                if (!mapContains(installResourcesMap, mr)) {
                    newMRList.add(mr);
                }
            }
            if (!newMRList.isEmpty())
                newList.add(newMRList);
        }
        return newList;
    }

    private void checkESAResources(List<List<RepositoryResource>> resolved) throws InstallException {
        for (List<RepositoryResource> mrList : resolved) {
            for (RepositoryResource mr : mrList) {
                ResourceType type = mr.getType();
                if (type.equals(ResourceType.FEATURE)) {
                    Visibility v = ((EsaResource) mr).getVisibility();
                    if (v.equals(Visibility.INSTALL)) {
                        throw ExceptionUtils.createByKey("ERROR_NON_FEATURE_CANNOT_INSTALL_TO_EXTENSION", InstallUtils.getResourceId(mr));
                    }
                } else {
                    throw ExceptionUtils.createByKey("ERROR_NON_FEATURE_CANNOT_INSTALL_TO_EXTENSION", InstallUtils.getResourceId(mr));
                }
            }
        }
    }

    Map<String, List<List<RepositoryResource>>> resolveMap(Collection<String> featureNames, DownloadOption downloadOption, String userId, String password) throws InstallException {
        fireProgressEvent(InstallProgressEvent.RESOLVE, 2, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_RESOLVING"));
        Map<String, List<List<RepositoryResource>>> installResourcesMap = new HashMap<String, List<List<RepositoryResource>>>();
        Map<String, Collection<String>> assetsMap = InstallUtils.getAssetsMap(featureNames, true);
        Collection<String> dAssets = assetsMap.get(DEFAULT_TO_EXTENSION);
        if (dAssets != null && !dAssets.isEmpty()) {
            List<List<RepositoryResource>> resolved = resolve(dAssets, downloadOption, userId, password);
            if (!isEmpty(resolved)) {
                installResourcesMap.put(DEFAULT_TO_EXTENSION, resolved);
            }
        }
        for (Entry<String, Collection<String>> assetsEntry : assetsMap.entrySet()) {
            if (!assetsEntry.getKey().equalsIgnoreCase(DEFAULT_TO_EXTENSION)) {
                List<List<RepositoryResource>> resolved = resolve(assetsEntry.getValue(), downloadOption, userId, password);
                resolved = removeDuplicated(installResourcesMap, resolved);
                if (!isEmpty(resolved)) {
                    installResourcesMap.put(assetsEntry.getKey(), resolved);
                }
            }
        }
        return installResourcesMap;
    }

    String resolve(String esaLocation, String toExtension, Set<String> features, ArrayList<InstallAsset> installAssets,
                   int progress, int interval) throws InstallException {
        InstallInformation installInformation = createInstallInformation(esaLocation, null, progress);
        return resolve(installInformation, toExtension, features, installAssets, progress, interval);

    }

    void resolve(Collection<String> featureIds, File fromDir, String toExtension, boolean offlineOnly, ArrayList<InstallAsset> installAssets,
                 ArrayList<String> unresolvedFeatures) throws InstallException {
        Map<String, ProvisioningFeatureDefinition> installedFeatureDefs = product.getFeatureDefinitions();
        Set<String> installedFeatures = product.getInstalledFeatures();
        for (String f : featureIds) {
            if (!containFeature(installedFeatureDefs, f) && !installedFeatures.contains(f)) {
                resolve(f, fromDir, toExtension, offlineOnly, installedFeatures, esaAssetCach, installAssets, unresolvedFeatures);
            }
        }
    }

    void resolveAutoFeatures(Collection<ESAAsset> autoFeatures, ArrayList<InstallAsset> installAssets) {
        Collection<ProvisioningFeatureDefinition> featureDefinitionsToCheck = new HashSet<ProvisioningFeatureDefinition>(product.getAllFeatureDefinitions().values());
        for (InstallAsset a : installAssets) {
            if (a.isFeature()) {
                ProvisioningFeatureDefinition pdf = ((ESAAsset) a).getProvisioningFeatureDefinition();
                featureDefinitionsToCheck.add(pdf);
            }
        }
        resolveAutoFeature(autoFeatures, installAssets, featureDefinitionsToCheck);
    }

    List<List<RepositoryResource>> resolve(Collection<String> assetNames, RepositoryConnectionList loginInfo, boolean download) throws InstallException {
        Collection<String> assetNamesProcessed = new ArrayList<String>();
        for (String s : assetNames) {
            assetNamesProcessed.add(s.replaceAll("\\\\+$", ""));
        }
        Collection<ProductDefinition> productDefinitions = new HashSet<ProductDefinition>();
        try {
            for (ProductInfo productInfo : ProductInfo.getAllProductInfo().values()) {
                productDefinitions.add(new ProductInfoProductDefinition(productInfo));
            }
        } catch (Exception e) {
            throw ExceptionUtils.create(e);
        }

        RepositoryResolver resolver;
        Collection<List<RepositoryResource>> installResources;
        try {
            Collection<String> assetsToInstall = getFeaturesToInstall(assetNamesProcessed, download);
            if (assetsToInstall.isEmpty()) {
                return new ArrayList<List<RepositoryResource>>(0);
            }
            Map<String, ProvisioningFeatureDefinition> installedFeatureDefinitions = product.getFeatureDefinitions();
            Collection<ProvisioningFeatureDefinition> installedFeatures = download
                                                                          && System.getProperty("INTERNAL_DOWNLOAD_FROM_FOR_BUILD") == null ? Collections.<ProvisioningFeatureDefinition> emptySet() : installedFeatureDefinitions.values();
            Collection<IFixInfo> installedIFixes = download ? Collections.<IFixInfo> emptySet() : FixAdaptor.getInstalledIFixes(product.getInstallDir());
            resolver = new RepositoryResolver(productDefinitions, installedFeatures, installedIFixes, loginInfo);
            installResources = resolver.resolve(assetsToInstall);
        } catch (RepositoryResolutionException e) {
            throw ExceptionUtils.create(e, assetNamesProcessed, product.getInstallDir(), true);
        } catch (RepositoryException e) {
            throw ExceptionUtils.create(e, assetNamesProcessed, true, proxy, defaultRepo());
        }

        List<List<RepositoryResource>> installResourcesCollection = new ArrayList<List<RepositoryResource>>(installResources.size());
        List<RepositoryResource> installResourcesSingleList = new ArrayList<RepositoryResource>();
        for (List<RepositoryResource> installResourcesList : installResources) {
            ArrayList<RepositoryResource> mrList = new ArrayList<RepositoryResource>(installResourcesList.size());
            installResourcesCollection.add(mrList);
            for (RepositoryResource installResource : installResourcesList) {
                if (!installResourcesSingleList.contains(installResource)) {
                    if (download || !product.isInstalled(installResource)) {
                        mrList.add(installResource);
                        installResourcesSingleList.add(installResource);
                    }
                }
            }
        }
        return installResourcesCollection;
    }

    Map<String, List<List<RepositoryResource>>> resolveMap(Collection<String> assetNames, RepositoryConnectionList loginInfo, boolean download) throws InstallException {
        fireProgressEvent(InstallProgressEvent.RESOLVE, 2, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_PREPARING_ASSETS"));
        Map<String, List<List<RepositoryResource>>> installResourcesMap = new HashMap<String, List<List<RepositoryResource>>>();
        Map<String, Collection<String>> assetsMap = InstallUtils.getAssetsMap(assetNames, download);
        Collection<String> dAssets = assetsMap.get(DEFAULT_TO_EXTENSION);
        if (dAssets != null && !dAssets.isEmpty()) {
            List<List<RepositoryResource>> resolved = resolve(dAssets, loginInfo, download);
            if (!isEmpty(resolved)) {
                installResourcesMap.put(DEFAULT_TO_EXTENSION, resolved);
            }
        }
        for (Entry<String, Collection<String>> assetsEntry : assetsMap.entrySet()) {
            if (!assetsEntry.getKey().equalsIgnoreCase(DEFAULT_TO_EXTENSION)) {
                List<List<RepositoryResource>> resolved = resolve(assetsEntry.getValue(), loginInfo, download);
                if (!download)
                    checkESAResources(resolved);
                resolved = removeDuplicated(installResourcesMap, resolved);
                if (!isEmpty(resolved)) {
                    installResourcesMap.put(assetsEntry.getKey(), resolved);
                }
            }
        }
        return installResourcesMap;
    }

    //return true when installResources is not empty, otherwise return false
    boolean resolveExistingAssetsFromDirectoryRepo(Collection<String> featureNames, File repoDir, boolean isOverwrite) throws InstallException {
        Collection<String> existingAssets = new ArrayList<String>();
        for (List<List<RepositoryResource>> targetList : installResources.values()) {
            for (Iterator<List<RepositoryResource>> installResourcesListIterator = targetList.iterator(); installResourcesListIterator.hasNext();) {
                List<RepositoryResource> installResourcesList = installResourcesListIterator.next();
                for (Iterator<RepositoryResource> resourcesIterator = installResourcesList.iterator(); resourcesIterator.hasNext();) {
                    RepositoryResource installResource = resourcesIterator.next();
                    ResourceType resourceType = installResource.getType();
                    String assetName = "";
                    if (resourceType.equals(ResourceType.FEATURE) || resourceType.equals(ResourceType.ADDON)) {
                        assetName = ((EsaResource) installResource).getShortName();
                    } else {
                        SampleResource sr = ((SampleResource) installResource);
                        assetName = sr.getName();
                    }
                    String mainAttachmentName = null;
                    String jsonFileName = null;
                    RepositoryConnection connection = installResource.getRepositoryConnection();
                    log(Level.FINEST, "resolveExistingAssetsFromDirectoryRepo " + installResource.getName() + " at " + connection.getRepositoryLocation());

                    if (connection instanceof DirectoryRepositoryConnection) {
                        if (isOverwrite && connection.getRepositoryLocation().equalsIgnoreCase(repoDir.getPath())) {
                            throw ExceptionUtils.createByKey(InstallException.IO_FAILURE, "ERROR_DOWNLOAD_TO_SOURCE_REPO",
                                                             InstallUtils.getFeatureListOutput(featureNames), repoDir.getAbsolutePath());
                        }
                    }
                    try {
                        AttachmentResource mainAttachment = installResource.getMainAttachment();
                        if (mainAttachment != null) {
                            mainAttachmentName = mainAttachment.getName();
                            jsonFileName = mainAttachmentName + ".json";
                        }
                    } catch (RepositoryBackendException e) {
                        log(Level.SEVERE, e.getLocalizedMessage(), e);
                        throw ExceptionUtils.createByKey(InstallException.IO_FAILURE, "ERROR_FAILED_TO_DOWNLOAD_FEATURE",
                                                         InstallUtils.getFeatureListOutput(featureNames), repoDir.getAbsolutePath());
                    } catch (RepositoryResourceException e) {
                        log(Level.SEVERE, e.getLocalizedMessage(), e);
                        throw ExceptionUtils.createByKey(InstallException.IO_FAILURE, "ERROR_FAILED_TO_DOWNLOAD_FEATURE",
                                                         InstallUtils.getFeatureListOutput(featureNames), repoDir.getAbsolutePath());
                    }
                    if (mainAttachmentName != null && InstallUtils.isResourceExistsInDirectory(installResource, repoDir, mainAttachmentName, jsonFileName)) {
                        //TODO: get the short name here
                        if (RepositoryDownloadUtil.isPublicAsset(installResource.getType(), installResource)) {
                            existingAssets.add(assetName);
                        }
                        if (!isOverwrite)
                            resourcesIterator.remove();
                    }
                }
                if (installResourcesList.size() == 0)
                    installResourcesListIterator.remove();
            }
        }

        if (!existingAssets.isEmpty()) {
            String existingAssetsString = InstallUtils.getFeatureListOutput(existingAssets);
            if (isOverwrite) {
                //show features will be overwritten
                if (existingAssets.size() <= 1)
                    log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_DOWNLOADING_REPLACE_ASSET", existingAssetsString));
                else
                    log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_DOWNLOADING_REPLACE_ASSETS", existingAssetsString));
            } else {
                //show features will be skipped
                if (existingAssets.size() <= 1)
                    log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_DOWNLOADING_IGNORE_ASSET", existingAssetsString));
                else
                    log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_DOWNLOADING_IGNORE_ASSETS", existingAssetsString));
            }
        }

        return !isEmpty(installResources);
    }

    void setRepositoryConnectionList(RepositoryConnectionList loginInfo) {
        this.repositoryConnectionList = loginInfo;
    }

    void setProxy(RestRepositoryConnectionProxy proxy) {
        this.proxy = proxy;
    }

    void setRepositoryProperties(Properties repoProperties) {
        this.repoProperties = repoProperties;
    }

    void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    void setUserAgent(String kernelUser) {
        String productVersion = this.product.getProductVersion();
        String productEdition = this.product.getProductEdition();
        String installType = this.product.getProductInstallType();

        String user = kernelUser;
        String clientInfo = String.format("%s.%s", user, installType);
        this.userAgent = String.format(InstallConstants.USER_AGENT, productVersion, productEdition, clientInfo);
    }

    private void checkResource(RepositoryResource installResource) throws InstallException {
        if (installResource.getType().equals(ResourceType.PRODUCTSAMPLE) ||
            installResource.getType().equals(ResourceType.OPENSOURCE)) {
            SampleResource sr = (SampleResource) installResource;
            String serverName = sr.getShortName();
            if (serverName != null) {
                File serverDir = new File(Utils.getUserDir(), "servers/" + serverName);
                if (serverDir.exists()) {
                    String msgId = installResource.getType().equals(ResourceType.PRODUCTSAMPLE) ? "ERROR_SAMPLE_SERVER_ALREADY_INSTALLED" : "ERROR_OPENSOURCE_SERVER_ALREADY_INSTALLED";
                    throw ExceptionUtils.createByKey(msgId, sr.getName(), serverName);
                }
            }
        }
        RepositoryConnection rc = installResource.getRepositoryConnection();
        if (rc instanceof DirectoryRepositoryConnection) {
            AttachmentResource ar = null;
            try {
                ar = installResource.getMainAttachment();
            } catch (RepositoryBackendException e) {
                log(Level.FINEST, "Failed to get main attachment", e);
            } catch (RepositoryResourceException e) {
                log(Level.FINEST, "Failed to get main attachment", e);
            }
            if (ar == null) {
                File f = new File(rc.getRepositoryLocation(), installResource.getId());
                throw ExceptionUtils.createByKey(InstallException.MISSING_CONTENT, "ERROR_INSTALL_ESA_FILE_NOTEXIST", f.getAbsolutePath());
            }
        }
    }

    private ESAAsset createESAAsset(File esaFile, String toExtension) {
        ESAAsset esaAsset;
        String esaPath = esaFile.getAbsolutePath();
        String debugHeader = "createESAAsset(" + esaFile.getAbsolutePath() + ", \"" + toExtension + "\"): ";
        try {
            if (esaAssetCach.containsKey(esaPath)) {
                return esaAssetCach.get(esaPath);
            }
            esaAsset = new ESAAsset(esaFile, toExtension, false);
        } catch (Exception e) {
            esaAssetCach.put(esaPath, null);
            log(Level.SEVERE, debugHeader + Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.bad.zip", esaFile.getAbsolutePath(), e.getMessage()), e);
            return null;
        }
        if (esaAsset.getSubsystemEntry() == null) {
            esaAssetCach.put(esaPath, null);
            log(Level.FINEST, debugHeader + Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.content.no.subsystem.manifest"));
            return null;
        }
        ProvisioningFeatureDefinition fd = esaAsset.getProvisioningFeatureDefinition();
        if (fd.isSupportedFeatureVersion()) {
            // Make sure that this feature applies to our build by re-using the utilities in the product installer
            String appliesToHeader = fd.getHeader("IBM-AppliesTo");
            @SuppressWarnings("rawtypes")
            List productMatchers = SelfExtractor.parseAppliesTo(appliesToHeader);
            wlp.lib.extract.ReturnCode validInstallRC = SelfExtractor.validateProductMatches(Utils.getInstallDir(), productMatchers);
            if (validInstallRC != wlp.lib.extract.ReturnCode.OK) {
                esaAssetCach.put(esaPath, null);
                log(Level.FINEST, debugHeader + validInstallRC.getErrorMessage());
            } else {
                esaAssetCach.put(fd.getSymbolicName(), esaAsset);
                esaAssetCach.put(esaFile.getAbsolutePath(), esaAsset);
                return esaAsset;
            }
        } else {
            esaAssetCach.put(esaPath, null);
            log(Level.FINEST, debugHeader + Messages.PROVISIONER_MESSAGES.getLogMessage("UNSUPPORTED_FEATURE_VERSION", fd.getFeatureName(), fd.getIbmFeatureVersion()));
        }
        return null;
    }

    private InstallInformation createInstallInformation(String esa, String feature, int progress) throws InstallException {
        File esaToInstall = null;
        String installRootFolder = null;
        boolean isTemporary = true;
        // first of all see if it is a URL.
        try {
            URL url = new URL(esa);
            String urlExtrernalForm = url.toExternalForm();
            installRootFolder = urlExtrernalForm.substring(0, urlExtrernalForm.lastIndexOf("/")) + "/";
            if (downloadCach.containsKey(esa)) {
                esaToInstall = downloadCach.get(esa);
            } else {
                fireProgressEvent(InstallProgressEvent.DOWNLOAD, progress, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_DOWNLOADING", url));
                esaToInstall = File.createTempFile("libertyfeature", ".esa");
                InstallUtils.download(url, esaToInstall);
                downloadCach.put(esa, esaToInstall);
            }
        } catch (MalformedURLException e) {
            // It is not a URL

            // clean up
            InstallUtils.delete(esaToInstall);

            // local file
            esaToInstall = new File(esa);
            isTemporary = false;

            // Don't know whether it was separated with / or \ so try both
            int lastSlash = esa.lastIndexOf("/");
            int lastBackSlash = esa.lastIndexOf("\\");
            int lastSeparator = (lastSlash > lastBackSlash) ? lastSlash : lastBackSlash;
            if (lastSeparator != -1) {
                // Keep the \ or / on the end of the string so we just need to append the name when searching for other features
                installRootFolder = esa.substring(0, lastSeparator + 1);
            } else {
                installRootFolder = "";
            }
        } catch (IOException e) {
            if (esaToInstall == null) {
                throw ExceptionUtils.create(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.download.tmpFile", e.getMessage()),
                                            InstallException.BAD_ARGUMENT);
            } else {
                throw ExceptionUtils.create(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.download.esa", esaToInstall, e.getMessage()),
                                            InstallException.BAD_ARGUMENT);
            }
        }

        if (!!!esaToInstall.exists()) {
            throw ExceptionUtils.create(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.file.notexist", esaToInstall),
                                        InstallException.BAD_ARGUMENT);
        }

        if (!!!esaToInstall.isFile()) {
            throw ExceptionUtils.create(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.file.notafile", esaToInstall),
                                        InstallException.BAD_ARGUMENT);
        }

        return new InstallInformation(esaToInstall, feature, installRootFolder, isTemporary);
    }

    private ESAAsset getEsaAsset(String feature, File fromDir, String toExtension) {
        File esaFile = new File(fromDir, feature + ".esa");
        if (esaFile.exists()) {
            ESAAsset esaAsset = createESAAsset(esaFile, toExtension);
            if (esaAsset != null && esaAsset.matchFeature(feature))
                return esaAsset;
        }
        FilenameFilter ff = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".esa");
            }
        };
        File[] esaes = fromDir.listFiles(ff);
        for (File esa : esaes) {
            if (esa.isFile()) {
                ESAAsset esaAsset = createESAAsset(esa, toExtension);
                if (esaAsset != null && esaAsset.matchFeature(feature))
                    return esaAsset;
            }
        }
        return null;
    }

    private ESAAsset getESAAssetFromCached(String feature) {
        ESAAsset esa = esaAssetCach.get(feature);
        if (esa != null)
            return esa;
        for (String k : esaAssetCach.keySet()) {
            if (k.toLowerCase().endsWith("." + feature.toLowerCase())) {
                esa = esaAssetCach.get(k);
                String s = esa.getShortName();
                if (s != null && s.equalsIgnoreCase(feature))
                    return esa;
                s = esa.getProvideFeature();
                if (s != null && s.equals(feature))
                    return esa;
            }
        }
        return null;
    }

    private boolean isFeatureManager() {
        return this.userAgent != null && this.userAgent.contains(InstallConstants.FEATURE_MANAGER);
    }

    private String resolve(InstallInformation installInformation, String toExtension, Set<String> features, ArrayList<InstallAsset> installAssets,
                           int progress, int interval) throws InstallException {
        ESAAsset esa = null;
        ProvisioningFeatureDefinition fd = null;
        try {
            String esaPath = installInformation.esaToInstall.getAbsolutePath();
            esa = esaAssetCach.get(esaPath);
            if (esa == null) {
                esa = new ESAAsset(installInformation.esaToInstall, toExtension, installInformation.isTemporary);
                esaAssetCach.put(esaPath, esa);
            } else {
                esa.setRepoType(toExtension);
            }
            if (esa.getSubsystemEntry() == null) {
                throw ExceptionUtils.create(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.content.no.subsystem.manifest"),
                                            InstallException.BAD_FEATURE_DEFINITION);
            }
            fd = esa.getProvisioningFeatureDefinition();
            if (!fd.isSupportedFeatureVersion()) {
                throw ExceptionUtils.create(Messages.PROVISIONER_MESSAGES.getLogMessage("UNSUPPORTED_FEATURE_VERSION", fd.getFeatureName(), fd.getIbmFeatureVersion()),
                                            InstallException.BAD_FEATURE_DEFINITION);
            }
        } catch (ZipException e) {
            throw ExceptionUtils.create(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.bad.zip", installInformation), e);
        } catch (IOException e) {
            throw ExceptionUtils.create(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.bad.zip", installInformation), e);
        }

        if (installInformation.feature != null) {
            if (!installInformation.feature.equals(fd.getSymbolicName())) {
                String shortName = InstallUtils.getShortName(fd);
                if (shortName == null || !shortName.equalsIgnoreCase(installInformation.feature)) {
                    throw ExceptionUtils.createByKey(InstallException.BAD_FEATURE_DEFINITION, "ERROR_FAILED_TO_RESOLVE_FEATURE_FROM_ESA",
                                                     installInformation.feature, installInformation.esaToInstall.getAbsolutePath());
                }
            }
        }

        if (features.contains(fd.getSymbolicName())) {
            return fd.getSymbolicName();
        }

        // Make sure that this feature applies to our build by re-using the utilities in the product installer
        InstallUtils.validateProductMatches(fd, product.getInstallDir());

        features.add(fd.getSymbolicName());

        Collection<FeatureResource> featureResources = fd.getConstituents(null);
        if (featureResources.size() > 0) {
            int p = progress;
            int i = interval / featureResources.size();
            for (FeatureResource fr : featureResources) {
                p += i;
                SubsystemContentType type = fr.getType();
                if (SubsystemContentType.FEATURE_TYPE == type) {
                    // Look to see if the feature already exists and try to find it in the same place as the current ESA if we don't have it
                    if (!features.contains(fr.getSymbolicName())) {
                        String esaLocation = installInformation.installRootFolder + fr.getSymbolicName() + ".esa";
                        try {
                            resolve(createInstallInformation(esaLocation, fr.getSymbolicName(), progress), toExtension, features, installAssets, p, i);
                        } catch (InstallException e) {
                            if (e.getRc() != InstallException.ALREADY_EXISTS) {
                                throw ExceptionUtils.create(e.getMessage()
                                                            + "\n"
                                                            + Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.missing.feature", fd.getSymbolicName(),
                                                                                                          fr.getSymbolicName()));
                            }
                        }
                    }
                }
            }
        }

        if (!product.isInstalled(esa))
            installAssets.add(esa);
        return fd.getSymbolicName();
    }

    private void resolve(String feature, File fromDir, String toExtension, boolean offlineOnly, Set<String> installedFeatures, Map<String, ESAAsset> esaAssetsMap,
                         ArrayList<InstallAsset> installAssets, ArrayList<String> unresolvedFeatures) throws InstallException {
        ESAAsset esa = null;
        esa = getESAAssetFromCached(feature);
        if (esa == null) {
            esa = getEsaAsset(feature, fromDir, toExtension);
            if (esa == null) {
                if (offlineOnly) {
                    throw ExceptionUtils.createByKey("ERROR_FAILED_TO_RESOLVE_FEATURE_FROM_DIR", feature, fromDir.getAbsolutePath());
                } else {
                    unresolvedFeatures.add(feature);
                    return;
                }
            }
        } else {
            try {
                esa.setRepoType(toExtension);
            } catch (IOException e) {
                throw ExceptionUtils.create(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.bad.zip", esa.getAsset().getAbsoluteFile(), e.getMessage()), e);
            }
        }

        String featureName = esa.getFeatureName();
        if (installedFeatures.contains(featureName))
            return;

        installedFeatures.add(featureName);

        ProvisioningFeatureDefinition fd = esa.getProvisioningFeatureDefinition();
        Collection<FeatureResource> featureResources = fd.getConstituents(null);
        if (featureResources.size() > 0) {
            for (FeatureResource fr : featureResources) {
                SubsystemContentType type = fr.getType();
                if (SubsystemContentType.FEATURE_TYPE == type) {
                    // Look to see if the feature already exists and try to find it in the same place as the current ESA if we don't have it
                    if (!installedFeatures.contains(fr.getSymbolicName())) {
                        try {
                            resolve(fr.getSymbolicName(), fromDir, toExtension, offlineOnly, installedFeatures, esaAssetsMap, installAssets, unresolvedFeatures);
                        } catch (InstallException e) {
                            if (offlineOnly) {
                                throw ExceptionUtils.create(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.missing.feature", fd.getSymbolicName(),
                                                                                                        fr.getSymbolicName()),
                                                            e);
                            } else {
                                unresolvedFeatures.add(feature);
                            }
                        }
                    }
                }
            }
        }

        if (!product.isInstalled(esa))
            installAssets.add(esa);
    }

    private void resolveAutoFeature(Collection<ESAAsset> autoFeatures, ArrayList<InstallAsset> installAssets, Collection<ProvisioningFeatureDefinition> featureDefinitionsToCheck) {
        Collection<ESAAsset> notSatisfied = new HashSet<ESAAsset>();
        for (ESAAsset autoFeature : autoFeatures) {
            if (installAssets.contains(autoFeature)) {
                log(Level.FINEST, "auto feature " + autoFeature.getProvideFeature() + " is already resolved.");
            } else {
                ProvisioningFeatureDefinition pdf = autoFeature.getProvisioningFeatureDefinition();
                if (pdf != null) {
                    if (pdf.isCapabilitySatisfied(featureDefinitionsToCheck)) {
                        log(Level.FINEST, "auto feature " + autoFeature.getProvideFeature() + " will be installed.");
                        featureDefinitionsToCheck.add(autoFeature.getProvisioningFeatureDefinition());
                        installAssets.add(autoFeature);
                    } else {
                        log(Level.FINEST, "auto feature " + autoFeature.getProvideFeature() + " is not satisfied.");
                        notSatisfied.add(autoFeature);
                    }
                }
            }
        }

        if (notSatisfied.isEmpty() || autoFeatures.size() == notSatisfied.size())
            return;

        resolveAutoFeature(notSatisfied, installAssets, featureDefinitionsToCheck);
    }

    private void setUserAgent() {
        String ua = System.getProperty(InstallConstants.UA_PROPERTY_NAME);
        String productVersion = this.product.getProductVersion();
        String productEdition = this.product.getProductEdition();
        if (ua != null && !ua.isEmpty()) {
            ua = String.format(InstallConstants.USER_AGENT, productVersion, productEdition, ua);
        }
        this.userAgent = ua;
    }

    void checkAssetsNotInstalled(Collection<String> assetIds) throws InstallException {

        log(Level.FINEST, "Check following assets whether they were installed or not: " + assetIds);

        if (assetIds == null || assetIds.isEmpty()) {
            throw ExceptionUtils.createByKey("ERROR_ASSETS_LIST_INVALID");
        }

        RepositoryConnectionList loginInfo = new RepositoryConnectionList();
        List<List<RepositoryResource>> resources = null;
        try {
            resources = resolve(assetIds, loginInfo, false);
        } catch (InstallException e) {
            // Should do nothing
            log(Level.FINEST, "checkAssetsNotInstalled() ignore exception: " + e.getMessage(), e);
            log(Level.FINEST, "checkAssetsNotInstalled() cause of exception: " + e.getCause().getMessage());
            return;
        }
        if (isEmpty(resources)) {
            throw ExceptionUtils.createByKey(InstallException.ALREADY_EXISTS, "ASSETS_ALREADY_INSTALLED",
                                             InstallUtils.getShortNames(product.getFeatureDefinitions(), assetIds).toString());
        }
    }
}
