/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
package com.ibm.ws.install.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallProgressEvent;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.adaptor.ESAAdaptor;
import com.ibm.ws.install.internal.adaptor.FixAdaptor;
import com.ibm.ws.install.internal.asset.UninstallAsset;
import com.ibm.ws.install.internal.asset.UninstallAsset.UninstallAssetType;
import com.ibm.ws.kernel.feature.internal.generator.ManifestFileProcessor;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.product.utility.extension.ifix.xml.Problem;

class UninstallDirector extends AbstractDirector {

    private final Engine engine;

    private FeatureDependencyChecker dependencyChecker;
    private FixDependencyChecker fixDependencyChecker;

    private List<UninstallAsset> uninstallAssets;

    private boolean setScriptsPermission = false;

    UninstallDirector(Product product, Engine engine, EventManager eventManager, Logger logger) {
        super(product, eventManager, logger);
        this.engine = engine;
    }

    void cleanUp() {
        uninstallAssets = null;
    }

    /**
     * Creates array and calls method below
     *
     * @param checkDependency if uninstall should check for dependencies
     * @param productId       product id to uninstall
     * @param toBeDeleted     Collection of files to uninstall
     * @throws InstallException
     */
    void uninstall(boolean checkDependency, String productId, Collection<File> toBeDeleted) throws InstallException {
        String[] productIds = new String[1];
        productIds[0] = productId;
        uninstall(checkDependency, productIds, toBeDeleted);
    }

    void retrieveUninstallFileList(UninstallAsset uninstallAsset, boolean checkDependency, Map<String, ProvisioningFeatureDefinition> features) throws InstallException {

        if (uninstallAsset.getType().equals(UninstallAssetType.feature) && uninstallAsset.getFeatureFileList().isEmpty()) {

            ProvisioningFeatureDefinition featureDef = uninstallAsset.getProvisioningFeatureDefinition();
            File baseDir = engine.getBaseDir(featureDef);
            uninstallAsset.setFeaturePath(ESAAdaptor.getFeaturePath(featureDef, baseDir));
            uninstallAsset.setFeatureFileList(ESAAdaptor.determineFilesToBeDeleted(featureDef,
                                                                                   features,
                                                                                   baseDir,
                                                                                   uninstallAsset.getFeaturePath(), checkDependency,
                                                                                   uninstallAsset.getFixUpdatesFeature()));
        }
    }

    /**
     * Uninstalls product depending on dependencies
     *
     * @param checkDependency if uninstall should check for dependencies
     * @param productIds      product ids to uninstall
     * @param toBeDeleted     Collection of files to uninstall
     * @throws InstallException
     */
    void uninstall(boolean checkDependency, String[] productIds, Collection<File> toBeDeleted) throws InstallException {
        if (uninstallAssets.isEmpty())
            return;

        //get all installed features
        Map<String, ProvisioningFeatureDefinition> features = product.getFeatureDefinitions();

        // Run file checking only on Windows
        if (InstallUtils.isWindows) {
            // check any file is locked
            fireProgressEvent(InstallProgressEvent.CHECK, 10, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_CHECKING"));
            for (UninstallAsset uninstallAsset : uninstallAssets) {
                retrieveUninstallFileList(uninstallAsset, checkDependency, features);
                engine.preCheck(uninstallAsset);
            }
            if (toBeDeleted != null) {
                for (File f : toBeDeleted) {
                    for (String productId : productIds) {
                        InstallUtils.isFileLocked("ERROR_UNINSTALL_PRODUCT_FILE_LOCKED", productId, f);
                    }
                }
            }
        }

        // proceed to uninstall
        int progress = 20;
        int interval = 70 / uninstallAssets.size();
        List<File> filesRestored = new ArrayList<File>();
        for (UninstallAsset uninstallAsset : uninstallAssets) {
            fireProgressEvent(InstallProgressEvent.UNINSTALL, progress, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_UNINSTALLING", uninstallAsset.getName()));
            progress += interval;
            try {
                if (!InstallUtils.isWindows) {
                    retrieveUninstallFileList(uninstallAsset, checkDependency, features);
                }
                engine.uninstall(uninstallAsset, checkDependency, filesRestored);
                log(Level.FINE, uninstallAsset.uninstalledLogMsg());
            } catch (IOException e) {
                throw ExceptionUtils.create(e);
            } catch (Exception e) {
                String errorMsg = uninstallAsset.getType().equals(UninstallAssetType.feature) ? Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNINSTALL_FEATURE_INVALID_META_DATA",
                                                                                                                                               uninstallAsset.getName()) : Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNINSTALL_FIX_INVALID_META_DATA",
                                                                                                                                                                                                                          uninstallAsset.getName());
                throw ExceptionUtils.create(errorMsg, e);
            }
        }
        checkSetScriptsPermission(filesRestored);
        if (toBeDeleted != null) {
            for (File f : toBeDeleted) {
                if (f.isFile())
                    InstallUtils.delete(f);
                else if (f.isDirectory())
                    InstallUtils.deleteDirectory(f);
            }
        }
    }

    void uninstall(Collection<String> ids, boolean force) throws InstallException {
        Collection<ProvisioningFeatureDefinition> installedFeatureDefinitions = product.getAllFeatureDefinitions().values();
        Collection<String> featureNames = new ArrayList<String>();
        featureNames.addAll(ids);
        Collection<String> installFeatureRequiredFeatures = getInstallFeatureRequiredFeatures(ids);
        if (!!!installFeatureRequiredFeatures.isEmpty()) {
            for (String featureName : featureNames) {
                ProvisioningFeatureDefinition pd = getProvisioningFeatureDefinition(installedFeatureDefinitions, featureName);
                if (installFeatureRequiredFeatures.contains(pd.getSymbolicName())) {
                    installFeatureRequiredFeatures.remove(pd.getSymbolicName());
                }
            }
        }
        featureNames.addAll(installFeatureRequiredFeatures);
        uninstallFeatures(featureNames, getInstallFeatures(ids), force);
        uninstall(true, (String) null, null);
        uninstallInternalAndAutoFeatures(featureNames, installedFeatureDefinitions, null);
        uninstall(true, (String) null, null);
    }

    /**
     * Initialize the feature uninstallation and perform feature dependency check to make sure
     * that there is no other features still require the uninstalling features.
     * The uninstalling features will be uninstalled according to the order which is determined by
     * the dependency checking.
     *
     * @param featureNames a list of the feature names and feature symbolic names to uninstall
     * @throws InstallException if there is a feature not installed or
     *                              there is another feature still requires the uninstalling features.
     */
    void uninstallFeatures(Collection<String> featureNames, Collection<String> uninstallInstallFeatures, boolean force) {
        product.refresh();
        Collection<ProvisioningFeatureDefinition> installedFeatureDefinitions = product.getAllFeatureDefinitions().values();
        Collection<ProvisioningFeatureDefinition> uninstallFeatures = getProvisioningFeatureDefinition(installedFeatureDefinitions, featureNames);
        uninstallAssets = new ArrayList<UninstallAsset>();
        for (ProvisioningFeatureDefinition uninstallFeature : uninstallFeatures) {
            if (!contains(uninstallFeature)) {
                log(Level.FINEST, "Feature is going to be uninstalled : " + uninstallFeature.getSymbolicName());
                uninstallAssets.add(new UninstallAsset(uninstallFeature));
            }
        }
        Map<UninstallAsset, String> nonUninstallableAssets = getNotUninstallableAssets(uninstallAssets, installedFeatureDefinitions,
                                                                                       uninstallInstallFeatures, false);

        if (!force) {
            uninstallAssets = removeUninstallableAssets(uninstallAssets, nonUninstallableAssets.keySet());
        }

        dependencyChecker = new FeatureDependencyChecker();
        // Determine the uninstall order
        uninstallAssets = dependencyChecker.determineOrder(uninstallAssets);
    }

    boolean contains(ProvisioningFeatureDefinition pfd) {
        for (UninstallAsset ua : uninstallAssets) {
            ProvisioningFeatureDefinition uaPfd = ua.getProvisioningFeatureDefinition();
            if (uaPfd != null && uaPfd.getSymbolicName().equals(pfd.getSymbolicName()))
                return true;
        }
        return false;
    }

    /**
     * Creates array and calls method below
     *
     * @param productId              product id to uninstall
     * @param exceptPlatfromFeatuers If platform features should be ignored
     * @throws InstallException
     */
    void uninstallFeaturesByProductId(String productId, boolean exceptPlatfromFeatuers) throws InstallException {
        String[] productIds = new String[1];
        productIds[0] = productId;
        uninstallFeaturesByProductId(productIds, exceptPlatfromFeatuers);
    }

    /**
     * Uninstalls features by product id
     *
     * @param productIds             product ids to uninstall
     * @param exceptPlatfromFeatuers If platform features should be ignored
     * @throws InstallException
     */
    void uninstallFeaturesByProductId(String[] productIds, boolean exceptPlatfromFeatuers) throws InstallException {
        fireProgressEvent(InstallProgressEvent.CHECK, 1, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_CHECKING"));
        Map<String, ProvisioningFeatureDefinition> installedFeatures = exceptPlatfromFeatuers ? product.getAllCoreFeatureDefinitionsExceptPlatform() : product.getAllCoreFeatureDefinitions();
        uninstallAssets = new ArrayList<UninstallAsset>();
        for (ProvisioningFeatureDefinition targetPd : installedFeatures.values()) {
            String pid = targetPd.getHeader("IBM-ProductID");
            if (pid != null) {
                for (String productId : productIds) {
                    if (pid.equals(productId))
                        uninstallAssets.add(new UninstallAsset(targetPd));
                }
            }
        }
    }

    void uninstallFeaturesPrereqChecking(Collection<String> featureNames, boolean allowUninstallAll, boolean force) throws InstallException {

        if (!!!allowUninstallAll) {
            Collection<String> usrFeatures = getUsrFeatures(featureNames);
            if (!!!usrFeatures.isEmpty()) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNINSTALL_FAILED_USR", usrFeatures.toArray()));
            }
        }

        Collection<ProvisioningFeatureDefinition> installedFeatureDefinitions = product.getAllFeatureDefinitions().values();
        Collection<String> featuresWithUnfoundExtension = getFeaturesWithUnfoundExtension(featureNames);
        if (!!!featuresWithUnfoundExtension.isEmpty()) {
            //show incorrect extension message
            String featureName = featuresWithUnfoundExtension.iterator().next();
            String[] extFeature = featureName.split(":");
            String ext = extFeature[0];
            String feaName = extFeature[1];
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_EXTENSION_NOT_FOUND", feaName, ext));
        }

        Collection<String> unfoundUserFeaturesUnderGivenExtension = getUnfoundUserFeatures(installedFeatureDefinitions, featureNames);
        if (!!!unfoundUserFeaturesUnderGivenExtension.isEmpty()) {
            //show incorrect extension message
            String featureName = unfoundUserFeaturesUnderGivenExtension.iterator().next();
            String[] extFeature = featureName.split(":");
            String ext = extFeature[0];
            String feaName = extFeature[1];
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FEATURE_NOT_FOUND_IN_EXTENSION", feaName, ext));
        }

        Collection<String> notInstalledFeatures = getNotInstalledFeatures(installedFeatureDefinitions, featureNames);
        if (!!!notInstalledFeatures.isEmpty()) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FEATURE_NOT_INSTALLED", notInstalledFeatures.toArray()));
        }

        List<UninstallAsset> uninstallAssets = new ArrayList<UninstallAsset>();
        Collection<ProvisioningFeatureDefinition> uninstallFeatures = getProvisioningFeatureDefinition(installedFeatureDefinitions, featureNames);
        log(Level.FINE, InstallUtils.NEWLINE + Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_PENDING_UNINSTALLING_FEATURE"));
        for (ProvisioningFeatureDefinition uninstallFeature : uninstallFeatures) {
            UninstallAsset ua = new UninstallAsset(uninstallFeature);
            uninstallAssets.add(ua);
            log(Level.FINE, ua.getDisplayName());
        }
        log(Level.FINE, InstallUtils.NEWLINE);
        Map<UninstallAsset, String> notUninstallableFeatures = getNotUninstallableAssets(uninstallAssets, installedFeatureDefinitions, null, true);

        if (!!!notUninstallableFeatures.isEmpty()) {
            StringBuffer message = new StringBuffer();
            Collection<String> installFeatureRequiredFeatures = getInstallFeatureRequiredFeatures(featureNames);
            boolean throwException = false;

            for (Entry<UninstallAsset, String> entries : notUninstallableFeatures.entrySet()) {
                if (!!!force && !!!installFeatureRequiredFeatures.contains(entries.getKey().getProvisioningFeatureDefinition().getSymbolicName())) {
                    throwException = true;
                }
                message.append(entries.getValue());
            }

            // Add additional information
            if (force) {
                message.insert(0, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_UNINSTALL_FEATURE_DEPENDENCIES") + InstallUtils.NEWLINE);
                message.append(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_UNINSTALL_FEATURE_DEPENDENCIES_EXPLANATION") + InstallUtils.NEWLINE);

            } else {
                message.insert(0, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNINSTALL_DEPENDENCY_CHECKING_FAILED"));
                message.append(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNINSTALL_DEPENDENCY_CHECKING_FAILED_ACTION"));
            }

            if (throwException) {
                throw new InstallException(message.toString());
            } else if (force) {
                log(Level.INFO, message.toString());
            } else {
                log(Level.FINEST, message.toString());
            }
        }
    }

    void uninstallFix(Collection<String> fixNames) throws InstallException {
        fireProgressEvent(InstallProgressEvent.CHECK, 1, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_CHECKING_FIXES"));
        Set<IFixInfo> fixInfoSet = FixAdaptor.getInstalledIFixes(product.getInstallDir());
        uninstallAssets = new ArrayList<UninstallAsset>();
        for (String fix : fixNames) {
            IFixInfo targetFix = null;
            for (IFixInfo fixInfo : fixInfoSet) {
                if (fixInfo.getId().equals(fix)) {
                    targetFix = fixInfo;
                    break;
                }
            }
            if (targetFix == null) {
                throw ExceptionUtils.createByKey("ERROR_IFIX_NOT_INSTALLED", fix);
            }
            uninstallAssets.add(new UninstallAsset(targetFix));
        }

        fixDependencyChecker = new FixDependencyChecker();

        for (UninstallAsset uninstallAsset : uninstallAssets) {
            // Determine if there is still another fix that is not being uninstalled that requires the uninstallAsset
            if (!fixDependencyChecker.isUninstallable(uninstallAsset, fixInfoSet, uninstallAssets)) {
                throw ExceptionUtils.createByKey("ERROR_IFIX_UNINSTALLABLE", uninstallAsset.getIFixInfo().getId());
            }
            for (Problem problem : uninstallAsset.getIFixInfo().getResolves().getProblems()) {
                ArrayList<String> dependency = FixDependencyChecker.fixRequiredByFeature(problem.getDisplayId(), product.getFeatureDefinitions());
                if (dependency != null) {
                    log(Level.FINE, "Dependent features:");
                    for (String f : dependency)
                        log(Level.FINE, f);
                    throw ExceptionUtils.createByKey("ERROR_IFIX_UNINSTALLABLE_REQUIRED_BY_FEATURE", uninstallAsset.getIFixInfo().getId(), dependency.get(0));
                }
            }
        }

        // Determine the uninstall order
        uninstallAssets = fixDependencyChecker.determineOrder(uninstallAssets);
    }

    /**
     * Initialize the fix uninstallation and verify the target fix is installed.
     *
     * @param fixId fix id
     * @throws InstallException if the fix uninstallation is not initialized successfully
     */
    void uninstallFix(String fixId) throws InstallException {
        fireProgressEvent(InstallProgressEvent.CHECK, 1, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_CHECKING_FIXES"));
        uninstallAssets = new ArrayList<UninstallAsset>();
        Set<IFixInfo> fixInfoSet = FixAdaptor.getInstalledIFixes(product.getInstallDir());
        for (IFixInfo fixInfo : fixInfoSet) {
            if (fixInfo.getId().equals(fixId)) {
                if (!!!FixDependencyChecker.isUninstallable(fixInfoSet, fixInfo)) {
                    throw ExceptionUtils.createByKey("ERROR_IFIX_UNINSTALLABLE", fixId);
                }
                for (Problem problem : fixInfo.getResolves().getProblems()) {
                    ArrayList<String> dependency = FixDependencyChecker.fixRequiredByFeature(problem.getDisplayId(), product.getFeatureDefinitions());
                    if (dependency != null) {
                        log(Level.FINE, "Dependent features:");
                        for (String f : dependency)
                            log(Level.FINE, f);
                        throw ExceptionUtils.createByKey("ERROR_IFIX_UNINSTALLABLE_REQUIRED_BY_FEATURE", fixId, dependency.get(0));
                    }
                }
                uninstallAssets.add(new UninstallAsset(fixInfo));
                return;
            }
        }

        throw ExceptionUtils.createByKey("ERROR_IFIX_NOT_INSTALLED", fixId);
    }

    private Collection<ProvisioningFeatureDefinition> getInstalledAutoFeatures(Collection<ProvisioningFeatureDefinition> installedFeatureDefinitions) {
        Collection<ProvisioningFeatureDefinition> autoFeatures = new ArrayList<ProvisioningFeatureDefinition>();
        for (ProvisioningFeatureDefinition pfd : installedFeatureDefinitions) {
            if (isAutoFeature(pfd)) {
                autoFeatures.add(pfd);
            }
        }
        return autoFeatures;
    }

    private Collection<String> getInstallFeatureRequiredFeatures(Collection<String> featureNames) {
        Collection<String> installFeatureRequiredFeatures = new ArrayList<String>();
        Map<String, ProvisioningFeatureDefinition> installFeatures = product.getManifestFileProcessor().getInstallFeatureDefinitions();
        if (installFeatures == null || installFeatures.isEmpty()) {
            return installFeatureRequiredFeatures;
        }
        for (String feature : featureNames) {
            ProvisioningFeatureDefinition pd = getProvisioningFeatureDefinition(installFeatures.values(), feature);
            if (pd != null && pd.getVisibility() != null && pd.getVisibility().equals(com.ibm.ws.kernel.feature.Visibility.INSTALL)) {
                Collection<FeatureResource> resources = pd.getConstituents(null);
                for (FeatureResource fr : resources) {
                    if (fr.getType().equals(SubsystemContentType.FEATURE_TYPE)) {
                        installFeatureRequiredFeatures.add(fr.getSymbolicName());
                    }
                }
            }
        }
        return installFeatureRequiredFeatures;
    }

    private Collection<String> getInstallFeatures(Collection<String> featureNames) {
        Collection<String> installTypeFeatures = new ArrayList<String>();
        Map<String, ProvisioningFeatureDefinition> installFeatures = product.getManifestFileProcessor().getInstallFeatureDefinitions();
        if (installFeatures == null || installFeatures.isEmpty()) {
            return installTypeFeatures;
        }
        for (String feature : featureNames) {
            ProvisioningFeatureDefinition pd = getProvisioningFeatureDefinition(installFeatures.values(), feature);
            if (pd != null && pd.getVisibility() != null && pd.getVisibility().equals(com.ibm.ws.kernel.feature.Visibility.INSTALL)) {
                installTypeFeatures.add(pd.getSymbolicName());
            }
        }
        return installTypeFeatures;
    }

    private Collection<String> getNotInstalledFeatures(Collection<ProvisioningFeatureDefinition> featureDefinitions,
                                                       Collection<String> features) {
        Collection<String> f = new ArrayList<String>();
        for (String feature : features) {
            ProvisioningFeatureDefinition pp = getProvisioningFeatureDefinition(featureDefinitions, feature);
            if (pp == null) {
                f.add(feature);
            }
        }
        return f;
    }

    private Collection<String> getUnfoundUserFeatures(Collection<ProvisioningFeatureDefinition> featureDefinitions,
                                                      Collection<String> features) {
        Collection<String> f = new ArrayList<String>();
        for (String feature : features) {
            ProvisioningFeatureDefinition ppE = getUserProvisioningFeatureDefinition(featureDefinitions, feature);
            if (ppE == null) {
                f.add(feature);
            }
        }
        return f;
    }

    private Collection<String> getFeaturesWithUnfoundExtension(Collection<String> features) {

        Collection<String> featuresWithUnfoundExtension = new ArrayList<String>();
        for (String f : features) {
            if (f != null && f.contains(":")) {
                if (f.split(":").length == 2) {
                    String[] extFeat = f.split(":");
                    String ext = extFeat[0];
                    if (!ext.isEmpty() && !BundleRepositoryRegistry.keys().contains(ext)) {
                        featuresWithUnfoundExtension.add(f);
                    }
                }
            }
        }

        return featuresWithUnfoundExtension;
    }

    private Map<UninstallAsset, String> getNotUninstallableAssets(List<UninstallAsset> uninstallAssets,
                                                                  Collection<ProvisioningFeatureDefinition> installedFeatureDefinitions,
                                                                  Collection<String> uninstallingInstallFeatures, boolean isChecking) {
        Map<UninstallAsset, String> notUninstallableAssets = new LinkedHashMap<UninstallAsset, String>();
        dependencyChecker = new FeatureDependencyChecker();
        // Determine the uninstall order
        uninstallAssets = dependencyChecker.determineOrder(uninstallAssets);
        for (UninstallAsset uninstallAsset : uninstallAssets) {
            // Determine there is another feature still requires the uninstalling feature
            Collection<ProvisioningFeatureDefinition> requiredByThisFeature = dependencyChecker.isUninstallable(uninstallAsset, installedFeatureDefinitions,
                                                                                                                uninstallingInstallFeatures, isChecking);
            Collection<ProvisioningFeatureDefinition> notToBeUninstalled = dependencyChecker.getNotToBeUninstall(requiredByThisFeature, uninstallAssets);
            if (!!!notToBeUninstalled.isEmpty() && !!!isAutoFeature(notToBeUninstalled)) {
                StringBuffer displayNames = new StringBuffer();
                int counter = 0;
                for (ProvisioningFeatureDefinition p : notToBeUninstalled) {
                    counter++;
                    String shortName = p.getHeader("IBM-ShortName");
                    if (shortName != null && !!!shortName.isEmpty()) {
                        displayNames.append(shortName);
                    } else {
                        displayNames.append(p.getSymbolicName());
                    }
                    if (counter < notToBeUninstalled.size()) {
                        displayNames.append(", ");
                    }
                }
                String featureName;
                if (uninstallAsset.getProvisioningFeatureDefinition().getHeader("IBM-ShortName") != null
                    && !!!uninstallAsset.getProvisioningFeatureDefinition().getHeader("IBM-ShortName").isEmpty()) {
                    featureName = uninstallAsset.getProvisioningFeatureDefinition().getHeader("IBM-ShortName");
                } else {
                    featureName = uninstallAsset.getProvisioningFeatureDefinition().getSymbolicName();
                }
                notUninstallableAssets.put(uninstallAsset,
                                           Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNINSTALL_DEPENDENCY_CHECKING_FAILED_REQUIRED_BY",
                                                                                          featureName, displayNames));
            }
        }
        return notUninstallableAssets;
    }

    private Collection<ProvisioningFeatureDefinition> getProvisioningFeatureDefinition(Collection<ProvisioningFeatureDefinition> featureDefinitions, Collection<String> features) {
        Collection<ProvisioningFeatureDefinition> p = new ArrayList<ProvisioningFeatureDefinition>();
        for (String feature : features) {
            ProvisioningFeatureDefinition pp = getProvisioningFeatureDefinition(featureDefinitions, feature);
            if (pp != null) {
                p.add(pp);
            }
        }
        return p;
    }

    private ProvisioningFeatureDefinition getProvisioningFeatureDefinition(Collection<ProvisioningFeatureDefinition> featureDefinitions, String feature) {
        for (ProvisioningFeatureDefinition pfd : featureDefinitions) {
            if (isFeatureDefinition(pfd, feature)) {
                return pfd;
            }
        }
        return null;
    }

    private ProvisioningFeatureDefinition getUserProvisioningFeatureDefinition(Collection<ProvisioningFeatureDefinition> featureDefinitions, String feature) {
        for (ProvisioningFeatureDefinition pfd : featureDefinitions) {
            if (isUserFeatureaDefinition(pfd, feature)) {
                return pfd;
            }
        }
        return null;
    }

    private Collection<String> getRequiredInternalFeatures(Collection<String> features, Collection<ProvisioningFeatureDefinition> installedFeatureDefinitions) {
        Collection<String> internalFeatures = new ArrayList<String>();
        for (String feature : features) {
            ProvisioningFeatureDefinition pd = getProvisioningFeatureDefinition(installedFeatureDefinitions, feature);
            if (pd != null) {
                Collection<FeatureResource> resources = pd.getConstituents(null);
                for (FeatureResource fr : resources) {
                    if (fr.getType().equals(SubsystemContentType.FEATURE_TYPE)) {
                        ProvisioningFeatureDefinition dependentPd = getProvisioningFeatureDefinition(installedFeatureDefinitions, fr.getSymbolicName());
                        if (dependentPd != null && (dependentPd.getVisibility() == null ||
                                                    !!!dependentPd.getVisibility().equals(com.ibm.ws.kernel.feature.Visibility.PUBLIC))) {
                            try {
                                log(Level.FINEST, "Perform uninstall prereq checking for the internal feature: " + fr.getSymbolicName());
                                uninstallFeaturesPrereqChecking(fr.getSymbolicName());
                                log(Level.FINEST, "Internel dependent feature to be uninstalled: " + fr.getSymbolicName());
                                internalFeatures.add(fr.getSymbolicName());
                            } catch (InstallException e) {
                                log(Level.FINEST, e.getMessage(), e);
                            }
                        }
                    }
                }
            } else {
                log(Level.FINEST, feature + " is already uninstalled.");
            }
        }
        return internalFeatures;
    }

    private Collection<String> getUsrFeatures(Collection<String> featureNames) {
        Collection<String> userFeatures = new ArrayList<String>();
        Map<String, ProvisioningFeatureDefinition> usrFeatures = product.getManifestFileProcessor().getFeatureDefinitions(ManifestFileProcessor.USR_PRODUCT_EXT_NAME);
        Map<String, ProvisioningFeatureDefinition> extFeatures = product.getManifestFileProcessor().getFeatureDefinitions("ext");
        Map<String, ProvisioningFeatureDefinition> allUsrfeatures = new HashMap<String, ProvisioningFeatureDefinition>();
        if ((usrFeatures == null || usrFeatures.isEmpty())
            && (extFeatures == null || extFeatures.isEmpty())) {
            return userFeatures;
        }
        if (usrFeatures != null && !!!usrFeatures.isEmpty()) {
            allUsrfeatures.putAll(usrFeatures);
        }
        if (extFeatures != null && !!!extFeatures.isEmpty()) {
            allUsrfeatures.putAll(extFeatures);
        }
        for (String feature : featureNames) {
            if (feature.contains(":") && feature.split(":").length == 2)
                feature = feature.split(":")[1];
            for (ProvisioningFeatureDefinition pfd : allUsrfeatures.values()) {
                if (pfd.getSymbolicName().equals(feature)
                    || (InstallUtils.getShortName(pfd) != null && InstallUtils.getShortName(pfd).equals(feature))) {
                    userFeatures.add(feature);
                }
            }
        }
        return userFeatures;
    }

    private boolean isAutoFeature(ProvisioningFeatureDefinition pfd) {
        String installPolicy = pfd.getHeader("IBM-Install-Policy");
        if (pfd.isAutoFeature() &&
            installPolicy != null &&
            installPolicy.equals("when-satisfied")) {
            log(Level.FINEST, pfd.getSymbolicName() + " is auto feature.");
            return true;
        }
        return false;
    }

    private boolean isAutoFeature(Collection<ProvisioningFeatureDefinition> pfd) {
        for (ProvisioningFeatureDefinition p : pfd) {
            if (!!!isAutoFeature(p)) {
                return false;
            }
        }
        return true;
    }

    private boolean isUserFeatureaDefinition(ProvisioningFeatureDefinition pd, String feature) {
        String featureName = feature;
        String ext = "";
        if (feature.contains(":") && feature.split(":").length == 2) {
            String[] extFeature = feature.split(":");
            ext = extFeature[0];
            featureName = extFeature[1];
            String shortName = InstallUtils.getShortName(pd);
            String br = pd.getBundleRepositoryType();
            if (ext.isEmpty())
                return true;
            return br.equals(ext)
                   && (pd.getSymbolicName().equals(featureName) ||
                       (shortName != null && shortName.equalsIgnoreCase(featureName)));
        }

        return true;
    }

    private boolean isFeatureDefinition(ProvisioningFeatureDefinition pd, String feature) {

        String featureName = feature;
        String ext = "";
        if (feature.contains(":") && feature.split(":").length == 2) {
            String[] extFeature = feature.split(":");
            ext = extFeature[0];
            if (!ext.isEmpty()) //:portlet-2.0 case will return true
                featureName = extFeature[1];
        }
        String shortName = InstallUtils.getShortName(pd);

        String br = pd.getBundleRepositoryType();
        return (br.equals(ext) || (ext.isEmpty() && !br.isEmpty()))
               && (pd.getSymbolicName().equals(featureName) ||
                   (shortName != null && shortName.equalsIgnoreCase(featureName)));
    }

    private boolean isInstalledAutoFeatureStillRequired(ProvisioningFeatureDefinition autoFeature,
                                                        Collection<ProvisioningFeatureDefinition> featureDefinitionsToCheck) {
        if (autoFeature.isCapabilitySatisfied(featureDefinitionsToCheck)) {
            log(Level.FINEST, "auto feature " + autoFeature.getSymbolicName() + " is still required.");
            return true;
        } else {
            log(Level.FINEST, "auto feature " + autoFeature.getSymbolicName() + " is not required.");
            return false;
        }
    }

    private List<UninstallAsset> removeUninstallableAssets(Collection<UninstallAsset> featuresToBeUninstalled, Collection<UninstallAsset> featuresNotUninstallable) {
        List<UninstallAsset> uninstallAssets = new ArrayList<UninstallAsset>();
        for (UninstallAsset asset : featuresToBeUninstalled) {
            boolean add = true;
            for (UninstallAsset featureNotUninstallable : featuresNotUninstallable) {
                if (asset.getProvisioningFeatureDefinition().getSymbolicName().equals(featureNotUninstallable.getProvisioningFeatureDefinition().getSymbolicName())) {
                    log(Level.FINEST, asset.getProvisioningFeatureDefinition().getSymbolicName() + " cannot be uninstalled.");
                    add = false;
                    break;
                }
            }
            if (add) {
                uninstallAssets.add(asset);
            }
        }
        return uninstallAssets;
    }

    private void uninstallInternalAndAutoFeatures(Collection<String> featureNames,
                                                  Collection<ProvisioningFeatureDefinition> installedFeatureDefinitions,
                                                  Collection<String> installFeatureNames) throws InstallException {
        Collection<String> uninstallInternalAndAutoFeatures = new ArrayList<String>();
        uninstallInternalAndAutoFeatures.addAll(getRequiredInternalFeatures(featureNames, installedFeatureDefinitions));
        Collection<ProvisioningFeatureDefinition> autoFeatures = getInstalledAutoFeatures(installedFeatureDefinitions);
        for (ProvisioningFeatureDefinition autoFeature : autoFeatures) {
            if (!!!isInstalledAutoFeatureStillRequired(autoFeature, product.getAllFeatureDefinitions().values())) {
                log(Level.FINEST, "Auto feature to be uninstalled: " + autoFeature.getSymbolicName());
                uninstallInternalAndAutoFeatures.add(autoFeature.getSymbolicName());
            }
        }
        uninstallFeatures(uninstallInternalAndAutoFeatures, installFeatureNames, false);
    }

    private void uninstallFeaturesPrereqChecking(String featureName) throws InstallException {
        Collection<String> featureNames = new ArrayList<String>();
        featureNames.add(featureName);
        uninstallFeaturesPrereqChecking(featureNames, true, false);
    }

    public boolean needToSetScriptsPermission() {
        return setScriptsPermission;
    }

    private void checkSetScriptsPermission(List<File> filesRestored) {
        if (setScriptsPermission)
            return;
        setScriptsPermission = containScript(filesRestored);
    }
}
