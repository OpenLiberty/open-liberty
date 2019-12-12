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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernel;
import com.ibm.ws.install.InstallKernelFactory;
import com.ibm.ws.install.InstallKernelInteractive;
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
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.CommandTaskRegistry;
import com.ibm.ws.product.utility.ExecutionContext;
import com.ibm.ws.product.utility.extension.ValidateCommandTask;

public class InstallFeatureAction implements ActionHandler {

        private FeatureUtility featureUtility;
        private InstallKernelInteractive installKernel;
        private Logger logger;
        private List<String> argList;
        private List<String> featureNames;
        private String fromDir;
        private String toDir;
        private Boolean noCache;
        private ProgressBar progressBar;

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
                return ReturnCode.OK;
        }

        // call the install kernel to verify we are installing at least 1 new asset
        private void checkAssetsNotInstalled(List<String> assetIds) throws InstallException {
                installKernel.checkAssetsNotInstalled(assetIds);
        }


        private ExitCode install() {
                try {
                        featureUtility = new FeatureUtility.FeatureUtilityBuilder().setFromDir(fromDir)
                                        .setFeaturesToInstall(featureNames).setNoCache(noCache).build();
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


        private ExitCode execute() {
                ExitCode rc = ReturnCode.OK;

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


