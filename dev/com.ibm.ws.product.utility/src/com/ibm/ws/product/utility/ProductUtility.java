/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility;

import java.io.File;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.product.utility.extension.DisplayLicenseAgreementTask;
import com.ibm.ws.product.utility.extension.DisplayLicenseInfoTask;
import com.ibm.ws.product.utility.extension.FeatureInfoCommandTask;
import com.ibm.ws.product.utility.extension.HelpCommandTask;
import com.ibm.ws.product.utility.extension.IFixCompareCommandTask;
import com.ibm.ws.product.utility.extension.ValidateCommandTask;
import com.ibm.ws.product.utility.extension.VersionCommandTask;

/**
 * The basic outline of the flow is as follows:
 * <ol>
 * <li>If no arguments are specified, print usage.</li>
 * <li>If one argument is specified, and it is help, print general verbose help.</li>
 * <li>If two or more arguments are specified, if task name is help, print
 * verbose help for 2nd argument (expected to be task name).</li>
 * <li>If the task name is not known, print usage.</li>
 * <li>All other cases, invoke task.</li>
 * </ol>
 */
public class ProductUtility {

    private static final CommandTaskRegistry COMMAND_TASK_REGISTRY = new CommandTaskRegistry();

    public static final String SCRIPT_NAME = "productInfo";

    private final CommandConsole console;

    private final File wlpInstallationFolder;

    public ProductUtility() {
        console = new DefaultCommandConsole(System.console(), System.out, System.err);
        wlpInstallationFolder = Utils.getInstallDir();
    }

    static {
        COMMAND_TASK_REGISTRY.registerCommandTask(HelpCommandTask.HELP_TASK_NAME, HelpCommandTask.class);
        COMMAND_TASK_REGISTRY.registerCommandTask(VersionCommandTask.VERSION_TASK_NAME, VersionCommandTask.class);
        COMMAND_TASK_REGISTRY.registerCommandTask(FeatureInfoCommandTask.FEATURE_INFO_TASK_NAME, FeatureInfoCommandTask.class);
        COMMAND_TASK_REGISTRY.registerCommandTask(IFixCompareCommandTask.IFIX_COMPARE_TASK_NAME, IFixCompareCommandTask.class);
        COMMAND_TASK_REGISTRY.registerCommandTask(ValidateCommandTask.VALIDATE_TASK_NAME, ValidateCommandTask.class);
        COMMAND_TASK_REGISTRY.registerCommandTask(DisplayLicenseAgreementTask.DISPLAY_LICENSE_AGREEMENT_TASK_NAME, DisplayLicenseAgreementTask.class);
        COMMAND_TASK_REGISTRY.registerCommandTask(DisplayLicenseInfoTask.DISPLAY_LICENSE_INFO_TASK_NAME, DisplayLicenseInfoTask.class);
    }

    // strip off any punctuation or other noise, see if the rest appears to be a help request.
    // note that the string is already trim()'d by command-line parsing unless user explicitly escaped a space 
    private static boolean looksLikeHelp(String taskname)
    {
        if (taskname == null)
            return false; // applied paranoia, since this isn't in a performance path
        int start = 0, len = taskname.length();
        while (start < len && !Character.isLetter(taskname.charAt(start)))
            ++start;
        return HelpCommandTask.HELP_TASK_NAME.equalsIgnoreCase(taskname.substring(start).toLowerCase());
    }

    public void execute(String[] args) {
        if (args.length == 0) {
            printHelpUsageMessage(false);
            return;
        }
        String taskName = args[0].toLowerCase();
        if (looksLikeHelp(taskName))
            taskName = HelpCommandTask.HELP_TASK_NAME;

        CommandTask commandTask = COMMAND_TASK_REGISTRY.getCommandTask(taskName);
        if (commandTask == null) {
            printBadCmdMessage(args[0]);
            printHelpUsageMessage(true);
            return;
        }
        String[] commandArguments = new String[args.length - 1];
        if (commandArguments.length > 0) {
            System.arraycopy(args, 1, commandArguments, 0, commandArguments.length);
        }
        ExecutionContext executionContext = createExecutionContext(commandArguments);
        commandTask.execute(executionContext);
    }

    private void printHelpUsageMessage(boolean error) {
        HelpCommandTask helpCommandTask = (HelpCommandTask) COMMAND_TASK_REGISTRY.getCommandTask(HelpCommandTask.HELP_TASK_NAME);
        helpCommandTask.doScriptUsage(createExecutionContext(new String[] {}), error);
    }

    private void printBadCmdMessage(String arg) {
        // Since this version of HelpCommandTask doesn't have an easier way to access the "unknown command" translated text... 
        HelpCommandTask helpCommandTask = (HelpCommandTask) COMMAND_TASK_REGISTRY.getCommandTask(HelpCommandTask.HELP_TASK_NAME);
        helpCommandTask.doExecute(createExecutionContext(new String[] { arg }));
    }

    private ExecutionContext createExecutionContext(String[] arguments) {
        ExecutionContext executionContext = new ExecutionContextImpl(console, arguments, COMMAND_TASK_REGISTRY);
        executionContext.setAttribute(CommandConstants.SCRIPT_NAME, SCRIPT_NAME);
        executionContext.setAttribute(CommandConstants.WLP_INSTALLATION_LOCATION, wlpInstallationFolder);
        return executionContext;
    }

    public static void main(String[] args) {
        ProductUtility productUtility = new ProductUtility();
        productUtility.execute(args);
    }
}
