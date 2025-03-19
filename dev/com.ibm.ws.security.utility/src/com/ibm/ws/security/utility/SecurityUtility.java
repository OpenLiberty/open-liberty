/*******************************************************************************
 * Copyright (c) 2011,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;
import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateFactory;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtility;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtilityImpl;
import com.ibm.ws.kernel.service.util.UtilityTemplate;
import com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask;
import com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask;
import com.ibm.ws.security.utility.tasks.EncodeTask;
import com.ibm.ws.security.utility.tasks.HelpTask;
import com.ibm.ws.security.utility.tasks.TLSProfilerTask;
import com.ibm.ws.security.utility.utils.CommandUtils;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;
import com.ibm.ws.security.utility.utils.FileUtility;

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
public class SecurityUtility extends UtilityTemplate {

    // TODO: figure out how to get the script name (without hard-coding!)
    static final String SCRIPT_NAME = "securityUtility";
    private final ConsoleWrapper stdin;
    private final PrintStream stdout;
    private final PrintStream stderr;
    List<SecurityUtilityTask> tasks = new ArrayList<SecurityUtilityTask>();

    SecurityUtility(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr) {
        this.stdin = stdin;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    /**
     * Register a task into the SecurityUtility script.
     * The order in which the tasks are registered will
     * affect the usage statement.
     *
     * @param task
     */
    void registerTask(SecurityUtilityTask task) {
        tasks.add(task);
    }

    /**
     * Given a task name, return the corresponding SecurityUtilityTask.
     *
     * @param taskName desired task name
     * @return corresponding SecurityUtilityTask, or null if
     *         no match is found
     */
    private SecurityUtilityTask getTask(String taskName) {
        SecurityUtilityTask task = null;
        for (SecurityUtilityTask availTask : tasks) {
            if (availTask.getTaskName().equals(taskName)) {
                task = availTask;
            }
        }
        return task;
    }

    /**
     * Drive the logic of the program.
     *
     * @param args
     */
    SecurityUtilityReturnCodes runProgram(String[] args) {
        if (stdin == null) {
            stderr.println(CommandUtils.getMessage("error.missingIO", "stdin"));

            return SecurityUtilityReturnCodes.ERR_GENERIC;
        }
        if (stdout == null) {
            stderr.println(CommandUtils.getMessage("error.missingIO", "stdout"));

            return SecurityUtilityReturnCodes.ERR_GENERIC;
        }
        if (stderr == null) {
            stdout.println(CommandUtils.getMessage("error.missingIO", "stderr"));

            return SecurityUtilityReturnCodes.ERR_GENERIC;
        }

        // Help is always available
        HelpTask help = new HelpTask(SCRIPT_NAME, tasks);
        registerTask(help);

        if (args.length == 0) {
            stdout.println(help.getScriptUsage());
            return SecurityUtilityReturnCodes.OK;
        }

        // Convenience: If the first argument can reasonably be interpreted as "help", do so.
        // Note NLS issue and presumption that none of the other commands rhyme with help...
        // which is part of why I'm checking starts/ends rather than contains.
        if (args[0].toLowerCase().endsWith(help.getTaskName().toLowerCase())) {
            args[0] = help.getTaskName();
        }

        SecurityUtilityTask task = getTask(args[0]);
        if (task == null) {
            stderr.println(CommandUtils.getMessage("task.unknown", args[0]));
            stderr.println(help.getScriptUsage());
            // It is unfortunate that this returns 0 (OK) but that's what it used to do,
            // so don't break anything.
            return SecurityUtilityReturnCodes.OK;
        } else {
            try {
                return task.handleTask(stdin, stdout, stderr, args);
            } catch (IllegalArgumentException e) {
                stderr.println("");
                stderr.println(CommandUtils.getMessage("error", e.getMessage()));
                stderr.println(help.getTaskUsage(task));
                return SecurityUtilityReturnCodes.ERR_GENERIC;
            } catch (Exception e) {
                stderr.println("");
                stderr.println(CommandUtils.getMessage("error", e.toString()));
                stderr.println(help.getTaskUsage(task));
		e.printStackTrace(stderr);
                return SecurityUtilityReturnCodes.ERR_GENERIC;
            }
        }
    }

    /**
     * Main method, which wraps the instance logic and registers
     * the known tasks.
     *
     * @param args
     */
    public static void main(String[] args) {
        ConsoleWrapper console = new ConsoleWrapper(System.console(), System.err);

        // Create / obtain the collaborators
        DefaultSSLCertificateCreator certCreator = DefaultSSLCertificateFactory.getDefaultSSLCertificateCreator();
        LTPAKeyFileUtility ltpaKeyFileCreator = new LTPAKeyFileUtilityImpl();

        // Create the SecurityUtility and register tasks
        SecurityUtility util = new SecurityUtility(console, System.out, System.err);
        IFileUtility fileUtil = new FileUtility(util.getUserDir(), util.getOutputDir(null));
        util.registerTask(new EncodeTask(SCRIPT_NAME));
        util.registerTask(new CreateSSLCertificateTask(certCreator, fileUtil, SCRIPT_NAME));
        util.registerTask(new CreateLTPAKeysTask(ltpaKeyFileCreator, fileUtil, SCRIPT_NAME));
        util.registerTask(new TLSProfilerTask(fileUtil, SCRIPT_NAME));

        // Kick everything off
        int rc = util.runProgram(args).getReturnCode();
        System.exit(rc);
    }
}
