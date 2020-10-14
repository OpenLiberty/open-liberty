/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.serverxml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

/**
 * Stores the properties from one appProperties element in server.xml.
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = "com.ibm.ws.appconfig.appProperties", service = AppPropertiesComponent.class)
public class AppPropertiesComponent {

    /**
     * Pattern matching property.(x).(key|value), used to extract properties from config
     */
    private static final Pattern keyValuePattern = Pattern.compile("property.(\\d+).(name|value)");

    /**
     * The current map of properties. This map must always be replaced rather than modified.
     */
    private volatile Map<String, String> properties = Collections.emptyMap();
    private volatile String pid;

    /**
     * Returns the properties defined in the appProperties config element
     * <p>
     * The caller must not modify this map
     *
     * @return a map of the properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Returns the PID for this appProperties config element
     *
     * @return the appProperties PID
     */
    public String getPid() {
        return pid;
    }

    @Activate
    protected void activate(Map<String, Object> properties) {
        this.properties = processProperties(properties);
        this.pid = (String) properties.get("service.pid");
    }

    @Modified
    protected void modified(Map<String, Object> properties) {
        this.properties = processProperties(properties);
    }

    @Deactivate
    protected void deactivate(Map<String, Object> properties) {
        this.pid = null;
        this.properties = Collections.emptyMap();
    }

    /**
     * Converts the properties from config into a simple key-value map
     * <p>
     * Properties arrive from config in the following form:
     * <ul>
     * <li>property.0.name=foo</li>
     * <li>property.0.value=bar</li>
     * <li>property.1.name=baz</li>
     * <li>property.1.value=qux</li>
     * <ul>
     * <p>
     * This method converts these values to the following form, ignoring any other properties:
     * <ul>
     * <li>foo=bar</li>
     * <li>baz=qux</li>
     * </ul>
     *
     * @param properties the properties as they're returned from config admin
     * @return a simple map of properties
     */
    private Map<String, String> processProperties(Map<String, Object> properties) {

        // Gather together matching name and value properties
        HashMap<String, PropertyEntry> propertyEntries = new HashMap<>();
        for (Entry<String, Object> entry : properties.entrySet()) {
            if (!(entry.getValue() instanceof String)) {
                continue;
            }

            Matcher m = keyValuePattern.matcher(entry.getKey());
            if (m.matches()) {
                String index = m.group(1);
                PropertyEntry propertyEntry = propertyEntries.computeIfAbsent(index, k -> new PropertyEntry());
                if (m.group(2).equals("name")) {
                    propertyEntry.name = (String) entry.getValue();
                } else {
                    propertyEntry.value = (String) entry.getValue();
                }
            }
        }

        // Now put each name and value pair into a map
        Map<String, String> result = new HashMap<>();
        for (PropertyEntry entry : propertyEntries.values()) {
            if (entry.name != null && entry.value != null) {
                result.put(entry.name, entry.value);
            }
        }

        return result;
    }

    private static class PropertyEntry {
        private String name;
        private String value;
    }
}
