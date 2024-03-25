/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *  This class holds all of the config property maps and processing code that is common between
 *  WebServiceClientConfigHolder and WebServiceConfigHolder
 *  
 *  It maintains a map of all of the maps provided by each instance of WebServiceConfigImpl or WebServiceClientImpl
 *  
 *  It maintains a map of all of the known serviceNames or portNames
 *  
 *  It maintains a cache of already associated maps by their serviceName and portName
 *  
 *  If the configuration in common between the two configurations it can be processed  here, if not, then the 
 *  unique configuration should be processed in either WebServiceClientConfigHolder or WebServiceConfigHolder
 *  
 *  It's used to return obtain configuration values for integration code.
 *  
 */
public class ConfigHolder {

    private static final TraceComponent tc = Tr.register(ConfigHolder.class);

    private static boolean debug = getDebugEnabled();

    // a map of configuration properties keyed on serviceName/portName.
    private static volatile Map<String, Map<String, Object>> configInfo = new HashMap<>();

    // a map of declarative service object id's to serviceName or portName, so we can track which service added
    // which serviceName/portName.
    private static volatile Map<String, String> nameMap = new HashMap<>();

    // a cached map of search results
    // that have been processed to look up the best match for the serviceName or portName strings
    private static volatile Map<String, Map<String, Object>> resolvedConfigInfoCacheByName = new HashMap<>();

    /**
     * add a configuration for a name (serviceName/portName). We'd like a set of hashmaps keyed by name,
     * however when osgi calls deactivate, we have no arguments, so we have to
     * associate a name with the object id of the service. That allows us to remove
     * the right one.
     *
     * @param id     - the object id of the service instance that added this.
     * @param name   - either the serviceName or portName depending on the record being added.
     * @param params - the properties applicable to this serviceName/portName.
     */
    public static synchronized void addConfig(String id, String name, Map<String, Object> params) {
        if(getDebugEnabled()) {
            Tr.debug(tc, "addConfig addConfig is id: " + id + " name = " + name + " params : " + params);
        }
        nameMap.put(id, name);
        if (name != null) {
            configInfo.put(name, params);
        }
        resolvedConfigInfoCacheByName.clear();
    }

    
    /**
    * remove a configuration (we'll look up the name (serviceName/portName) from the objectId)
    *
    * @param id - the object id of the service that called us
    */
    public static synchronized void removeConfig(String objectId) {

        if(getDebugEnabled()) {
            Tr.debug(tc, "addConfig removeConfig is objectId: " + objectId );
        }
        String serviceName = nameMap.get(objectId);
        if (serviceName != null) {
            configInfo.remove(serviceName);
        }
        nameMap.remove(objectId);
        resolvedConfigInfoCacheByName.clear();
    }
    
    /**
     * Get the value of enableSchemaValidation from all known property maps using the name (serviceName/portName) provided.
     *
     * @param name - either the serviceName or portName depending on which config is being used to get the enableSchemaValidation
     * 
     * @return value of enableSchemaValidation if set, or null if the configuration doesn't contain the property
     */
    public static Object getEnableSchemaValidation(String name) {
        
        Map<String, Object> props = getNameProps(name);
        
        if (props != null) {

            Object enableSchemaValidation = props.get(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP);
            
            if(getDebugEnabled()) {
                Tr.debug(tc, "getEnableSchemaValidation is returning: " + enableSchemaValidation);
            }
            return enableSchemaValidation;
        } else {
            // return the default value
            return null;
        }
    }

    /**
     * Get the value of ignoreUnexpectedElements from all known property maps using the name (serviceName/portName) provided.
     *
     * @param name - either the serviceName or portName depending on which config is being used to get the ignoreUnexpectedElements
     * 
     * @return value of ignoreUnexpectedElements if set, or null if the configuration doesn't contain the property
     */
    public static Object getIgnoreUnexpectedElements(String name) {
        Map<String, Object> props = getNameProps(name);
        
        if (props != null) {

            Object ignoreUnexpectedElements = props.get(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP);
            if(getDebugEnabled()) {
                Tr.debug(tc, "getIgnoreUnexpectedElements is returning: " + ignoreUnexpectedElements);
            }
            
            return ignoreUnexpectedElements;
            
        } else {
            
            if(getDebugEnabled()) {
                Tr.debug(tc, "getIgnoreUnexpectedElements is returning null because no properties exist for name - " + name);
            }
            return null; // No properties exist so return null
            
        }
    }

    /**
     * This method uses the prpvided name (serviceName/portName) to find the associated property map
     * It will first check if the configInfo map is empty, if it is then we return null because there 
     * are no known configuration properties known to this class. Then if name is null, it will see if 
     * @param name - either the portName/serviceName used to look up the property map
     * @return A Map<String, Object> Properties object based on portName/serviceName
     */
    public static Map<String, Object> getNameProps(String name) {
        boolean debug = getDebugEnabled();
        if (configInfo.isEmpty()) {
            if (debug) {
                Tr.debug(tc, "configInfo is empty, returning null");
            }
            return null;
        }

        // look in the cache in case we've resolved this before
        if (name != null) {
            synchronized (resolvedConfigInfoCacheByName) {
                Map<String, Object> props = resolvedConfigInfoCacheByName.get(name);
                if (props != null) {
                    if (debug) {
                        Tr.debug(tc, "resolvedConfigInfoCacheByName cache hit, name: " + name + " props: " + props);
                    }
                    return (props.isEmpty() ? null : props);
                }
            }
        }

        // at this point we might have to merge something, set up a new hashmap to hold
        // the results
        HashMap<String, Object> mergedProps = new HashMap<>();

        if (debug) {
            Tr.debug(tc, "begin name search - configInfo = " + configInfo);
        }
        
        String foundName = ""; 
        // try to find a match from name of serviceName/portName or default if exists
        synchronized (ConfigHolder.class) {
            Iterator<String> it = configInfo.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                if (key.equals(name)) {

                    if (debug) {
                        Tr.debug(tc, "key found - " + key + " adding associated props");
                    } 

                    Map<String, Object> props = configInfo.get(key);

                    mergedProps.putAll(props); // merge props for this name.

                    foundName = key; // assign the foundName to the key name.
                    continue;
                } else if (key.equals(WebServiceConfigConstants.DEFAULT_PROP)) {

                    if (debug) {
                        Tr.debug(tc, "default found - " + key + " adding default props");
                    }
                    
                    Map<String, Object> props = configInfo.get(key);

                    // Merge all key value pairs from props if they don't already exist in the merged map
                    // This should allow a named config to override the default and inherit the default if it's not specified for the named config
                    for (Map.Entry<String, Object> entry : props.entrySet()) {
                        String propsKey = entry.getKey();
                        Object propsValue = entry.getValue();
                        mergedProps.putIfAbsent(propsKey, propsValue);
                        
                        if (debug) {
                            Tr.debug(tc, "default prop to be added if absent - propsKey " + propsKey + " propsValue " + propsValue);
                        }
                    }

                    if(foundName.isEmpty()) { // Only set it to default if default exists and the key hasn't already matched the name
                        foundName = key;
                    }
                }
            }
            
            if (foundName.contains(name)) { // We found a properties map for this serviceName/portName store it in cache
                
                resolvedConfigInfoCacheByName.put(name, mergedProps);
                
            } else if (foundName.contains(WebServiceConfigConstants.DEFAULT_PROP)) { // We found the default properties map so store it in the cache
                
                resolvedConfigInfoCacheByName.put(WebServiceConfigConstants.DEFAULT_PROP, mergedProps);
                
            } else {

                if (debug) {
                    Tr.debug(tc, "No properties found for: " + name );
                }
                return null; // We found neither properties for the serviceName/portName or any default properties
            }
            if (debug) {
                Tr.debug(tc, "getNameProps final result for nameame: " + name + " values: " + mergedProps);
            }

            return mergedProps;
        } // end sync block
    }

    // Method to ensure dynamic tracing is honored while still using a class variable
    // Call directly if only one debug statement is needed in the method
    // Set debug = getDebugEnabled() if multiple checks are needed per method
    private static boolean getDebugEnabled() {
        return tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled();
    }
}
