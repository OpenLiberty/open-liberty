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
import java.util.Dictionary;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

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

    private BundleContext bContext = null;
    private volatile ComponentContext cc = null;

    private Map<String, Dictionary> serialFilterConfigMap = new HashMap<String, Dictionary>();

    @Override
    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        // If we are stopping ignore the update
        if (FrameworkState.isStopping()) {
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "updated filterConfig pid : " + pid, properties);
        }

        serialFilterConfigMap.put(pid, properties);
        propagateConfigMap(serialFilterConfigMap);
    }

    @Override
    public void deleted(String pid) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "deleted filterConfig pid : " + pid);
        }
        serialFilterConfigMap.remove(pid);
    }

    @Override
    public String getName() {
        return "filter config";
    }

    protected void activate(ComponentContext ctx) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "activate", ctx.getProperties());
        }
        cc = ctx;
        bContext = ctx.getBundleContext();
    }

    protected void deactivate(ComponentContext ctx, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "deactivate, reason=" + reason);
        }
    }

    protected void propagateConfigMap(Map<String, Dictionary> map) throws ConfigurationException {
        SimpleConfig configObject = ConfigFacade.getSystemConfigProxy();
        Properties filterConfig = new Properties();
        for ( Map.Entry<String, Dictionary> entry : map.entrySet() ) {
            String pid = entry.getKey();
            Dictionary props = entry.getValue();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "propagateConfigMap", pid, props);
            }
            if (props != null && !props.isEmpty()) {
                String clazz = (String)props.get(CONFIG_CLASS);
                String mode = (String)props.get(CONFIG_MODE);
                String permission = (String)props.get(CONFIG_PERMISSION);
                String method = (String)props.get(CONFIG_METHOD);
                if (clazz != null && !clazz.isEmpty() && mode != null && !mode.isEmpty()) {
                    String key = clazz;
                    String value = mode.toUpperCase();
                    // valid value.
                    if (method != null && !method.isEmpty()) {
                        clazz = clazz + "#" + method;
                    }
                    if (permission != null && !permission.isEmpty()) {
                        value =  value + "," + permission.toUpperCase();
                    }
                    filterConfig.setProperty(clazz, value);
                } else {
                    // in case of error, disable everything and throws an exception.
                    filterConfig.setProperty("*", "REJECT");
                    configObject.load(filterConfig);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                       Tr.debug(this, tc, "Due to an error, set serialFilter for rejecting everything.");
                    }

                    String propName;
                    if (clazz == null || clazz.isEmpty()) {
                        propName = CONFIG_CLASS;
                    } else {
                        propName = CONFIG_MODE;
                    }
                    // todo: nls
                    String reason = "There is no properties.";
                    throw new ConfigurationException(propName, reason);
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "serialFilter configuration is being updated.", filterConfig);
        }
        configObject.load(filterConfig);
    }

}
