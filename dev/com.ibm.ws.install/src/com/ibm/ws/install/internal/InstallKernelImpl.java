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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallConstants.AssetType;
import com.ibm.ws.install.InstallConstants.DownloadOption;
import com.ibm.ws.install.InstallConstants.ExistsAction;
import com.ibm.ws.install.InstallEventListener;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernel;
import com.ibm.ws.install.InstallKernelInteractive;
import com.ibm.ws.install.InstallLicense;
import com.ibm.ws.install.InstallProgressEvent;
import com.ibm.ws.install.InstalledFeature;
import com.ibm.ws.install.InstalledFeatureCollection;
import com.ibm.ws.install.ReapplyFixException;
import com.ibm.ws.install.RepositoryConfigUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.asset.ServerAsset;
import com.ibm.ws.install.internal.asset.ServerPackageAsset;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.RestRepositoryConnectionProxy;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;

/**
 * This class contains the implementation of Liberty installation.
 */
public class InstallKernelImpl implements InstallKernel, InstallKernelInteractive {

    private static final String FEATURE = "FEATURE";
    private static final String IFIX = "IFIX";

    private Director director = null;

    public InstallKernelImpl() {
        this.director = new Director();
    }

    public InstallKernelImpl(File installRoot) {
        this.director = new Director(installRoot);
    }

    @Override
    public void setUserAgent(String kernelUser) {
        this.director.setUserAgent(kernelUser);
    }

    // For testing only
    public void setRepositoryUrl(String repositoryUrl) {
        this.director.setRepositoryUrl(repositoryUrl);
    }

    @Override
    public void setRepositoryProperties(Properties repoProperties) {
        this.director.setRepositoryProperties(repoProperties);
    }

    @Override
    public void addListener(InstallEventListener listener, String notificationType) {
        this.director.addListener(listener, notificationType);
    }

    @Override
    public void removeListener(InstallEventListener listener) {
        this.director.removeListener(listener);
    }

    protected void setLocale(Locale locale) {
        Messages.setLocale(locale);
    }

    public Collection<String> install(String assetType, Collection<String> ids, String toExtension,
                                      boolean acceptLicense, ExistsAction existsAction, String userId, String password) throws InstallException {
        this.director.fireProgressEvent(InstallProgressEvent.BEGIN, 0,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_STARTING_INSTALL"));
        Collection<String> installed;
        try {
            this.director.refresh();
            if (assetType.equalsIgnoreCase(FEATURE)) {
                this.director.installFeatures(ids, toExtension, acceptLicense, userId, password);
            } else if (assetType.equalsIgnoreCase(IFIX)) {
                this.director.installFixes(ids, userId, password);
            } else {
                InstallException e = new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNSUPPORTED_ASSETTYPE", assetType));
                this.director.log(Level.SEVERE, e.getMessage());
                this.director.log(Level.FINEST, null, e);
                throw e;
            }
            this.director.install(existsAction, false, false);
            installed = this.director.getInstalledFeatureNames();
            this.director.reapplyFixIfNeeded();
        } catch (ReapplyFixException e) {
            throw e;
        } catch (InstallException e) {
            // TODO: consider to remove installed files
            throw e;
        } finally {
            this.director.setScriptsPermission(InstallProgressEvent.POST_INSTALL);
            this.director.cleanUp();
        }

        this.director.fireProgressEvent(InstallProgressEvent.COMPLETE, 100,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_COMPLETED_INSTALL"));
        return installed;
    }

    @Override
    public Collection<String> installFeature(String featureId, String toExtension, boolean acceptLicense,
                                             ExistsAction existsAction) throws InstallException {
        return installFeature(featureId, toExtension, acceptLicense, existsAction, null, null);
    }

    @Override
    public Collection<String> installFeature(String featureId, String toExtension, boolean acceptLicense,
                                             ExistsAction existsAction, String userId, String password) throws InstallException {
        ArrayList<String> featureIds = new ArrayList<String>(1);
        if (featureId != null)
            featureIds.add(featureId);
        return installFeature(featureIds, toExtension, acceptLicense, existsAction, userId, password);
    }

    @Override
    public Collection<String> installFeature(Collection<String> featureIds, String toExtension, boolean acceptLicense,
                                             ExistsAction existsAction) throws InstallException {
        return installFeature(featureIds, toExtension, acceptLicense, existsAction, null, null);
    }

    public Collection<String> installLocalFeature(String esaLocation, String toExtension, boolean acceptLicense,
                                                  ExistsAction existsAction) throws InstallException {
        this.director.log(Level.FINE,
                          Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_INSTALL_FEATURES", esaLocation));
        this.director.fireProgressEvent(InstallProgressEvent.BEGIN, 0,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_STARTING_INSTALL"));
        Collection<String> installed;
        try {
            this.director.refresh();
            this.director.installFeature(esaLocation, toExtension, acceptLicense);
            this.director.install(existsAction, false, false);
            installed = this.director.getInstalledFeatureNames();
        } catch (InstallException e) {
            throw e;
        } finally {
            this.director.setScriptsPermission(InstallProgressEvent.POST_INSTALL);
            this.director.cleanUp();
        }
        this.director.fireProgressEvent(InstallProgressEvent.COMPLETE, 100,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_COMPLETED_INSTALL"));
        return installed;
    }

    @Override
    public Collection<String> installFeature(Collection<String> featureIds, File fromDir, String toExtension,
                                             boolean acceptLicense, ExistsAction existsAction, boolean offlineOnly) throws InstallException {
        this.director.log(Level.FINE,
                          Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_INSTALL_FEATURES", featureIds));
        this.director.fireProgressEvent(InstallProgressEvent.BEGIN, 0,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_STARTING_INSTALL"));
        Collection<String> installed;
        try {
            this.director.refresh();
            this.director.installFeature(featureIds, fromDir, toExtension, acceptLicense, offlineOnly);
            this.director.install(existsAction, false, false);
            installed = this.director.getInstalledFeatureNames();
        } catch (InstallException e) {
            throw e;
        } finally {
            this.director.setScriptsPermission(InstallProgressEvent.POST_INSTALL);
            this.director.cleanUp();
        }
        this.director.fireProgressEvent(InstallProgressEvent.COMPLETE, 100,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_COMPLETED_INSTALL"));
        return installed;
    }

    @Override
    public Collection<String> installFeature(Collection<String> featureIds, String toExtension, boolean acceptLicense,
                                             ExistsAction existsAction, String userId, String password) throws InstallException {
        this.director.log(Level.FINE,
                          Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_INSTALL_FEATURES", featureIds));
        return install(FEATURE, featureIds, toExtension, acceptLicense, existsAction, userId, password);
    }

    @Override
    public ServerPackageAsset deployServerPackage(File archiveFile, String toExtension, boolean downloadDependencies) throws InstallException {
        this.director.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_DEPLOY_FILE", archiveFile));
        this.director.fireProgressEvent(InstallProgressEvent.BEGIN, 0,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_STARTING_DEPLOY"));
        ServerPackageAsset spa = null;
        try {
            this.director.refresh();
            spa = this.director.resolveServerPackage(archiveFile);

            // Check if any server from the server packages already exists. If
            // so then do not attempt to deploy the server.
            for (ServerAsset server : spa.getServers()) {
                if (InstallUtils.serverExists(server.getServerName())) {
                    throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage(
                                                                                              "ERROR_SERVER_PACKAGE_SERVER_ALREADY_INSTALLED", archiveFile.getAbsolutePath(),
                                                                                              server.getServerName()), InstallException.ALREADY_EXISTS);
                }

            }
            this.director.install(ExistsAction.replace, true, downloadDependencies);
        } catch (InstallException ie) {
            throw ie;
        } finally {
            this.director.setScriptsPermission(InstallProgressEvent.POST_INSTALL);
            this.director.cleanUp();
        }
        this.director.fireProgressEvent(InstallProgressEvent.COMPLETE, 100,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_COMPLETED_DEPLOY"));
        return spa;
    }

    @Override
    public Collection<String> getServerFeaturesToInstall(Set<ServerAsset> servers, boolean offlineOnly) throws InstallException, IOException {
        this.director.fireProgressEvent(InstallProgressEvent.RESOLVE, 0,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_CHECKING_MISSING_SERVER_FEATURES"));
        return this.director.getServerFeaturesToInstall(servers, offlineOnly);
    }

    @Override
    public void installFix(String fixId) throws InstallException {
        installFix(fixId, null, null);
    }

    @Override
    public void installFix(String fixId, String userId, String password) throws InstallException {
        ArrayList<String> fixIds = new ArrayList<String>(1);
        fixIds.add(fixId);
        this.director.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_INSTALL_FIXES", fixIds));
        install(IFIX, fixIds, null, true, ExistsAction.replace, userId, password);
    }

    private void uninstall(String assetType, Collection<String> ids, boolean force) throws InstallException {
        this.director.fireProgressEvent(InstallProgressEvent.BEGIN, 0,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_STARTING_UNINSTALL"));

        try {
            this.director.refresh();
            if (assetType.equalsIgnoreCase(FEATURE)) {
                this.director.uninstall(ids, force);
            } else if (assetType.equalsIgnoreCase(IFIX)) {
                this.director.uninstallFix(ids);
                this.director.uninstall(true, null, null);
            } else {
                InstallException e = ExceptionUtils.createByKey("ERROR_UNSUPPORTED_ASSETTYPE", assetType);
                throw e;
            }
        } catch (InstallException e) {
            throw e;
        } finally {
            this.director.setScriptsPermission(InstallProgressEvent.POST_UNINSTALL);
            this.director.cleanUp();
        }
        this.director.fireProgressEvent(InstallProgressEvent.COMPLETE, 100,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_COMPLETED_UNINSTALL"));
    }

    @Override
    public void uninstallFeature(String featureId, boolean force) throws InstallException {
        ArrayList<String> featureIds = new ArrayList<String>(1);
        if (featureId != null)
            featureIds.add(featureId);
        uninstall(FEATURE, featureIds, force);
    }

    @Override
    public void uninstallFeature(Collection<String> featureIds) throws InstallException {
        uninstall(FEATURE, featureIds, false);
    }

    @Override
    public void uninstallFix(String fixId) throws InstallException {
        ArrayList<String> fixIds = new ArrayList<String>(1);
        if (fixId != null)
            fixIds.add(fixId);
        uninstall(IFIX, fixIds, false);
    }

    @Override
    public void uninstallFix(Collection<String> fixIds) throws InstallException {
        this.director.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_UNINSTALL_FIXES", fixIds));
        uninstall(IFIX, fixIds, false);
    }

    public Set<InstallLicense> getLocalFeatureLicense(String esaLocation, Locale locale) throws InstallException {
        return this.director.getFeatureLicense(esaLocation, locale);
    }

    public Set<InstallLicense> getServerPackageFeatureLicense(File archive, boolean offlineOnly, Locale locale) throws InstallException {
        return this.director.getServerPackageFeatureLicense(archive, offlineOnly, locale);
    }

    public Set<InstallLicense> getServerFeatureLicense(File archive, boolean offlineOnly, Locale locale) throws InstallException, IOException {
        return this.director.getServerFeatureLicense(archive, offlineOnly, locale);
    }

    public Set<InstallLicense> getFeatureLicense(Collection<String> featureIds, File fromDir, String toExtension,
                                                 boolean offlineOnly, Locale locale) throws InstallException {
        return this.director.getFeatureLicense(featureIds, fromDir, toExtension, offlineOnly, locale);
    }

    @Override
    public Set<InstallLicense> getFeatureLicense(String featureId, Locale locale) throws InstallException {
        return getFeatureLicense(featureId, locale, null, null);
    }

    @Override
    public Set<InstallLicense> getFeatureLicense(String featureId, Locale locale, String userId, String password) throws InstallException {
        ArrayList<String> featureIds = new ArrayList<String>(1);
        if (featureId != null)
            featureIds.add(featureId);
        return getFeatureLicense(featureIds, locale, userId, password);
    }

    @Override
    public Set<InstallLicense> getFeatureLicense(Collection<String> featureIds, Locale locale) throws InstallException {
        return getFeatureLicense(featureIds, locale, null, null);
    }

    @Override
    public Set<InstallLicense> getFeatureLicense(Collection<String> featureIds, Locale locale, String userId,
                                                 String password) throws InstallException {
        return this.director.getFeatureLicense(featureIds, locale, userId, password);
    }

    @Override
    public Set<String> getInstalledLicense() {
        this.director.refresh();
        return this.director.getInstalledLicense();
    }

    @Override
    public Set<String> getInstalledFeatures(String installedBy) {
        this.director.refresh();
        return this.director.getInstalledFeatures(installedBy);
    }

    @Override
    public void enableConsoleLog(Level level) {
        boolean verbose = (Level.FINEST.equals(level));
        this.director.enableConsoleLog(level, verbose);
    }

    public Map<ResourceType, List<RepositoryResource>> queryAssets(String searchStr, AssetType type) throws InstallException {
        return this.director.queryAssets(searchStr, type);
    }

    public List<EsaResource> queryFeatures(String searchStr) throws InstallException {
        return this.director.queryFeatures(searchStr);
    }

    public Collection<String> downloadFeatureFeatureManager(Set<String> featureIdSet, File toDir,
                                                            DownloadOption downloadOption, ExistsAction action, String user, String password) throws InstallException {
        this.director.fireProgressEvent(InstallProgressEvent.BEGIN, 0,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_STARTING_INSTALL"));
        Collection<String> installed;
        try {
            this.director.refresh();
            installed = this.director.downloadFeatureFeatureManager(featureIdSet, toDir, downloadOption, action, user,
                                                                    password);
        } catch (InstallException e) {
            throw e;
        } finally {
            this.director.cleanUp();
        }
        this.director.fireProgressEvent(InstallProgressEvent.COMPLETE, 100,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_COMPLETED_DOWNLOAD"));
        return installed;
    }

    public Map<String, Collection<String>> downloadAssetsInstallUtility(Set<String> assetIdSet, File toDir,
                                                                        DownloadOption downloadOption, String user, String password, boolean isOverride) throws InstallException {
        this.director.fireProgressEvent(InstallProgressEvent.BEGIN, 0,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_STARTING_DOWNLOAD"));
        Map<String, Collection<String>> installed;
        try {
            this.director.refresh();
            installed = this.director.downloadAssetsInstallUtility(assetIdSet, toDir, downloadOption, user, password,
                                                                   isOverride);
        } catch (InstallException e) {
            throw e;
        } finally {
            this.director.cleanUp();
        }
        this.director.fireProgressEvent(InstallProgressEvent.COMPLETE, 100,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_COMPLETED_DOWNLOAD"));
        return installed;
    }

    @Override
    public void uninstallFeaturesByProductId(String productId) throws InstallException {
        uninstallFeaturesByProductId(productId, null);
    }

    @Override
    public void uninstallFeaturesByProductId(String productId, Collection<File> toBeDeleted) throws InstallException {
        uninstallFeaturesByProductId(productId, toBeDeleted, false);
    }

    @Override
    public void uninstallProductFeatures(String productId, Collection<File> toBeDeleted) throws InstallException {
        uninstallFeaturesByProductId(productId, toBeDeleted, true);
    }

    public void uninstallFeaturesByProductId(String productId, Collection<File> toBeDeleted,
                                             boolean exceptPlatfromFeatuers) throws InstallException {
        this.director.fireProgressEvent(InstallProgressEvent.BEGIN, 0,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_STARTING_UNINSTALL"));
        try {
            this.director.refresh();
            this.director.uninstallFeaturesByProductId(productId, exceptPlatfromFeatuers);
            this.director.uninstall(false, productId, toBeDeleted);
        } catch (InstallException e) {
            throw e;
        } finally {
            this.director.cleanUp();
        }
        this.director.fireProgressEvent(InstallProgressEvent.COMPLETE, 100,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_COMPLETED_UNINSTALL"));
    }

    @Override
    public Map<String, InstalledFeature> getInstalledFeatures() {
        this.director.refresh();
        return this.director.getInstalledCoreFeatures();
    }

    @Override
    public Map<String, InstalledFeatureCollection> getInstalledFeatureCollections() {
        this.director.refresh();
        return this.director.getInstalledFeatureCollections();
    }

    @Override
    public void uninstallFeaturePrereqChecking(String featureId, boolean allowUserFeatureUninstall, boolean force) throws InstallException {
        Collection<String> featureIds = new ArrayList<String>();
        featureIds.add(featureId);
        this.director.uninstallFeaturesPrereqChecking(featureIds, allowUserFeatureUninstall, force);
    }

    @Override
    public void uninstallFeaturePrereqChecking(Collection<String> featureIds) throws InstallException {
        this.director.uninstallFeaturesPrereqChecking(featureIds, true, false);
    }

    @Override
    public void uninstallCoreFeaturePrereqChecking(Collection<String> featureIds) throws InstallException {
        this.director.uninstallFeaturesPrereqChecking(featureIds, false, false);
    }

    @Override
    public Map<String, Collection<String>> installAsset(Collection<String> assetIds, RepositoryConnectionList loginInfo,
                                                        String proxyHost, String proxyPort, String proxyUser, String proxyPwd) throws InstallException {
        return installAsset(assetIds, null, loginInfo, proxyHost, proxyPort, proxyUser, proxyPwd);
    }

    @Override
    public Map<String, Collection<String>> installAsset(Collection<String> assetIds, File fromDir,
                                                        RepositoryConnectionList loginInfo, String proxyHost, String proxyPort, String proxyUser,
                                                        String proxyPwd) throws InstallException {
        return installAsset(assetIds, fromDir, loginInfo, proxyHost, proxyPort, proxyUser, proxyPwd, false);
    }

    @Override
    public Map<String, Collection<String>> installAsset(Collection<String> assetIds, File fromDir,
                                                        RepositoryConnectionList loginInfo, String proxyHost, String proxyPort, String proxyUser, String proxyPwd,
                                                        boolean downloadDependencies) throws InstallException {
        this.director.fireProgressEvent(InstallProgressEvent.BEGIN, 0,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_STARTING_INSTALL"));
        Map<String, Collection<String>> installed;
        try {
            this.director.refresh();
            RepositoryConfigUtils.setProxyAuthenticator(proxyHost, proxyPort, proxyUser, proxyPwd);
            try {
                this.director.installAssets(assetIds, loginInfo);
            } catch (InstallException e) {
                if (e.getRc() != InstallException.ALREADY_EXISTS && fromDir != null)
                    this.director.installAssets(assetIds, fromDir, loginInfo);
                else
                    throw e;
            }
            this.director.checkResources();
            this.director.install(ExistsAction.replace, true, downloadDependencies);
            installed = this.director.getInstalledAssetNames();
            this.director.reapplyFixIfNeeded();
        } catch (ReapplyFixException e) {
            throw e;
        } catch (InstallException e) {
            throw e;
        } finally {
            this.director.setScriptsPermission(InstallProgressEvent.POST_INSTALL);
            this.director.cleanUp();
        }

        this.director.fireProgressEvent(InstallProgressEvent.COMPLETE, 100,
                                        Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_COMPLETED_INSTALL"));
        return installed;
    }

    @Override
    public void resolve(Collection<String> assetIds, boolean download) throws InstallException {
        this.director.refresh();
        this.director.resolve(assetIds, download);
    }

    @Override
    public void resolve(String feature, File esaFile, String toExtension) throws InstallException {
        this.director.refresh();
        this.director.resolve(feature, esaFile, toExtension);
    }

    @Override
    public void checkResources() throws InstallException {
        this.director.checkResources();
    }

    @Override
    public Set<InstallLicense> getFeatureLicense(Locale locale) throws InstallException {
        return this.director.getFeatureLicense(locale);
    }

    @Override
    public Collection<String> getSampleLicense(Locale locale) throws InstallException {
        return this.director.getSampleLicense(locale);
    }

    @Override
    public Collection<String> getSamplesOrOpenSources() {
        return this.director.getSamplesOrOpenSources();
    }

    @Override
    public int getInstallResourcesSize() {
        return this.director.getInstallResourcesSize();
    }

    @Override
    public int getLocalInstallAssetsSize() {
        return this.director.getLocalInstallAssetsSize();
    }

    @Override
    public int getPublicInstallResourcesSize() {
        return this.director.getPublicInstallResourcesSize();
    }

    @Override
    public int getPublicLocalInstallAssetsSize() {
        return this.director.getPublicLocalInstallAssetsSize();
    }

    @Override
    public Map<String, Collection<String>> install(String toExtension, boolean rollbackAll,
                                                   boolean downloadDependencies) throws InstallException {
        Map<String, Collection<String>> installed;
        try {
            this.director.downloadAssets(toExtension);
            this.director.log(Level.FINEST,
                              "install(String toExtension, boolean rollbackAll,boolean downloadDependencies) - " + InstallConstants.IGNORE_FILE_PROPERTY
                                            + ":" + InstallConstants.IGNORE_FILE_OPTION);
            if (Boolean.valueOf(InstallConstants.IGNORE_FILE_OPTION)) {
                this.director.install(ExistsAction.ignore, rollbackAll, downloadDependencies);
            } else {
                this.director.install(ExistsAction.replace, rollbackAll, downloadDependencies);
            }

            installed = this.director.getInstalledAssetNames();
            this.director.reapplyFixIfNeeded();
        } finally {
            this.director.setScriptsPermission(InstallProgressEvent.POST_INSTALL);
            this.director.cleanUp();
        }
        return installed;
    }

    @Override
    public RepositoryConnectionList getLoginInfo() throws InstallException {
        return this.director.getRepositoryConnectionList();
    }

    @Override
    public void setLoginInfo(RepositoryConnectionList loginInfo) {
        this.director.setLoginInfo(loginInfo);
    }

    @Override
    public void setProxy(RestRepositoryConnectionProxy proxy) {
        this.director.setProxy(proxy);
    }

    @Override
    public void setFirePublicAssetOnly(boolean firePublicAssetOnly) {
        this.director.setFirePublicAssetOnly(firePublicAssetOnly);
    }

    @Override
    public boolean resolveExistingAssetsFromDirectoryRepo(Collection<String> featureNames, File repoDir,
                                                          boolean isOverwrite) throws InstallException {
        return this.director.resolveExistingAssetsFromDirectoryRepo(featureNames, repoDir, isOverwrite);
    }

    @Override
    public void checkAssetsNotInstalled(Collection<String> assetIds) throws InstallException {
        this.director.checkAssetsNotInstalled(assetIds);
    }
}
