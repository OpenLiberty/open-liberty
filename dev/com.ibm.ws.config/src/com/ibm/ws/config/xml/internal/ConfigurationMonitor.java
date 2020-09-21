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

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

/**
 *
 */
class ConfigurationMonitor implements ManagedService {
    static final TraceComponent tc = Tr.register(ConfigurationMonitor.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    private final String CONFIG_PID = "com.ibm.ws.config";

    /** Property name for setting how configuration updates should be triggered in the server.xml */
    private final String UPDATE_TRIGGER = "updateTrigger";

    /** Property name for setting how configuration updates should be triggered in the bootstrap.properties */
    private final String UPDATE_TRIGGER_PROP = "com.ibm.ws.config." + UPDATE_TRIGGER;

    /** Property that configures how often the server configuration files are checked for changes */
    private final String MONITOR_INTERVAL = "monitorInterval";

    private final String MONITOR_INTERVAL_PROP = "com.ibm.ws.config." + MONITOR_INTERVAL;

    private final BundleContext bundleContext;

    private final ServerXMLConfiguration serverXMLConfig;
    private final ConfigRefresher configRefresher;

    private ServiceRegistration<ManagedService> managedServiceRegistration;

    private boolean isFirstUpdate = true;

    /** Configuration file monitor */
    private ConfigFileMonitor fileMonitor;

    public ConfigurationMonitor(BundleContext bc, ServerXMLConfiguration serverXMLConfig, ConfigRefresher configRefresher) {
        this.bundleContext = bc;
        this.serverXMLConfig = serverXMLConfig;
        this.configRefresher = configRefresher;
    }

    public void registerService() {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(Constants.SERVICE_PID, CONFIG_PID);
        managedServiceRegistration = bundleContext.registerService(ManagedService.class, this, properties);
    }

    public void unregisterService() {
        if (managedServiceRegistration != null) {
            managedServiceRegistration.unregister();
        }
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        // The monitor interval can only be null when the element has not been metatype processed.
        // update() should only run with the processed properties
        if ((properties == null) || (properties.get(MONITOR_INTERVAL) == null))
            return;

        /*
         * See what type (if any) of monitoring we need. We won't necessarily of read the server.xml by now so first check if there is a framework property set (these come from
         * the bootstrap.properties file) and then see what is set in the server.xml. Note that this means that if something is set in the bootstrap.properties then it is not
         * overrideable in the server.xml
         */
        String updateTriggerSetting = this.bundleContext.getProperty(UPDATE_TRIGGER_PROP);
        if (updateTriggerSetting == null || !updateTriggerSetting.isEmpty()) {
            updateTriggerSetting = (String) properties.get(UPDATE_TRIGGER);
        }

        ErrorHandler.INSTANCE.setOnError((OnError) properties.get(OnErrorUtil.CFG_KEY_ON_ERROR));

        final boolean monitorConfiguration;
        final String fileMonitorType;
        if ("disabled".equals(updateTriggerSetting)) {
            monitorConfiguration = false;
            fileMonitorType = null;
        } else {
            monitorConfiguration = true;
            if (updateTriggerSetting.equals("mbean")) {
                fileMonitorType = FileMonitor.MONITOR_TYPE_EXTERNAL;
            } else {
                // default or "polled" setting
                fileMonitorType = FileMonitor.MONITOR_TYPE_TIMED;
            }
        }

        String monitorIntervalProp = this.bundleContext.getProperty(MONITOR_INTERVAL_PROP);
        Long monitorInterval;
        if (monitorIntervalProp == null) {
            monitorInterval = (Long) properties.get(MONITOR_INTERVAL);
        } else {
            monitorInterval = Long.valueOf(monitorIntervalProp);
        }

        resetConfigurationMonitoring(monitorConfiguration, monitorInterval, fileMonitorType);
        isFirstUpdate = false;
    }

    synchronized void resetConfigurationMonitoring(boolean monitorConfiguration, Long monitorInterval, String fileMonitorType) {
        if (fileMonitor == null) {
            if (monitorConfiguration) {
                // check if configuration was changed when monitoring was disabled
                boolean modified = isFirstUpdate ? false : serverXMLConfig.isModified();
                Collection<String> filesToMonitor = serverXMLConfig.getFilesToMonitor();
                Collection<String> directoriesToMonitor = serverXMLConfig.getDirectoriesToMonitor();
                fileMonitor = new ConfigFileMonitor(bundleContext, filesToMonitor, directoriesToMonitor, monitorInterval, modified, fileMonitorType, configRefresher);
                fileMonitor.register();
            }
        } else {
            if (monitorConfiguration) {
                fileMonitor.updateFileMonitor(monitorInterval, fileMonitorType);
            } else {
                fileMonitor.unregister();
                fileMonitor = null;
            }
        }
    }

    public synchronized void stopConfigurationMonitoring() {
        if (fileMonitor != null) {
            fileMonitor.unregister();
        }
        unregisterService();
    }

    public synchronized void updateFileMonitor(Collection<String> filesToMonitor) {
        if (fileMonitor != null) {
            fileMonitor.updateFileMonitor(filesToMonitor);
        }
    }

    public synchronized void updateDirectoryMonitor(Collection<String> directoriesToMonitor) {
        if (fileMonitor != null) {
            fileMonitor.updateDirectoryMonitor(directoriesToMonitor);
        }
    }
}
