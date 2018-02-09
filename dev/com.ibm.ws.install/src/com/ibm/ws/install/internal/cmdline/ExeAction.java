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
import com.ibm.ws.kernel.feature.internal.cmdline.ClasspathAction;
import com.ibm.ws.kernel.feature.internal.cmdline.FeatureListAction;
import com.ibm.ws.kernel.feature.internal.cmdline.FeatureToolException;
import com.ibm.ws.kernel.feature.internal.cmdline.ReturnCode;

/**
 * This API holds possible command options associated with each action.
 * It also performs the action logic and confirms if the command is valid.
 *
 */
public enum ExeAction implements ActionDefinition {
    install(new ExeInstallAction(), -1, "--to", "--when-file-exists", "--acceptLicense", "--viewLicenseAgreement", "--viewLicenseInfo", "--downloadOnly", "--location", "--offlineOnly", "--verbose", "name"),
    uninstall(new ExeUninstallAction(), -1, "--noPrompts", "--verbose", "name"),
    featureList(new FeatureListAction(), 1, "--encoding", "--locale", "--productextension", "fileName"),
    find(new ExeFindAction(), 1, "--viewInfo", "--verbose", "searchString"),
    classpath(new ClasspathAction(), 1, "--features", "fileName"),
    help(new ExeHelpAction(), 0);

    private List<String> commandOptions;
    private ActionHandler action;
    private int positionalOptions;

    private ExeAction(ActionHandler a, int count, String... args) {
        commandOptions = Collections.unmodifiableList(Arrays.asList(args));
        action = a;
        positionalOptions = count;
    }

    /**
     * Returns a list of command options
     * {@inheritDoc}
     */
    @Override
    public List<String> getCommandOptions() {
        return commandOptions;
    }

    /**
     * Returns the number of positional arguments
     * {@inheritDoc}
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
}