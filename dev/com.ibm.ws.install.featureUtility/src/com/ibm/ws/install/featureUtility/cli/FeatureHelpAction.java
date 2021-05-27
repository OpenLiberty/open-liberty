/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.install.featureUtility.cli;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;

public class FeatureHelpAction implements ActionHandler {
    private static final String COMMAND = "featureUtility";
    private static final String NL = System.getProperty("line.separator");
    public static final ResourceBundle featureUtilityToolOptions = ResourceBundle.getBundle("com.ibm.ws.install.featureUtility.internal.resources.FeatureUtilityToolOptions");


    public String getScriptUsage() {
        StringBuffer scriptUsage = new StringBuffer(NL);
        scriptUsage.append(getHelpPart("usage", COMMAND));
        scriptUsage.append(" {");

        FeatureAction [] tasks = FeatureAction.values();
        for(int i =0; i < tasks.length; i ++){
            FeatureAction task = tasks[i];
            scriptUsage.append(task.toString());
            if (i != (tasks.length - 1)) {
                scriptUsage.append("|");
            }
        }
        scriptUsage.append("} [");
        scriptUsage.append(getHelpPart("global.options.lower"));
        scriptUsage.append("]");
        scriptUsage.append(NL);
        return scriptUsage.toString();
    }

    private String getDescription(FeatureAction action){
        return getHelpPart(action + ".desc");
    }

    public StringBuilder getVerboseHelp() {
        StringBuilder verboseHelp = new StringBuilder(getScriptUsage());
        verboseHelp.append(NL);
        verboseHelp.append(getHelpPart("global.actions"));
        verboseHelp.append(NL);
        for (FeatureAction action : FeatureAction.values()) {
            verboseHelp.append(NL);
            verboseHelp.append("    ");
            verboseHelp.append(action.toString());
            if(!action.getAbbreviation().isEmpty()){
                verboseHelp.append(", ").append(action.getAbbreviation());
            }
            verboseHelp.append(NL);
            verboseHelp.append(getDescription(action));
            verboseHelp.append(NL);
        }
        verboseHelp.append(NL);
        verboseHelp.append(getHelpPart("global.options"));
        verboseHelp.append(NL);
        verboseHelp.append(getHelpPart("global.options.statement"));
        verboseHelp.append(NL);
        verboseHelp.append(NL);
        return verboseHelp;
    }

    public String getTaskUsage(FeatureAction task) {
        StringBuilder taskUsage = new StringBuilder(NL);

        // print a empty line
        taskUsage.append(getHelpPart("global.usage"));
        taskUsage.append(NL);
        taskUsage.append('\t');
        taskUsage.append(COMMAND);
        taskUsage.append(' ');
        taskUsage.append(task);
        if (task.showOptions()) {
            taskUsage.append(" [");
            taskUsage.append(getHelpPart("global.options.lower"));
            taskUsage.append("]");
        }
        List<String> options = task.getCommandOptions();
        for (String option : options) {
            if (option.charAt(0) != '-') {
                taskUsage.append(' ');
                taskUsage.append(option);
            }
        }
        taskUsage.append(NL);
        if(!task.getAbbreviation().isEmpty()){
            taskUsage.append('\t');
            taskUsage.append(COMMAND);
            taskUsage.append(' ');
            taskUsage.append(task.getAbbreviation());
            if (task.showOptions()) {
                taskUsage.append(" [");
                taskUsage.append(getHelpPart("global.options.lower"));
                taskUsage.append("]");
            }
            for (String option : options) {
                if (option.charAt(0) != '-') {
                    taskUsage.append(' ');
                    taskUsage.append(option);
                }
            }
            taskUsage.append(NL);
        }

        taskUsage.append(NL);
        taskUsage.append(getHelpPart("global.description"));
        taskUsage.append(NL);
        taskUsage.append(getDescription(task));
        taskUsage.append(NL);
        if (options.size() > 0) {
            taskUsage.append(NL);
            taskUsage.append(getHelpPart("global.options"));

            for (String option : task.getCommandOptions()) {
                taskUsage.append(NL);
                String optionKey;
                try {
                    optionKey = getHelpPart(task + ".option-key." + option);
                } catch (MissingResourceException e) {
                    // This happens because we don't have a message for the key for the
                    // license arguments but these are not translated and don't need any
                    // arguments after them so can just use the option
                    optionKey = "    " + option;
                }
                taskUsage.append(optionKey);
                taskUsage.append(NL);
                taskUsage.append(getHelpPart(task + ".option-desc." + option));
                taskUsage.append(NL);
            }
        }
        taskUsage.append(NL);
        return taskUsage.toString();
    }

    public static String getHelpPart(String key, Object... args) {
        String option = featureUtilityToolOptions.getString(key);
        return args.length == 0 ? option : MessageFormat.format(option, args);
    }

    @Override
    public ExitCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {
    	ReturnCode retCode = ReturnCode.OK;
        String actionName = args.getAction();
        if (actionName == null) {
            stdout.println(getScriptUsage());
        } else if (args.getPositionalArguments().isEmpty()) {
            stdout.println(getVerboseHelp());
        } else {
            try {
                FeatureAction task = FeatureAction.getEnum(args.getPositionalArguments().get(0));
                stdout.println(getTaskUsage(task));
            } catch (IllegalArgumentException e) {
                stderr.println();
                stderr.println(getHelpPart("task.unknown", args.getPositionalArguments().get(0)));
                stdout.println(getScriptUsage());
                retCode = ReturnCode.BAD_ARGUMENT;
            }

        }
        return retCode;
    }



}
