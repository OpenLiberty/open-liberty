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
package com.ibm.websphere.ras;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import test.TestConstants;

import com.ibm.ws.logging.internal.impl.LogProviderConfigImpl;
import com.ibm.ws.logging.internal.impl.LoggingConstants;
import com.ibm.wsspi.logging.TextFileOutputStreamFactory;
import com.ibm.wsspi.logprovider.LogProviderConfig;
import com.ibm.wsspi.logprovider.TrService;

public class SharedTr extends Tr {
    public static final TextFileOutputStreamFactory fileStreamFactory = new TextFileOutputStreamFactory() {
        @Override
        public FileOutputStream createOutputStream(File file) throws IOException {
            return new FileOutputStream(file);
        }

        @Override
        public FileOutputStream createOutputStream(File file, boolean append) throws IOException {
            return new FileOutputStream(file, append);
        }

        @Override
        public FileOutputStream createOutputStream(String name) throws IOException {
            return new FileOutputStream(name);
        }

        @Override
        public FileOutputStream createOutputStream(String name, boolean append) throws IOException {
            return new FileOutputStream(name, append);
        }
    };

    public static void clearComponents() {
        allTraceComponents.clear();

        try {
            Field f = TraceComponent.class.getDeclaredField("numActiveTraceComponents");
            f.setAccessible(true);
            f.set(null, 0);
        } catch (Exception e) {
        }
    }

    public static void clearConfig() {
        TrConfigurator.loggingConfig.set(null);
        TrConfigurator.delegate = null;
    }

    public static TrService getDelegate() {
        return TrConfigurator.getDelegate();
    }

    public static LogProviderConfig getDefaultConfig() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("com.ibm.ws.logging.ffdc.summary.policy", LoggingConstants.FFDCSummaryPolicy.IMMEDIATE.toString());
        LogProviderConfigImpl config = new LogProviderConfigImpl(map,
                        TestConstants.BUILD_TMP,
                        fileStreamFactory);
        return config;
    }
}