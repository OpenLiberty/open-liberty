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

package com.ibm.ws.install.featureUtility.cli;

import java.io.PrintStream;

import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;

public class FeatureHelpAction implements ActionHandler {
    private static final String COMMAND = "featureUtility";
    private static final String NL = System.getProperty("line.separator");

    public String getScriptUsage() {
        StringBuffer scriptUsage = new StringBuffer(NL);
        scriptUsage.append("usage: featureUtility {install|download|help} [options]");
        scriptUsage.append(NL);

        return scriptUsage.toString();

    }

    public String getVerboseHelp() {
        StringBuilder sb = new StringBuilder(getScriptUsage());
        // TODO
        return sb.toString();
    }

    public String getTaskUsage(FeatureAction task) {
        // TODO
        return getScriptUsage(); // TODO return task usage for each task
    }

    @Override
    public ExitCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {
        String actionName = args.getAction();
        if (actionName == null) {
            stdout.println(getScriptUsage());
        } else if (args.getPositionalArguments().isEmpty()) {
            stdout.println(getVerboseHelp());
        } else {
            stdout.println(getTaskUsage(null));

        }
        return ReturnCode.OK;
    }

}
