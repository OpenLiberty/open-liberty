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
package com.ibm.ws.install.internal.cmdline;

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallConstants.DownloadOption;
import com.ibm.ws.install.InstallConstants.ExistsAction;
import com.ibm.ws.install.InstallEventListener;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernel;
import com.ibm.ws.install.InstallKernelFactory;
import com.ibm.ws.install.InstallLicense;
import com.ibm.ws.install.InstallProgressEvent;
import com.ibm.ws.install.RepositoryConfigUtils;
import com.ibm.ws.install.internal.InstallKernelImpl;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.feature.internal.cmdline.NLS;
import com.ibm.ws.kernel.feature.internal.cmdline.ReturnCode;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.CommandTaskRegistry;
import com.ibm.ws.product.utility.ExecutionContext;
import com.ibm.ws.product.utility.extension.ValidateCommandTask;

import wlp.lib.extract.SelfExtract;

/**
 * This API contains methods to execute the Install Action.
 */
public class ExeInstallAction implements ActionHandler {

    static final private String UNSPECIFIED_LICENSE_TYPE = "UNSPECIFIED";

    // Supported options
    private String[] featureIds;
    private Set<String> featureIdSetRaw;
    private Set<String> featureIdSet;
    private String repoType;
    private boolean acceptLicense;
    private boolean viewLicenseAgreement;
    private boolean viewLicenseInfo;
    private String eaString;
    private File localDir;
    private boolean download;
    private boolean noDirectory;
    private boolean offlineOnly = false;

    // Default file already exists action
    private ExistsAction action = ExistsAction.fail;

    // Default file already exists action
    private DownloadOption downloadOption = DownloadOption.required;

    private InstallKernel installKernel;
    private InstallEventListener ielistener;
    private Set<InstallLicense> featureLicense;
    private Logger logger;

    private int progressSteps = -1;
    private int progressCurrentStep = 0;
    private static int numOfRemoteFeatures = 0;
    private static int numOfLocalFeatures = 0;

    /**
     * Executes the install action.
     * {@inheritDoc}
     */
    @Override
    public ReturnCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {
        ReturnCode rc = initialize(args);
        if (!!!rc.equals(ReturnCode.OK)) {
            return rc;
        }
        if (viewLicenseAgreement) {
            return viewLicense(true);
        }
        if (viewLicenseInfo) {
            return viewLicense(false);
        }
        return install();
    }

    private ReturnCode initialize(Arguments args) {
        featureIdSetRaw = new HashSet<String>(args.getPositionalArguments());
        featureIdSet = new HashSet<String>();
        for (String s : featureIdSetRaw) {
            String[] temp;
            temp = s.split(",");
            featureIdSet.addAll(Arrays.asList(temp));

        }

        featureIds = featureIdSet.toArray(new String[featureIdSet.size()]);
        repoType = args.getOption("to");
        acceptLicense = args.getOption("acceptlicense") != null;
        viewLicenseAgreement = args.getOption("viewlicenseagreement") != null;
        viewLicenseInfo = args.getOption("viewlicenseinfo") != null;
        eaString = args.getOption("when-file-exists");
        String downloadOnly = args.getOption("downloadonly");
        download = downloadOnly != null;
        String directory = args.getOption("location");
        noDirectory = directory == null || directory.isEmpty();
        String esaLocation = getLocalEsaLocation(featureIdSet);

        logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
        if (eaString == null && !download && noDirectory && esaLocation == null)
            logger.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_STABILIZING_FEATUREMANAGER", "install") + "\n");

        if (eaString != null) {
            try {
                action = ExistsAction.valueOf(eaString);
            } catch (Exception e) {
                logger.log(Level.SEVERE, NLS.getMessage("install.invalid.when.file.exists.value", eaString), e);
                return ReturnCode.BAD_ARGUMENT;
            }
        }

        if (download) {
            if (!downloadOnly.isEmpty()) {
                try {
                    downloadOption = DownloadOption.valueOf(downloadOnly);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DOWNLOADONLY_INVALID_OPTION", downloadOnly), e);
                    return ReturnCode.BAD_ARGUMENT;
                }
            }
            if (noDirectory) {
                logger.log(Level.SEVERE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_MISSING_DIRECTORY", "--downloadOnly"));
                return ReturnCode.BAD_ARGUMENT;
            }
            localDir = getDownloadOnlyDirectory(directory);
            if (localDir == null) {
                return ReturnCode.IO_FAILURE;
            }
        }

        offlineOnly = args.getOption("offlineonly") != null;
        if (offlineOnly) {
            if (download) {
                logger.log(Level.SEVERE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DOWNLOADONLY_WITH_OFFLINEONLY"));
                return ReturnCode.BAD_ARGUMENT;
            }
            if (noDirectory) {
                logger.log(Level.SEVERE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_MISSING_DIRECTORY", "--offlineOnly"));
                return ReturnCode.BAD_ARGUMENT;
            }
        }

        if (!noDirectory && localDir == null) {
            localDir = new File(directory);
            if (!localDir.exists()) {
                logger.log(Level.SEVERE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DIRECTORY_NOT_EXISTS", localDir.getAbsolutePath()));
                return ReturnCode.IO_FAILURE;
            }
        }

        installKernel = InstallKernelFactory.getInstance();
        installKernel.setUserAgent(InstallConstants.FEATURE_MANAGER);
        installKernel.addListener(getListener(), InstallConstants.EVENT_TYPE_PROGRESS);

        //Load the repository properties instance from properties file
        try {
            Properties repoProperties = RepositoryConfigUtils.loadRepoProperties();
            if (repoProperties != null) {
                //Set the repository properties instance in Install Kernel
                installKernel.setRepositoryProperties(repoProperties);
            }
        } catch (InstallException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return InstallExecutor.returnCode(e.getRc());
        }

        try {
            if (esaLocation == null) {
                featureLicense = localDir != null
                                 && !download ? ((InstallKernelImpl) installKernel).getFeatureLicense(featureIdSet, localDir, repoType, offlineOnly,
                                                                                                      Locale.getDefault()) : installKernel.getFeatureLicense(featureIdSet,
                                                                                                                                                             Locale.getDefault(),
                                                                                                                                                             null, null);
            } else {
                if (download) {
                    logger.log(Level.SEVERE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DOWNLOADONLY_LOCAL_ESA", esaLocation));
                    removeEventListener();
                    return ReturnCode.BAD_ARGUMENT;
                }
                if (localDir != null) {
                    logger.log(Level.SEVERE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_LOCATION_WITH_ESA", esaLocation));
                    return ReturnCode.BAD_ARGUMENT;
                }
                featureLicense = ((InstallKernelImpl) installKernel).getLocalFeatureLicense(esaLocation, Locale.getDefault());
            }
        } catch (InstallException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            removeEventListener();
            return InstallExecutor.returnCode(e.getRc());
        }
        setTotalSteps();
        removeEventListener();
        return ReturnCode.OK;
    }

    private ReturnCode viewLicense(boolean showAgreement) {
        for (InstallLicense license : featureLicense) {
            if (showAgreement) {
                if (license.getAgreement() != null) {
                    logger.log(Level.INFO, license.getAgreement());
                }
            } else {
                if (license.getInformation() != null) {
                    logger.log(Level.INFO, license.getInformation());
                }
            }
            logger.log(Level.INFO, "");
        }
        return ReturnCode.OK;
    }

    private String getLocalEsaLocation(Collection<String> featureIds) {
        if (featureIds.size() != 1)
            return null;
        String esa = featureIds.iterator().next();
        if (esa.indexOf(",") >= 0) {
            return null;
        }
        File esaToInstall = new File(esa);
        if (esaToInstall.exists() && esaToInstall.isFile()) {
            return esa;
        }
        try {
            new URL(esa);
            return esa;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private File getDownloadOnlyDirectory(String downloadDir) {
        File toDir = new File(downloadDir);
        if (toDir.exists()) {
            if (!toDir.isDirectory()) {
                logger.log(Level.SEVERE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DOWNLOADONLY_IS_FILE", toDir.getAbsolutePath()));
                return null;
            }
        } else {
            if (!toDir.mkdirs()) {
                logger.log(Level.SEVERE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DOWNLOADONLY_UNABLE_TO_CREATE_DIR", toDir.getAbsolutePath()));
                return null;
            }
        }
        return toDir;
    }

    private ReturnCode install() {
        Collection<String> installedFeatures = null;
        try {
            ReturnCode rc = handleLicenseAcknowledgmentAcceptance();
            if (!!!rc.equals(ReturnCode.OK)) {
                return rc;
            }
            rc = handleLicenseAcceptance();
            if (!!!rc.equals(ReturnCode.OK)) {
                return rc;
            }

            installKernel.addListener(getListener(), InstallConstants.EVENT_TYPE_PROGRESS);
            String esaLocation = getLocalEsaLocation(featureIdSet);
            if (esaLocation == null) {
                if (download) {
                    if (eaString == null)
                        action = ExistsAction.ignore;
                    Collection<String> downloaded = ((InstallKernelImpl) installKernel).downloadFeatureFeatureManager(featureIdSet, localDir, downloadOption, action, null, null);
                    logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage(downloaded.size() > 1 ? "TOOL_DOWNLOAD_FEATURES_OK" : "TOOL_DOWNLOAD_FEATURE_OK"));
                    removeEventListener();
                    return ReturnCode.OK;
                } else {
                    if (localDir != null) {
                        installedFeatures = ((InstallKernelImpl) installKernel).installFeature(featureIdSet,
                                                                                               localDir,
                                                                                               repoType,
                                                                                               true,
                                                                                               action,
                                                                                               offlineOnly);
                    } else {
                        installedFeatures = installKernel.installFeature(featureIdSet, repoType, true, action, null, null);
                    }
                }
            } else {
                if (download) {
                    logger.log(Level.SEVERE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DOWNLOADONLY_LOCAL_ESA", esaLocation));
                    removeEventListener();
                    return ReturnCode.BAD_ARGUMENT;
                }
                installedFeatures = ((InstallKernelImpl) installKernel).installLocalFeature(esaLocation, repoType, acceptLicense, action);
            }
        } catch (InstallException ie) {
            logger.log(ie.getRc() == InstallException.ALREADY_EXISTS ? Level.INFO : Level.SEVERE, ie.getMessage(), ie);
            removeEventListener();
            return InstallExecutor.returnCode(ie.getRc());
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            removeEventListener();
            return ReturnCode.RUNTIME_EXCEPTION;
        }
        if (installedFeatures != null && !installedFeatures.isEmpty()) {
            logger.log(Level.FINE, "");
            logger.log(Level.FINE, NLS.getMessage("install.feature.ok", installedFeatures.toString().replaceAll(",", " ")));
            logger.log(Level.INFO, "");
            logger.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("TOOL_FEATURES_INSTALLATION_COMPLETED"));
        }
        validateProduct();
        removeEventListener();
        return ReturnCode.OK;
    }

    private void validateProduct() {
        logger.log(Level.INFO, "");
        BundleRepositoryRegistry.disposeAll();
        BundleRepositoryRegistry.initializeDefaults(null, false);
        ValidateCommandTask vcTask = new ValidateCommandTask();
        vcTask.setPrintErrorOnly(true);
        vcTask.setPrintStartMessage(true);
        vcTask.doExecute(new ExecutionContext() {

            @Override
            public CommandConsole getCommandConsole() {
                return new CommandConsole() {

                    @Override
                    public boolean isInputStreamAvailable() {
                        return false;
                    }

                    @Override
                    public String readMaskedText(String prompt) {
                        return null;
                    }

                    @Override
                    public String readText(String prompt) {
                        return null;
                    }

                    @Override
                    public void printInfoMessage(String message) {
                        logger.log(Level.INFO, message);
                    }

                    @Override
                    public void printlnInfoMessage(String message) {
                        logger.log(Level.INFO, message);
                    }

                    @Override
                    public void printErrorMessage(String errorMessage) {
                        logger.log(Level.SEVERE, errorMessage);
                    }

                    @Override
                    public void printlnErrorMessage(String errorMessage) {
                        logger.log(Level.SEVERE, errorMessage);
                    }
                };
            }

            @Override
            public String[] getArguments() {
                return null;
            }

            @Override
            public Set<String> getOptionNames() {
                return new HashSet<String>();
            }

            @Override
            public String getOptionValue(String option) {
                return null;
            }

            @Override
            public boolean optionExists(String option) {
                return false;
            }

            @Override
            public CommandTaskRegistry getCommandTaskRegistry() {
                return null;
            }

            @Override
            public <T> T getAttribute(String name, Class<T> cls) {
                return null;
            }

            @Override
            public Object getAttribute(String name) {
                return null;
            }

            @Override
            public void setAttribute(String name, Object value) {}

            @Override
            public void setOverrideOutputStream(PrintStream outputStream) {}
        });
    }

    private Set<InstallLicense> getLicenseToAccept(Set<String> installedLicense, Set<InstallLicense> featureLicense) {
        HashSet<InstallLicense> licenseToAccept = new HashSet<InstallLicense>();
        if (featureLicense != null) {
            for (InstallLicense license : featureLicense) {
                if (!!!isUnspecifiedType(license) && !!!installedLicense.contains(license.getId())) {
                    licenseToAccept.add(license);
                }
            }
        }
        return licenseToAccept;
    }

    private boolean isUnspecifiedType(InstallLicense license) {
        return license.getType() != null && license.getType().equals(UNSPECIFIED_LICENSE_TYPE);
    }

    private String getLicenseAcknowledgment(Set<InstallLicense> featureLicense) {
        for (InstallLicense license : featureLicense) {
            if (isUnspecifiedType(license)) {
                return license.getAgreement();
            }
        }
        return null;
    }

    private ReturnCode handleLicenseAcknowledgmentAcceptance() {
        String licenseAcknowledgment = getLicenseAcknowledgment(featureLicense);
        if (licenseAcknowledgment != null && !!!licenseAcknowledgment.trim().equals("")) {
            if (acceptLicense) {
                // Indicate license acceptance via option
                SelfExtract.wordWrappedOut(SelfExtract.format("licenseAccepted", "--acceptLicense"));
                logger.log(Level.INFO, "");
            } else {
                logger.log(Level.INFO, "");
                logger.log(Level.INFO, licenseAcknowledgment);
                logger.log(Level.INFO, "");
                boolean accept = SelfExtract.getResponse(SelfExtract.format("licensePrompt", new Object[] { "[1]", "[2]" }),
                                                         "1", "2");
                logger.log(Level.INFO, "");
                if (!accept) {
                    return ReturnCode.RUNTIME_EXCEPTION;
                }
            }
        }
        return ReturnCode.OK;
    }

    private ReturnCode handleLicenseAcceptance() {
        Set<String> installedLicense = installKernel.getInstalledLicense();
        Set<InstallLicense> licenseToAccept = getLicenseToAccept(installedLicense, featureLicense);
        for (InstallLicense license : licenseToAccept) {
            if (!!!handleLicenseAcceptance(license)) {
                return ReturnCode.RUNTIME_EXCEPTION;
            }
        }
        return ReturnCode.OK;
    }

    private boolean handleLicenseAcceptance(InstallLicense licenseToAccept) {
        //
        // Display license requirement
        //
        SelfExtract.wordWrappedOut(SelfExtract.format("licenseStatement", new Object[] { licenseToAccept.getProgramName(), licenseToAccept.getName() }));
        logger.log(Level.INFO, "");

        if (acceptLicense) {
            // Indicate license acceptance via option
            SelfExtract.wordWrappedOut(SelfExtract.format("licenseAccepted", "--acceptLicense"));
            logger.log(Level.INFO, "");
        } else {
            // Check for license agreement: exit if not accepted.
            if (!obtainLicenseAgreement(licenseToAccept)) {
                return false;
            }
        }
        return true;
    }

    private boolean obtainLicenseAgreement(InstallLicense license) {
        // Prompt for word-wrapped display of license agreement & information
        boolean view;

        SelfExtract.wordWrappedOut(SelfExtract.format("showAgreement", "--viewLicenseAgreement"));
        view = SelfExtract.getResponse(SelfExtract.format("promptAgreement"), "", "xX");
        if (view) {
            logger.log(Level.INFO, license.getAgreement());
            logger.log(Level.INFO, "");
        }

        SelfExtract.wordWrappedOut(SelfExtract.format("showInformation", "--viewLicenseInfo"));
        view = SelfExtract.getResponse(SelfExtract.format("promptInfo"), "", "xX");
        if (view) {
            logger.log(Level.INFO, license.getInformation());
            logger.log(Level.INFO, "");
        }

        logger.log(Level.INFO, "");
        SelfExtract.wordWrappedOut(SelfExtract.format("licenseOptionDescription"));
        logger.log(Level.INFO, "");

        boolean accept = SelfExtract.getResponse(SelfExtract.format("licensePrompt", new Object[] { "[1]", "[2]" }),
                                                 "1", "2");
        logger.log(Level.INFO, "");

        return accept;
    }

    private InstallEventListener getListener() {
        if (ielistener == null) {
            ielistener = new InstallEventListener() {
                @Override
                public void handleInstallEvent(InstallProgressEvent event) {
                    if (event.state != InstallProgressEvent.POST_INSTALL) {
                        if (progressSteps > 0) {
                            progressCurrentStep++;
                            if (progressCurrentStep > progressSteps) {
                                progressSteps = progressCurrentStep;
                            }
                            logger.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("PROGRESS_STEP", progressCurrentStep, progressSteps) + ": " + event.message);
                        } else {
                            logger.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_CONTACTING_REPO"));
                        }
                    }
                }
            };
        }
        return ielistener;
    }

    public static void incrementNumOfRemoteFeatures() {
        numOfRemoteFeatures++;
    }

    public static void incrementNumOfLocalFeatures() {
        numOfLocalFeatures++;
    }

    private void setTotalSteps() {
        int constantSteps = !download && !noDirectory ? 3 : 5;
        int downloadOnly = download || !noDirectory ? 1 : 2;
        if (downloadOption.equals(DownloadOption.none)) {
            numOfLocalFeatures = 0;
            numOfRemoteFeatures = featureIds.length;
        }
        progressSteps = constantSteps + numOfLocalFeatures + downloadOnly * numOfRemoteFeatures;
    }

    private void removeEventListener() {
        logger.log(Level.INFO, "");
        installKernel.removeListener(getListener());
        ielistener = null;
    }
}