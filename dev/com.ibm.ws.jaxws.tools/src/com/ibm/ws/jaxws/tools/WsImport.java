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
package com.ibm.ws.jaxws.tools;

import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.ws.jaxws.tools.internal.JaxWsToolsConstants;
import com.ibm.ws.jaxws.tools.internal.JaxWsToolsUtil;
import com.sun.tools.ws.wscompile.WsimportTool;

/**
 * IBM Wrapper for wsimport tool.
 */
public class WsImport {
    private static final PrintStream err = System.err;

    public static void main(String[] args) {
        if (isTargetRequired(args)) {
            String errMsg = JaxWsToolsUtil.formatMessage(JaxWsToolsConstants.ERROR_PARAMETER_TARGET_MISSED_KEY);
            err.println(errMsg);
            System.exit(1);
        }
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                System.setProperty("javax.xml.accessExternalSchema", "all");
                return null;
            }
        });

        System.exit(new WsimportTool(System.out).run(args) ? 0 : 1);
    }

    private static boolean isTargetRequired(String[] args) {
        boolean helpExisted = false;
        boolean versionExisted = false;
        boolean targetExisted = false;

        for (String arg : args) {
            if (arg.equals(JaxWsToolsConstants.PARAM_HELP)) {
                helpExisted = true;
            } else if (arg.equals(JaxWsToolsConstants.PARAM_VERSION)) {
                versionExisted = true;
            } else if (arg.equals(JaxWsToolsConstants.PARAM_TARGET)) {
                targetExisted = true;
            }

            continue;
        }

        return args.length > 0 && !helpExisted && !versionExisted && !targetExisted;
    }
}
