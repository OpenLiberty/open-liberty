package com.ibm.ws.kernel.boot.internal.commands;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import com.ibm.ws.kernel.boot.HelpActions;
import com.ibm.ws.kernel.boot.LaunchArguments;
import com.ibm.ws.kernel.boot.ReturnCode;

public class HelpCommand {
    private final HelpActions actionHelper;
    private final ResourceBundle options;

    public HelpCommand(HelpActions actions) {
        actionHelper = actions;
        options = actionHelper.getResourceBundle();
    }

    public ReturnCode showHelp(LaunchArguments launchArgs) {
        // If we are showing help but someone put in a messed up command,
        // e.g. "server package --help", we should show/prefer script usage
        String script = launchArgs.getScript();

        Object action = actionHelper.toAction(launchArgs.getAction());
        Object actionHelp = actionHelper.toAction(launchArgs.getProcessName());
        
        if (actionHelper.isHelpAction(action)) {
            Object insert = (actionHelp == null) ? actionHelper.allActions() : actionHelp;

            System.out.println();
            // show java args only if requested: otherwise prefer the script
            if (script == null) {
                System.out.println(MessageFormat.format(options.getString("briefUsage"), insert));
            } else {
                System.out.println(MessageFormat.format(options.getString("scriptUsage"), script, insert));
            }
        }

        if (actionHelper.isHelpAction(action)) {

            if (actionHelp == null) {
                showHelp();
            } else {
                showHelp(actionHelp);
            }
        }

        if (script == null) {
            System.out.println();
            System.out.println(options.getString("use.jvmarg"));
            System.out.println();
            System.out.println(options.getString("javaAgent.key"));
            System.out.println(options.getString("javaAgent.desc"));
            System.out.println();
        }

        return ReturnCode.OK;
    }

    private void showHelp() {
        System.out.println();
        System.out.println(options.getString("processName.key"));
        System.out.println(options.getString("processName.desc"));
        System.out.println();
        System.out.println(options.getString("use.actions"));
        System.out.println();

        for (Object c : actionHelper.getCategories()) {
            System.out.println(options.getString("category-key." + c));
            System.out.println();
            for (Object cmd : actionHelper.geActionsForCategories(c)) {
                System.out.print("    ");
                System.out.println(cmd);
                System.out.println(options.getString("action-desc." + cmd));
            }
            System.out.println();
        }

        System.out.println(options.getString("use.options"));
        System.out.println(options.getString("use.options.gen.desc"));
    }

    private void showHelp(Object command) {
        System.out.println();
        System.out.println(options.getString("action-desc." + command));
        System.out.println();

        System.out.println(options.getString("use.options"));

        System.out.println();

        for (String option : actionHelper.options(command)) {
            String nlsText;
            if (options.containsKey("option-desc." + command + '.' + option)) {
                nlsText = options.getString("option-desc." + command + '.' + option);
            } else if (options.containsKey("option-desc." + option)) {
                nlsText = options.getString("option-desc." + option);
            } else {
                nlsText = null;
            }

            if (nlsText != null) {
                System.out.println(options.getString("option-key." + option));
                System.out.println(nlsText);
            }
        }
    }
}