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
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallConstants.DownloadOption;
import com.ibm.ws.install.InstallEventListener;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallProgressEvent;
import com.ibm.ws.install.RepositoryConfig;
import com.ibm.ws.install.RepositoryConfigUtils;
import com.ibm.ws.install.internal.InstallKernelImpl;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.utility.cmdline.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.feature.internal.cmdline.ArgumentsImpl;

/**
 * This API contains methods to execute the Download Action.
 */
public class DownloadAction extends RequireAcceptLicenseAction {

    private File localDir;
    private String localDirString;
    private boolean isOverwrite;
    private InstallEventListener ielistener;
    private InstallEventListener ieNoStepListener;
    private final DownloadOption downloadOption = DownloadOption.all;
    private HashSet<String> assetIds;
    private boolean isDirExistBefore;
    private boolean isBadConnectionFound = false;

    @Override
    ReturnCode initialize(Arguments args) {
        super.initialize(args);
        installKernel.addListener(getNoStepListener(), InstallConstants.EVENT_TYPE_PROGRESS);
        if (argList.isEmpty()) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   CmdUtils.getMessage("ERROR_NO_ARGUMENT", "download"));
            Action.help.handleTask(new ArgumentsImpl(new String[] { "help", "download" }));
            return ReturnCode.BAD_ARGUMENT;
        }

        assetIds = new HashSet<String>(argList);

        localDirString = args.getOption("location");
        isOverwrite = args.getOption("overwrite") != null;

        if (localDirString == null) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_MISSING_DIRECTORY", "download"));
            return ReturnCode.BAD_ARGUMENT;
        }

        if (localDirString.isEmpty()) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DIRECTORY_REQUIRED", "location"));
            return ReturnCode.BAD_ARGUMENT;
        }

        //Get valid configured repositories and prompt user if authentication is required
        try {
            ReturnCode rc = CmdUtils.checkRepositoryStatus(installKernel, repoProperties, "download", getRepoZipForBuild());
            if (rc.equals(ReturnCode.BAD_CONNECTION_FOUND)) {
                rc = ReturnCode.OK;
                isBadConnectionFound = true;
            }
            if (rc.equals(ReturnCode.USER_ABORT)) {
                return rc;
            }
        } catch (InstallException e) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   e.getMessage(), e);
            return InstallUtilityExecutor.returnCode(e.getRc());
        }

        try {
            installKernel.resolve(assetIds, true);
            featureLicenses = installKernel.getFeatureLicense(Locale.getDefault());
            sampleLicenses = installKernel.getSampleLicense(Locale.getDefault());
        } catch (InstallException e) {
            InstallException newError = CmdUtils.convertToBadConnectionError(e, isBadConnectionFound);

            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   newError.getMessage(), newError);
            return InstallUtilityExecutor.returnCode(newError.getRc());
        }
        removeEventListener();
        return ReturnCode.OK;
    }

    private File getDownloadOnlyDirectory(String downloadDir) {
        File toDir = new File(downloadDir);
        if (toDir.exists()) {
            isDirExistBefore = true;
            if (!toDir.isDirectory()) {
                InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                       Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DOWNLOADONLY_IS_FILE", toDir.getAbsolutePath()));
                return null;
            }

            //check if directory is readable
            if (toDir.list() == null) {
                InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                       Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DIRECTORY_INACCESSIBLE", toDir.getAbsolutePath()));
                return null;
            }

            try {
                File test = new File(toDir, "test");
                test.createNewFile();
                test.delete();
            } catch (Exception e) {
                InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                       Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DIRECTORY_INACCESSIBLE", toDir.getAbsolutePath()));
                return null;
            }

            if (toDir.isDirectory() && toDir.list().length > 0) {
                if (!CmdUtils.isValidDirectoryRepo(toDir)) {
                    InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                           Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_INVALID_DOWNLOAD_DIRECTORY", toDir.getAbsolutePath()));
                    return null;
                }
            }
        } else {
            isDirExistBefore = false;
            if (!toDir.mkdirs()) {
                InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                       Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DOWNLOADONLY_UNABLE_TO_CREATE_DIR", toDir.getAbsolutePath()));
                return null;
            }

        }
        return toDir;
    }

    private void checkForRepoConfig() {
        boolean isRepoAlreadyOnRepoConfig = false;
        List<RepositoryConfig> repositoryConfigs;
        try {
            repositoryConfigs = RepositoryConfigUtils.getRepositoryConfigs(repoProperties);
            for (RepositoryConfig config : repositoryConfigs) {
                String url = config.getUrl();
                if (url != null && url.toLowerCase().startsWith("file:")) {
                    try {
                        URL urlProcessed = new URL(url);
                        File repoDir = new File(urlProcessed.getPath());
                        if (repoDir.exists() && repoDir.isDirectory()) {
                            if (repoDir.getAbsolutePath().toLowerCase().equals(localDir.getAbsolutePath().toLowerCase())) {
                                isRepoAlreadyOnRepoConfig = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        break;
                    }
                }
            }
            if (isRepoAlreadyOnRepoConfig) {
                InstallLogUtils.getInstallLogger().log(Level.INFO,
                                                       CmdUtils.getMessage("MSG_DIRECTORY_REPO_CONFIGURED", localDir.toURI().toURL().toString()));
            } else {
                InstallLogUtils.getInstallLogger().log(Level.INFO,
                                                       CmdUtils.getMessage("MSG_DIRECTORY_REPO_NOT_CONFIGURED", localDir.toURI().toURL().toString()));
            }

        } catch (Exception e) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   e.getMessage(), e);
        }

    }

    @Override
    ReturnCode execute() {
        ReturnCode rc = super.execute();
        if (!!!rc.equals(ReturnCode.OK)) {
            return rc;
        }
        try {
            localDir = getDownloadOnlyDirectory(localDirString);
            if (localDir == null) {
                return ReturnCode.IO_FAILURE;
            }
            if (!installKernel.resolveExistingAssetsFromDirectoryRepo(assetIds, localDir, isOverwrite)) {
                InstallLogUtils.getInstallLogger().log(Level.INFO,
                                                       CmdUtils.getMessage("MSG_ALL_DOWNLOADING_FILES_EXISTS", localDir.getAbsolutePath()));
                checkForRepoConfig();
                return ReturnCode.ALREADY_EXISTS;
            }
        } catch (InstallException e) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   e.getMessage(), e);
            if (!!!isDirExistBefore) {
                localDir.delete();
            }
            return InstallUtilityExecutor.returnCode(e.getRc());
        }
        try {
            installKernel.addListener(getListener(), InstallConstants.EVENT_TYPE_PROGRESS);
            Map<String, Collection<String>> downloadedAssets = ((InstallKernelImpl) installKernel).downloadAssetsInstallUtility(assetIds, localDir, downloadOption, null, null,
                                                                                                                                isOverwrite);

            if (downloadedAssets != null && !downloadedAssets.isEmpty()) {
                Collection<String> downloadedFeatures = downloadedAssets.get(InstallConstants.FEATURES);
                if (downloadedFeatures != null && !downloadedFeatures.isEmpty())
                    InstallLogUtils.getInstallLogger().log(Level.FINE,
                                                           CmdUtils.getMessage("MSG_DOWNLOADED_FEATURES", InstallUtils.getFeatureListOutput(downloadedFeatures)));
                Collection<String> downloadedAddons = downloadedAssets.get(InstallConstants.ADDONS);
                if (downloadedAddons != null && !downloadedAddons.isEmpty())
                    InstallLogUtils.getInstallLogger().log(Level.FINE,
                                                           CmdUtils.getMessage("MSG_DOWNLOADED_ADDONS", InstallUtils.getFeatureListOutput(downloadedAddons)));
                Collection<String> downloadedSamples = downloadedAssets.get(InstallConstants.SAMPLES);
                if (downloadedSamples != null && !downloadedSamples.isEmpty())
                    InstallLogUtils.getInstallLogger().log(Level.FINE,
                                                           CmdUtils.getMessage("MSG_DOWNLOADED_SAMPLES", InstallUtils.getFeatureListOutput(downloadedSamples)));
                Collection<String> downloadedOpensources = downloadedAssets.get(InstallConstants.OPENSOURCE);
                if (downloadedOpensources != null && !downloadedOpensources.isEmpty())
                    InstallLogUtils.getInstallLogger().log(Level.FINE,
                                                           CmdUtils.getMessage("MSG_DOWNLOADED_OPENSOURCE", InstallUtils.getFeatureListOutput(downloadedOpensources)));
            }

            InstallLogUtils.getInstallLogger().log(Level.INFO,
                                                   Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_DOWNLOAD_FILES_OK"));

            checkForRepoConfig();

            return ReturnCode.OK;
        } catch (InstallException ie) {
            String errMsg = ie.getMessage();
            if (errMsg.contains("CWWKF1247E")) {
                errMsg = CmdUtils.getMessage("ERROR_DOWNLOAD_ALREADY_INSTALLED", assetIds.toString());
            }
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   errMsg, ie);
            if (!!!isDirExistBefore) {
                localDir.delete();
            }
            return InstallUtilityExecutor.returnCode(ie.getRc());
        } catch (Exception e) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   e.getMessage(), e);

            if (!!!isDirExistBefore) {
                localDir.delete();
            }
            return ReturnCode.RUNTIME_EXCEPTION;

        } finally {
            removeEventListener();
        }
    }

    private InstallEventListener getNoStepListener() {
        if (ieNoStepListener == null) {
            ieNoStepListener = new InstallEventListener() {
                @Override
                public void handleInstallEvent(InstallProgressEvent event) {
                    if (event.state == InstallProgressEvent.RESOLVE) {
                        InstallLogUtils.getInstallLogger().log(Level.INFO,
                                                               event.message);
                    }
                }
            };
        }
        return ieNoStepListener;
    }

    private InstallEventListener getListener() {
        int numInstallResources = installKernel.getPublicInstallResourcesSize();
        int constantSteps = 4;
        final int progressSteps = numInstallResources + constantSteps;
        if (ielistener == null) {
            ielistener = new InstallEventListener() {
                int progressCurrentStep = 0;

                @Override
                public void handleInstallEvent(InstallProgressEvent event) {
                    if (event.state != InstallProgressEvent.POST_INSTALL) {
                        InstallLogUtils.getInstallLogger().log(Level.INFO,
                                                               Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("PROGRESS_STEP", ++progressCurrentStep, progressSteps) + ": "
                                                                           + event.message);
                    }
                }
            };
        }
        return ielistener;
    }

    private void removeEventListener() {
        installKernel.removeListener(getListener());
        ielistener = null;
    }

    @Override
    protected void showMessagesForAdditionalFeatures() {
        Collection<String> samplesOrOpenSources = installKernel.getSamplesOrOpenSources();
        if (!samplesOrOpenSources.isEmpty()) {
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, CmdUtils.getMessage("MSG_DOWNLOAD_ADDITIONAL_FEATURES_FOR_SAMPLES", InstallUtils.getFeatureListOutput(samplesOrOpenSources)));
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, CmdUtils.getMessage("MSG_DOWNLOAD_ACCEPT_LICENSE_FOR_ADDITIONAL_FEATURES"));
        }
    }

    private String getRepoZipForBuild() {
        String zipRepoPath = System.getProperty("INTERNAL_DOWNLOAD_FROM_FOR_BUILD");
        if (zipRepoPath != null) {
            logger.log(Level.FINEST, "INTERNAL_DOWNLOAD_FROM_FOR_BUILD=" + zipRepoPath);
            File zipRepoFile = new File(zipRepoPath);
            if (zipRepoFile.exists()) {
                return zipRepoPath;
            }
        }
        return null;
    }
}