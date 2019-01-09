/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.serverconfig;

import java.util.HashMap;
import java.util.Map;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.Properties;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.instrument.serialfilter.agenthelper.PreMainUtil;
import com.ibm.ws.kernel.instrument.serialfilter.config.ConfigFacade;
import com.ibm.ws.kernel.instrument.serialfilter.config.SimpleConfig;

import com.ibm.wsspi.kernel.service.utils.FrameworkState;

@Component(service = ManagedServiceFactory.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM", "service.pid=com.ibm.ws.kernel.instrument.serialfilter.serverconfig" })
public class FilterConfigFactory implements ManagedServiceFactory {
    private static final TraceComponent tc = Tr.register(FilterConfigFactory.class);

    /**
     * Constant for the configuration keys.
     */
    public static final String CONFIG_MODE = "mode";
    public static final String CONFIG_PERMISSION = "permission";
    public static final String CONFIG_CLASS = "class";
    public static final String CONFIG_METHOD = "method";
    
    public static final String VALUE_MODE_INACTIVE = "Inactive";
    public static final String VALUE_MODE_DISCOVER = "Discover";
    public static final String VALUE_MODE_ENFORCE = "Envorce";
    public static final String VALUE_MODE_REJECT = "Reject";

    public static final String VALUE_PERMISSION_ALLOW = "Allow";
    public static final String VALUE_PERMISSION_DENY = "Deny";

    @SuppressWarnings("rawtypes")
	private Map<String, Dictionary> serialFilterConfigMap = new HashMap<String, Dictionary>();
    private static boolean isEnableMessageShown=false;
    
    @Override
    public void updated(String pid, @SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
        // If we are stopping ignore the update
        if (isStopping()) {
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "updated filterConfig pid : " + pid, properties);
        }
        if (isEnabled()) {
            serialFilterConfigMap.put(pid, properties);
            try {
                propagateConfigMap(serialFilterConfigMap);
                if (!isEnableMessageShown) {
                	isEnableMessageShown=true;
                	Tr.info(tc, "SF_INFO_ENABLED");
                }
            } catch (ConfigurationException e) {
                serialFilterConfigMap.remove(pid);
                throw e;
            }
        }
    }

    @Override
    public void deleted(String pid) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "deleted filterConfig pid : " + pid);
        }
        if (isEnabled()) {
            if (serialFilterConfigMap.containsKey(pid)) {
                serialFilterConfigMap.remove(pid);
                try {
                    propagateConfigMap(serialFilterConfigMap);
                } catch (ConfigurationException e) {
                    // ignore.
                }
            }
        }
    }

    @Override
    public String getName() {
        return "filter config";
    }

    protected void activate(ComponentContext ctx) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "activate", ctx.getProperties());
        }
    }

    protected void deactivate(ComponentContext ctx, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "deactivate, reason=" + reason);
        }
    }

    protected void propagateConfigMap(@SuppressWarnings("rawtypes") Map<String, Dictionary> map) throws ConfigurationException {
        SimpleConfig configObject = getSystemConfigProxy();
        Properties filterConfig = new Properties();
        try {
            convertData(map, filterConfig);
        } catch (ConfigurationException e) {
            // in case of error, disable everything and throws an exception.
            filterConfig.clear();
            filterConfig.setProperty("*", "REJECT");
            configObject.reset();
            configObject.load(filterConfig);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Due to an error, set serialFilter for rejecting everything.");
            }
            throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "serialFilter configuration is being updated.", filterConfig);
        }
        // if there is any entry in the properties, propagate it to configObject.
        configObject.reset();
        if (!filterConfig.isEmpty()) {
            configObject.load(filterConfig);
        }
    }

    @SuppressWarnings("rawtypes")
	protected void convertData(Map<String, Dictionary> map, Properties filterConfig) throws ConfigurationException {
        for ( Map.Entry<String, Dictionary> entry : map.entrySet() ) {
            String pid = entry.getKey();
            Dictionary props = entry.getValue();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "propagateConfigMap", pid, props);
            }
            // class and mode are always set. if not, configService will not allow to process, since metadata is
            // set as required for these attributes.
            if (props != null && !props.isEmpty()) {
                String clazz = (String)props.get(CONFIG_CLASS);
                String mode = (String)props.get(CONFIG_MODE);
                String permission = (String)props.get(CONFIG_PERMISSION);
                String method = (String)props.get(CONFIG_METHOD);
                String value = mode.toUpperCase();
                // valid value.
                if (method != null && !method.isEmpty()) {
                    clazz = clazz + "#" + method;
                }
                if (permission != null && !permission.isEmpty()) {
                    value =  value + "," + permission.toUpperCase();
                }
                filterConfig.setProperty(clazz, value);
            }
        }
        return;
    }


    protected boolean isStopping() {
        return FrameworkState.isStopping();
    }

    protected boolean isEnabled() {
        String activeSerialFilter = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty(PreMainUtil.KEY_SERIALFILTER_AGENT_ACTIVE);
            }
        });
        if ("true".equalsIgnoreCase(activeSerialFilter)) {
	        return true;
	    }
        return false;
    }

    protected SimpleConfig getSystemConfigProxy() {
        return ConfigFacade.getSystemConfigProxy();
    }

    @SuppressWarnings("rawtypes")
	protected Map<String, Dictionary> getConfigMap() {
        return serialFilterConfigMap;
    }
}
