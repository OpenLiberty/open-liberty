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

import java.util.List;

import com.ibm.ws.kernel.boot.cmdline.ExitCode;
import com.ibm.ws.kernel.feature.internal.cmdline.ArgumentsImpl;
import com.ibm.ws.kernel.feature.internal.cmdline.NLS;
import com.ibm.ws.kernel.feature.internal.cmdline.ReturnCode;

/**
 *
 */
public class InstallExecutor {

    /**
     * @param args
     */
    public static void main(String[] argsArray) {
        ExitCode rc;

        ArgumentsImpl args = new ArgumentsImpl(argsArray);
        String actionName = args.getAction();
        if (actionName == null) {
            rc = ExeAction.help.handleTask(args);
        } else {
            try {
                if (looksLikeHelp(actionName))
                    actionName = ExeAction.help.toString();

                ExeAction exeAction = ExeAction.valueOf(actionName);
                List<String> invalid = args.findInvalidOptions(exeAction.getCommandOptions());
                if (!!!invalid.isEmpty()) {
                    // NLS messages go to system out. Other exceptions/stack traces go to System.err
                    System.out.println(NLS.getMessage("unknown.options", exeAction, invalid));
                    rc = ReturnCode.BAD_ARGUMENT;
                } else if (exeAction != ExeAction.help && exeAction.numPositionalArgs() >= 0 && args.getPositionalArguments().size() != exeAction.numPositionalArgs()) {
                    // NLS messages go to system out. Other exceptions/stack traces go to System.err
                    System.out.println(NLS.getMessage("missing.args", exeAction, exeAction.numPositionalArgs(), args.getPositionalArguments().size()));
                    rc = ReturnCode.BAD_ARGUMENT;
                } else {
                    rc = exeAction.handleTask(args);
                }
            } catch (IllegalArgumentException iae) {
                rc = ExeAction.help.handleTask(new ArgumentsImpl(new String[] { ExeAction.help.toString(), actionName }));
            }
        }
        System.exit(rc.getValue());
    }

    // strip off any punctuation or other noise, see if the rest appears to be a help request.
    // note that the string is already trim()'d by command-line parsing unless user explicitly escaped a space
    private static boolean looksLikeHelp(String taskname) {
        if (taskname == null)
            return false; // applied paranoia, since this isn't in a performance path
        int start = 0, len = taskname.length();
        while (start < len && !Character.isLetter(taskname.charAt(start)))
            ++start;
        return ExeAction.help.toString().equalsIgnoreCase(taskname.substring(start).toLowerCase());
    }

    public static ReturnCode returnCode(int rc) {
        for (ReturnCode r : ReturnCode.values()) {
            if (r.getValue() == rc)
                return r;
        }
        return ReturnCode.RUNTIME_EXCEPTION;
    }
}
