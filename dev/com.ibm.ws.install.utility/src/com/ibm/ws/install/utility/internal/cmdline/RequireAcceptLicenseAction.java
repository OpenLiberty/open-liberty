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

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernelFactory;
import com.ibm.ws.install.InstallKernelInteractive;
import com.ibm.ws.install.InstallLicense;
import com.ibm.ws.install.RepositoryConfigUtils;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.utility.cmdline.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.CommandConstants;
import com.ibm.ws.product.utility.CommandTaskRegistry;
import com.ibm.ws.product.utility.ExecutionContext;
import com.ibm.ws.product.utility.extension.ValidateCommandTask;

import wlp.lib.extract.SelfExtract;

/**
 * This API is used to execute the Require Accept License Action.
 */
public abstract class RequireAcceptLicenseAction implements ActionHandler {

    static final private String UNSPECIFIED_LICENSE_TYPE = "UNSPECIFIED";
    static final protected Logger logger = InstallLogUtils.getInstallLogger();

    // Supported options
    //String[] featureIds;
    List<String> argList;
    boolean acceptLicense;
    boolean viewLicenseAgreement;
    boolean viewLicenseInfo;

    Properties repoProperties;
    InstallKernelInteractive installKernel;
    Set<InstallLicense> featureLicenses = Collections.emptySet();
    Collection<String> sampleLicenses = Collections.emptyList();

    /**
     * Depending on viewLicenseAgreement and viewLicenseInfo boolean conditions
     * certain codes are returned
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
        return execute();
    }

    /**
     * Calls handleLicenseAcknowledgementAcceptance function
     *
     * @return returnCode based on above function's execution
     */
    ReturnCode execute() {
        ReturnCode rc = handleLicenseAcknowledgmentAcceptance();
        if (!!!rc.equals(ReturnCode.OK)) {
            return rc;
        }
        rc = handleLicenseAcceptance();
        if (!!!rc.equals(ReturnCode.OK)) {
            return rc;
        }
        return ReturnCode.OK;
    }

    ReturnCode initialize(Arguments args) {
        argList = args.getPositionalArguments();

        acceptLicense = args.getOption("acceptlicense") != null;
        viewLicenseAgreement = args.getOption("viewlicenseagreement") != null;
        viewLicenseInfo = args.getOption("viewlicenseinfo") != null;

        installKernel = InstallKernelFactory.getInteractiveInstance();
        installKernel.setUserAgent(InstallConstants.ASSET_MANAGER);

        //Load the repository properties instance from properties file
        try {
            repoProperties = RepositoryConfigUtils.loadRepoProperties();
            if (repoProperties != null) {
                //Set the repository properties instance in Install Kernel
                installKernel.setRepositoryProperties(repoProperties);
            }
        } catch (InstallException e) {
            System.out.println(e.getMessage());
            return InstallUtilityExecutor.returnCode(e.getRc());
        }

        return ReturnCode.OK;
    }

    protected ReturnCode viewLicense(boolean showAgreement) {
        for (InstallLicense license : featureLicenses) {
            if (showAgreement) {
                if (license.getAgreement() != null) {
                    System.out.println(license.getAgreement());
                }
            } else {
                if (license.getInformation() != null) {
                    System.out.println(license.getInformation());
                }
            }
            System.out.println();
        }
        return ReturnCode.OK;
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
        String licenseAcknowledgment = getLicenseAcknowledgment(featureLicenses);
        if (licenseAcknowledgment != null && !!!licenseAcknowledgment.trim().equals("")) {
            showMessagesForAdditionalFeatures();
            if (acceptLicense) {
                // Indicate license acceptance via option
                SelfExtract.wordWrappedOut(SelfExtract.format("licenseAccepted", "--acceptLicense"));
                System.out.println();
            } else {
                System.out.println();
                System.out.println(licenseAcknowledgment);
                System.out.println();
                boolean accept = SelfExtract.getResponse(SelfExtract.format("licensePrompt", new Object[] { "[1]", "[2]" }),
                                                         "1", "2");
                System.out.println();
                if (!accept) {
                    return ReturnCode.RUNTIME_EXCEPTION;
                }
            }
        }
        return ReturnCode.OK;
    }

    protected void showMessagesForAdditionalFeatures() {
        // do nothing
    }

    protected ReturnCode handleLicenseAcceptance() {
        Set<String> installedLicense = installKernel.getInstalledLicense();
        Set<InstallLicense> licenseToAccept = getLicenseToAccept(installedLicense, featureLicenses);
        if (!licenseToAccept.isEmpty()) {
            showMessagesForAdditionalFeatures();
        }
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
        System.out.println();

        if (acceptLicense) {
            // Indicate license acceptance via option
            SelfExtract.wordWrappedOut(SelfExtract.format("licenseAccepted", "--acceptLicense"));
            System.out.println();
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
            System.out.println(license.getAgreement());
            System.out.println();
        }

        SelfExtract.wordWrappedOut(SelfExtract.format("showInformation", "--viewLicenseInfo"));
        view = SelfExtract.getResponse(SelfExtract.format("promptInfo"), "", "xX");
        if (view) {
            System.out.println(license.getInformation());
            System.out.println();
        }

        System.out.println();
        SelfExtract.wordWrappedOut(SelfExtract.format("licenseOptionDescription"));
        System.out.println();

        boolean accept = SelfExtract.getResponse(SelfExtract.format("licensePrompt", new Object[] { "[1]", "[2]" }),
                                                 "1", "2");
        System.out.println();

        return accept;
    }

    /**
     * List of methods used to validate the Product
     *
     * @return boolean variable depending on success of validation
     */
    boolean validateProduct() {
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
                        System.out.print(message);
                    }

                    @Override
                    public void printlnInfoMessage(String message) {
                        System.out.println(message);
                    }

                    @Override
                    public void printErrorMessage(String errorMessage) {
                        System.err.print(errorMessage);
                    }

                    @Override
                    public void printlnErrorMessage(String errorMessage) {
                        System.err.println(errorMessage);
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

            @SuppressWarnings("unchecked")
            @Override
            public <T> T getAttribute(String name, Class<T> cls) {
                if (name.equals(CommandConstants.WLP_INSTALLATION_LOCATION)) {
                    return (T) Utils.getInstallDir();
                }
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
        return vcTask.isSuccessful();
    }
}