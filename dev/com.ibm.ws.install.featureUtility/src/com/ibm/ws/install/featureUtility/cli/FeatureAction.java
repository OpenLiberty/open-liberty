package com.ibm.ws.install.featureUtility.cli;

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


public enum FeatureAction implements ActionDefinition {
    install(new FeatureInstallAction(), -1, "--from", "--to", "--verbose", "name"),
    download(new FeatureDownloadAction(), -1, "--from", "--to", "--verbose", "name"), help(new FeatureHelpAction(), 0);

    private List<String> commandOptions;
    private ActionHandler action;
    private int positionalOptions;

    private FeatureAction(ActionHandler a, int count, String... args) {
        commandOptions = Collections.unmodifiableList(Arrays.asList(args));
        action = a;
        positionalOptions = count;
    }

    @Override
    public List<String> getCommandOptions() {
        return commandOptions;
    }

    @Override
    public int numPositionalArgs() {
        return positionalOptions;
    }

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
