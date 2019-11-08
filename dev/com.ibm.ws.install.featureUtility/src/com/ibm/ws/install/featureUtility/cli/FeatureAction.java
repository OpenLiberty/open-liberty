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
    installFeature(new InstallFeatureAction(), -1, "--no-cache", "--to", "--verbose", "name..."),
    installServer(new InstallServerAction(), -1, "--no-cache", "--verbose", "name..."),
    help(new FeatureHelpAction(), 0);
//    install(new FeatureInstallAction(), -1, "--from", "--to", "--verbose", "name"),

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
