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

/**
 *
 */
public class HealthFileUtils {

    private static volatile File healthDirFile;
    private static volatile File startedFile;
    private static volatile File readyFile;
    private static volatile File liveFile;

    public synchronized static File getHealthDirFile() {

        if (healthDirFile == null) {
            File serverConfigDirFile = new File(System.getProperty("server.config.dir"));
            healthDirFile = new File(serverConfigDirFile, "health");
        }

        return healthDirFile;
    }

    public synchronized static File getStartFile() {

        if (startedFile == null) {
            File healthDirFile = getHealthDirFile();
            startedFile = new File(healthDirFile, HealthCheckFileName.STARTED_FILE.getFileName());

        }
        return startedFile;
    }

    public synchronized static File getReadyFile() {

        if (readyFile == null) {
            File healthDirFile = getHealthDirFile();
            readyFile = new File(healthDirFile, HealthCheckFileName.READY_FILE.getFileName());

        }
        return readyFile;
    }

    public synchronized static File getLiveFile() {

        if (liveFile == null) {
            File healthDirFile = getHealthDirFile();
            liveFile = new File(healthDirFile, HealthCheckFileName.LIVE_FILE.getFileName());

        }
        return liveFile;
    }

    /*
     * Determine if this is a valid system.
     * Utility class will not keep state, up to the caller to remember.
     */
    public static boolean initHealthFileValidation() throws IOException {
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

        healthDirFile = getHealthDirFile();

        //Health Dir does not exist -> create and test write
        if (!healthDirFile.exists()) {
            if (!HealthFileUtils.createDirectory(healthDirFile)) {
                return false;
            }

            //Testing write.
            if (!HealthFileUtils.canWriteToDirectory(healthDirFile)) {
                return false;
            }

        } else { // /health dir exists
            healthDirFile.deleteOnExit();
            File[] fileArr = healthDirFile.listFiles();

            //delete
            for (File f : fileArr) {
                if (f.getName().equals(HealthCheckFileName.STARTED_FILE.getFileName()) ||
                    f.getName().equals(HealthCheckFileName.LIVE_FILE.getFileName()) ||
                    f.getName().equals(HealthCheckFileName.READY_FILE.getFileName())) {
                    //failure on delete, return

                    System.out.println("Detected Existing File: " + f.getAbsolutePath());

                    if (f.isDirectory()) {
                        //TODO: Issue warning
                        System.out.println("Warning: existing directory. Will not delete");
                        return false;
                    } else {
                        if (!HealthFileUtils.deleteFiles(f)) {
                            System.out.println("Could not delete existing file");
                            return false;
                        }
                    }
                }
            } //end for

            //Testing write.
            if (!HealthFileUtils.canWriteToDirectory(healthDirFile)) {
                return false;
            }
        }

        return true;
    }

    static boolean canWriteToDirectory(File parentDir) {
        Object token = ThreadIdentityManager.runAsServer();
        try {

            File tempFile = File.createTempFile("test", null);
            if (!tempFile.canWrite()) {
                return false;
            }

            if (!tempFile.delete()) {
                //TODO: debug warning, couldn't delete testFile
            }

            return true;
        } catch (SecurityException | IOException exception) {
            //TODO: Warning about creation? or let caller handle that
            return false;
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    static boolean createFile(File file) {
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

    static boolean createDirectory(File file) {
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

    static boolean deleteFiles(File file) {
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

    static boolean setLastModified(File file) {
        Object token = ThreadIdentityManager.runAsServer();
        try {

            boolean ret = file.setLastModified(System.currentTimeMillis());
            if (!ret) {
                //TODO: failed to touch. Warning.
            }
            return ret;
        } catch (Exception e) {
            //TODO: failed, warning.
            return false;
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

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
