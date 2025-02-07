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

import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;

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
            if (!createDirectory(healthDir)) {
                isValidSystem = false;
                return;
            }

            //Testing write.
            if (!canWrite(healthDir)) {
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
                        if (!deleteFiles(f)) {
                            isValidSystem = false;
                            return;
                        }
                    }
                }
            } //end for

            //Testing write.
            if (!canWrite(healthDir)) {
                isValidSystem = false;
                return;
            }
        }
    }

    //Called by AppTracker (After a appliation is started)
    public void startProcesses() {

        File startFile = new File(healthDir, "started");

        //Initial Check
        HealthCheck40ServiceImpl inst;
        //refactor make these classes extend parent class
        if ((inst = HealthCheck40ServiceImpl.getInstance()) != null) {
            inst.performFileHealthCheck(startFile, HealthCheckConstants.HEALTH_CHECK_START);
            inst.performFileHealthCheck(startFile, HealthCheckConstants.HEALTH_CHECK_READY);
            inst.performFileHealthCheck(startFile, HealthCheckConstants.HEALTH_CHECK_LIVE);
        }

        //Timer tasks.
    }

    ///////////////////////////////////
    //Utility
    ///////////////////////////////////

    private boolean xkccheckWrite(File parentDir) {
        try {
            File tempHealthFile = File.createTempFile("health", null, parentDir);
            tempHealthFile.delete();
            return true;
        } catch (IOException | SecurityException e) {
            //failed -> return;
            //TODO warning
            return false;
        }
    }

    private boolean canWrite(File parentDir) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            File isCanWrite = new File(parentDir, "file");
            return isCanWrite.canWrite();
        } catch (SecurityException ioe) {
            //TODO: Warning about creation.
            return false;
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    public boolean createFile(File file) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            file.deleteOnExit();
            return file.createNewFile();
        } catch (IOException ioe) {
            //TODO: Warning about creation.
            return false;
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    public boolean createDirectory(File file) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            file.deleteOnExit();
            return file.mkdir();
        } catch (SecurityException se) {
            //TODO: Warning about creation.
            return false;
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    public boolean deleteFiles(File file) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return file.delete();
        } catch (SecurityException e) {
            //TODO: issue waring
            return false;
        } finally {
            ThreadIdentityManager.reset(token);
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

}
