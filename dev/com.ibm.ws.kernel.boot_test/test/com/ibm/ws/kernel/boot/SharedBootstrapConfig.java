/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.kernel.boot.internal.FileUtils;

import test.common.SharedOutputManager;
import test.shared.TestUtils;

public class SharedBootstrapConfig extends BootstrapConfig {

    public static SharedBootstrapConfig createSharedConfig(SharedOutputManager outputMgr) {
        try {
            return new SharedBootstrapConfig();
        } catch (IOException e) {
            outputMgr.failWithThrowable("createSharedConfig", e);
            // unreachable: make compiler happy
            throw new RuntimeException(e);
        }
    }

    public static SharedBootstrapConfig createSharedConfig(SharedOutputManager outputMgr, String serverName) {
        try {
            return new SharedBootstrapConfig(serverName);
        } catch (IOException e) {
            outputMgr.failWithThrowable("createSharedConfig", e);
            // unreachable: make compiler happy
            throw new RuntimeException(e);
        }
    }

    private SharedBootstrapConfig(String serverName) throws IOException {
        this.processName = serverName;

        File root = TestUtils.createTempDirectory(serverName);
        if (root == null || !root.exists())
            throw new IllegalArgumentException("root directory does not exist");

        final String rootDirStr = root.getAbsolutePath();

        HashMap<String, String> map = new HashMap<String, String>();

        BootstrapLocations locations = new BootstrapLocations();
        locations.setProcessName(serverName);
        locations.setServerDir(rootDirStr);
        this.findLocations(locations);
        this.configure(map);
    }

    private SharedBootstrapConfig() throws IOException {
        this("defaultServer");
    }

    public void setInitProps(Map<String, String> initProps) {
        this.initProps = initProps;
    }

    public void setInstallRoot(File installRoot) {
        this.installRoot = installRoot;
    }

    public void cleanServerDir() {
        FileUtils.recursiveClean(getConfigFile(null));
    }
}