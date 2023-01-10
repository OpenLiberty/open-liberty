/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

/**
 *
 */
public class BootstrapLocations {

    private String processName;
    private String userDir;
    private String serverDir;
    private String logDir;
    private String consoleLogFile;
    private String workAreaDirectory;
    private String variableSourceDirs;

    /**
     * @return the processName
     */
    public String getProcessName() {
        return processName;
    }

    /**
     * @param processName the processName to set
     */
    public void setProcessName(String processName) {
        this.processName = processName;
    }

    /**
     * @return the userDir
     */
    public String getUserDir() {
        return userDir;
    }

    /**
     * @param userDir the userDir to set
     */
    public void setUserDir(String userDir) {
        this.userDir = userDir;
    }

    /**
     * @return the serverDir
     */
    public String getServerDir() {
        return serverDir;
    }

    /**
     * @param serverDir the serverDir to set
     */
    public void setServerDir(String serverDir) {
        this.serverDir = serverDir;
    }

    /**
     * @return the logDir
     */
    public String getLogDir() {
        return logDir;
    }

    /**
     * @param logDir the logDir to set
     */
    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    /**
     * @return the consoleLogFile
     */
    public String getConsoleLogFile() {
        return consoleLogFile;
    }

    /**
     * @param consoleLogFile the consoleLogFile to set
     */
    public void setConsoleLogFile(String consoleLogFile) {
        this.consoleLogFile = consoleLogFile;
    }

    /**
     * @return
     */
    public String getWorkAreaDir() {
        return this.workAreaDirectory;
    }

    public void setWorkAreaDir(String directory) {
        this.workAreaDirectory = directory;
    }

    public String getVariableSourceDirs() {
        return this.variableSourceDirs;
    }

    public void setVariableSourceDirs(String path) {
        this.variableSourceDirs = path;
    }
}