/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health40.internal;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.Status;

import io.openliberty.microprofile.health.internal.common.HealthCheckConstants;

/**
 * singleton
 */
public class FileHealthCheck {

    final String SERVER_CONFIG_DIR;

    private static FileHealthCheck instance;

    File healthDir;

    private static volatile boolean isValidSystem = true;

    private FileHealthCheck() {
        SERVER_CONFIG_DIR = System.getProperty("server.config.dir");
    };

    public static synchronized FileHealthCheck getInstance() {
        if (instance == null) {
            instance = new FileHealthCheck();
        }
        return instance;
    }

    //Called by activation of AppTracker
    public void initHealthFileValidation() throws IOException {

        // Check we have I/O for directory
        // Check if /health directory exists
        //      YES
        //              See if we can write
        //                      YES good
        //                      NO warning return
        //              If files exists, delete
        //                      YES good
        //                      NO warning return
        //      NO
        //              create /health
        //                      YES
        //                      Test file creation
        //                              YES good
        //                              NO warning, return
        //                      NO
        //                      warning return

        File serverConfigDir = new File(SERVER_CONFIG_DIR);
        //System.out.println("isDirectory? " + serverConfigDir.isDirectory());
        healthDir = new File(serverConfigDir, "health");

        //Health Dir does not exist -> create and test write
        if (!healthDir.exists()) {
            if (!FileUtils.createDirectory(healthDir)) {
                isValidSystem = false;
                return;
            }

            //Testing write.
            if (!FileUtils.canWrite(healthDir)) {
                isValidSystem = false;
                return;
            }

        } else { // /health dir exists
            healthDir.deleteOnExit();
            File[] fileArr = healthDir.listFiles();

            //delete
            for (File f : fileArr) {
                if (f.getName().equals(HealthCheckFileName.STARTED_FILE.getFileName()) ||
                    f.getName().equals(HealthCheckFileName.LIVE_FILE.getFileName()) ||
                    f.getName().equals(HealthCheckFileName.READY_FILE.getFileName())) {
                    //failure on delete, return

                    System.out.println(" AHH BAD " + f.getAbsolutePath());

                    if (f.isDirectory()) {
                        //TODO: Issue warning
                        isValidSystem = false;
                        return;
                    } else {
                        if (!FileUtils.deleteFiles(f)) {
                            isValidSystem = false;
                            return;
                        }
                    }
                }
            } //end for

            //Testing write.
            if (!FileUtils.canWrite(healthDir)) {
                isValidSystem = false;
                return;
            }
        }
    }

    //Called by AppTracker (After a appliation is started)
    public void startProcesses() {

        File startFile = new File(healthDir, HealthCheckFileName.STARTED_FILE.getFileName());
        File readyFile = new File(healthDir, HealthCheckFileName.READY_FILE.getFileName());
        File liveFile = new File(healthDir, HealthCheckFileName.LIVE_FILE.getFileName());

        //TODO: if no instance found, will need to fail.

        //Initial Check
        HealthCheck40ServiceImpl inst;
        //refactor make these classes extend parent class
        if ((inst = HealthCheck40ServiceImpl.getInstance()) != null) {

            if (inst.performFileHealthCheck(startFile, HealthCheckConstants.HEALTH_CHECK_START).equals(Status.DOWN)) {
                Timer startedTimer = new Timer(false);
                startedTimer.schedule(new FileUpdateProcess(startFile, inst, HealthCheckConstants.HEALTH_CHECK_START, true), 0, 1000);
            }

            //TODO: Read Config. Substitute values.

            /*
             * Below are the perpertual tasks.
             */

            inst.performFileHealthCheck(readyFile, HealthCheckConstants.HEALTH_CHECK_READY).equals(Status.DOWN);
            Timer readyTimer = new Timer(false);
            readyTimer.schedule(new FileUpdateProcess(readyFile, inst, HealthCheckConstants.HEALTH_CHECK_READY), 0, 1000);

            inst.performFileHealthCheck(liveFile, HealthCheckConstants.HEALTH_CHECK_LIVE).equals(Status.DOWN);
            Timer liveTimer = new Timer(false);
            liveTimer.schedule(new FileUpdateProcess(liveFile, inst, HealthCheckConstants.HEALTH_CHECK_LIVE), 0, 1000);

        }
    }

    public class FileUpdateProcess extends TimerTask {

        HealthCheck40ServiceImpl inst;
        File file;
        String healthCheckProcedure;
        boolean isStopOnCreate = false;

        public FileUpdateProcess(File file, HealthCheck40ServiceImpl inst, String healthCheckProcedure) {
            this(file, inst, healthCheckProcedure, false);
        }

        public FileUpdateProcess(File file, HealthCheck40ServiceImpl inst, String healthCheckProcedure, boolean isStopOnCreate) {
            this.inst = inst;
            this.file = file;
            this.isStopOnCreate = isStopOnCreate;
        }

        @Override
        public void run() {
            System.out.println("hi running for file " + file.getName());
            inst.performFileHealthCheck(file, HealthCheckConstants.HEALTH_CHECK_START);
            if (isStopOnCreate && file.exists()) {
                System.out.println("Misson accomplished, canceleling for file " + file.getName());
                cancel();
            }

        }
    }

    //Timer tasks?

    enum HealthCheckFileName {
        STARTED_FILE("started"),
        READY_FILE("ready"),
        LIVE_FILE("live");

        private final String fileName;

        HealthCheckFileName(String fileName) {
            this.fileName = fileName;
        }

        String getFileName() {
            return fileName;
        }
    }

    public static class FileHealthCheckBuilder {

        private Status overallStatus = Status.UP;

        private final File file;

        /*
         * Never used
         */
        private FileHealthCheckBuilder() {
            file = null;
        }

        public FileHealthCheckBuilder(File file) {
            this.file = file;
        }

        /**
         * Ultimately checks if there is a DOWN and sets overallStatus as down.
         *
         * @param hcResponseSet Set of queried health checks.
         */
        public void addResponses(Set<HealthCheckResponse> hcResponseSet) {
            for (HealthCheckResponse hcr : hcResponseSet) {
                if (hcr.getStatus().equals(Status.DOWN)) {
                    overallStatus = Status.DOWN;
                    return;
                }
            }
        }

        /*
         * No information, means down.
         */
        public void handleUndeterminedResponse() {
            overallStatus = Status.DOWN;
        }

        public void setOverallStatus(Status status) {
            overallStatus = status;
        }

        public void updateFile() {

            if (overallStatus.equals(Status.DOWN)) {
                return;
            }

            if (!file.exists()) {
                //Any failures during runtime? Count failures and at some point. stop?
                if (!FileUtils.createFile(file)) {
                    return;
                }
            }

            FileUtils.setLastModified(file);

        }

        public Status getOverallStatus() {
            return overallStatus;
        }

    }

}
