/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxb.tools;

import java.io.PrintStream;

import com.ibm.ws.jaxb.tools.internal.JaxbToolsConstants;
import com.ibm.ws.jaxb.tools.internal.JaxbToolsUtil;

/**
 * IBM Wrapper for XJC tool.
 */
public class XJC {
    private static final PrintStream err = System.err;

    public static void main(String args[]) throws java.lang.Throwable {
        if (isTargetRequired(args)) {
            String errMsg = JaxbToolsUtil.formatMessage(JaxbToolsConstants.ERROR_PARAMETER_TARGET_MISSED_KEY);
            err.println(errMsg);

            return;
        }

        com.sun.tools.xjc.Driver.main(args);
    }

    private static boolean isTargetRequired(String[] args) {
        boolean helpExisted = false;
        boolean versionExisted = false;
        boolean targetExisted = false;

        for (String arg : args) {
            if (arg.equals(JaxbToolsConstants.PARAM_HELP)) {
                helpExisted = true;
            } else if (arg.equals(JaxbToolsConstants.PARAM_VERSION)) {
                versionExisted = true;
            } else if (arg.equals(JaxbToolsConstants.PARAM_TARGET)) {
                targetExisted = true;
            }

            continue;
        }

        return args.length > 0 && !helpExisted && !versionExisted && !targetExisted;
    }
}
