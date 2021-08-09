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
package com.ibm.ws.kernel.boot.internal.commands;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.BootstrapLocations;
import com.ibm.ws.kernel.boot.internal.FileUtils;

import test.common.SharedOutputManager;

public class SharedBootstrapConfig extends BootstrapConfig {
    private boolean isClient = false;

    public static SharedBootstrapConfig createSharedConfig(SharedOutputManager outputMgr, boolean isClient) {
        SharedBootstrapConfig instance = createSharedConfig(outputMgr);
        instance.setClient(isClient);
        return instance;
    }

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

        File root = createTempDirectory(serverName);
        if (root == null || !root.exists())
            throw new IllegalArgumentException("root directory does not exist");

        final String rootDirStr = root.getAbsolutePath();

        HashMap<String, String> map = new HashMap<String, String>();

        BootstrapLocations bl = new BootstrapLocations();
        bl.setProcessName(serverName);
        bl.setUserDir(rootDirStr);
        this.findLocations(bl);
        this.configure(map);
    }

    private static AtomicInteger count = new AtomicInteger(0);
    static final String TEST_TMP_ROOT = "../com.ibm.ws.appclient.boot/build/tmp/";
    static final File TEST_TMP_ROOT_FILE = new File(TEST_TMP_ROOT);

    private File createTempDirectory(String name) throws IOException {
        File f = new File(TEST_TMP_ROOT_FILE, name + count.incrementAndGet());

        if (!f.exists() && !f.mkdirs()) {
            System.out.println("alex: file creation failed for: " + f.getAbsolutePath());
            throw new IOException("Unable to create temporary directory");
        }
        return f;
    }

    private SharedBootstrapConfig() throws IOException {
        this("defaultServer");
    }

    public void setInitProps(Map<String, String> initProps) {
        this.initProps = initProps;
    }

    public void cleanServerDir() {
        FileUtils.recursiveClean(getConfigFile(null));
    }

    @Override
    public String getProcessType() {
        if (isClient) {
            return "client";
        } else {
            return "server";
        }
    }

    private void setClient(boolean isClient) {
        this.isClient = isClient;
    }
}