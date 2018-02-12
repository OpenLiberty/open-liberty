/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2014, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.utility.internal.cmdline;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallEventListener;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallProgressEvent;
import com.ibm.ws.install.internal.ArchiveUtils;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.internal.asset.ServerAsset;
import com.ibm.ws.install.internal.asset.ServerPackageAsset;
import com.ibm.ws.install.utility.cmdline.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.feature.internal.cmdline.ArgumentsImpl;

import wlp.lib.extract.SelfExtract;

/**
 * This API is used to execute the Install Action.
 */
public class InstallAction extends RequireAcceptLicenseAction {

    private boolean isBadConnectionFound = false;

    private static enum JarContentType {
        ADDON(ArchiveUtils.ArchiveContentType.ADDON, false),
        INSTALL(ArchiveUtils.ArchiveContentType.INSTALL, true),
        SAMPLE(ArchiveUtils.ArchiveContentType.SAMPLE, true),
        OPENSOURCE(ArchiveUtils.ArchiveContentType.OPENSOURCE, false),
        UNKNOWN(null, false);

        private final ArchiveUtils.ArchiveContentType type;
        private final boolean supported;

        private JarContentType(ArchiveUtils.ArchiveContentType type, boolean supported) {
            this.type = type;
            this.supported = supported;
        }

        boolean isServerPackage() {
            return (null == this.type) ? false : this.type.isServerPackage();
        }

        boolean isSupported() {
            return this.supported;
        }
    }

    class InstallPackage {

        private File packageFile = null;
        private boolean serverPackage = false;

        InstallPackage(String fileName) throws InstallException, Throwable {
            initialize(fileName);
        }

        ReturnCode initialize(String fileName) throws InstallException, Throwable {
            packageFile = getValidPackageFile(fileName);

            if (ArchiveUtils.ArchiveFileType.JAR.isType(fileName)) {
                Map<String, String> manifestAttrs = ArchiveUtils.processArchiveManifest(packageFile);
                JarContentType jarType = determineJarContentType(manifestAttrs);

                if (!jarType.isSupported()) {
                    throw new InstallException(CmdUtils.getMessage("ERROR_ARCHIVE_NOT_SUPPORT", packageFile.getAbsolutePath()), InstallException.RUNTIME_EXCEPTION);
                }

                serverPackage = jarType.isServerPackage();

                if (JarContentType.SAMPLE.equals(jarType)) {
                    String license = ArchiveUtils.getLicenseAgreement(new JarFile(packageFile), manifestAttrs);

                    if (license != null) {
                        sampleLicenses = new HashSet<String>();
                        sampleLicenses.add(license);
                    }
                }

            } else if (ArchiveUtils.ArchiveFileType.ZIP.isType(fileName) || ArchiveUtils.ArchiveFileType.PAX.isType(fileName)) {
                serverPackage = true;
            }

            return ReturnCode.OK;
        }

        private File getValidPackageFile(String fileName) throws InstallException {
            File f = new File(fileName);
            if (!f.exists())
                throw new InstallException(CmdUtils.getMessage("ERROR_DEPOLY_SERVER_PACKAGE_FILE_NOTEXIST", f.getAbsolutePath()), InstallException.RUNTIME_EXCEPTION);
            if (f.isDirectory())
                throw new InstallException(CmdUtils.getMessage("ERROR_DEPOLY_DIRECTORY", f.getAbsolutePath()), InstallException.RUNTIME_EXCEPTION);
            if (isPackage(fileName))
                return f;
            throw new InstallException(CmdUtils.getMessage("ERROR_ARCHIVE_NOT_SUPPORT", f.getAbsolutePath()), InstallException.RUNTIME_EXCEPTION);
        }

        boolean isServerPackage() {
            return this.serverPackage;
        }

        File getPackageFile() {
            return this.packageFile;
        }

        private JarContentType determineJarContentType(Map<String, String> manifestAttrs) {
            String archiveContentType = manifestAttrs.get(ArchiveUtils.ARCHIVE_CONTENT_TYPE);

            if (ArchiveUtils.ArchiveContentType.INSTALL.isContentType(archiveContentType)) {
                return JarContentType.INSTALL;
            } else if (ArchiveUtils.ArchiveContentType.SAMPLE.isContentType(archiveContentType)) {
                return JarContentType.SAMPLE;
            } else if (ArchiveUtils.ArchiveContentType.ADDON.isContentType(archiveContentType)) {
                return JarContentType.ADDON;
            } else if (ArchiveUtils.ArchiveContentType.OPENSOURCE.isContentType(archiveContentType)) {
                return JarContentType.OPENSOURCE;
            } else {
                return JarContentType.UNKNOWN;
            }
        }
    }

    // Supported options
    private String repoType;

    private InstallPackage installPackage = null;
    private Set<String> featureIds = null;
    private Set<ServerAsset> servers = null;
    private Logger logger;

    private InstallEventListener ielistener;
    private InstallEventListener ieNoStepListener;

    private boolean downloadDependencies = false;
    private boolean agreedToDownloadDependencies = false;
    private File esaFile = null;
    private String fromDir;

    @Override
    ReturnCode initialize(Arguments args) {
        super.initialize(args);

        logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
        String downloadDependenciesOption = args.getOption("downloaddependencies");
        downloadDependencies = downloadDependenciesOption != null && !downloadDependenciesOption.isEmpty();
        if (downloadDependencies) {
            if (downloadDependenciesOption.equalsIgnoreCase("false"))
                agreedToDownloadDependencies = false;
            else if (downloadDependenciesOption.equalsIgnoreCase("true"))
                agreedToDownloadDependencies = true;
            else {
                logger.log(Level.SEVERE, CmdUtils.getMessage("ERROR_INVALID_DOWNLOAD_DEPENDENCIES_VALUE", downloadDependenciesOption));
                return ReturnCode.BAD_ARGUMENT;
            }
        }

        if (argList.isEmpty()) {
            logger.log(Level.SEVERE, CmdUtils.getMessage("ERROR_NO_ARGUMENT", "install"));
            Action.help.handleTask(new ArgumentsImpl(new String[] { "help", "install" }));
            return ReturnCode.BAD_ARGUMENT;
        }
        repoType = args.getOption("to");

        String arg = argList.get(0);

        // Initialize feature and server list
        featureIds = new HashSet<String>();
        servers = new TreeSet<ServerAsset>();

        fromDir = args.getOption("from");

        if (fromDir != null && fromDir.isEmpty()) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DIRECTORY_REQUIRED", "from"));
            return ReturnCode.BAD_ARGUMENT;
        }

        try {
            if (isPackage(arg)) {
                installPackage = new InstallPackage(arg);
                if (installPackage.isServerPackage()) {
                    //return getServerPackageFeatureLicense(installPackage.getPackageFile());
                }
                return ReturnCode.OK;
            } else if (isServer(arg)) {
                return serverInit(arg);
            } else if (isValidEsa(arg)) {
                return esaInstallInit(arg);
            } else {
                try {
                    Collection<String> assetIds = new HashSet<String>(this.argList);
                    installKernel.checkAssetsNotInstalled(assetIds);
                    return assetInstallInit(assetIds);
                } catch (InstallException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    return InstallUtilityExecutor.returnCode(e.getRc());
                }
            }
        } catch (InstallException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return InstallUtilityExecutor.returnCode(e.getRc());
        } catch (Throwable e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return InstallUtilityExecutor.returnCode(InstallException.IO_FAILURE);
        }
    }

    private ReturnCode assetInstallInit(Collection<String> featureIdSet) {
        featureIds.addAll(featureIdSet);
        //Get valid configured repositories and prompt user if authentication is required
        try {
            ReturnCode rc = CmdUtils.checkRepositoryStatus(installKernel, repoProperties, "install", fromDir);
            if (rc.equals(ReturnCode.BAD_CONNECTION_FOUND)) {
                rc = ReturnCode.OK;
                isBadConnectionFound = true;
            }
            if (rc.equals(ReturnCode.USER_ABORT)) {
                return rc;
            }
        } catch (InstallException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return InstallUtilityExecutor.returnCode(e.getRc());
        }

        try {
            installKernel.addListener(getNoStepListener(), InstallConstants.EVENT_TYPE_PROGRESS);
            installKernel.resolve(featureIdSet, false);
            featureLicenses = installKernel.getFeatureLicense(Locale.getDefault());
            sampleLicenses = installKernel.getSampleLicense(Locale.getDefault());
        } catch (InstallException e) {
            InstallException newError = CmdUtils.convertToBadConnectionError(e, isBadConnectionFound);
            logger.log(newError.getRc() == InstallException.ALREADY_EXISTS ? Level.INFO : Level.SEVERE, newError.getMessage(), newError);
            return InstallUtilityExecutor.returnCode(newError.getRc());
        }
        return ReturnCode.OK;
    }

    private ReturnCode esaInstallInit(String esaPath) {
        esaFile = new File(esaPath);
        try {
            String feature = InstallUtils.getFeatureName(esaFile);
            installKernel.addListener(getNoStepListener(), InstallConstants.EVENT_TYPE_PROGRESS);
            installKernel.resolve(feature, esaFile, repoType);
            featureLicenses = installKernel.getFeatureLicense(Locale.getDefault());
        } catch (InstallException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return InstallUtilityExecutor.returnCode(e.getRc());
        }
        return ReturnCode.OK;
    }

    private ReturnCode serverInit(String fileName) throws InstallException, IOException {

        File serverXML = (fileName.toLowerCase().endsWith(InstallUtils.SERVER_XML)) ? new File(fileName) : new File(InstallUtils.getServersDir(), fileName + File.separator
                                                                                                                                                  + InstallUtils.SERVER_XML);

        if (!serverXML.isFile()) {
            throw new InstallException(CmdUtils.getMessage("ERROR_UNABLE_TO_FIND_SERVER_XML", serverXML.getParent()), InstallException.RUNTIME_EXCEPTION);
        }

        servers.add(new ServerAsset(serverXML));

        return ReturnCode.OK;
    }

    @Override
    protected ReturnCode handleLicenseAcceptance() {
        ReturnCode rc = super.handleLicenseAcceptance();
        if (!!!rc.equals(ReturnCode.OK)) {
            return rc;
        }

        if (sampleLicenses != null && !sampleLicenses.isEmpty()) {
            logger.log(Level.INFO, CmdUtils.getMessage("MSG_REQUIRE_DOWNLOAD_DEPENDENCIES"));
            logger.log(Level.INFO, "");
            if (downloadDependencies) {
                logger.log(Level.INFO, CmdUtils.getMessage(agreedToDownloadDependencies ? "MSG_DOWNLOAD_DEPENDENCIES" : "MSG_NOT_DOWNLOAD_DEPENDENCIES", "--downloadDependencies",
                                                           agreedToDownloadDependencies));
            } else {
                for (String license : sampleLicenses) {
                    logger.log(Level.INFO, license);
                    logger.log(Level.INFO, "");
                }
                agreedToDownloadDependencies = SelfExtract.getResponse(SelfExtract.format("externalDepsPrompt", new Object[] { "[1]", "[2]" }),
                                                                       "1", "2", "1");
            }
            logger.log(Level.INFO, "");
        }
        return ReturnCode.OK;
    }

    @Override
    protected ReturnCode viewLicense(boolean showAgreement) {
        ReturnCode rc = super.viewLicense(showAgreement);
        if (!!!rc.equals(ReturnCode.OK)) {
            return rc;
        }
        if (showAgreement && sampleLicenses != null && !sampleLicenses.isEmpty()) {
            logger.log(Level.INFO, CmdUtils.getMessage("MSG_REQUIRE_DOWNLOAD_DEPENDENCIES"));
            logger.log(Level.INFO, "");
            for (String license : sampleLicenses) {
                logger.log(Level.INFO, license);
                logger.log(Level.INFO, "");
            }
        }
        return ReturnCode.OK;
    }

    private boolean isPackage(String fileName) {
        return ArchiveUtils.ArchiveFileType.ZIP.isType(fileName) || ArchiveUtils.ArchiveFileType.PAX.isType(fileName) || ArchiveUtils.ArchiveFileType.JAR.isType(fileName);
    }

    private boolean isValidEsa(String fileName) {
        return ArchiveUtils.ArchiveFileType.ESA.isType(fileName);
    }

    private boolean isServer(String fileName) {
        return new File(InstallUtils.getServersDir(), fileName).isDirectory()
               || fileName.toLowerCase().endsWith("server.xml");
    }

    private ReturnCode install() {
        try {
            installKernel.checkResources();
        } catch (InstallException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return InstallUtilityExecutor.returnCode(e.getRc());
        }
        ReturnCode rc = super.execute();
        if (!!!rc.equals(ReturnCode.OK)) {
            return rc;
        }
        Map<String, Collection<String>> installedAssets = null;
        try {
            installKernel.addListener(getInstallListener(), InstallConstants.EVENT_TYPE_PROGRESS);
            installedAssets = installKernel.install(repoType, false, agreedToDownloadDependencies);
        } catch (InstallException ie) {
            logger.log(Level.SEVERE, ie.getMessage(), ie);
            return InstallUtilityExecutor.returnCode(ie.getRc());
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return ReturnCode.RUNTIME_EXCEPTION;
        } finally {
            removeEventListener(true);
        }
        if (installedAssets != null && !installedAssets.isEmpty()) {
            Collection<String> installedFeatures = installedAssets.get(InstallConstants.FEATURE);
            if (installedFeatures != null && !installedFeatures.isEmpty())
                logger.log(Level.FINE, CmdUtils.getMessage("MSG_INSTALLED_FEATURES", InstallUtils.getFeatureListOutput(installedFeatures)));
            Collection<String> installedAddons = installedAssets.get(InstallConstants.ADDON);
            if (installedAddons != null && !installedAddons.isEmpty())
                logger.log(Level.FINE, CmdUtils.getMessage("MSG_INSTALLED_ADDONS", InstallUtils.getFeatureListOutput(installedAddons)));
            Collection<String> installedSamples = installedAssets.get(InstallConstants.SAMPLE);
            if (installedSamples != null && !installedSamples.isEmpty())
                logger.log(Level.FINE, CmdUtils.getMessage("MSG_INSTALLED_SAMPLES", InstallUtils.getFeatureListOutput(installedSamples)));
            Collection<String> installedOpenSource = installedAssets.get(InstallConstants.OPENSOURCE);
            if (installedOpenSource != null && !installedOpenSource.isEmpty())
                logger.log(Level.FINE, CmdUtils.getMessage("MSG_INSTALLED_OPENSOURCE", InstallUtils.getFeatureListOutput(installedOpenSource)));
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("TOOL_INSTALLATION_COMPLETED"));
        }

        return ReturnCode.OK;
    }

    private ReturnCode deployServerPackage() {
        ReturnCode rc = super.execute();
        if (!!!rc.equals(ReturnCode.OK)) {
            return rc;
        }

        try {
            // Add listener.  There are 4 steps for deploying a server package.
            installKernel.addListener(getListener(4), InstallConstants.EVENT_TYPE_PROGRESS);
            ServerPackageAsset spa = installKernel.deployServerPackage(installPackage.getPackageFile(),
                                                                       repoType,
                                                                       agreedToDownloadDependencies);
            servers.addAll(spa.getServers());
        } catch (InstallException ie) {
            logger.log(Level.SEVERE, ie.getMessage(), ie);
            return InstallUtilityExecutor.returnCode(ie.getRc());
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return ReturnCode.RUNTIME_EXCEPTION;
        } finally {
            removeEventListener(true);
        }

        return rc;
    }

    private ReturnCode installServerFeatures() {
        ReturnCode rc = ReturnCode.OK;
        Collection<String> featuresToInstall = new HashSet<String>();

        try {
            installKernel.addListener(getNoStepListener(), InstallConstants.EVENT_TYPE_PROGRESS);
            featuresToInstall.addAll(installKernel.getServerFeaturesToInstall(servers, false));
        } catch (InstallException ie) {
            logger.log(Level.SEVERE, ie.getMessage(), ie);
            return InstallUtilityExecutor.returnCode(ie.getRc());
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            rc = ReturnCode.RUNTIME_EXCEPTION;
        } finally {
            removeEventListener(false);
        }

        if (featuresToInstall.isEmpty()) {
            logger.log(Level.INFO, CmdUtils.getMessage("MSG_SERVER_NEW_FEATURES_NOT_REQUIRED"));
        } else {
            logger.log(Level.INFO, CmdUtils.getMessage("MSG_SERVER_NEW_FEATURES_REQUIRED", InstallUtils.getFeatureListOutput(featuresToInstall)));
            rc = assetInstallInit(featuresToInstall);
        }

        return rc;
    }

    @Override
    ReturnCode execute() {
        ReturnCode rc = ReturnCode.OK;

        // Order is important.
        // First deploy any server packages
        if (null != installPackage) {
            rc = deployServerPackage();
        }

        // Second check any newly deployed servers for missing required features
        if (ReturnCode.OK.equals(rc) && !servers.isEmpty()) {
            rc = installServerFeatures();
        }

        // Third install features
        if (ReturnCode.OK.equals(rc) && (null != esaFile || !featureIds.isEmpty())) {
            rc = install();
        }

        if (ReturnCode.OK.equals(rc)) {
            if (null != installPackage && installPackage.isServerPackage()) {
                logger.log(Level.INFO, CmdUtils.getMessage("MSG_DEPLOY_SERVER_PACKAGE_OK"));
            }

            if (!!!validateProduct()) {
                return ReturnCode.NOT_VALID_FOR_CURRENT_PRODUCT;
            }
        }

        return rc;
    }

    private InstallEventListener getNoStepListener() {
        if (ieNoStepListener == null) {
            ieNoStepListener = new InstallEventListener() {
                @Override
                public void handleInstallEvent(InstallProgressEvent event) {
                    if (event.state == InstallProgressEvent.RESOLVE) {
                        logger.log(Level.INFO, event.message);
                    }
                }
            };
        }
        return ieNoStepListener;
    }

    private InstallEventListener getInstallListener() {
        int numInstallResources = installKernel.getPublicInstallResourcesSize();
        int numInstallAssets = installKernel.getPublicLocalInstallAssetsSize();;

        return getListener(numInstallResources * 2 + numInstallAssets + 1 + 1);
    }

    private InstallEventListener getListener(final int progressSteps) {
        if (ielistener == null) {
            ielistener = new InstallEventListener() {
                int progressCurrentStep = 0;

                @Override
                public void handleInstallEvent(InstallProgressEvent event) {
                    if (event.state != InstallProgressEvent.POST_INSTALL) {
                        logger.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("PROGRESS_STEP", ++progressCurrentStep, progressSteps) + ": " + event.message);
                    }
                }
            };
        }
        return ielistener;
    }

    private void removeEventListener(boolean printNewline) {
        if (printNewline)
            logger.log(Level.INFO, "");

        if (null != ielistener) {
            installKernel.removeListener(ielistener);
            ielistener = null;
        }

        if (null != ieNoStepListener) {
            installKernel.removeListener(ieNoStepListener);
            ieNoStepListener = null;
        }
    }

    @Override
    protected void showMessagesForAdditionalFeatures() {

        boolean show = false;
        if (installPackage != null) {
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, CmdUtils.getMessage("MSG_ADDITIONAL_FEATURES_FOR_SERVER_PACKAGE"));
            show = true;
        } else if (!servers.isEmpty()) {
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, CmdUtils.getMessage("MSG_ADDITIONAL_FEATURES_FOR_SERVER"));
            show = true;
        }

        Collection<String> samplesOrOpenSources = installKernel.getSamplesOrOpenSources();
        if (!samplesOrOpenSources.isEmpty()) {
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, CmdUtils.getMessage("MSG_INSTALL_ADDITIONAL_FEATURES_FOR_SAMPLES", InstallUtils.getFeatureListOutput(samplesOrOpenSources)));
            show = true;
        }

        if (show) {
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, CmdUtils.getMessage("MSG_INSTALL_ACCEPT_LICENSE_FOR_ADDITIONAL_FEATURES"));
        }
    }

}