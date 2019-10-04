/*******************************************************************************n * Copyright (c) 2019 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License v1.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-v10.htmln *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package com.ibm.ws.install.featureUtility;


import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernel;
import com.ibm.ws.install.InstallKernelFactory;
import com.ibm.ws.install.internal.InstallKernelImpl;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.feature.internal.cmdline.ArgumentsImpl;

/**
 *
 */
public class FeatureUtilityExecutor {
    private static String helpString = "usage: ./featureUtility install featureA";

    /**
     * @param args
     */
    public static void main(String[] argsArray) {
        Arguments args = new ArgumentsImpl(argsArray);
        String actionName = args.getAction();
        if (actionName == null || actionName.equalsIgnoreCase("help")) {
            System.out.println(helpString);
            // todo arg processing
        } else {
            InstallKernel installKernel = InstallKernelFactory.getInstance();

            String verboseLevel = args.getOption("verbose");
            Level logLevel = Level.INFO;
            if (verboseLevel != null && verboseLevel.isEmpty()) {
                logLevel = Level.FINE;
            } else if (verboseLevel != null && verboseLevel.equalsIgnoreCase("debug")) {
                logLevel = Level.FINEST;
            }
            ((InstallKernelImpl) installKernel).enableConsoleLog(logLevel);

            // assume install for now
            String fromDir = args.getOption("from");
            String repoType = args.getOption("to");
            List<String> featuresToInstall = args.getPositionalArguments();

            try {
                FeatureUtility featureUtility = new FeatureUtility.FeatureUtilityBuilder()
                        .setFeaturesToInstall(featuresToInstall).setFromDir(fromDir).setToExtension(repoType).build();
                featureUtility.installFeatures();
            } catch (IOException | InstallException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }


}
