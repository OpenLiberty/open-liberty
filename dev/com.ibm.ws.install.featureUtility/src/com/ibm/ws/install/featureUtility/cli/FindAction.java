/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.featureUtility.cli;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.featureUtility.FeatureUtility;
import com.ibm.ws.install.internal.ProgressBar;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;
import com.ibm.ws.kernel.feature.internal.cmdline.ArgumentsImpl;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FindAction implements ActionHandler {
    private List<String> argList;
    private ProgressBar progressBar;

    @Override
    public ExitCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {
//        if (args.getPositionalArguments().isEmpty()) {
//            FeatureAction.help.handleTask(new ArgumentsImpl(new String[]{"help", FeatureAction.getEnum(args.getAction()).toString()}));
//            return ReturnCode.BAD_ARGUMENT;
//        }
        ExitCode rc = initialize(args);
        if (!!!rc.equals(ReturnCode.OK)) {
            return rc;
        }
        rc = execute();
        return rc;
    }


    private ExitCode initialize(Arguments args) {
        // get the json
        this.argList = args.getPositionalArguments(); // args = the query

        this.progressBar = ProgressBar.getInstance();

        HashMap<String, Double> methodMap = new HashMap<>();
        // initialize feature utility and install kernel map
        methodMap.put("initializeMap", 5.00);//done
        methodMap.put("fetchJsons", 20.00); //dpne
        methodMap.put("findFeatures", 75.00);

        progressBar.setMethodMap(methodMap);


        return ReturnCode.OK;
    }

    private ExitCode execute() {
        try {
            FeatureUtility featureUtility = new FeatureUtility.FeatureUtilityBuilder().setFeaturesToInstall(argList).setAdditionalJsons(new ArrayList<>()).build();
            featureUtility.findFeatures();
            progressBar.finish();
        } catch (IOException e) {
            e.printStackTrace();
            progressBar.finishWithError();
        } catch (InstallException e) {
            e.printStackTrace();
            progressBar.finishWithError();
        }

        return ReturnCode.OK;
    }
}
