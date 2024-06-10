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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;


import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * The abstract class of the WebServiceConfig which is used to process the
 * common parts of webService and webServiceClient config.
 * 
 * Each Declarative Service for the two configurations should use this class
 */
public abstract class WebServiceConfig {

    private static final TraceComponent tc = Tr.register(WebServiceConfig.class);
    
    protected static final HashSet<String> propertiesToRemove = new HashSet<>();

    static {
            // this is stuff the framework always adds, we don't need it so we'll filter it
            // out.
            propertiesToRemove.add("config.overrides");
            propertiesToRemove.add("config.id");
            propertiesToRemove.add("component.id");
            propertiesToRemove.add("config.displayId");
            propertiesToRemove.add("component.name");
            propertiesToRemove.add("config.source");
            propertiesToRemove.add("service.pid");
            propertiesToRemove.add("service.vendor");
            propertiesToRemove.add("service.factoryPid");
    }

    /**
     * given the map of properties, remove ones we don't care about, and translate
     * some others. If it's not one we're familiar with, transfer it unaltered
     *
     * @param props - input list of properties
     * @return - a new Map of the filtered properties.
     */
    protected Map<String, Object> filterProps(Map<String, Object> props) {
            HashMap<String, Object> filteredProps = new HashMap<>();
            Iterator<String> it = props.keySet().iterator();
            boolean debug = tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled();

            while (it.hasNext()) {
                    String key = it.next();

                    if(key == null) {
                        continue;
                    }
                    if (debug) {
                            Tr.debug(tc, "key: " + key + " value: " + props.get(key) + " of type " + props.get(key).getClass());
                    }
                    // skip stuff we don't care about
                    if (propertiesToRemove.contains(key)) {
                            continue;
                    }
                    if (key.compareTo(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP) == 0) {
                            if (!ConfigValidation.validateEnableSchemaValidation((boolean) props.get(key)))
                                    continue;
                    }
                    if (key.compareTo(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP) == 0) {
                            if (!ConfigValidation.validateIgnoreUnexpectedElements((boolean) props.get(key)))
                                    continue;
                    }
                    filteredProps.put(key, props.get(key));

            }
            return filteredProps;
    }

    protected abstract void modified(Map<String, Object> properties);
    

    protected abstract void deactivate();
    
}
