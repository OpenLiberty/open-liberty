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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import com.ibm.ws.install.InstallKernel;
import com.ibm.ws.install.InstallKernelFactory;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.feature.internal.cmdline.ReturnCode;

import wlp.lib.extract.SelfExtract;

/**
 * This API contains methods to execute the Uninstall action.
 */
public class ExeUninstallAction implements ActionHandler {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.boot.cmdline.ActionHandler#handleTask(java.io.PrintStream, java.io.PrintStream, com.ibm.ws.kernel.boot.cmdline.Arguments)
     */
    @Override
    public ReturnCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {

        InstallLogUtils.getInstallLogger().log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_STABILIZING_FEATUREMANAGER", "uninstall") + "\n");

        Set<String> featureRaw = new HashSet<String>(args.getPositionalArguments());
        ArrayList<String> features = new ArrayList<String>();

        for (String s : featureRaw) {
            String[] temp;
            temp = s.split(",");
            features.addAll(Arrays.asList(temp));
        }

        String cmdLineOption = args.getOption("noprompts");
        boolean noInteractive = cmdLineOption != null && (cmdLineOption.isEmpty() || Boolean.valueOf(cmdLineOption));

        cmdLineOption = args.getOption("force");
        boolean forceUninstall = cmdLineOption != null && (cmdLineOption.isEmpty() || Boolean.valueOf(cmdLineOption));

        InstallKernel installKernel = InstallKernelFactory.getInstance();

        if (forceUninstall && features.size() > 1) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INVALID_NUMBER_OF_FEATURES_FORCE_UNINSTALL"));
            return ReturnCode.BAD_ARGUMENT;
        }

        try {
            if (forceUninstall) {
                installKernel.uninstallFeaturePrereqChecking(features.get(0), true, forceUninstall);
            } else {
                installKernel.uninstallFeaturePrereqChecking(features);
            }

            if (!!!noInteractive && !!!SelfExtract.getResponse(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("TOOL_UNININSTALL_FEATURE_CONFIRMATION"),
                                                               "", "Xx")) {
                return ReturnCode.OK;
            }

            if (forceUninstall) {
                installKernel.uninstallFeature(features.get(0), forceUninstall);
            } else {
                installKernel.uninstallFeature(features);
            }
        } catch (Exception e) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNINSTALL_FEATURE_INVALID_META_DATA",
                                                                                                  InstallUtils.getFeatureListOutput(features)),
                                                   e);
            InstallLogUtils.getInstallLogger().log(Level.SEVERE, e.getMessage(), e);
            return ReturnCode.RUNTIME_EXCEPTION;
        }
        // Workaround to trim the message code prefix
        InstallLogUtils.getInstallLogger().log(Level.INFO,
                                               Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("TOOL_UNINSTALL_FEATURE_OK",
                                                                                              InstallUtils.getFeatureListOutput(features)).replaceAll("CWWKF1350I:",
                                                                                                                                                      "").trim());
        return ReturnCode.OK;
    }
}
