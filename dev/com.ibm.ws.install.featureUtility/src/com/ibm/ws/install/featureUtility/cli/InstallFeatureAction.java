/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.featureUtility.cli;

import java.io.File;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.*;
import com.ibm.ws.install.featureUtility.FeatureUtility;
import com.ibm.ws.install.featureUtility.FeatureUtilityExecutor;
import com.ibm.ws.install.internal.InstallKernelImpl;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.ProgressBar;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;
import com.ibm.ws.kernel.feature.internal.cmdline.ArgumentsImpl;
import com.ibm.ws.kernel.productinfo.DuplicateProductInfoException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.kernel.productinfo.ProductInfoReplaceException;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.CommandTaskRegistry;
import com.ibm.ws.product.utility.ExecutionContext;
import com.ibm.ws.product.utility.extension.ValidateCommandTask;
import wlp.lib.extract.SelfExtract;

public class InstallFeatureAction implements ActionHandler {

        private FeatureUtility featureUtility;
        private InstallKernelInteractive installKernel;
        private Logger logger;
        private List<String> argList;
        private List<String> featureNames;
        private String fromDir;
        private String toDir;
        private Boolean noCache;
        private boolean acceptLicense;
        private ProgressBar progressBar;
        Set<InstallLicense> featureLicenses = Collections.emptySet();
        static final private String UNSPECIFIED_LICENSE_TYPE = "UNSPECIFIED";


        @Override 
        public ExitCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {
                if(args.getPositionalArguments().isEmpty()){
                        FeatureAction.help.handleTask(new ArgumentsImpl(new String[] { "help", FeatureAction.getEnum(args.getAction()).toString() }));
                        return ReturnCode.BAD_ARGUMENT;
                }
                ExitCode rc = initialize(args);
                if (!!!rc.equals(ReturnCode.OK)) {
                        return rc;
                }
                rc = execute();
                return rc;

        }


        private ExitCode initialize(Arguments args) {
                ExitCode rc = ReturnCode.OK;

                this.logger = InstallLogUtils.getInstallLogger();
                this.installKernel = InstallKernelFactory.getInteractiveInstance();
                this.featureNames = new ArrayList<String>();

                this.argList = args.getPositionalArguments();
                this.fromDir = args.getOption("from");
                if ((rc = validateFromDir(this.fromDir)) != ReturnCode.OK) {
                        return rc;
                }
                this.acceptLicense = args.getOption("acceptLicense") != null;
                this.noCache = args.getOption("noCache") != null;
                this.progressBar = ProgressBar.getInstance();

                HashMap<String, Double> methodMap = new HashMap<>();
                // initialize feature utility and install kernel map
                methodMap.put("initializeMap", 5.00);//done
                methodMap.put("fetchJsons", 10.00); //dpne
                methodMap.put("resolvedFeatures", 10.00); //done
                // in installFeature we have 80 units to work with
                methodMap.put("fetchArtifacts", 10.00);
                methodMap.put("downloadArtifacts", 25.00);
                // 10 + 15 = 35 for download artifact
                methodMap.put("installFeatures", 35.00);
                methodMap.put("cleanUp", 5.00);

                progressBar.setMethodMap(methodMap);

                String arg = argList.get(0);
                try {
                	Collection<String> assetIds = new HashSet<String>(argList);
                	checkAssetsNotInstalled(new ArrayList<String>(assetIds));
                	return assetInstallInit(assetIds);
                } catch (InstallException e) {
                	logger.log(Level.SEVERE, e.getMessage(), e);
                    return FeatureUtilityExecutor.returnCode(e.getRc());
                } catch (Throwable e) {
                	logger.log(Level.SEVERE, e.getMessage(), e);
                	return FeatureUtilityExecutor.returnCode(InstallException.IO_FAILURE);
                }

        }
        private ReturnCode validateFromDir(String fromDir) {
                if (fromDir == null) {
                        return ReturnCode.OK;
                }
                if (fromDir.isEmpty()) {
                        logger.log(Level.SEVERE,
                                   InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DIRECTORY_REQUIRED", "from"));
                        return ReturnCode.BAD_ARGUMENT;
                }

                if (!new File(fromDir).exists()) {
                        logger.log(Level.SEVERE,
                                   InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DIRECTORY_NOT_EXISTS", fromDir));
                        return ReturnCode.BAD_ARGUMENT;
                }

                return ReturnCode.OK;
        }

        private ExitCode assetInstallInit(Collection<String> assetIds) {
                featureNames.addAll(assetIds);
                try {
                        installKernel.resolve(featureNames, false);
                        featureLicenses = installKernel.getFeatureLicense(Locale.getDefault());
                } catch (InstallException e) {
                        logger.severe(e.getMessage());
                        return FeatureUtilityExecutor.returnCode(e.getRc());
                }

                return ReturnCode.OK;
        }

        // call the install kernel to verify we are installing at least 1 new asset
        private void checkAssetsNotInstalled(List<String> assetIds) throws InstallException {
                installKernel.checkAssetsNotInstalled(assetIds);
        }


        private ExitCode install() {
                try {
                        featureUtility = new FeatureUtility.FeatureUtilityBuilder().setFromDir(fromDir)
                                        .setFeaturesToInstall(featureNames).setNoCache(noCache).setAcceptLicense(acceptLicense).build();
                        featureUtility.installFeatures();
                } catch (InstallException e) {
                        logger.log(Level.SEVERE, e.getMessage(), e);
                        return FeatureUtilityExecutor.returnCode(e.getRc());
                } catch (Throwable e) {
                        logger.log(Level.SEVERE, e.getMessage(), e);
                        return FeatureUtilityExecutor.returnCode(InstallException.IO_FAILURE);
                }
                return ReturnCode.OK;
        }

        private boolean shouldNotShowLicense(){
                return acceptLicense || determineIfOpenLiberty();
        }

        private ExitCode execute() {
                ExitCode rc = ReturnCode.OK;

                if(!shouldNotShowLicense()) {
                        logger.fine("We should show license.");
                        rc = handleLicenseAcknowledgmentAcceptance();
                        if (!!!rc.equals(ReturnCode.OK)) {
                                return rc;
                        }
                        rc = handleLicenseAcceptance();
                        if (!!!rc.equals(ReturnCode.OK)) {
                                return rc;
                        }
                } else {
                        logger.fine("not showign lciense");
                }

                if ( !featureNames.isEmpty()) {
                        rc = install();
                }
                if(ReturnCode.OK.equals(rc)){
                        progressBar.finish();
                } else {
                        progressBar.finishWithError();
                }

                if (ReturnCode.OK.equals(rc)) {
                        if (!!!validateProduct()) {
                                rc = ReturnCode.INVALID;
                        }
                }


                return rc;
        }

        private boolean determineIfOpenLiberty() {
                try {
                        for (ProductInfo productInfo : ProductInfo.getAllProductInfo().values()) {
                                if (productInfo.getReplacedBy() == null && productInfo.getId().equals("io.openliberty")) {
                                        return true;
                                }
                        }
                } catch (ProductInfoParseException e) {
                        e.printStackTrace();
                } catch (DuplicateProductInfoException e) {
                        e.printStackTrace();
                } catch (ProductInfoReplaceException e) {
                        e.printStackTrace();
                }
                return false;
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








        private boolean validateProduct() {
//                logger.log(Level.INFO, "");
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
                        public void setAttribute(String name, Object value) {
                        }

                        @Override
                        public void setOverrideOutputStream(PrintStream outputStream) {
                        }
                });
                return vcTask.isSuccessful();
        }

}


