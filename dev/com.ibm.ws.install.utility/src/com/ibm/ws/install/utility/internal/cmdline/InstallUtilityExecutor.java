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

import java.util.List;

import com.ibm.ws.install.utility.cmdline.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;
import com.ibm.ws.kernel.feature.internal.cmdline.ArgumentsImpl;
import com.ibm.ws.kernel.feature.internal.cmdline.NLS;

/**
 * This class validates and executes the actions that are in argsArray.
 */
public class InstallUtilityExecutor {

    /**
     * @param args - task/ action
     */
    public static void main(String[] argsArray) {
        ExitCode rc;

        ArgumentsImpl args = new ArgumentsImpl(argsArray);
        String actionName = args.getAction();
        if (actionName == null) {
            rc = Action.help.handleTask(args);
        } else {
            try {
                if (looksLikeHelp(actionName))
                    actionName = Action.help.toString();

                Action exeAction = Action.valueOf(actionName);
                List<String> invalid = args.findInvalidOptions(exeAction.getCommandOptions());
                if (!!!invalid.isEmpty()) {
                    // NLS messages go to system out. Other exceptions/stack traces go to System.err
                    System.out.println(NLS.getMessage("unknown.options", exeAction, invalid));
                    Action.help.handleTask(new ArgumentsImpl(new String[] { "help", actionName }));
                    rc = ReturnCode.BAD_ARGUMENT;
                } else if (exeAction != Action.help && exeAction.numPositionalArgs() >= 0 && args.getPositionalArguments().size() != exeAction.numPositionalArgs()) {
                    // NLS messages go to system out. Other exceptions/stack traces go to System.err
                    System.out.println(NLS.getMessage("missing.args", exeAction, exeAction.numPositionalArgs(), args.getPositionalArguments().size()));
                    rc = ReturnCode.BAD_ARGUMENT;
                } else {
                    rc = exeAction.handleTask(args);
                }
            } catch (IllegalArgumentException iae) {
                rc = Action.help.handleTask(new ArgumentsImpl(new String[] { Action.help.toString(), actionName }));
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
        return Action.help.toString().equalsIgnoreCase(taskname.substring(start).toLowerCase());
    }

    /**
     *
     * @param rc - value
     * @return - code associated with rc value
     */
    public static ReturnCode returnCode(int rc) {
        for (ReturnCode r : ReturnCode.values()) {
            if (r.getValue() == rc)
                return r;
        }
        return ReturnCode.RUNTIME_EXCEPTION;
    }
}
