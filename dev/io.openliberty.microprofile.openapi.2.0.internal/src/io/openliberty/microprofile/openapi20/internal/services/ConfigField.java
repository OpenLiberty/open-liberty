/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.smallrye.openapi.api.OpenApiConfig;

/**
 * Describes a configuration field stored in {@link OpenApiConfig}
 * <p>
 * This interface allows us to read config data from different versions of this class.
 * <p>
 * Instances of this interface are retrieved from {@link ConfigFieldProvider}
 */
public interface ConfigField {

    /**
     * The name of the method on {@link OpenApiConfig} which this {@link ConfigField} represents
     *
     * @return the corresponding method name
     */
    public String getMethod();

    /**
     * The name of the MP Config Property that is used to set this config field
     *
     * @return the config property name
     */
    public String getProperty();

    /**
     * Extract the value of this field from a config and convert it to a string
     *
     * @param config the config
     * @return the extracted value
     */
    public String getValue(OpenApiConfig config);

    /**
     * Utility method to convert a map of strings to a single string
     *
     * @param map the map
     * @return a string representation of {@code map}
     */
    public static String serializeMap(Map<String, String> map) {
        // Sort to ensure repeatability
        List<Entry<String, String>> entryList = new ArrayList<>(map.entrySet());
        Collections.sort(entryList, Map.Entry.comparingByKey());

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Entry<String, String> e : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(e.getKey());
            sb.append("=");
            sb.append(e.getValue());
        }
        return sb.toString();
    }

    /**
     * Utility method to convert a set of strings to a single string
     *
     * @param set the set
     * @return a string representation of {@code set}
     */
    public static String serializeSet(Set<String> set) {
        // Sort to ensure repeatability
        ArrayList<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return String.join(",", list);
    }

}
