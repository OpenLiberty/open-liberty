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
        if (actionName == null || actionName.equalsIgnoreCase("help")) {
            rc = FeatureAction.help.handleTask(args);
        } else {
            try {
                FeatureAction action = FeatureAction.valueOf(actionName);
                List<String> invalid = args.findInvalidOptions(action.getCommandOptions());

                if (!!!invalid.isEmpty()) {
                    // TODO unknown message
                    FeatureAction.help.handleTask(args);
                    rc = ReturnCode.BAD_ARGUMENT;
                } else {
                    rc = action.handleTask(args);
                }


            } catch (IllegalArgumentException iae) {
                rc = FeatureAction.help
                        .handleTask(new ArgumentsImpl(new String[] { FeatureAction.help.toString(), actionName }));
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


}



