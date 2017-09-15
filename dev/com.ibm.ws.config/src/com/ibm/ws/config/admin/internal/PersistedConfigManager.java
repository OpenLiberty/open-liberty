/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.admin.internal;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 *
 */
class PersistedConfigManager {

    private final File persistDir;

    public PersistedConfigManager(File dataArea) {
        this.persistDir = dataArea;

        if (!persistDir.exists()) {
            ConfigUtil.mkdirs(persistDir);
        }
    }

    /**
     *
     */
    public void close() {

    }

    /**
     * @param pid
     * @return
     */
    public File getConfigFile(String pid) {
        return new File(persistDir, generateFilenameFromPid(pid));
    }

    public void deleteConfigFile(String pid) {
        File configFile = getConfigFile(pid);
        if (configFile != null && configFile.exists()) {
            ConfigUtil.delete(configFile);
        }
    }

    /**
     * @return
     */
    public String[] getCachedPids() {
        String[] files = AccessController.doPrivileged(new PrivilegedAction<String[]>() {
            @Override
            public String[] run() {
                return persistDir.list();
            }
        });
        if (files == null) {
            files = new String[0];
        } else {
            for (int i = 0; i < files.length; ++i) {
                files[i] = files[i].substring(0, files[i].lastIndexOf("!"));
            }
        }

        return files;
    }

    /**
     * Generate a name (a file name) based on given pid.
     * It is used to distinguish difference between a.pid vs. a.Pid in Windows
     * file system
     * since new File("a.pid") and new File("a.Pid") both points to same file in
     * Windows file system.
     * 
     * @param pid
     * @return
     */
    private String generateFilenameFromPid(String pid) {
        return pid + "!" + pid.hashCode();
    }
}
