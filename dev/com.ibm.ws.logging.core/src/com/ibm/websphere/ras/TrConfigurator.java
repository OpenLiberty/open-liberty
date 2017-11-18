/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ras;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.ws.kernel.boot.logging.WsLogManager;
import com.ibm.ws.kernel.provisioning.packages.PackageIndex;
import com.ibm.ws.logging.WsTraceRouter;
import com.ibm.ws.logging.internal.DisabledTraceService;
import com.ibm.ws.logging.internal.SafeTraceLevelIndexFactory;
import com.ibm.ws.logging.internal.TraceSpecification;
import com.ibm.ws.logging.internal.TraceSpecification.TraceSpecificationException;
import com.ibm.ws.staticvalue.StaticValue;
import com.ibm.wsspi.logging.MessageRouter;
import com.ibm.wsspi.logging.TextFileOutputStreamFactory;
import com.ibm.wsspi.logprovider.LogProviderConfig;
import com.ibm.wsspi.logprovider.TrService;

/**
 * Configurator: Uses a LogProvider configuration to initialize the TrService service.
 * The log provider is required to provide a non-null delegate. This delegate
 * can not be reset.
 */
public class TrConfigurator {
    static class DisabledDelegateSingleton {
        static DisabledTraceService instance = new DisabledTraceService();
    }

    /** Mark the initialization of the Tr */
    static final StaticValue<AtomicReference<LogProviderConfig>> loggingConfig = StaticValue.createStaticValue(new Callable<AtomicReference<LogProviderConfig>>() {
        @Override
        public AtomicReference<LogProviderConfig> call() throws Exception {
            return new AtomicReference<LogProviderConfig>(null);
        }
    });

    /** Active configuration */
    static StaticValue<TrService> delegate = StaticValue.createStaticValue(null);

    /** List of registered trace component change listeners */
    final static StaticValue<Set<TraceComponentChangeListener>> registeredListeners = StaticValue.createStaticValue(new Callable<Set<TraceComponentChangeListener>>() {
        @Override
        public Set<TraceComponentChangeListener> call() throws Exception {
            return new CopyOnWriteArraySet<TraceComponentChangeListener>();
        }
    });

    private static boolean instrumentationAvailable;

    /** Only used in setTraceSpec to skip processing identical specification strings */
    static String traceString = "";

    /** Index of sensitive packages to suppress */
    private static PackageIndex<Integer> safeLevelsIndex = null;

    /** Location of the liberty trace list to obtain the packages from */
    private static String sensitiveTraceListResourceName;

    /** Prevent potentially sensitive information from being exposed in log and trace files. */
    private static boolean suppressSensitiveTrace = false;

    /**
     * Have we been through the first update? Need to allow the first update containing defaults
     * to go through without issuing any trace spec warnings.
     */
    private static boolean defaultUpdated = false;

    /**
     * Initialize Tr (and underlying Tr service).
     */
    public static synchronized void init(LogProviderConfig config) {
        if (config == null)
            throw new NullPointerException("LogProviderConfig must not be null");

        if (loggingConfig.get().compareAndSet(null, config)) {
            // Only initialize Tr once -- all subsequent changes go through update
            // The synchronization of this method is gratuitous (just makes us feel better), 
            // it is called while the system is single threaded at startup. 

            // config.getTrDelegate() must not return null -- it should either
            // return a dummy/disabled delegate, or throw an exception so that startup
            // does not proceed.
            final TrService tr = config.getTrDelegate();
            if (tr == null)
                throw new NullPointerException("LogProviderConfig must provide a TrService delegate");
            Callable<TrService> result = new Callable<TrService>() {
                @Override
                public TrService call() throws Exception {
                    return tr;
                }
            };
            delegate = StaticValue.mutateStaticValue(delegate, result);
            delegate.get().init(config);

            // Validate and propagate the initial trace specification 
            setTraceSpec(config.getTraceString());

            // Set class that we want the LogManager to instantiate when someone
            // calls getLogger: This logger interacts only with the Tr/FFDC API: 
            // it is not dependent on the implementation of the underlying delegate
            WsLogManager.setWsLogger(com.ibm.ws.logging.internal.WsLogger.class);
        }
    }

    /**
     * Update Tr with new configuration values (based on injection via config
     * admin). The parameter map should be modified to match actual values used
     * (e.g. substitution in case of error).
     * 
     * @param newConfig
     */
    public static synchronized void update(Map<String, Object> newConfig) {
        if (newConfig == null)
            throw new NullPointerException("Updated config must not be null");

        boolean traceWasDisabled = !TraceComponent.isAnyTracingEnabled();

        // Update the logging configuration
        LogProviderConfig config = loggingConfig.get().get();
        if (config != null) {
            config.update(newConfig);

            Object o = newConfig.get("suppressSensitiveTrace");

            if (o != null) {
                if (o instanceof String) {
                    suppressSensitiveTrace = Boolean.parseBoolean((String) o);
                } else if (o instanceof Boolean) {
                    suppressSensitiveTrace = (Boolean) o;
                }
                if (suppressSensitiveTrace == true && safeLevelsIndex == null) {
                    setupSafeLevelsIndex();
                }
            }

            TraceSpecification newTs = setTraceSpec(config.getTraceString());

            // For updates only, warn if there are strings that we don't know
            if (defaultUpdated && newTs != null)
                newTs.warnUnmatchedSpecs();

            // we've updated the defaults from server.xml (we can set this a million times.. 
            // first pass through is the key.. 
            defaultUpdated = true;

            // Propagate updates to the delegate
            getDelegate().update(config);
        }

        // issue warning if trace has been enabled and there's no instrumentation agent
        if (!instrumentationAvailable && traceWasDisabled && TraceComponent.isAnyTracingEnabled()) {
            Tr.warning(TraceSpecification.getTc(), "INSTRUMENTATION_SERVICE_UNAVAILABLE");
        }
    }

    /**
     * Enabling point for setting the sensitiveTraceListResourceName
     * for loading the sensitive list from. This is useful for overriding
     * the default location for testing.
     * 
     * @param sensitiveTraceListResourceName
     */
    protected static void setSensitiveTraceListResourceName(String resourceName) {
        sensitiveTraceListResourceName = resourceName;
    }

    /*
     * Set up the PackageIndex of sensitive packages that will be used to
     * filter the levels in TraceComponent. Create index only once!!!
     */
    private static synchronized void setupSafeLevelsIndex() {
        if (safeLevelsIndex != null) {
            return;
        }
        if (sensitiveTraceListResourceName == null) {
            sensitiveTraceListResourceName = "META-INF/logging/liberty.ras.rawtracelist.properties";
        }
        safeLevelsIndex = SafeTraceLevelIndexFactory.createPackageIndex(sensitiveTraceListResourceName);
    }

    /**
     * Package protected: retrieve the delegate (possibly creating first..).
     * <ul>
     * <li>If Tr has not yet been configured, the disabled delegate will be
     * returned
     * <li>If a delegate has been set, that instance will be returned.
     * <li>Otherwise, the default delegate will be created, if necessary, and
     * returned
     * </ul>
     * 
     * @return active delegate
     */
    static TrService getDelegate() {
        TrService result = delegate.get();
        if (result != null) {
            return result;
        }

        LogProviderConfig config = loggingConfig.get().get();
        if (config != null) {
            final TrService tr = config.getTrDelegate();
            if (tr != null) {
                Callable<TrService> initializer = new Callable<TrService>() {
                    @Override
                    public TrService call() throws Exception {
                        return tr;
                    }
                };
                delegate = StaticValue.mutateStaticValue(delegate, initializer);
                delegate.get().init(config);
                return delegate.get();
            }
        }

        return DisabledDelegateSingleton.instance;
    }

    /**
     * Call {@link TraceComponentChangeListener}s indicating the specified trace
     * component was registered.
     * 
     * @param tc
     *            the {@link TraceComponent} that was registered
     */
    static void traceComponentRegistered(TraceComponent tc) {
        for (TraceComponentChangeListener listener : registeredListeners.get()) {
            listener.traceComponentRegistered(tc);
        }
    }

    /**
     * Call {@link TraceComponentChangeListener}s indicating the specified trace
     * component was updated.
     * 
     * @param tc
     *            the {@link TraceComponent} that was updated
     */
    static void traceComponentUpdated(TraceComponent tc) {
        for (TraceComponentChangeListener listener : registeredListeners.get()) {
            listener.traceComponentUpdated(tc);
        }
    }

    public static void addTraceComponentListener(TraceComponentChangeListener tcl) {
        registeredListeners.get().add(tcl);
    }

    public static void removeTraceComponentListener(TraceComponentChangeListener tcl) {
        registeredListeners.get().remove(tcl);
    }

    /**
     * Set the trace specification of the service to the input value.
     * 
     * @param spec New string trace specification
     * @return new TraceSpecification, or null if unchanged
     */
    synchronized static TraceSpecification setTraceSpec(String spec) {
        // If logger is used & configured by logger properties, 
        // we're done as far as trace string processing is concerned
        if (WsLogManager.isConfiguredByLoggingProperties()) {
            return null;
        }

        // If the specified string is null, or it is equal to a string,
        // or the sensitive flag has not been toggled,
        // we've already parsed, skip it.
        if ((spec == null || spec.equals(traceString)) && Tr.activeTraceSpec.isSensitiveTraceSuppressed() == suppressSensitiveTrace) {
            return null;
        }

        traceString = spec;

        // Parse the trace specification string, this will gather
        // exceptions that occur for different elements of the string
        TraceSpecification newTs = new TraceSpecification(spec, safeLevelsIndex, suppressSensitiveTrace);
        TraceSpecificationException tex = newTs.getExceptions();

        if (tex != null) {
            do {
                tex.warning(loggingConfig.get() != null);
                tex = tex.getPreviousException();
            } while (tex != null);
        }

        Tr.setTraceSpec(newTs);

        // Return the new/updated TraceSpecification to the caller. The caller can
        // then determine whether or not all elements of the TraceSpecification 
        // were known to the system or not.
        return newTs;
    }

    static TraceSpecification getTraceSpec() {
        return Tr.activeTraceSpec;
    }

    /**
     * @return the trace specification the server is using.
     */
    public static String getEffectiveTraceSpec() {
        return Tr.activeTraceSpec.toDisplayString();
    }

    /**
     * @return
     */
    public static String getLogLocation() {
        LogProviderConfig cfg = loggingConfig.get().get();
        if (cfg == null)
            throw new IllegalStateException("Tr not initialized");

        return cfg.getLogDirectory().getAbsolutePath();
    }

    /**
     * Stop the Tr service (the disabled delegate will be used until
     * reconfigured).
     */
    public synchronized static void stop() {
        TrService service = getDelegate();
        if (service != null) {
            // Stop the delegate
            service.stop();
        }
    }

    /**
     * @param instrumentation
     */
    public static void setInstrumentation(boolean instrumentation) {
        instrumentationAvailable = instrumentation;
    }

    /**
     * Set the MessageRouter into the TrService delegate.
     */
    public static void setMessageRouter(MessageRouter msgRouter) {
        getDelegate().setMessageRouter(msgRouter);
    }

    /**
     * UnSet the MessageRouter from the TrService delegate.
     */
    public static void unsetMessageRouter(MessageRouter msgRouter) {
        getDelegate().unsetMessageRouter(msgRouter);
    }

    /**
     * Set the TraceRouter into the TrService delegate.
     */
    public static void setTraceRouter(WsTraceRouter msgRouter) {
        getDelegate().setTraceRouter(msgRouter);
    }

    /**
     * UnSet the TraceRouter from the TrService delegate.
     */
    public static void unsetTraceRouter(WsTraceRouter msgRouter) {
        getDelegate().unsetTraceRouter(msgRouter);
    }

    /**
     * This method (to register pre-created TraceComponents) is needed:
     * but it should not be on the main/base Tr API, as it is not something
     * that should be called frequently.
     * 
     * @param tc
     */
    public static void registerTraceComponent(TraceComponent tc) {
        Tr.registerTraceComponent(tc);
    }

    /**
     * @return
     */
    public static TextFileOutputStreamFactory getFileOutputStreamFactory() {
        LogProviderConfig cfg = loggingConfig.get().get();
        if (cfg == null)
            throw new IllegalStateException("Tr not initialized");

        return cfg.getTextFileOutputStreamFactory();
    }
}
