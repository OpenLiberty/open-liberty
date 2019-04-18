/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.config.ConfigUpdateException;
import com.ibm.websphere.config.ConfigValidationException;
import com.ibm.websphere.config.WSConfigurationHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.admin.SystemConfigSupport;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

/**
 * Represents the configuration of the entire system at runtime, comprising variables, all XML configuration, and all default configuration
 */
class SystemConfiguration {
    static final TraceComponent tc = Tr.register(SystemConfiguration.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    private final ServerXMLConfiguration serverXMLConfig;
    private final ConfigVariableRegistry variableRegistry;
    private final DefaultConfiguration defaultConfiguration;

    private final BundleProcessor bundleProcessor;

    private final ConfigUpdater configUpdater;

    private final ChangeHandler changeHandler;
    private final ConfigValidator validator;
    private final ExtendedMetatypeManager extendedMetatypeManager;
    private final ConfigRetriever configRetriever;
    private final ConfigRefresher configRefresher;

    /** Tracker for standard runtime handling of the Location admin service */
    private ServiceTracker<WsLocationAdmin, WsLocationAdmin> locationTracker = null;

    /** Tracker for variable registry service */
    private ServiceTracker<VariableRegistry, VariableRegistry> variableRegistryTracker = null;

    /** Tracker for metatype registry service */
    private ServiceTracker<MetaTypeRegistry, MetaTypeRegistry> metatypeRegistryTracker = null;

    SystemConfiguration(BundleContext bc,
                        SystemConfigSupport caSupport,
                        ConfigurationAdmin configAdmin,
                        OnError onError) {

        if (onError != OnError.WARN) {
            // If the value of onError is the default (WARN) instantiate lazily
            ErrorHandler.INSTANCE.setOnError(onError);
        }

        locationTracker = new ServiceTracker<WsLocationAdmin, WsLocationAdmin>(bc, WsLocationAdmin.class.getName(), null);
        locationTracker.open();

        variableRegistryTracker = new ServiceTracker<VariableRegistry, VariableRegistry>(bc, VariableRegistry.class.getName(), null);
        variableRegistryTracker.open();

        metatypeRegistryTracker = new ServiceTracker<MetaTypeRegistry, MetaTypeRegistry>(bc, MetaTypeRegistry.class.getName(), null);
        metatypeRegistryTracker.open();

        WsLocationAdmin locationService = locationTracker.getService();
        VariableRegistry variableRegistryService = variableRegistryTracker.getService();

        ServiceReference<LibertyProcess> procRef = bc.getServiceReference(LibertyProcess.class);
        LibertyProcess libertyProcess = bc.getService(procRef);
        ConfigVariableRegistry variableRegistry = new ConfigVariableRegistry(variableRegistryService, libertyProcess.getArgs(), bc.getDataFile("variableCache"));

        MetaTypeRegistry metatypeRegistry = metatypeRegistryTracker.getService();

        this.extendedMetatypeManager = new ExtendedMetatypeManager(metatypeRegistry, configAdmin);

        this.configRetriever = new ConfigRetriever(caSupport, configAdmin, variableRegistry);
        this.validator = new ConfigValidator(metatypeRegistry);

        XMLConfigParser parser = new XMLConfigParser(locationService);
        this.serverXMLConfig = new ServerXMLConfiguration(bc, locationService, parser);

        ConfigEvaluator ce = new ConfigEvaluator(configRetriever, metatypeRegistry, variableRegistry, this.serverXMLConfig);

        this.configUpdater = new ConfigUpdater(ce, caSupport, variableRegistry, metatypeRegistry, extendedMetatypeManager);

        this.changeHandler = new ChangeHandler(caSupport, variableRegistry, extendedMetatypeManager, configRetriever, validator, configUpdater, metatypeRegistry);

        this.variableRegistry = variableRegistry;
        this.defaultConfiguration = new DefaultConfiguration(parser);

        this.validator.setConfiguration(serverXMLConfig);

        bundleProcessor = new BundleProcessor(bc, this, locationService, configUpdater, changeHandler, validator, configRetriever);

        this.configRefresher = new ConfigRefresher(bc, changeHandler, serverXMLConfig);

        extendedMetatypeManager.init();

        // Create and register WSConfigurationHelper
        WSConfigurationHelper wsConfigHelper = new WSConfigurationHelperImpl(metatypeRegistry, ce, bundleProcessor);
        registerService(bc, WSConfigurationHelper.class.getName(), wsConfigHelper);

    }

    // Register a service with default properties
    private void registerService(BundleContext bc, String name, Object serviceInstance) {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("service.vendor", "IBM");
        bc.registerService(name, serviceInstance, properties);
    }

    void start() throws ConfigUpdateException, ConfigValidationException, ConfigParserTolerableException {
        if (serverXMLConfig.hasConfigRoot()) {
            configRefresher.start();
            serverXMLConfig.loadInitialConfiguration(variableRegistry);
        }

        final boolean reprocessConfig;
        if (serverXMLConfig.isModified() || variableRegistry.variablesChanged()) {
            variableRegistry.clearVariableCache();
            changeHandler.updateAtStartup(serverXMLConfig.getConfiguration());
            serverXMLConfig.setConfigReadTime();
            reprocessConfig = true;
        } else {
            reprocessConfig = false;
        }
        bundleProcessor.startProcessor(reprocessConfig);
    }

    void stop() {
        bundleProcessor.stopProcessor();
        configRefresher.stop();

        // close trackers
        if (null != locationTracker) {
            locationTracker.close();
            locationTracker = null;
        }
        if (null != variableRegistryTracker) {
            variableRegistryTracker.close();
            variableRegistryTracker = null;
        }
        if (null != metatypeRegistryTracker) {
            metatypeRegistryTracker.close();
            metatypeRegistryTracker = null;
        }
    }

    ServerConfiguration getServerConfiguration() {
        return this.serverXMLConfig.getConfiguration();
    }

    ServerConfiguration copyServerConfiguration() {
        return this.serverXMLConfig.copyConfiguration();
    }

    Collection<String> fetchConfigurationFilePaths() {
        return this.serverXMLConfig.getFilesToMonitor();
    }

    BaseConfiguration loadDefaultConfiguration(Bundle bundle) throws ConfigUpdateException, ConfigValidationException {
        return defaultConfiguration.load(bundle, serverXMLConfig, variableRegistry);
    }

    /**
     * Add configuration to the default configuration add runtime
     *
     * @param pid
     * @param props
     * @return
     */
    BaseConfiguration addDefaultConfiguration(String pid, Dictionary<String, String> props) throws ConfigUpdateException {
        return defaultConfiguration.add(pid, props, serverXMLConfig, variableRegistry);
    }

    /**
     * Add configuration to the default configuration at runtime using a url
     *
     * @param pid
     * @param props
     * @return
     * @throws ConfigUpdateException
     * @throws ConfigValidationException
     */
    BaseConfiguration addDefaultConfiguration(InputStream defaultConfig) throws ConfigValidationException, ConfigUpdateException {
        return defaultConfiguration.add(defaultConfig, serverXMLConfig, variableRegistry);
    }

    void bundleRemoved(Bundle bundle) {
        BaseConfiguration config = serverXMLConfig.getDefaultConfiguration();
        config.remove(defaultConfiguration.remove(bundle));
    }

    /**
     * @param pid
     * @throws ConfigUpdateException
     */
    boolean removeDefaultConfiguration(String pid, String id) throws ConfigUpdateException {

        // Create a copy of the old config
        ServerConfiguration oldConfig = serverXMLConfig.copyConfiguration();

        // Remove the default configuration
        BaseConfiguration cfg = serverXMLConfig.getDefaultConfiguration();
        boolean removed = cfg.remove(pid, id);
        BaseConfiguration runtimeCfg = defaultConfiguration.getRuntimeDefaultConfiguration(pid);
        if (runtimeCfg != null)
            runtimeCfg.remove(pid, id);

        if (removed) {
            variableRegistry.setDefaultVariables(cfg.getVariables());
            removeDefaultConfiguration(oldConfig);
        }

        return removed;
    }

    void removeDefaultConfiguration(ServerConfiguration oldConfig) throws ConfigUpdateException {
        changeHandler.removeDefaultConfiguration(oldConfig, serverXMLConfig);
    }

}
