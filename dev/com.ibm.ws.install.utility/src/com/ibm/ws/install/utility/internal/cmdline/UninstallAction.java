/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.utility.internal.cmdline;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernel;
import com.ibm.ws.install.InstallKernelFactory;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.feature.internal.cmdline.ArgumentsImpl;
import com.ibm.ws.kernel.feature.internal.cmdline.ReturnCode;

import wlp.lib.extract.SelfExtract;

/**
 * This API is used to execute the Uninstall Action.
 */
public class UninstallAction implements ActionHandler {

    /**
     * Executes Uninstall task.
     *
     * @return - return code depending on task success.
     */
    @Override
    public ReturnCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {
        List<String> argList = args.getPositionalArguments();

        if (argList.isEmpty()) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   CmdUtils.getMessage("ERROR_NO_ARGUMENT", "uninstall"));
            Action.help.handleTask(new ArgumentsImpl(new String[] { "help", "uninstall" }));
            return ReturnCode.BAD_ARGUMENT;
        }

        //Uninstall action options
        String cmdLineOption = args.getOption("noprompts");
        boolean noInteractive = cmdLineOption != null && (cmdLineOption.isEmpty() || Boolean.valueOf(cmdLineOption));

        cmdLineOption = args.getOption("force");
        boolean forceUninstall = cmdLineOption != null && (cmdLineOption.isEmpty() || Boolean.valueOf(cmdLineOption));

        ArrayList<String> features = new ArrayList<String>(argList);

        if (forceUninstall && features.size() > 1) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INVALID_NUMBER_OF_FEATURES_FORCE_UNINSTALL"));
            return ReturnCode.BAD_ARGUMENT;
        }

        InstallKernel installKernel = InstallKernelFactory.getInstance();

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
        } catch (InstallException ie) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE, ie.getMessage(), ie);
            return ReturnCode.RUNTIME_EXCEPTION;
        } catch (Exception e) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE, CmdUtils.getMessage("ERROR_UNINSTALL_FEATURES_FAIL",
                                                                                     InstallUtils.getFeatureListOutput(features)),
                                                   e);
            return ReturnCode.RUNTIME_EXCEPTION;
        }

        InstallLogUtils.getInstallLogger().log(Level.INFO, CmdUtils.getMessage("MSG_UNINSTALL_FEATURES", InstallUtils.getFeatureListOutput(features)));
        return ReturnCode.OK;
    }
}
