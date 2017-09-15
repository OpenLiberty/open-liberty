/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.product;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.simplicity.Machine;

/**
 * This class represents a generic software installation. It provides access to information common
 * to all installations such as an install path and access to the {@link Machine} on which the
 * installation exists.
 */
public abstract class Installation {

    static Map<String, Installation> installs = new HashMap<String, Installation>();

    private Machine machine;
    private String installRoot;
    private InstallationType installType;
    private String bootstrapFileKey;

    /**
     * Constructor
     * 
     * @param machine The machine of this install
     * @param installRoot The installation root
     * @param installType The type of install
     */
    Installation(Machine machine, String installRoot, InstallationType installType) {
        this.machine = machine;
        this.installRoot = installRoot;
        this.installType = installType;
    }

    /**
     * Get the installation root of this Install. The installation root is the file system
     * location on the machine where the install exists.
     * 
     * @return The installation root
     */
    public String getInstallRoot() {
        return this.installRoot;
    }

    /**
     * Set the installation root for this install
     * 
     * @param path The install root path
     */
    void setInstallRoot(String path) {
        this.installRoot = path;
    }

    /**
     * This method returns the type of product that this Installation represents. An example product
     * would be the WebSphere Application Server.
     * 
     * @return The {@link InstallationType} which signifies what type of product this installation
     *         represents
     */
    public InstallationType getInstallType() {
        return this.installType;
    }

    /**
     * Set the {@link InstallationType} for this Installation
     * 
     * @param installType The {@link InstallationType} to set
     */
    void setInstallType(InstallationType installType) {
        this.installType = installType;
    }

    /**
     * This method returns the {@link Machine} representation of the machine that this installation
     * is installed on.
     * 
     * @return The {@link Machine} of this Install.
     */
    public Machine getMachine() {
        return this.machine;
    }

    /**
     * Get the key used to cache this installation in the bootstrapping properties file. If not
     * bootstrapping file is being used, this returns null.
     * 
     * @return The key used to cache this installation
     */
    public String getBootstrapFileKey() {
        return bootstrapFileKey;
    }

    /**
     * Set the key used to cache this installation in the bootstrapping properties file.
     * 
     * @param bootstrapFileKey The key used to cache the installation
     */
    public void setBootstrapFileKey(String bootstrapFileKey) {
        this.bootstrapFileKey = bootstrapFileKey;
    }

    /**
     * This factory method returns an Install based on the provided data. If the install is cached,
     * the cached instance will be returned. Otherwise a new instance is created and cached.
     * 
     * @param machine The {@link Machine} that the installation is on
     * @param installRoot The path of the installation
     * @param installType The type of installation
     * @return An {@link Installation} instance
     */
    public static Installation getInstall(Machine machine, String installRoot, InstallationType installType) {
        String key = machine.getHostname() + installRoot + installType;
        Installation install = installs.get(key);
        if (install == null) {
            if (installType == InstallationType.WAS_INSTALL) {
                install = new WASInstallation(machine, installRoot, installType);
            }
            installs.put(key, install);
        }
        return install;
    }
}
