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
package com.ibm.ws.ffdc;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.ws.logging.internal.DisabledFFDCService;
import com.ibm.wsspi.logging.TextFileOutputStreamFactory;
import com.ibm.wsspi.logprovider.FFDCFilterService;
import com.ibm.wsspi.logprovider.LogProviderConfig;

/**
 * Configurator: Uses a LogProvider configuration to initialize the FFDC service.
 * The log provider is required to provide a non-null delegate. This delegate
 * can not be reset.
 */
public class FFDCConfigurator {
    static public final String FFDC_DIR = "ffdc";
    static public final String FFDC_FILE_NAME = "ffdc";
    static public final String FFDC_SUMMARY_FILE_NAME = "exception_summary";
    static public final String FFDC_EXTENSION = ".log";

    static class DisabledDelegateSingleton {
        static DisabledFFDCService instance = new DisabledFFDCService();
    }

    /** Mark the initialization of the Tr */
    static final AtomicReference<LogProviderConfig> loggingConfig = new AtomicReference<LogProviderConfig>(null);

    /** Optional delegate implementation (can override the default/fallback) */
    static FFDCFilterService delegate = null;

    /**
     * Initialize FFDC (and underlying FFDC service).
     */
    public static synchronized void init(LogProviderConfig config) {
        if (config == null)
            throw new NullPointerException("LogProviderConfig must not be null");

        if (loggingConfig.compareAndSet(null, config)) {
            // Only initialize FFDC once -- all subsequent changes go through update
            // The synchronization of this method is gratuitous (just makes us feel better), 
            // it is called while the system is single threaded at startup. 

            // config.getTrDelegate() must not return null -- it should either
            // return a dummy/disabled delegate, or throw an exception so that startup
            // does not proceed.
            delegate = config.getFfdcDelegate();
            if (delegate == null)
                throw new NullPointerException("LogProviderConfig must provide a FFDCFilterService delegate");

            delegate.init(config);
        }
    }

    /**
     * Update FFDC service with new configuration values (based on injection via config
     * admin). The parameter map should be modified to match actual values used
     * (e.g. substitution in case of error).
     * 
     * @param newConfig
     */
    public static synchronized void update(Map<String, Object> newConfig) {
        if (newConfig == null)
            throw new NullPointerException("Updated config must not be null");

        // Update the logging configuration
        LogProviderConfig config = loggingConfig.get();
        if (config != null) {
            config.update(newConfig);
            // Propagate updates to the delegate
            getDelegate().update(config);
        }
    }

    public static FFDCFilterService getDelegate() {
        FFDCFilterService ffdcDelegate = delegate;
        if (ffdcDelegate != null)
            return ffdcDelegate;

        LogProviderConfig config = loggingConfig.get();
        if (config != null) {
            ffdcDelegate = config.getFfdcDelegate();
            if (ffdcDelegate != null) {
                delegate = ffdcDelegate;
                return ffdcDelegate;
            }
        }

        return DisabledDelegateSingleton.instance;
    }

    /**
     * Stop the FFDC service (the disabled delegate will be used until
     * reconfigured).
     */
    public static synchronized void stop() {
        FFDCFilterService service = getDelegate();
        if (service != null) {
            // Stop the delegate
            service.stop();
        }
    }

    public static File getFFDCLocation() {
        LogProviderConfig cfg = loggingConfig.get();
        if (cfg == null)
            throw new IllegalStateException("FFDC not initialized");

        return getDelegate().getFFDCLogLocation();
    }

    /**
     * @return
     */
    public static TextFileOutputStreamFactory getFileOutputStreamFactory() {
        LogProviderConfig cfg = loggingConfig.get();
        if (cfg == null)
            throw new IllegalStateException("FFDC not initialized");

        return cfg.getTextFileOutputStreamFactory();
    }
}
