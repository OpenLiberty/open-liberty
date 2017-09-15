/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webserver.plugin.utility;

import com.ibm.ws.product.utility.CommandTask;
import com.ibm.ws.product.utility.CommandTaskRegistry;
import com.ibm.ws.product.utility.ExecutionContextImpl;
import com.ibm.ws.webserver.plugin.utility.tasks.GeneratePluginTask;
import com.ibm.ws.webserver.plugin.utility.tasks.HelpTask;
import com.ibm.ws.webserver.plugin.utility.tasks.MergePluginFilesTask;
import com.ibm.ws.webserver.plugin.utility.utils.CommandUtils;
import com.ibm.ws.webserver.plugin.utility.utils.PluginUtilityConsole;

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
public class WebServerPluginUtility {

    static final String SCRIPT_NAME = "pluginUtility";
    private final PluginUtilityConsole commandConsole;
    CommandTaskRegistry taskResgistry = new CommandTaskRegistry();

    WebServerPluginUtility(PluginUtilityConsole cmdConsole) {
          commandConsole = cmdConsole;
    }

    /**
     *
     * @param task
     */
    void registerTask(CommandTask task) {
    	taskResgistry.registerCommandTask(task.getTaskName(),task);
    }

    /**
     * @param taskName desired task name
     * @return corresponding CommandTask, or null if
     *         no match is found
     */
    private CommandTask getTask(String taskName) {     
        return taskResgistry.getCommandTask(taskName);
    }

    /**
     * Drive the logic of the program.
     *
     * @param args
     */
    int runProgram(String[] args) {
        //if (!commandConsole.isInputStreamAvailable()) {
        //	commandConsole.printErrorMessage(CommandUtils.getMessage("error.missingIO", "stdin"));
        //    return 254;
        //}
        if (!commandConsole.isStandardOutAvailable()) {
            commandConsole.printErrorMessage(CommandUtils.getMessage("error.missingIO", "stdout"));
            return 253;
        }
        if (!commandConsole.isStandardErrorAvailable()) {
        	commandConsole.printInfoMessage(CommandUtils.getMessage("error.missingIO", "stderr"));
            return 252;
        }

        // Help is always available
        HelpTask help = new HelpTask(SCRIPT_NAME);
        registerTask((CommandTask)help);
        
        
        ExecutionContextImpl executionCtx = new ExecutionContextImpl(commandConsole,args,this.taskResgistry);

        if (args.length == 0) {
        	help.execute(executionCtx);
            return 0;
        }

        if (looksLikeHelp(help, args[0]))
            args[0] = help.getTaskName();
        
        CommandTask task = getTask(args[0]);
        if (task == null) {
        	commandConsole.printlnErrorMessage(CommandUtils.getMessage("task.unknown", args[0]));
        	help.execute(executionCtx);
            return 0;
        } else {
            try {
                task.execute(executionCtx);
            } catch (IllegalArgumentException e) {
            	commandConsole.printlnErrorMessage("");
                commandConsole.printlnErrorMessage(CommandUtils.getMessage("error", e.getMessage()));
                help.execute(executionCtx);
                return 20;
            } 
        }
        return 0;
    }

    // strip off any punctuation or other noise, see if the rest appears to be a help request.
    // note that the string is already trim()'d by command-line parsing unless user explicitly escaped a space
    private static boolean looksLikeHelp(HelpTask help, String taskname) {
        if (taskname == null)
            return false; // applied paranoia, since this isn't in a performance path
        int start = 0, len = taskname.length();
        while (start < len && !Character.isLetter(taskname.charAt(start)))
            ++start;
        return help.getTaskName().equalsIgnoreCase(taskname.substring(start).toLowerCase());
    }

    /**
     * Main method, which wraps the instance logic and registers
     * the known tasks.
     *
     * @param args
     */
    public static void main(String[] args) {

    	PluginUtilityConsole console = new PluginUtilityConsole(System.console(), System.out,System.err);
    
        // This is only needed for the GenerateFromServerPluginsTask so comment out for now.
        //IGeneratePluginConfigMBeanConnection pluginUtilityMBeanConn = new GeneratePluginConfigMBeanConnection(System.out, System.err, fileUtil);

        WebServerPluginUtility util = new WebServerPluginUtility(console);

        // Initially we are only going to deliver the function for MergePluginFilesTask
        //util.registerTask(new GenerateFromServerPluginTask(SCRIPT_NAME, fileUtil, pluginUtilityMBeanConn));
        util.registerTask(new MergePluginFilesTask(SCRIPT_NAME));
        util.registerTask(new GeneratePluginTask(SCRIPT_NAME,console));
       // util.registerTask(new DynamicRoutingPluginTask(SCRIPT_NAME, fileUtil));

        int rc = util.runProgram(args);

        System.exit(rc);
    }
}
