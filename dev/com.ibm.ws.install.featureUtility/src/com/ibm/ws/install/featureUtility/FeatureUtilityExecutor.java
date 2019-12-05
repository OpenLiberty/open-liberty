/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.featureUtility;


import java.io.IOException;

import java.util.List;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.featureUtility.cli.FeatureAction;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;
import com.ibm.ws.kernel.feature.internal.cmdline.ArgumentsImpl;
import com.ibm.ws.kernel.feature.internal.cmdline.NLS;

/**
 *
 */
public class FeatureUtilityExecutor {
    /**
     * @param argsArray
     * @throws InstallException
     * @throws IOException
     */
    public static void main(String[] argsArray) {
        ExitCode rc;
        Arguments args = new ArgumentsImpl(argsArray);
        String actionName = args.getAction();
        if (actionName == null) {
            rc = FeatureAction.help.handleTask(args);
        } else {
            try {
                if (looksLikeHelp(actionName))
                    actionName = FeatureAction.help.toString();
                FeatureAction action = FeatureAction.getEnum(actionName);
                List<String> invalid = args.findInvalidOptions(action.getCommandOptions());


                if (!!!invalid.isEmpty()) {
                    System.out.println(NLS.getMessage("unknown.options", action, invalid));
                    FeatureAction.help.handleTask(new ArgumentsImpl(new String[] { "help", action.toString() }));
                    rc = ReturnCode.BAD_ARGUMENT;
                } else if(action != FeatureAction.help && action.numPositionalArgs() >= 0 && args.getPositionalArguments().size() != action.numPositionalArgs()) {
                    // NLS messages go to system out. Other exceptions/stack traces go to System.err
                    System.out.println(NLS.getMessage("missing.args", action, action.numPositionalArgs(), args.getPositionalArguments().size()));
                    rc = ReturnCode.BAD_ARGUMENT;
                } else{
                    rc = action.handleTask(args);
                }

            } catch (IllegalArgumentException iae) {
                rc = FeatureAction.help
                                .handleTask(new ArgumentsImpl(new String[] { FeatureAction.help.toString(), actionName}));
            }

        }
        System.exit(rc.getValue());
    }

    public static ReturnCode returnCode(int rc) {
        for (ReturnCode r : ReturnCode.values()) {
            if (r.getValue() == rc)
                return r;
        }
        return ReturnCode.RUNTIME_EXCEPTION;
    }

    // strip off any punctuation or other noise, see if the rest appears to be a help request.
    // note that the string is already trim()'d by command-line parsing unless user explicitly escaped a space
    private static boolean looksLikeHelp(String taskname) {
        if (taskname == null)
            return false; // applied paranoia, since this isn't in a performance path
        int start = 0, len = taskname.length();
        while (start < len && !Character.isLetter(taskname.charAt(start)))
            ++start;
        return FeatureAction.help.toString().equalsIgnoreCase(taskname.substring(start).toLowerCase());
    }



}



