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
package com.ibm.ws.install.featureUtility.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernelFactory;
import com.ibm.ws.install.InstallKernelInteractive;
import com.ibm.ws.install.featureUtility.FeatureUtility;
import com.ibm.ws.install.featureUtility.FeatureUtilityExecutor;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.internal.ProgressBar;
import com.ibm.ws.install.internal.asset.ServerAsset;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;

public class FeatureInstallAction implements ActionHandler {

    private FeatureUtility featureUtility;
    private InstallKernelInteractive installKernel;
    private Set<ServerAsset> servers;
    private Logger logger;
    private List<String> argList;
    private List<String> featureNames;
    private String fromDir;
    private String toDir;
    private ProgressBar progressBar;

    @Override
    public ExitCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {
        ExitCode rc = initialize(args);
        if (!!!rc.equals(ReturnCode.OK)) {
            return rc;
        }
        rc = execute();
        return rc;

    }

    // initialize feature utility
    private ExitCode initialize(Arguments args) {
        ExitCode rc = ReturnCode.OK;
        
        this.logger = InstallLogUtils.getInstallLogger();
        this.installKernel = InstallKernelFactory.getInteractiveInstance();
        this.featureNames = new ArrayList<String>();
        this.servers = new HashSet<>();

        this.argList = args.getPositionalArguments();
        this.fromDir = args.getOption("from");
        this.toDir = args.getOption("to");

        this.progressBar = ProgressBar.getInstance();

        HashMap<String, Integer> methodMap = new HashMap<>();
        // initialize feature utility and install kernel map
        methodMap.put("initializeMap", 10);
        methodMap.put("fetchJsons", 10);
        // in installFeature we have 80 units to work with
        methodMap.put("resolvedFeatures", 20);
        methodMap.put("fetchArtifacts", 20);
        methodMap.put("installFeatures", 30);
        methodMap.put("cleanUp", 10);

        progressBar.setMethodMap(methodMap);

        String arg = argList.get(0);
        try {
            if(isServer(arg)) {
                serverInit(arg);
            } else {
                Collection<String> assetIds = new HashSet<String>(argList);
                return assetInstallInit(assetIds);

            }
        } catch (InstallException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return FeatureUtilityExecutor.returnCode(e.getRc());
        } catch (Throwable e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return FeatureUtilityExecutor.returnCode(InstallException.IO_FAILURE);
        }

        return rc;
        
    }

    private ExitCode assetInstallInit(Collection<String> assetIds) {
        featureNames.addAll(assetIds);
        return ReturnCode.OK;
    }

    private ReturnCode serverInit(String fileName) throws InstallException, IOException {

        File serverXML = (fileName.toLowerCase().endsWith(InstallUtils.SERVER_XML)) ? new File(fileName) : new File(InstallUtils.getServersDir(), fileName + File.separator
                                                                                                                                                  + InstallUtils.SERVER_XML);

        if (!serverXML.isFile()) {
            throw new InstallException("Unable to find server.xml file", InstallException.RUNTIME_EXCEPTION);
        }

        servers.add(new ServerAsset(serverXML));

        return ReturnCode.OK;
    }
    
    private static boolean isServer(String fileName) {
        return new File(InstallUtils.getServersDir(), fileName).isDirectory()
               || fileName.toLowerCase().endsWith("server.xml");
    }

    private ExitCode installServerFeatures() {
        ExitCode rc = ReturnCode.OK;
        Collection<String> featuresToInstall = new HashSet<String>();

        try {
            featuresToInstall.addAll(installKernel.getServerFeaturesToInstall(servers, false));
        } catch (InstallException ie) {
            logger.log(Level.SEVERE, ie.getMessage(), ie);
            return FeatureUtilityExecutor.returnCode(ie.getRc());
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            rc = ReturnCode.RUNTIME_EXCEPTION;
        }

        if (featuresToInstall.isEmpty()) {
            logger.log(Level.INFO, "Additional server features not required");
        } else {
            logger.log(Level.INFO, "New server features required");
            rc = assetInstallInit(featuresToInstall);
        }
 
        return rc;
    }

    private ExitCode install() {
        try {
            featureUtility = new FeatureUtility.FeatureUtilityBuilder().setFromDir(fromDir).setToExtension(toDir)
                    .setFeaturesToInstall(featureNames).build();
            featureUtility.installFeatures();
        } catch (InstallException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return FeatureUtilityExecutor.returnCode(e.getRc());
        } catch (Throwable e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return FeatureUtilityExecutor.returnCode(InstallException.IO_FAILURE);
        }
        return ReturnCode.OK;
    }

    private ExitCode execute() {
        ExitCode rc = ReturnCode.OK;
        // Second check any newly deployed servers for missing required features
        if (!servers.isEmpty()) {
            rc = installServerFeatures();
        }
        if (ReturnCode.OK.equals(rc) && !featureNames.isEmpty()) {
            rc = install();
        }

        progressBar.finish();

        return rc;

    }


}
