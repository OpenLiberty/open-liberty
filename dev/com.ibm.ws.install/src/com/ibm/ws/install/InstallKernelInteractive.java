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
package com.ibm.ws.install;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.ibm.ws.install.internal.asset.ServerAsset;
import com.ibm.ws.install.internal.asset.ServerPackageAsset;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.RestRepositoryConnectionProxy;

/**
 * This interface provides the APIs for the install clients to perform Liberty installation interactively.
 */
public interface InstallKernelInteractive {

    /**
     * Sets the User Agent.
     *
     * @param kernelUser the agent
     */
    public void setUserAgent(String kernelUser);

    /**
     * Sets the repository properties
     *
     * @param repoProperties the repository properties
     */
    public void setRepositoryProperties(Properties repoProperties);

    /**
     * Gets the installed License as a Set of Strings
     *
     * @return the License Set
     */
    public Set<String> getInstalledLicense();

    /**
     * Adds a listener with a given notification type
     *
     * @param listener the listener
     * @param notificationType the notification type
     */
    public void addListener(InstallEventListener listener, String notificationType);

    /**
     * Removes the specifies listener
     *
     * @param listener the listener
     */
    public void removeListener(InstallEventListener listener);

    /**
     * Resolves assetIds into a map of installation resources.
     *
     * @param assetIds the asset Ids
     * @param download if assets should be downloaded or not
     * @throws InstallException
     */
    public void resolve(Collection<String> assetIds, boolean download) throws InstallException;

    /**
     * Resolves a feature from a local file.
     *
     * @param feature feature to resolve
     * @param esaFile file where esa is
     * @param toExtension location of a product extension
     * @throws InstallException
     */
    public void resolve(String feature, File esaFile, String toExtension) throws InstallException;

    /**
     * Resolves features and returns true if the class installResources is not empty
     *
     * @param featureNames the features
     * @param repoDir where the features exist
     * @param isOverwrite if the features will be overwritten or not
     * @return true if there are features to install
     * @throws InstallException
     */
    public boolean resolveExistingAssetsFromDirectoryRepo(Collection<String> featureNames, File repoDir, boolean isOverwrite) throws InstallException;

    /**
     * Checks that resources in installResources are valid.
     *
     * @throws InstallException
     */
    public void checkResources() throws InstallException;

    /**
     *
     * @param locale Locale license should be generated for
     * @return set of install licenses
     * @throws InstallException
     */
    public Set<InstallLicense> getFeatureLicense(Locale locale) throws InstallException;

    /**
     *
     * @param locale Locale license should be generated for
     * @return set of sample licenses as Strings
     * @throws InstallException
     */
    public Collection<String> getSampleLicense(Locale locale) throws InstallException;

    /**
     * Returns a collection of sample and open source resources from installResources.
     *
     * @return String collection of resources
     */
    public Collection<String> getSamplesOrOpenSources();

    /**
     *
     * @return
     */
    public int getInstallResourcesSize();

    /**
     *
     * @return
     */
    public int getLocalInstallAssetsSize();

    /**
     *
     * @return
     */
    public int getPublicInstallResourcesSize();

    /**
     *
     * @return
     */
    public int getPublicLocalInstallAssetsSize();

    /**
     * Installs assets
     *
     * @param toExtension location of a product extension
     * @param rollbackAll if assets should be rolled back
     * @param downloadDependencies if any dependencies should be downloaded
     * @return map of asset types to a collection of installed assets as Strings of that type
     * @throws InstallException
     */
    public Map<String, Collection<String>> install(String toExtension, boolean rollbackAll, boolean downloadDependencies) throws InstallException;

    /**
     *
     * @return
     * @throws InstallException
     */
    public RepositoryConnectionList getLoginInfo() throws InstallException;

    /**
     *
     * @param loginInfo
     */
    public void setLoginInfo(RepositoryConnectionList loginInfo);

    /**
     *
     * @param proxy
     */
    public void setProxy(RestRepositoryConnectionProxy proxy);

    /**
     *
     * @param firePublicAssetOnly
     */
    public void setFirePublicAssetOnly(boolean firePublicAssetOnly);

    /**
     *
     * @param servers set of ServerAssets
     * @param offlineOnly if features should only be acquired offline
     * @return Collection of server features as Strings
     * @throws InstallException
     * @throws IOException
     */
    public Collection<String> getServerFeaturesToInstall(Set<ServerAsset> servers, boolean offlineOnly) throws InstallException, IOException;

    /**
     * Deploys the server package given by an archive file
     *
     * @param archiveFile The archive file to deploy
     * @param toExtension location of a product extension
     * @param downloadDependencies if dependencies should be downloaded
     * @return The ServerPackageAsset that is deployed
     * @throws InstallException
     */
    public ServerPackageAsset deployServerPackage(File archiveFile, String toExtension, boolean downloadDependencies) throws InstallException;

    /**
     * Checks if the assets are installed. If so throws an Install Exception.
     *
     * @param assetIds Collection of assetIds as String
     * @throws InstallException
     */
    public void checkAssetsNotInstalled(Collection<String> assetIds) throws InstallException;
}
