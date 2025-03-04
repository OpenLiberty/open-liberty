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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;

/**
 *
 */
public class HealthFileUtils {

    private static final TraceComponent tc = Tr.register(HealthFileUtils.class);

    private static volatile File healthDirFile;
    private static volatile File startedFile;
    private static volatile File readyFile;
    private static volatile File liveFile;

    public static File getHealthDirFile() {

        if (healthDirFile == null) {
            File serverConfigDirFile = new File(System.getProperty("server.output.dir"));
            healthDirFile = new File(serverConfigDirFile, "health");
        }

        return healthDirFile;
    }

    public static File getStartFile() {

        if (startedFile == null) {
            File healthDirFile = getHealthDirFile();
            startedFile = new File(healthDirFile, HealthCheckFileName.STARTED_FILE.getFileName());

        }
        return startedFile;
    }

    public static File getReadyFile() {

        if (readyFile == null) {
            File healthDirFile = getHealthDirFile();
            readyFile = new File(healthDirFile, HealthCheckFileName.READY_FILE.getFileName());

        }
        return readyFile;
    }

    public static File getLiveFile() {

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
    /**
     * This is called upon activation of HealthCheckService (i.e. HealthCheck40ServiceImpl and up).
     * This determines if a system is valid for File Health check by checking write permissions and "sanitizing"
     * the existing location of any previous files caused by a server crash.
     *
     *
     * - Check if the /health directory exists (at server.config.dir)
     * -NO
     * -- Check if we can create the /health directory
     * --- YES
     * ---- Check if we can write to directory
     * -----YES == all good
     * -----NO == issue warning that we cannot write to /health directory
     * --- NO Issue warning that we cannot create /health directory
     * -YES
     * --Check if existing 'started', 'live', 'ready' files exist.
     * --YES
     * ---Attempt to delete
     * ----If the file is a Directory => issue warning about directory with matching name. Can not delete as this may contain user data (created by user).
     * ---Issue warning if we can not delete.
     *
     * @return boolean if this is a valid system for file health check functionality
     * @throws IOException
     */
    public static boolean isValidSystem() throws IOException {
        healthDirFile = getHealthDirFile();

        //Health Dir does not exist -> create and test write
        if (!healthDirFile.exists()) {
            if (!HealthFileUtils.createDirectory(healthDirFile)) {
                //TODO: Create warning message with ID
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to create the /health directory. The mpHealth file-based health check reporting will be disabled.");
                }

                return false;
            }

            //Testing write.
            if (!HealthFileUtils.canWriteToDirectory(healthDirFile)) {
                //TODO: Create warning message with ID
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to write to the /health directory. The mpHealth file-based health check reporting will be disabled.");
                }
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

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Detected existing file [" + f.getAbsolutePath() + "]");
                    }

                    if (f.isDirectory()) {
                        //TODO: Create warning message with ID
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc,
                                     "The MP Health runtime needs to create files with the name \"started\", \"live\" and \"ready\" in the /health directory for file-based health check reporting.\r\n"
                                         + "However, the runtime has detected that a directory with a matching name is present in the /health directory which prevents the necessary files to be crated.The mpHealth file-based health check reporting will be disabled.");

                        }
                        return false;
                    } else {
                        if (!HealthFileUtils.deleteFiles(f)) {
                            //TODO: Create warning message with ID
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Unable to delete existing file " + f.getAbsolutePath() + ". The mpHealth file-based health check reporting will be disabled.");
                            }
                            return false;
                        }
                    }
                }
            } //end for

            //Testing write.
            if (!HealthFileUtils.canWriteToDirectory(healthDirFile)) {
                //TODO: Create warning message with ID
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to write to the /health directory. The mpHealth file-based health check reporting will be disabled.");
                }

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
                //Not important enough to issue warning, and not important enough to stop functionality.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to delete temp file that was crated. Temp file: " + tempFile.getAbsolutePath());
                }
            }

            return true;
        } catch (SecurityException | IOException exception) {
            //Let FFDC happen.
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
            //Let FFDC happen
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
            //Let FFDC happen.
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
            //Let FFDC happen.
            return false;
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    static boolean setLastModified(File file) {
        Object token = ThreadIdentityManager.runAsServer();
        try {

            boolean ret = file.setLastModified(System.currentTimeMillis());
            return ret;
        } catch (Exception e) {
            //Let FFDC happen.
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
