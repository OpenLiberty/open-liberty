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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.ibm.ws.install.InstallConstants.ExistsAction;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.adaptor.ESAAdaptor;
import com.ibm.ws.install.internal.adaptor.FixAdaptor;
import com.ibm.ws.install.internal.adaptor.ServerPackageJarAdaptor;
import com.ibm.ws.install.internal.adaptor.ServicePackageAdaptor;
import com.ibm.ws.install.internal.asset.ESAAsset;
import com.ibm.ws.install.internal.asset.FixAsset;
import com.ibm.ws.install.internal.asset.InstallAsset;
import com.ibm.ws.install.internal.asset.JarAsset;
import com.ibm.ws.install.internal.asset.ServerPackageAsset;
import com.ibm.ws.install.internal.asset.UninstallAsset;
import com.ibm.ws.install.internal.asset.UninstallAsset.UninstallAssetType;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.provisioning.ExtensionConstants;
import com.ibm.ws.repository.connections.RestRepositoryConnectionProxy;

/**
 * This class directs IM operations to the appropriate classes based on asset type
 */
public class Engine {

    private final Product product;

    public Engine(Product product) {
        this.product = product;
    }

    /**
     * Determines which install method to call based on the type of installAsset
     *
     * @param installAsset InstallAsset to install
     * @param filesInstalled List of files to be installed
     * @param featuresToBeInstalled Collection of feature names to install
     * @param existsAction Action to take if asset exists
     * @param executableFiles Set of executable file names
     * @param extattrFiles Extendible attribute files as a set
     * @param downloadDependencies If dependencies should be downloaded
     * @param proxy RestRepositoryConnectionProxy to connect to
     * @param checksumsManager ChecksumsManager for installed files
     * @throws IOException
     * @throws InstallException
     */
    public void install(InstallAsset installAsset, List<File> filesInstalled, Collection<String> featuresToBeInstalled, ExistsAction existsAction,
                        Set<String> executableFiles, Map<String, Set<String>> extattrFiles, boolean downloadDependencies,
                        RestRepositoryConnectionProxy proxy, ChecksumsManager checksumsManager) throws IOException, InstallException {
        if (installAsset.isFeature())
            ESAAdaptor.install(product, (ESAAsset) installAsset, filesInstalled, featuresToBeInstalled, existsAction, executableFiles, extattrFiles, checksumsManager);
        else if (installAsset.isFix())
            FixAdaptor.install(product, (FixAsset) installAsset);
        else if (installAsset.isServerPackage())
            if (installAsset instanceof JarAsset) {
                ServerPackageJarAdaptor.install(product, (JarAsset) installAsset, filesInstalled, downloadDependencies, proxy);
            } else {
                ServicePackageAdaptor.install(product, (ServerPackageAsset) installAsset, filesInstalled, existsAction);
            }
        else if (installAsset.isSample())
            ServerPackageJarAdaptor.install(product, (JarAsset) installAsset, filesInstalled, downloadDependencies, proxy);
        else if (installAsset.isOpenSource())
            ServerPackageJarAdaptor.install(product, (JarAsset) installAsset, filesInstalled, downloadDependencies, proxy);
    }

    /**
     * Determines which install method to call based on the type of uninstallAsset
     *
     * @param uninstallAsset UninstallAsset to uninstall
     * @param checkDependency If dependencies should be checked
     * @param filesRestored Files to be restored
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws InstallException
     */
    public void uninstall(UninstallAsset uninstallAsset, boolean checkDependency,
                          List<File> filesRestored) throws IOException, ParserConfigurationException, SAXException, InstallException {
        if (uninstallAsset.getType().equals(UninstallAssetType.feature)) {
            // Remove the feature contents and metadata
            ESAAdaptor.uninstallFeature(uninstallAsset.getProvisioningFeatureDefinition(), product.getFeatureDefinitions(),
                                        getBaseDir(uninstallAsset.getProvisioningFeatureDefinition()), checkDependency, filesRestored);
        } else if (uninstallAsset.getType().equals(UninstallAssetType.fix)) {
            FixAdaptor.uninstallFix(uninstallAsset.getIFixInfo(), product.getInstallDir(), filesRestored);
        }
        InstallUtils.updateFingerprint(product.getInstallDir());
    }

    private File getBaseDir(ProvisioningFeatureDefinition pd) throws InstallException {
        if (pd.getBundleRepositoryType().equals(ExtensionConstants.USER_EXTENSION)) {
            return product.getUserExtensionDir();
        } else if (pd.getBundleRepositoryType().equals(ExtensionConstants.CORE_EXTENSION)) {
            return product.getInstallDir();
        } else {
            String repoType = pd.getBundleRepositoryType();
            return product.getUserDirExternal(repoType);
        }
    }

    /**
     * Determines which preCheck method to call based on the uninstalLAsset type
     *
     * @param uninstallAsset UninstallAsset to be uninstalled
     * @param checkDependency If dependencies should be checked
     * @throws InstallException
     */
    public void preCheck(UninstallAsset uninstallAsset, boolean checkDependency) throws InstallException {
        if (uninstallAsset.getType().equals(UninstallAssetType.feature)) {
            ESAAdaptor.preCheck(uninstallAsset.getProvisioningFeatureDefinition(), product.getFeatureDefinitions(),
                                getBaseDir(uninstallAsset.getProvisioningFeatureDefinition()), checkDependency);
        } else if (uninstallAsset.getType().equals(UninstallAssetType.fix)) {
            FixAdaptor.preCheck(uninstallAsset.getIFixInfo(), product.getInstallDir());
        }
    }

}
