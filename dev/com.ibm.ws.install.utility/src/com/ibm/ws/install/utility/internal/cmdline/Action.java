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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.ibm.ws.install.InstallKernel;
import com.ibm.ws.install.InstallKernelFactory;
import com.ibm.ws.install.internal.InstallKernelImpl;
import com.ibm.ws.kernel.boot.cmdline.ActionDefinition;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;
import com.ibm.ws.kernel.feature.internal.cmdline.FeatureToolException;
import com.ibm.ws.kernel.feature.internal.cmdline.ReturnCode;

/**
 * This API holds possible command options associated with each action.
 * It also performs the action logic and confirms if the command is valid.
 */
public enum Action implements ActionDefinition {
    download(new DownloadAction(), -1, "--overwrite", "--location", "--acceptLicense", "--viewLicenseAgreement", "--viewLicenseInfo", "--verbose", "name..."),
    find(new FindAction(), -1, "--showDescriptions", "--type", "--name", "--from", "--verbose", "[searchString]"),
    install(new InstallAction(), -1, "--to", "--acceptLicense", "--viewLicenseAgreement", "--viewLicenseInfo", "--from", "--downloadDependencies", "--verbose", "name..."),
    testConnection(new TestConnectionAction(), -1, "[repoName]"),
    uninstall(new UninstallAction(), -1, "--noPrompts", "--force", "--verbose", "name..."),
    viewSettings(new ViewSettingsAction(), 0, "--viewValidationMessages"),
    help(new HelpAction(), 0);

    private List<String> commandOptions;
    private ActionHandler action;
    private int positionalOptions;

    private Action(ActionHandler a, int count, String... args) {
        commandOptions = Collections.unmodifiableList(Arrays.asList(args));
        action = a;
        positionalOptions = count;
    }

    /**
     * Return all command options associated with an action
     */

    @Override
    public List<String> getCommandOptions() {
        return commandOptions;
    }

    /**
     * Return total number of command options
     */

    @Override
    public int numPositionalArgs() {
        return positionalOptions;
    }

    /**
     * Perform the action logic.
     *
     * @param args The arguments passed to the script.
     * @throws IllegalArgumentException if the task was called with invalid arguments
     */
    @Override
    public ExitCode handleTask(Arguments args) {
        try {
            // Set log level if --verbose specified
            InstallKernel installKernel = InstallKernelFactory.getInstance();

            String verboseLevel = args.getOption("verbose");
            Level logLevel = Level.INFO;
            if (verboseLevel != null && verboseLevel.isEmpty()) {
                logLevel = Level.FINE;
            } else if (verboseLevel != null && verboseLevel.equalsIgnoreCase("debug")) {
                logLevel = Level.FINEST;
            }
            ((InstallKernelImpl) installKernel).enableConsoleLog(logLevel);

            return action.handleTask(System.out, System.err, args);
        } catch (FeatureToolException fte) {
            System.err.println(fte.getMessage());
            fte.printStackTrace();
            return fte.getReturnCode();
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return ReturnCode.RUNTIME_EXCEPTION;
        }
    }

    /**
     * Finds all values from CommandOptions and returns if it is a command
     *
     * @return if option is a command (true)
     */
    public boolean showOptions() {
        List<String> options = getCommandOptions();
        for (String option : options) {
            if (option.startsWith("-"))
                return true;
        }
        return false;
    }
}