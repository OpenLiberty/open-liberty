/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.ws.javaee.ddmodel.wsbnd.WebserviceEndpointProperties;

@Component(configurationPid = "com.ibm.ws.javaee.ddmodel.wsbnd.WebserviceEndpointProperties",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = "service.vendor = IBM")
public class WebserviceEndpointPropertiesComponentImpl implements WebserviceEndpointProperties {

    private final Map<String, String> attributes = new HashMap<String, String>();

    // These are properties added by the config runtime -- ignore them.
    private final String[] ignoredPrefixes = { "service.", "config.", "component." };

    @Activate
    protected void activate(Map<String, Object> config) {
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String name = entry.getKey();
            if (!isInternal(name))
                attributes.put(entry.getKey(), (String) entry.getValue());
        }
    }

    @Deactivate
    protected void deactivate() {
        attributes.clear();
    }

    private boolean isInternal(String name) {
        for (String prefix : ignoredPrefixes) {
            if (name.startsWith(prefix))
                return true;
        }

        if (name.equals("id"))
            return true;

        return false;
    }

    @Override
    public Map<String, String> getAttributes() {
        return this.attributes;
    }
}
