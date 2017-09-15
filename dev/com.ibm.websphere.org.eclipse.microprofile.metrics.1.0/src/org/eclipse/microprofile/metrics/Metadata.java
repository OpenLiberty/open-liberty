/*
 **********************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *               2017 Red Hat, Inc. and/or its affiliates
 *               and other contributors as indicated by the @author tags.
 *
 * See the NOTICES file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 **********************************************************************/
package org.eclipse.microprofile.metrics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Bean holding the metadata of one single metric.
 * 
 * The metadata contains:
 * <ul>
 * <li>
 * {@code Name}: (Required) The name of the metric.
 * </li>
 * <li>
 * {@code Display name}: (Optional) The display (friendly) name of the metric.
 * By default, it is set to the {@code Name}.
 * </li>
 * <li>
 * {@code Description}: (Optional) A human readable description of the metric.
 * </li>
 * <li>
 * {@code Type}: (Required) The type of the metric. See {@link MetricType}.
 * </li>
 * <li>
 * {@code Unit}: (Optional) The unit of the metric.
 * The unit may be any unit specified as a String or one specified in {@link MetricUnits}.
 * </li>
 * <li>
 * {@code Tags}: (Optional) The tags (represented by key/value pairs) of the metric which is augmented by global tags (if available).
 * Global tags can be set by passing the list of tags in an environment variable {@code MP_METRICS_TAGS}.
 * For example, the following can be used to set the global tags:
 * <pre><code>
 *      export MP_METRICS_TAGS=app=shop,tier=integration
 * </code></pre>
 * </li>
 * </ul> 
 * 
 * @author hrupp, Raymond Lam
 */
public class Metadata {
    
     /**
     * Name of the metric.
     * <p>
     * A required field which holds the name of the metric object.
     * </p>
     */
    private String name;
    
    /**
     * Display name of the metric. If not set, the name is taken.
     * <p>
     * An optional field which holds the display (Friendly) name of the metric object.
     * By default it is set to the name of the metric object.
     * </p>
     */
    private String displayName;

    /**
     * A human readable description.
     * <p>
     * An optional field which holds the description of the metric object.
     * </p>
     */
    private String description;
    
    /**
     * Type of the metric.
     * <p>
     * A required field which holds the type of the metric object.
     * </p>
     */
    private MetricType type = MetricType.INVALID;
    /**
     * Unit of the metric.
     * <p>
     * An optional field which holds the Unit of the metric object.
     * </p>
     */
    private String unit = MetricUnits.NONE;
    
    /**
     * Tags of the metric. Augmented by global tags.
     * <p>
     * An optional field which holds the tags of the metric object which can be
     * augmented by global tags.
     * </p>
     */
    private HashMap<String, String> tags = new HashMap<String, String>();

    /**
     * The environment variable used to pass in global tags.
     */
    public static final String GLOBAL_TAGS_VARIABLE = "MP_METRICS_TAGS";
    
    /**
     * Defines if the metric can have multiple objects and needs special
     * treatment or if it is a singleton.
     * <p/>
     */
    Metadata() {
        String globalTagsFromEnv = System.getenv(GLOBAL_TAGS_VARIABLE);

        addTags(globalTagsFromEnv);
    }

    /**
     * Constructs a Metadata object with default units
     * 
     * @param name The name of the metric
     * @param type The type of the metric
     */
    public Metadata(String name, MetricType type) {
        this();
        this.name = name;
        this.type = type;

        // Assign default units
        switch (type) {
        case TIMER:
            this.unit = MetricUnits.NANOSECONDS;
            break;
        case METERED:
            this.unit = MetricUnits.PER_SECOND;
            break;
        case HISTOGRAM:
        case GAUGE:
        case COUNTER:
        default:
            this.unit = MetricUnits.NONE;
            break;
        }
    }

    /**
     * Constructs a Metadata object
     * 
     * @param name The name of the metric
     * @param type The type of the metric
     * @param unit The units of the metric
     */
    public Metadata(String name, MetricType type, String unit) {
        this();
        this.name = name;
        this.type = type;
        this.unit = unit;
    }

    /**
     * Constructs a Metadata object
     * 
     * @param name The name of the metric
     * @param displayName The display (friendly) name of the metric
     * @param description The description of the metric
     * @param type The type of the metric
     * @param unit The units of the metric
     */
    public Metadata(String name, String displayName, String description, MetricType type, String unit) {
        this();
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.unit = unit;
    }

    /**
     * Constructs a Metadata object
     * 
     * @param name The name of the metric
     * @param displayName The display (friendly) name of the metric
     * @param description The description of the metric
     * @param type The type of the metric
     * @param unit The units of the metric
     * @param tags The tags of the metric
     */
    public Metadata(String name, String displayName, String description, MetricType type, String unit, String tags) {
        this();
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.unit = unit;
        addTags(tags);
    }

    /**
     * Constructs a Metadata object from a map with the following keys
     * <ul>
     * <li>{@code name} - The name of the metric</li>
     * <li>{@code displayName} - The display (friendly) name of the metric</li>
     * <li>{@code description} - The description of the metric</li>
     * <li>{@code type} - The type of the metric</li>
     * <li>{@code unit} - The units of the metric</li>
     * <li>{@code tags} - The tags of the metric  - cannot be null</li>
     * </ul>
     * 
     * @param in a map of key/value pairs representing Metadata
     */
    public Metadata(Map<String, String> in) {
        this();
        this.name = (String) in.get("name");
        this.description = (String) in.get("description");
        this.displayName = (String) in.get("displayName");
        this.setType((String) in.get("type"));
        this.setUnit((String) in.get("unit"));
        if (in.keySet().contains("tags")) {
            String tagString = (String) in.get("tags");
            addTags(tagString);
        }
    }

    /**
     * Returns the metric name.
     * 
     * @return the metric name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the metric name.
     * 
     * @param name the new metric name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the display name if set, otherwise this method returns the metric name.
     * @return the display name
     */
    public String getDisplayName() {
        if (displayName == null) {
            return name;
        }
        return displayName;
    }

    /**
     * Sets the display name.
     * 
     * @param displayName the new display name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the description of the metric.
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the metric.
     * @param description the new description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the String representation of the {@link MetricType}.
     * 
     * @return the MetricType as a String
     * 
     * @see MetricType
     */
    public String getType() {
        return type == null ? MetricType.INVALID.toString() : type.toString();
    }

    /**
     * Returns the {@link MetricType} of the metric
     * @return the {@link MetricType}
     */
    public MetricType getTypeRaw() {
        return type;
    }

    /**
     * Sets the metric type using a String representation of {@link MetricType}.
     * @param type the new metric type
     * @throws IllegalArgumentException if the String is not a valid {@link MetricType}
     */
    public void setType(String type) throws IllegalArgumentException {
        this.type = MetricType.from(type);
    }

    /**
     * Sets the type of the metric
     * @param type the new metric type
     */
    public void setType(MetricType type) {
        this.type = type;
    }

    /**
     * Returns the unit of the metric.
     * @return the unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Sets the unit of the metric.
     * @param unit the new unit
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * Gets the list of tags as a single String in the format 'key="value",key2="value2",...'
     * @return a String containing the tags
     */
    public String getTagsAsString() {
        StringBuilder result = new StringBuilder();

        Iterator<Entry<String, String>> iterator = this.tags.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> pair = iterator.next();
            result.append(pair.getKey()).append("=\"").append(pair.getValue()).append("\"");
            if (iterator.hasNext()) {
                result.append(",");
            }

        }

        return result.toString();
    }

    /**
     * Returns the underlying HashMap containing the tags.
     * @return a hashmap of tags
     */
    public HashMap<String, String> getTags() {
        return this.tags;
    }

    /**
     * Add one single tag with the format: 'key=value'. If the input is empty or does
     * not contain a '=' sign, the entry is ignored.
     * 
     * @param kvString
     *            Input string
     */
    public void addTag(String kvString) {
        if (kvString == null || kvString.isEmpty() || !kvString.contains("=")) {
            return;
        }
        tags.put(kvString.substring(0, kvString.indexOf("=")), kvString.substring(kvString.indexOf("=") + 1));
    }

    /**
     * Add multiple tags delimited by commas.
     * The format must be in the form 'key1=value1, key2=value2'.
     * This method will call {@link #addTag(String)} on each tag.
     * 
     * @param tagsString a string containing multiple tags
     */
    public void addTags(String tagsString) {
        if (tagsString == null || tagsString.isEmpty()) {
            return;
        }

        String[] singleTags = tagsString.split(",");
        for (String singleTag : singleTags) {
            addTag(singleTag.trim());
        }
    }

    /**
     * Sets the tags hashmap.
     * 
     * @param tags a hashmap containing tags.
     */
    public void setTags(HashMap<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + unit.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MetadataEntry{");
        sb.append("name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", unit='").append(unit).append('\'');
        sb.append(", tags='").append(tags).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
