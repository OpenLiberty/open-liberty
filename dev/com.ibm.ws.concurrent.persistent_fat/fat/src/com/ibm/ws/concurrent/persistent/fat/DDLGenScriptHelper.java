/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;

import componenttest.topology.impl.LibertyServer;

/**
 * Helps us build and run the command that generates DDL.
 */
public class DDLGenScriptHelper {

    private final String scriptName;
    private final String scriptName2;
    private final String installRoot;
    private final String serverName;
    private final String actionName = "generate";
    private final String userDir; // The WLP_USER_DIR for this server

    DDLGenScriptHelper(LibertyServer server) throws Exception {
        Machine m = server.getMachine();
        installRoot = server.getInstallRoot();

        if (m.getOperatingSystem() == OperatingSystem.WINDOWS) {
            scriptName = installRoot + File.separator + "bin" + File.separator + "ddlGen.bat";
            scriptName2 = null;
        } else {
            scriptName = installRoot + File.separator + "bin" + File.separator + "ddlGen";
            scriptName2 = installRoot + File.separator + "bin" + File.separator + "ddlGenDebug";
        }

        serverName = server.getServerName();
        userDir = server.getUserDir();
    }

    // Creates a process builder whose working directory is set to the wlp
    // install dir.
    private ProcessBuilder getProcessBuilder() {
        return new ProcessBuilder(scriptName, actionName, serverName).directory(new File(installRoot));
    }

    /**
     * Inner class that holds the output from a process.
     */
    private static class ProcessOutput {
        private final List<String> sysout;
        private final List<String> syserr;

        ProcessOutput(Process p) throws Exception {
            sysout = new ArrayList<String>();
            syserr = new ArrayList<String>();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (br.ready()) {
                sysout.add(br.readLine());
            }
            br.close();

            br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while (br.ready()) {
                syserr.add(br.readLine());
            }
            br.close();
        }

        void printOutput() {
            System.out.println("SYSOUT:");
            for (String x : sysout) {
                System.out.println(" " + x);
            }

            System.out.println("SYSERR:");
            for (String x : syserr) {
                System.out.println(" " + x);
            }
        }

        private static String search(List<String> list, String z) {
            for (String x : list) {
                if (x.contains(z)) {
                    return x;
                }
            }

            return null;
        }

        String getLineInSysoutContaining(String s) {
            return search(sysout, s);
        }
    }

    /**
     * Return the file name containing the DDL that we need.
     */
    String getPersistentExecutorDDL() throws Exception {
        ProcessBuilder pb = getProcessBuilder();
        Process p = pb.start();
        int rc = p.waitFor();

        ProcessOutput po = new ProcessOutput(p);
        po.printOutput();

        if (rc != 0) {
            throw new Exception("Expected return code 0, actual return code " + rc);
        }

        String successMessage = po.getLineInSysoutContaining("CWWKD0107I");
        if (successMessage == null) {
            throw new Exception("Output did not contain success message CWWKD0107I");
        }

        /*
         * In Chinese locale, it doesn't work simply getting the generated ddl directory from the ddlGen output
         * Instead, set ddl directory as <user directory>+"/servers/" + <server name> + ddl
         * fix this in Chinese locale defect 219914
         */
        //File outputPath = new File(successMessage.substring(72));
        String ddlDir;
        if (userDir.length() >= 1 && userDir.endsWith(File.separator)) {
            ddlDir = userDir + "servers" + File.separator + serverName + File.separator + "ddl";
        } else {
            ddlDir = userDir + File.separator + "servers" + File.separator + serverName + File.separator + "ddl";
        }
        File outputPath = new File(ddlDir);
        if (outputPath.exists() == false) {
            throw new Exception("Output path did not exist: " + outputPath.toString());
        }

        String[] ddlFiles = outputPath.list();
        if (ddlFiles.length == 0) {
            throw new Exception("There was no output in the output directory: " + outputPath.toString());
        }
        if (ddlFiles.length > 1) {
            throw new Exception("We expected just a single file in the DDL directory, but there were " + ddlFiles.length + "files.");
        }

        File ddlFile = new File(outputPath, ddlFiles[0]);
        return ddlFile.getAbsolutePath();
    }

    /**
     * Issue some commands and output the results to aid debug.
     */
    public void getDebugInfo() throws Exception {

        // failed on iseries. do not debug on windows.
        if (scriptName2 != null) {
            ProcessBuilder pb = new ProcessBuilder("uname").directory(new File(installRoot));
            Process p = pb.start();
            int rc = p.waitFor();

            ProcessOutput po = new ProcessOutput(p);
            po.printOutput();

            if (rc != 0) {
                throw new Exception("Expected return code 0 for uname, actual return code " + rc);
            }

            pb = new ProcessBuilder("dirname", "`pwd`").directory(new File(installRoot));
            p = pb.start();
            rc = p.waitFor();

            po = new ProcessOutput(p);
            po.printOutput();

            if (rc != 0) {
                throw new Exception("Expected return code 0 for dirname, actual return code " + rc);
            }

            pb = new ProcessBuilder("env").directory(new File(installRoot));
            p = pb.start();
            rc = p.waitFor();

            po = new ProcessOutput(p);
            po.printOutput();

            if (rc != 0) {
                throw new Exception("Expected return code 0 for env, actual return code " + rc);
            }

            pb = new ProcessBuilder("cat", scriptName).directory(new File(installRoot));
            p = pb.start();
            rc = p.waitFor();

            po = new ProcessOutput(p);
            po.printOutput();

            if (rc != 0) {
                throw new Exception("Expected return code 0 for cat, actual return code " + rc);
            }

        }

    }
}
