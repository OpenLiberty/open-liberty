/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.ibm.ws.logging.internal.impl.LogProviderConfigImpl;
import com.ibm.wsspi.logging.TextFileOutputStreamFactory;
import com.ibm.wsspi.logprovider.TrService;

/**
 *
 */
public class TrConfigZapper extends TrConfigurator {
    private static final String WS_LOG_MANAGER = "com.ibm.ws.kernel.boot.logging.WsLogManager";

    private static final String RAS_TRACE_SPEC = "com.ibm.ws.logging.trace.specification";
    private static final String PROP_TRACE_DELEGATE = "com.ibm.ws.logging.trace.delegate";

    public static String getSystemTraceSpec() {
        return System.getProperty(RAS_TRACE_SPEC);
    }

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

    /**
     * @param rasTraceSpec
     * @param logDirectory
     */
    public static CapturedOutputHolder zapTrConfig(String rasTraceSpec, File logDirectory) {
        System.setProperty("java.util.logging.manager", WS_LOG_MANAGER);
        System.setProperty("java.util.logging.configureByServer", "true");

        Map<String, String> rasSettings = new HashMap<String, String>();
        Properties prop = System.getProperties();
        for (Entry<Object, Object> entry : prop.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith("com.ibm.ws.logging")) {
                rasSettings.put(key, (String) entry.getValue());
            }
        }
        rasSettings.put("com.ibm.ws.logging.max.files", "10");
        rasSettings.put(RAS_TRACE_SPEC, rasTraceSpec);

        if (rasSettings.get(PROP_TRACE_DELEGATE) == null) {
            rasSettings.put(PROP_TRACE_DELEGATE, CapturedOutputHolder.class.getName());
        }

        LogProviderConfigImpl config = new LogProviderConfigImpl(rasSettings, logDirectory, fileStreamFactory);
        init(config);
        TrService delegate = config.getTrDelegate();
        if (delegate instanceof CapturedOutputHolder)
            return (CapturedOutputHolder) delegate;
        else
            return null;
    }

    /**
     * Tweaking / cleanup to allow re-initialization
     */
    public static void revert() {
        stop();
        delegate = null;
        loggingConfig.set(null);
    }

    public TrService getTrDelegate() {
        return delegate;
    }

    public void setTrDelegate(final TrService newDelegate) {
        delegate = newDelegate;
    }

}
