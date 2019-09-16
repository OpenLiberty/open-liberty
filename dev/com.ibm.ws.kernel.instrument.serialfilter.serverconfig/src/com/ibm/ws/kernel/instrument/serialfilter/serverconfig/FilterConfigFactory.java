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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Properties;
import java.util.Set;
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
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.kernel.instrument.serialfilter.agenthelper.PreMainUtil;
import com.ibm.ws.kernel.instrument.serialfilter.config.ConfigFacade;
import com.ibm.ws.kernel.instrument.serialfilter.config.SimpleConfig;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

@Component(configurationPid = "com.ibm.ws.kernel.instrument.serialfilter.serverconfig",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = { "service.vendor=IBM"})
public class FilterConfigFactory {
    private static final TraceComponent tc = Tr.register(FilterConfigFactory.class);

    /**
     * Constant for the configuration keys.
     */
    public static final String CONFIG_MODE = "filterMode";
    public static final String CONFIG_POLICY = "policy";
    public static final String CONFIG_OPERATION = "operation";
    public static final String CONFIG_PERMISSION = "permission";
    public static final String CONFIG_CLASS = "class";
    public static final String CONFIG_METHOD = "method";
    
    public static final String VALUE_MODE_INACTIVE = "Inactive";
    public static final String VALUE_MODE_DISCOVER = "Discover";
    public static final String VALUE_MODE_ENFORCE = "Enforce";
    public static final String VALUE_MODE_REJECT = "Reject";

    public static final String VALUE_PERMISSION_ALLOW = "Allow";
    public static final String VALUE_PERMISSION_DENY = "Deny";

    @SuppressWarnings("rawtypes")
	private Map<String, Dictionary> serialFilterConfigMap = new HashMap<String, Dictionary>();  // to be deleted    
   
    @Activate
    protected void activate(ComponentContext ctx, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "activate", properties);
        }
        if(isEnabled()) {
            loadMaps(properties);
        } else {
        	Tr.error(tc, "SF_ERROR_NOT_ENABLED");
        }
    }

    @Modified
    protected void modified(Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "modified", properties);
        }
        if(isEnabled()) {
            loadMaps(properties);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "deactivate, reason : " + reason);
        }
    }

    private void loadMaps(Map<String, Object> properties) {
        Map<String, String> modeMap = new HashMap<String, String>();
        Map<String, String> policyMap = new HashMap<String, String>();
   	    loadModeMap(Nester.nest(CONFIG_MODE, properties), modeMap);
   	    loadPolicyMap(Nester.nest(CONFIG_POLICY, properties), policyMap);
        propagateConfigMap(modeMap, policyMap);
      	Tr.info(tc, "SF_INFO_ENABLED");
    }

    private void loadModeMap(List<Map<String, Object>> items, Map<String, String> modeMap) {
        if (items != null && !items.isEmpty()) {
            for (Map<String, Object> item : items) {
                String clazz = (String)item.get(CONFIG_CLASS);
                String method = (String)item.get(CONFIG_METHOD);
                String operation = (String)item.get(CONFIG_OPERATION);
                String key;
                if (method != null) {
                	key = clazz + "#" + method;
                } else {
                	key = clazz;                	
                }
                if(!modeMap.containsKey(key)) {
                    modeMap.put(key, operation);
                } else {
                	Tr.warning(tc, "SF_WARNING_DUPLICATE_MODE", new Object[] {operation, clazz, method});
                }
            }
        }
    }

    private void loadPolicyMap(List<Map<String, Object>> items, Map<String, String> policyMap) {
        if (items != null && !items.isEmpty()) {
            for (Map<String, Object> item : items) {
                String clazz = (String)item.get(CONFIG_CLASS);
                String permission = (String)item.get(CONFIG_PERMISSION);
                if(!policyMap.containsKey(clazz)) {
                  	policyMap.put(clazz, permission);                	
                } else {
                	Tr.warning(tc, "SF_WARNING_DUPLICATE_POLICY", new Object[] {permission, clazz});
                }
            }
        }
    }

    protected void propagateConfigMap(Map<String, String> modeMap, Map<String, String> policyMap) {
        SimpleConfig configObject = getSystemConfigProxy();
        Properties filterConfig = new Properties();

        convertData(modeMap, policyMap, filterConfig);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "serialFilter configuration is being updated.", filterConfig);
        }
        // if there is any entry in the properties, propagate it to configObject.
        configObject.reset();
        if (!filterConfig.isEmpty()) {
            configObject.load(filterConfig);
        }
    }

	protected void convertData(Map<String, String> modeMap, Map<String, String> policyMap, Properties filterConfig) {
        // first create a combined key list
    	// Set from keySet() does not support addAll, convert it to HashSet.
    	Set<String> keys = new HashSet(modeMap.keySet());
    	keys.addAll(policyMap.keySet());
    	
    	for ( String key : keys ) {
            String operation = (String)modeMap.get(key);
            String permission = (String)policyMap.get(key);
            if (operation != null && permission != null) {
                filterConfig.setProperty(key, operation.toUpperCase() + "," + permission.toUpperCase());
            } else if (operation != null) {
                filterConfig.setProperty(key, operation.toUpperCase());            	
            } else {
                filterConfig.setProperty(key, permission.toUpperCase());
            }
        }
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
}
