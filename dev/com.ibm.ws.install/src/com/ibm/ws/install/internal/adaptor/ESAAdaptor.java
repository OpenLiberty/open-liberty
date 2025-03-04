/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.install.internal.adaptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.xml.sax.SAXException;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallConstants.ExistsAction;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.ChecksumsManager;
import com.ibm.ws.install.internal.ExceptionUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.internal.InstallUtils.FileWriter;
import com.ibm.ws.install.internal.InstallUtils.InputStreamFileWriter;
import com.ibm.ws.install.internal.Product;
import com.ibm.ws.install.internal.asset.ESAAsset;
import com.ibm.ws.install.internal.asset.UninstallAsset;
import com.ibm.ws.install.internal.platform.InstallPlatformUtils;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.HashUtils;
import com.ibm.ws.kernel.feature.internal.generator.ManifestFileProcessor;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry.BundleRepositoryHolder;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;

import wlp.lib.extract.platform.Platform;

public class ESAAdaptor extends ArchiveAdaptor {

    //Enum for valid values in OsList
    private static enum OS_LIST_VALUE {
        ZOS("Z/OS");

        private final String alias;

        private OS_LIST_VALUE(String alias) {
            this.alias = alias;
        }

        private static boolean checkOSList(FeatureResource fr) {
            List<String> osList = fr.getOsList();
            if (osList == null || osList.size() == 0)
                return true;
            // If on z/OS install everything
            if (Platform.isZOS()) {
                return true;
            }
            for (String value : osList) {
                if (value.toUpperCase().equals(ZOS.alias))
                    return false;
            }
            return true;
        }

    }

    private static final String EBCDIC = "ebcdic";
    private static final String ASCII = "ascii";
    private static ArrayList<ProvisioningFeatureDefinition> kernelFeatures = new ArrayList<>();

    public static void install(Product product, ESAAsset featureAsset, List<File> filesInstalled, Collection<String> featuresToBeInstalled, ExistsAction existsAction,
                               Set<String> executableFiles, Map<String, Set<String>> extattrFiles, ChecksumsManager checksumsManager) throws IOException, InstallException {
        install(product, featureAsset, filesInstalled, featuresToBeInstalled, existsAction, executableFiles, extattrFiles, checksumsManager, false);
    }

    public static void install(Product product, ESAAsset featureAsset, List<File> filesInstalled, Collection<String> featuresToBeInstalled, ExistsAction existsAction,
                               Set<String> executableFiles, Map<String, Set<String>> extattrFiles, ChecksumsManager checksumsManager,
                               boolean skipFeatureDependencyCheck) throws IOException, InstallException {
        ProvisioningFeatureDefinition featureDefinition = featureAsset.getProvisioningFeatureDefinition();
        if (featureDefinition == null)
            return;

        try {
            install(product, featureAsset, featureDefinition, filesInstalled, featuresToBeInstalled, existsAction, executableFiles, extattrFiles, checksumsManager,
                    skipFeatureDependencyCheck);
        } catch (IOException e) {
            InstallUtils.delete(filesInstalled);
            throw e;
        } catch (InstallException e) {
            InstallUtils.delete(filesInstalled);
            throw e;
        }
    }

    private static void install(Product product, ESAAsset featureAsset, ProvisioningFeatureDefinition featureDefinition, List<File> filesInstalled,
                                Collection<String> featuresToBeInstalled, ExistsAction existsAction, Set<String> executableFiles, Map<String, Set<String>> extattrFiles,
                                ChecksumsManager checksumsManager, boolean skipFeatureDependencyCheck) throws IOException, InstallException {
        File baseDir;
        String repoType = featureAsset.getRepoType();
        boolean installToCore = ManifestFileProcessor.CORE_PRODUCT_NAME.equals(featureDefinition.getHeader("IBM-InstallTo"));
        if (repoType == null || InstallConstants.TO_USER.equalsIgnoreCase(repoType)) {
            baseDir = product.getUserExtensionDir();
        } else if (ManifestFileProcessor.CORE_PRODUCT_NAME.equalsIgnoreCase(repoType)) {

            //feature is not a core feature but user attempted to install to core
            if (!installToCore) {
                throw new InstallException(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.userfeature.install.location.invalid",
                                                                                       featureAsset.getFeatureName()), InstallException.USER_FEATURE_REPO_TYPE_INVALID);
            }

            baseDir = product.getInstallDir();
        } else {
            baseDir = product.getUserDirExternal(repoType);
        }
        final String manifestName;
        if (installToCore)
            // Override the install dir to point to core
            baseDir = Utils.getInstallDir();
        manifestName = featureDefinition.getSymbolicName() + ManifestFileProcessor.MF_EXTENSION;
        // See if there is a checksum file for this feature
        ZipEntry checksumEntry = featureAsset.getEntry("OSGI-INF/checksums.cs");
        Properties checksumInput = null;
        Properties checksumOutput = null;
        if (checksumEntry != null) {
            checksumInput = new Properties();
            checksumInput.load(featureAsset.getInputStream(checksumEntry));
            checksumOutput = new Properties();
        }

        // Store the feature definition in the file system.
        String manifestDirectoryPath = featureDefinition.getVisibility() == Visibility.INSTALL ? "lib/assets" : "lib/features";
        File featureDir = new File(baseDir, manifestDirectoryPath);
        String checksumManifestValue = null;
        if (checksumInput != null) {
            checksumManifestValue = checksumInput.getProperty(featureAsset.getSubsystemEntryName());
            if (checksumManifestValue != null) {
                checksumOutput.put(manifestDirectoryPath + "/" + manifestName, checksumManifestValue);
            }
        }

        write(false, filesInstalled, new File(featureDir, manifestName), featureAsset.getInputStream(featureAsset.getSubsystemEntry()), ExistsAction.fail, checksumManifestValue);

        boolean ibmFeature = featureDefinition.getIbmFeatureVersion() > 0;

        Map<String, FeatureResource> bundleConstituents = new HashMap<String, FeatureResource>();
        Map<String, FeatureResource> jarConstituents = new HashMap<String, FeatureResource>();

        // Make sure we don't process the same zip entry twice, this is important in case you have a
        // type="file" that is actually a JAR, especially if it is a non-OSGi bundle as we definitely
        // wouldn't want to try to process those as bundles
        Set<String> processedEntries = new HashSet<String>();
        for (FeatureResource fr : featureDefinition.getConstituents(null)) {
            SubsystemContentType type = fr.getType();
            if (SubsystemContentType.BUNDLE_TYPE == type) {
                // Handle bundle types differently as they always come in the root of the ESA and their
                // name is not guaranteed as it may include a version string.
                bundleConstituents.put(fr.getSymbolicName(), fr);
            } else if (SubsystemContentType.JAR_TYPE == type && ibmFeature) {
                // There are two types of JARs:
                // * ones that are identified directly by their location and may not be OSGi bundles.
                // As these are may not be OSGi bundles they need to be treated the same as files
                // * ones that have a location that points to a directory or collection of directories,
                // in this case they are most similar to bundles
                String resourceLoc = fr.getLocation();
                if (resourceLoc == null || resourceLoc.contains(",") || !resourceLoc.endsWith(".jar")) {
                    // Bundle type
                    jarConstituents.put(fr.getSymbolicName(), fr);
                } else {
                    // File type
                    extractFileType(resourceLoc, baseDir, featureDir, existsAction, filesInstalled, featureAsset.getZip(),
                                    featureDefinition, fr, processedEntries, checksumInput, checksumOutput, executableFiles, extattrFiles, checksumsManager);

                }
            } else if (SubsystemContentType.FEATURE_TYPE == type) {
                if (!skipFeatureDependencyCheck) {
                    // Look to see if the feature already exists and try to find it in the same place as the current ESA if we don't have it
                    String symName = fr.getSymbolicName();
                    if (!isFeatureAvailable(product, featuresToBeInstalled, symName)) {
                        boolean foundToleratesFeature = false;
                        List<String> toleratesVersions = fr.getTolerates();
                        if (toleratesVersions != null && !toleratesVersions.isEmpty()) {
                            String symNameWithoutVersion = extractSymbolicNameWithoutVersion(symName);
                            for (String toleratesVersion : toleratesVersions) {
                                String toleratesSymName = symNameWithoutVersion + "-" + toleratesVersion;
                                if (isFeatureAvailable(product, featuresToBeInstalled, toleratesSymName)) {
                                    foundToleratesFeature = true;
                                    break;
                                }
                            }
                        }
                        if (!foundToleratesFeature) {
                            throw new InstallException(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.missing.feature", featureDefinition.getSymbolicName(), symName));
                        }
                    }
                }
            } else if (ibmFeature) {
                String resourceLoc = fr.getLocation();
                if (resourceLoc == null || resourceLoc.contains(",")) {
                    // we can't deal with this because we don't know the location.
                    // Use the best possible error message as there isn't a specific one for this situation
                    throw new InstallException(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.missing.content", fr.getSymbolicName(),
                                                                                           null), InstallException.BAD_FEATURE_DEFINITION);
                }
                extractFileType(resourceLoc, baseDir, featureDir, existsAction, filesInstalled, featureAsset.getZip(), featureDefinition, fr,
                                processedEntries, checksumInput, checksumOutput, executableFiles, extattrFiles, checksumsManager);

            }
        }

        /* Now we go through all the entries to handle the remaining content, need to know about some special files: localization and licenses */
        String licenseAgreementPrefix = featureDefinition.getHeader("IBM-License-Agreement");
        String licenseInformationPrefix = featureDefinition.getHeader("IBM-License-Information");
        boolean hasLicenseAgreements = licenseAgreementPrefix != null && !licenseAgreementPrefix.isEmpty();
        boolean hasLicenseInformation = licenseInformationPrefix != null && !licenseInformationPrefix.isEmpty();
        File licenseDir = null;
        if (hasLicenseAgreements || hasLicenseInformation) {
            licenseDir = new File(baseDir, "lafiles" + File.separator + featureDefinition.getSymbolicName());
        }

        // Need to work out where any localizations might be and where they should go.
        String loc = featureDefinition.getHeader("Subsystem-Localization");
        File l10nDir = new File(featureDir, "l10n");

        // Also need the list of icons, relative to the root of the folder
        Collection<String> icons = featureDefinition.getIcons();
        File iconDir = new File(featureDir, "icons/" + featureDefinition.getSymbolicName());

        Enumeration<? extends ZipEntry> entries = featureAsset.getZipEntries();
        while (entries.hasMoreElements()) {
            ZipEntry ze = entries.nextElement();
            if (!ze.isDirectory()) {
                String entryName = ze.getName();
                String entryChecksum = null;
                if (checksumInput != null) {
                    entryChecksum = checksumInput.getProperty(entryName);
                }
                // We want to handle OSGI-INF content specially as its usually only the localization info
                // However, we need to handle icons first, as they are likely to be in OSGI-INF but could
                // be anywhere in the feature package.
                if (icons != null && icons.contains(entryName)) {
                    // If this is a path to an icon extract it
                    File iconFile = new File(iconDir, entryName);
                    write(false, filesInstalled, iconFile, featureAsset.getInputStream(ze), existsAction, entryChecksum);
                } else if (entryName.startsWith("OSGI-INF/")) {
                    // Generally we ignore files in OSGI-INF, except for localizations and icons
                    if (loc != null && entryName.startsWith(loc) && entryName.indexOf('/', loc.length()) == -1) {
                        // so if the file starts with the loc name and doesn't have a / after the prefix
                        // then we extract it.
                        File locFile = new File(l10nDir, featureDefinition.getSymbolicName() + entryName.substring(loc.length()));
                        write(false, filesInstalled, locFile, featureAsset.getInputStream(ze), existsAction, entryChecksum);
                    }
                    // anything that isn't in a dir. We munge entryName to start with / so we look after index 0.
                } else if (entryName.indexOf('/', 1) == -1) {
                    // We have something in the root which means it is a bundle, handle it as such
                    extractBundleType(ze, featureAsset.getZip(), existsAction, bundleConstituents, featureDefinition, filesInstalled, baseDir, featureDir, false, checksumInput,
                                      checksumOutput, checksumsManager);
                } else if (!processedEntries.contains(entryName) && entryName.startsWith("wlp") && entryName.endsWith(".jar")) {
                    // Same as bundles, need to make sure that we have the right JAR to match one of the entries
                    // in the manifest
                    extractBundleType(ze, featureAsset.getZip(), existsAction, jarConstituents, featureDefinition, filesInstalled, baseDir, featureDir, true, checksumInput,
                                      checksumOutput, checksumsManager);
                } else if ((hasLicenseAgreements && entryName.startsWith(licenseAgreementPrefix))
                           || (hasLicenseInformation && entryName.startsWith(licenseInformationPrefix))) {
                    // We have a license file so copy it in. Put it in a folder named after the feature so there are not clashes.
                    File licenseFile = new File(licenseDir, entryName);
                    write(false, filesInstalled, licenseFile, featureAsset.getInputStream(ze), existsAction, entryChecksum);
                    // No checksums for license files
                } // Entry not in OSGi-INF, a bundle or license file
            } // ze is directory
        } // next entry in zip

        if (!bundleConstituents.isEmpty()) {
            throw new InstallException(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.content.notfound", bundleConstituents), InstallException.MISSING_CONTENT);
        }

        if (!jarConstituents.isEmpty()) {
            throw new InstallException(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.content.notfound", jarConstituents), InstallException.MISSING_CONTENT);
        }

        // Finally write out the checksum file for this feature if we have one
        if (checksumOutput != null) {
            File checksumFile = new File(featureDir, "checksums/" + featureDefinition.getSymbolicName() + ".cs");
            write(false, filesInstalled, checksumFile, null, new PropertiesFileWriter(checksumOutput), existsAction, null);
        }

        if (featureDefinition.getVisibility() == Visibility.INSTALL)
            product.addFeatureCollection(featureAsset.getFeatureName(), featureDefinition);
        else
            product.addFeature(featureAsset.getFeatureName(), featureDefinition);

    }

    private static boolean isFeatureAvailable(Product product, Collection<String> featuresToBeInstalled, String symName) {
        return product.containsFeature(symName) || product.containsFeatureCollection(symName)
               || featuresToBeInstalled.contains(symName);
    }

    private static String extractSymbolicNameWithoutVersion(String symName) {
        int dashVersionIndex = symName.lastIndexOf("-");
        if (dashVersionIndex != -1) {
            return symName.substring(0, dashVersionIndex);
        } else {
            return symName;
        }
    }

    public static String getFeaturePath(ProvisioningFeatureDefinition targetFd, File baseDir) {
        if (targetFd.getVisibility() == Visibility.INSTALL)
            return "lib/assets/";
        File mf = targetFd.getFeatureDefinitionFile();
        if (mf != null)
            return mf.getAbsolutePath().contains("platform") ? "lib/platform/" : "lib/features/";
        File featurePath = new File(baseDir, "lib/features/");
        mf = new File(featurePath, InstallUtils.getShortName(targetFd) + ".mf");
        if (mf.exists())
            return "lib/features/";
        mf = new File(featurePath, targetFd.getSymbolicName() + ".mf");
        return mf.exists() ? "lib/features/" : "lib/platform/";
    }

    public static List<File> determineFilesToBeDeleted(ProvisioningFeatureDefinition targetFd, Map<String, ProvisioningFeatureDefinition> features, File baseDir,
                                                       String featurePath,
                                                       boolean checkDependency, Set<IFixInfo> uninstallFixInfo) {
        // Set kernel features
        initKernelFeatures(baseDir, features);
        // Determine the feature contents
        Map<String, File> featureContents = getUninstallFeatureContents(targetFd, features, baseDir, checkDependency);
        // Determine the bundles to remove according to the symbolic names of the uninstalling feature resources
        Map<String, File> uninstallFixBundleContents = FixAdaptor.getBundleFiles(baseDir, featureContents.keySet());
        // File list to delete
        List<File> filesToDelete = new ArrayList<File>();

        // Add runtime jars to the remove file list
        filesToDelete.addAll(featureContents.values());
        filesToDelete.addAll(uninstallFixBundleContents.values());

        // Add manifest files to the remove file list
        File mf = targetFd.getFeatureDefinitionFile();
        if (mf == null) {
            mf = new File(baseDir, featurePath + InstallUtils.getShortName(targetFd) + ".mf");
            if (!mf.exists()) {
                mf = new File(baseDir, featurePath + targetFd.getSymbolicName() + ".mf");
            }
        }
        filesToDelete.add(mf);

        //Add license files to the remove file list
        InstallUtils.getAllFiles(new File(baseDir, "/lafiles/" + targetFd.getSymbolicName()), filesToDelete);

        // Add message files to the remove file list
        Collection<File> locFileList = targetFd.getLocalizationFiles();
        if (locFileList == null || locFileList.isEmpty()) {
            List<File> fileList = new ArrayList<File>();
            InstallUtils.getAllFiles(new File(baseDir, featurePath + "l10n"), fileList);
            for (File f : fileList) {
                if ((f.getName().equals(targetFd.getSymbolicName() + ".properties") || f.getName().startsWith(targetFd.getSymbolicName() + "_"))) {
                    filesToDelete.add(f);
                }
            }
        } else {
            filesToDelete.addAll(locFileList);
        }

        //Add checksum file to the remove file list
        File csFile = targetFd.getFeatureChecksumFile();
        if (csFile == null) {
            csFile = new File(baseDir, featurePath + "checksums/" + targetFd.getSymbolicName() + ".cs");
        }
        filesToDelete.add(csFile);

        // Determine the fixes which require uninstall completely
        uninstallFixInfo.addAll(FixAdaptor.getUninstallFixInfo(baseDir, filesToDelete));

        return filesToDelete;
    }

    public static void uninstallFeature(UninstallAsset uninstallAsset, ProvisioningFeatureDefinition targetFd, File baseDir,
                                        List<File> filesRestored) throws ParserConfigurationException, IOException, SAXException {
        List<File> filesToDelete = uninstallAsset.getFeatureFileList();

        // Uninstall the fix
        for (IFixInfo fixInfo : uninstallAsset.getFixUpdatesFeature()) {
            FixAdaptor.uninstallFix(fixInfo, baseDir, filesRestored);
        }

        // Remove the uninstall feature files (static files) from the fix backup zip file
        FixAdaptor.removeFilesFromBackup(baseDir, filesToDelete);

        // Uninstall the files of the feature
        InstallUtils.delete(filesToDelete);

        // Remove feature license folder
        InstallUtils.deleteDirectory(new File(baseDir, "/lafiles/" + targetFd.getSymbolicName()));

        // remove the icons folder if it exists.
        // We could check for the files in here first but no-one should be adding their own stuff into this folder,
        // so we should be able to remove the whole dir every time.
        InstallUtils.deleteDirectory(new File(baseDir, uninstallAsset.getFeaturePath() + "icons/" + targetFd.getSymbolicName()));
    }

    private static boolean requiredByJar(ContentBasedLocalBundleRepository br, ProvisioningFeatureDefinition fd, File b) {
        for (FeatureResource fr : fd.getConstituents(null)) {
            SubsystemContentType type = fr.getType();
            if (SubsystemContentType.BUNDLE_TYPE == type || SubsystemContentType.JAR_TYPE == type || SubsystemContentType.BOOT_JAR_TYPE == type) {
                File bb = br.selectBundle(fr.getLocation(), fr.getSymbolicName(), fr.getVersionRange());
                if (bb != null && bb.exists() && b.getAbsolutePath().equals(bb.getAbsolutePath())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Determine there is another feature requires this resource
    private static boolean requiredByOtherFeature(ContentBasedLocalBundleRepository br, Map<String, ProvisioningFeatureDefinition> features, File b) {
        for (ProvisioningFeatureDefinition fd : features.values()) {
            if (requiredByJar(br, fd, b))
                return true;
        }
        return false;
    }

    /**
     * Set list of kernel features from all features map
     *
     * @param baseDir
     * @param features
     */
    private static void initKernelFeatures(File baseDir, Map<String, ProvisioningFeatureDefinition> features) {
        if (features == null) {
            features = new Product(baseDir).getFeatureDefinitions();
        }

        if (kernelFeatures == null || kernelFeatures.isEmpty()) {
            kernelFeatures = (ArrayList<ProvisioningFeatureDefinition>) features.values().stream().filter(ProvisioningFeatureDefinition::isKernel).collect(Collectors.toList());
        }

        return;

    }

    private static boolean requiredByPlatformFeature(ContentBasedLocalBundleRepository br, File baseDir, File b, Map<String, ProvisioningFeatureDefinition> features) {
        try {
            if (features == null) {
                features = new Product(baseDir).getFeatureDefinitions();
            }

            for (ProvisioningFeatureDefinition fd : kernelFeatures) {
                if (requiredByJar(br, fd, b))
                    return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static boolean requiredByFile(ProvisioningFeatureDefinition fd, File baseDir, File testFile) {
        for (FeatureResource fr : fd.getConstituents(null)) {
            SubsystemContentType type = fr.getType();
            if (SubsystemContentType.FILE_TYPE == type) {
                String locString = fr.getLocation();
                if (locString != null) {
                    String[] locs = locString.contains(",") ? locString.split(",") : new String[] { locString };
                    for (String loc : locs) {
                        File f = new File(loc);
                        if (!f.isAbsolute()) {
                            f = new File(baseDir, loc);
                        }
                        // Mark delete to false if there is another feature requires this resource
                        if (f.getAbsolutePath().equals(testFile.getAbsolutePath())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // Determine there is another feature requires this resource
    private static boolean requiredByOtherFeature(Map<String, ProvisioningFeatureDefinition> features, File baseDir, File testFile) {
        for (ProvisioningFeatureDefinition fd : features.values()) {
            if (requiredByFile(fd, baseDir, testFile))
                return true;
        }
        return false;
    }

    // Determine there is another platform feature requires this resource
    private static boolean requiredByPlatformFeature(File baseDir, File testFile, Map<String, ProvisioningFeatureDefinition> features) {
        try {
            if (features == null) {
                features = new Product(baseDir).getFeatureDefinitions();
            }

            for (ProvisioningFeatureDefinition fd : kernelFeatures) {
                if (requiredByFile(fd, baseDir, testFile))
                    return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static Map<String, File> getUninstallFeatureContents(ProvisioningFeatureDefinition targetFd, Map<String, ProvisioningFeatureDefinition> features, File baseDir,
                                                                 boolean checkDependency) {
        HashMap<String, File> resourceMap = new HashMap<String, File>();

        // Need to update the bundle repository registry
        BundleRepositoryRegistry.disposeAll();
        BundleRepositoryRegistry.initializeDefaults(null, false);
        BundleRepositoryRegistry.addBundleRepository(baseDir.getAbsolutePath(), targetFd.getBundleRepositoryType());
        BundleRepositoryHolder brh = BundleRepositoryRegistry.getRepositoryHolder(targetFd.getBundleRepositoryType());
        ContentBasedLocalBundleRepository br = brh.getBundleRepository();
        Collection<FeatureResource> resources = targetFd.getConstituents(null);

        features.remove(targetFd.getSymbolicName());
        for (FeatureResource fr : resources) {
            switch (fr.getType()) {
                case FEATURE_TYPE: {
                    // Skip feature type
                    break;
                }
                case BUNDLE_TYPE:
                case JAR_TYPE:
                case BOOT_JAR_TYPE: {
                    String locString = fr.getLocation();
                    String[] locs = locString == null ? new String[] { null } : locString.contains(",") ? locString.split(",") : new String[] { locString };

                    for (String loc : locs) {
                        File b = br.selectBundle(loc, fr.getSymbolicName(), fr.getVersionRange());
                        if (b != null && b.exists()) {
                            if (checkDependency) {
                                if (!requiredByOtherFeature(br, features, b)) {
                                    resourceMap.put(fr.getSymbolicName(), b);
                                }
                            } else {
                                if (targetFd.isKernel() || !requiredByPlatformFeature(br, baseDir, b, features)) {
                                    resourceMap.put(fr.getSymbolicName(), b);
                                }
                            }
                        }
                    }
                    break;
                }

                case FILE_TYPE: {
                    String locString = fr.getLocation();
                    if (locString != null) {
                        String[] locs = locString.contains(",") ? locString.split(",") : new String[] { locString };
                        for (String loc : locs) {
                            File testFile = new File(loc);
                            if (!testFile.isAbsolute()) {
                                testFile = new File(baseDir, loc);
                            }
                            if (testFile.exists()) {
                                if (checkDependency) {
                                    if (!requiredByOtherFeature(features, baseDir, testFile)) {
                                        resourceMap.put(fr.getSymbolicName(), testFile);
                                    }
                                } else {
                                    if (targetFd.isKernel() || !requiredByPlatformFeature(baseDir, testFile, features)) {
                                        resourceMap.put(fr.getSymbolicName(), testFile);
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
                default: {
                    break;
                }
            }
        }
        return resourceMap;
    }

    private static boolean write(boolean tmpFile, List<File> installedFiles, File fileToWrite, String toFileEncoding, InputStream inputStream,
                                 String fromFileEncoding, ExistsAction existsAction, String fileToWriteChecksum) throws IOException, InstallException {
        return write(tmpFile, installedFiles, fileToWrite, toFileEncoding, new InputStreamFileWriter(inputStream, fromFileEncoding),
                     existsAction, fileToWriteChecksum);
    }

    private static File extractFileType(String resourceLoc, File baseDir, File featureDir, ExistsAction existsAction, List<File> filesInstalled, ZipFile zip,
                                        ProvisioningFeatureDefinition fd, FeatureResource fr, Set<String> processedEntries, Properties checksumInput, Properties checksumOutput,
                                        Set<String> executableFiles, Map<String, Set<String>> extattrFiles,
                                        ChecksumsManager checksumsManager) throws IOException, InstallException {

        // Check if the resource is valid for the current OS. If not, skip it. Note: Only z/OS values are looked at.
        if (!OS_LIST_VALUE.checkOSList(fr)) {
            Logger.getLogger(InstallConstants.LOGGER_NAME).log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_INVALID_FOR_OS", fr.getSymbolicName()));
            return null;
        }
        // if it is an IBM feature we can handled extract of non-bundle content under wlp.
        // According to the cookbook items of type="file" are always paths directly to the resource rather than a containing directory.
        String fileName = resourceLoc + (resourceLoc.endsWith("/") ? fr.getSymbolicName() : "");
        String entryName = "wlp/" + fileName;
        processedEntries.add(entryName);

        // Find the checksum for this entry
        String checksumEntry = null;
        if (checksumInput != null) {
            checksumEntry = checksumInput.getProperty(entryName);
            if (checksumEntry != null) {
                checksumOutput.put(fileName, checksumEntry);
                if (ExistsAction.replace == existsAction)
                    checksumsManager.registerNewChecksums(featureDir, fileName, checksumEntry);
                else if (ExistsAction.ignore == existsAction)
                    checksumsManager.registerExistingChecksums(featureDir, fd.getSymbolicName(), fileName);
            }
        }

        ZipEntry ze = zip.getEntry(entryName);
        if (ze != null) {
            File fileToWrite = new File(baseDir, fileName);
            String asciiCharSet = null;
            String ebcdicCharSet = null;
            boolean replaceChecksum = false;
            if (Platform.isZOS() && !ASCII.equalsIgnoreCase(fr.getFileEncoding()) && (EBCDIC.equalsIgnoreCase(fr.getFileEncoding()) || isScript(fr.getSymbolicName(), zip))) {
                asciiCharSet = InstallPlatformUtils.getASCIISystemCharSet();
                ebcdicCharSet = InstallPlatformUtils.getEBCIDICSystemCharSet();
                replaceChecksum = true;
            }

            if (write(false, filesInstalled, fileToWrite, ebcdicCharSet, zip.getInputStream(ze),
                      asciiCharSet, existsAction, checksumEntry)) {
                if (replaceChecksum) {
                    String cs = HashUtils.getFileMD5String(fileToWrite);
                    checksumsManager.registerNewChecksums(featureDir, fileName, cs);
                }

                if (Boolean.parseBoolean(fr.setExecutablePermission())) {
                    executableFiles.add(fileToWrite.getAbsolutePath());
                }

                String attr = fr.getExtendedAttributes();

                if (null != attr && !attr.equals("")) {
                    attr = attr.toLowerCase();
                    char[] charArr = attr.toCharArray();
                    Arrays.sort(charArr);
                    attr = new String(charArr);
                    Set<String> files = (extattrFiles.containsKey(attr)) ? extattrFiles.get(attr) : new HashSet<String>();

                    files.add(fileToWrite.getAbsolutePath());
                    extattrFiles.put(attr, files);
                }
            }

        } else {
            throw new InstallException(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.missing.content",
                                                                                   fr.getSymbolicName(), entryName), InstallException.BAD_FEATURE_DEFINITION);
        }

        return null;
    }

    /**
     * Look for matching *.bat files in zip file. If there is then assume it is a script
     *
     * @return true if matching *.bat file is found, false otherwise.
     */
    private static boolean isScript(String base, ZipFile zip) {
        Enumeration<? extends ZipEntry> files = zip.entries();
        while (files.hasMoreElements()) {
            ZipEntry f = files.nextElement();
            if ((f.getName()).equals(base + ".bat") || (f.getName()).contains("/" + base + ".bat")) {
                return true;
            }
        }
        return false;
    }

    private static void extractBundleType(ZipEntry ze, ZipFile zip, ExistsAction existsAction, Map<String, FeatureResource> constiuents,
                                          ProvisioningFeatureDefinition fd, List<File> filesInstalled, File baseDir, File featureDir, boolean checkPathInZip,
                                          Properties checksumInput, Properties checksumOutput, ChecksumsManager checksumsManager) throws IOException, InstallException {

        // 1) Write to tmp storage
        // 2) check the manifest against the content headers
        // 3) copy into target place
        String checksumEntry = checksumInput != null ? checksumInput.getProperty(ze.getName()) : null;
        File tmp = File.createTempFile(fd.getSymbolicName(), ".jar");
        write(true, null, tmp, zip.getInputStream(ze), ExistsAction.fail, checksumEntry);

        ZipFile jar = null;
        Manifest man = null;
        try {
            jar = new ZipFile(tmp);
            ZipEntry manEntry = jar.getEntry("META-INF/MANIFEST.MF");
            man = ManifestProcessor.parseManifest(jar.getInputStream(manEntry));
        } catch (ZipException e) {
            throw ExceptionUtils.createByKey(e, "ERROR_INVALID_BUNDLE_IN_ESA", fd.getSymbolicName(), ze.getName());
        } finally {
            InstallUtils.close(jar);
        }
        Attributes mainAttr = man.getMainAttributes();
        String bundleSymbolicName = mainAttr.getValue(Constants.BUNDLE_SYMBOLICNAME);
        if (bundleSymbolicName == null) {
            // The jar is not laid down because it is not an OSGI bundle
            InstallUtils.delete(tmp);
            return;
        }
        NameValuePair nvp = ManifestHeaderProcessor.parseBundleSymbolicName(bundleSymbolicName);
        String symbolicName = nvp.getName();
        String versionStr = mainAttr.getValue(Constants.BUNDLE_VERSION);
        Version version = Version.parseVersion(versionStr);
        FeatureResource fr = constiuents.get(symbolicName);

        // If it is null then we don't have an entry in the manifest for this JAR or this JAR was listed as
        // type="file" and therefore has already been handled.  Therefore ignore it as we aren't interested!
        boolean deleteTmp = true;
        if (fr != null) {
            VersionRange vr = fr.getVersionRange();

            if (vr != null && vr.includes(version)) {
                String location = fr.getLocation();
                if (location == null) {
                    location = "lib/";
                } else if (location.contains(",")) {
                    int index = location.indexOf(',');
                    location = location.substring(0, index);
                }

                // Do a test that we have the right JAR, can't do this with bundles as they are always
                // in root hence why wrapped with the checkPathInZip boolean
                if (checkPathInZip) {
                    String entryName = ze.getName();

                    // We know we aren't in root as we can only do this check if we are not in root so don't need to check for an index
                    // of -1. Also remove the wlp
                    String pathInZip = entryName.substring(4, entryName.lastIndexOf("/") + 1);
                    if (!location.equals(pathInZip)) {
                        if (deleteTmp) {
                            InstallUtils.delete(tmp);
                        }
                        return;
                    }
                }

                final File targetFile;
                final String filePathInInstall;
                if (location.endsWith(".jar")) {
                    filePathInInstall = location;
                    targetFile = new File(baseDir, location);
                } else {
                    File dir = new File(baseDir, location);
                    InstallUtils.mkdirs(filesInstalled, dir);
                    String fileName = symbolicName + "_" +
                                      version.getMajor() + "." + version.getMinor() + "." + version.getMicro() +
                                      ".jar";
                    filePathInInstall = location + fileName;
                    targetFile = new File(dir, fileName);
                }

                if (!targetFile.exists()) {
                    if (!tmp.renameTo(targetFile)) {
                        // copy, but only if the bundle isn't there. We ignore it if we can.
                        FileInputStream fis = null;
                        try {
                            fis = new FileInputStream(tmp);
                            write(false, null, targetFile, fis, ExistsAction.ignore, checksumEntry);
                        } finally {
                            if (fis != null)
                                fis.close();
                        }
                    } else {
                        deleteTmp = false;
                    }

                    if (targetFile.exists()) {
                        filesInstalled.add(targetFile);
                        constiuents.remove(symbolicName);

                        // We've put the file on disk so add the checksum entry for it
                        if (checksumEntry != null) {
                            checksumOutput.put(filePathInInstall, checksumEntry);
                        }
                    }
                } else {
                    /*
                     * The file already exists, do we need to fail? Not if it only exists because we have already installed it (i.e. through a dependency to another
                     * feature)
                     */
                    if (existsAction == ExistsAction.fail && !filesInstalled.contains(targetFile) && !InstallUtils.isFileSame(targetFile, symbolicName, version, checksumEntry)) {
                        if (deleteTmp) {
                            InstallUtils.delete(tmp);
                        }
                        throw new InstallException(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.file.exists", targetFile), InstallException.IO_FAILURE);
                    } else if (existsAction == ExistsAction.replace) {
                        FileInputStream fis = null;
                        try {
                            fis = new FileInputStream(tmp);
                            write(false, null, targetFile, fis, ExistsAction.replace, checksumEntry);
                            constiuents.remove(symbolicName);
                            if (checksumEntry != null) {
                                checksumOutput.put(filePathInInstall, checksumEntry);
                                checksumsManager.registerNewChecksums(featureDir, filePathInInstall, checksumEntry);
                            }
                        } finally {
                            if (fis != null)
                                fis.close();
                        }
                    } else {
                        // It's fine it is there so remove it from processing
                        constiuents.remove(symbolicName);
                        if (checksumEntry != null) {
                            checksumOutput.put(filePathInInstall, checksumEntry);
                            checksumsManager.registerExistingChecksums(featureDir, fd.getSymbolicName(), filePathInInstall);
                        }
                    }
                }
            }
        }
        if (deleteTmp) {
            InstallUtils.delete(tmp);
        }
    }

    private static class PropertiesFileWriter implements FileWriter {

        private final Properties properties;

        public PropertiesFileWriter(Properties properties) {
            this.properties = properties;
        }

        @Override
        public void writeToFile(File fileToWrite) throws IOException {
            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(fileToWrite);
                properties.store(fOut, null);
            } finally {
                InstallUtils.close(fOut);
            }
        }

        /**
         * The charsetName is ignored because properties files only use ISO 8859-1
         * Same as writeToFile(File fileToWrite)
         */
        @Override
        public void writeToFile(File fileToWrite, String charsetName) throws IOException {
            writeToFile(fileToWrite);
        }

    }

    private static void isFileLocked(ProvisioningFeatureDefinition targetFd, File f) throws InstallException {
        InstallUtils.isFileLocked("ERROR_UNINSTALL_FEATURE_FILE_LOCKED", targetFd.getFeatureName(), f);
    }

    /**
     * @param provisioningFeatureDefinition
     * @param featureDefinitions
     * @param baseDir
     */
    public static void preCheck(UninstallAsset uninstallAsset, ProvisioningFeatureDefinition targetFd, File baseDir) throws InstallException {
        List<File> filesToDelete = uninstallAsset.getFeatureFileList();

        // check the fixes to be uninstalled
        for (IFixInfo fixInfo : uninstallAsset.getFixUpdatesFeature())
            FixAdaptor.preCheck(fixInfo, baseDir);

        // check the files to be uninstalled
        for (File f : filesToDelete)
            isFileLocked(targetFd, f);

        // check the feature license folder
        isFileLocked(targetFd, new File(baseDir, "/lafiles/" + targetFd.getSymbolicName()));

        // check the icons folder
        isFileLocked(targetFd, new File(baseDir, uninstallAsset.getFeaturePath() + "icons/" + targetFd.getSymbolicName()));
    }
}