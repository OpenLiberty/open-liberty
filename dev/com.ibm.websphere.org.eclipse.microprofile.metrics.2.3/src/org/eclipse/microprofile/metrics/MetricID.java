/*
 **********************************************************************
 * Copyright (c) 2018, 2019 Contributors to the Eclipse Foundation
 *               2018, 2019 IBM Corporation and others
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

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * A unique identifier for {@link Metric} and {@link Metadata} that are registered
 * in the {@link MetricRegistry}
 *
 * The MetricID contains:
 * <ul>
 * <li>
 * {@code Name}: (Required) The name of the metric.
 * </li>
 * <li>
 * {@code Tags}: (Optional) The tags (represented by {@link Tag} objects)
 * of the metric which is augmented by global tags (if available). The tag name
 * must match the regex `[a-zA-Z_][a-zA-Z0-9_]*` (Ascii alphabet, numbers and
 * underscore). The tag value may contain any UTF-8 encoded character. Global
 * tags can be set by passing the list of tags in an environment variable
 * {@code MP_METRICS_TAGS} or a system property {@code mp.metrics.tags}. Tag
 * values set through `MP_METRICS_TAGS` or `mp.metrics.tags` MUST escape equal
 * symbols `=` and commas `,` with a backslash `\`
 *
 * For example, the following can be used to set the global tags:
 *
 * <pre>
 * <code>
 *      export MP_METRICS_TAGS=app=shop,tier=integration,special=deli\=ver\,y
 * </code>
 * </pre>
 *
 * </li>
 * </ul>
 */
public class MetricID implements Comparable<MetricID> {

    public static final String GLOBAL_TAGS_VARIABLE = "mp.metrics.tags";

    public static final String APPLICATION_NAME_VARIABLE = "mp.metrics.appName";
    public static final String APPLICATION_NAME_TAG = "_app";

    private static final String GLOBAL_TAG_MALFORMED_EXCEPTION = "Malformed list of Global Tags. Tag names "
                                                                + "must match the following regex [a-zA-Z_][a-zA-Z0-9_]*."
                                                                + " Global Tag values must not be empty."
                                                                + " Global Tag values MUST escape equal signs `=` and commas `,`"
                                                                + " with a backslash `\\` ";

    /**
     * Name of the metric.
     * <p>
     * A required field which holds the name of the metric object.
     * </p>
     */
    private final String name;

    /**
     * Tags of the metric. Augmented by global tags.
     * <p>
     * An optional field which holds the tags of the metric object which can be
     * augmented by global tags.
     * </p>
     */
    private final Map<String, String> tags = new TreeMap<String, String>();

    /**
     * Constructs a MetricID with the given metric name and no tags.
     * If global tags are available then they will be appended to this MetricID.
     *
     * @param name the name of the metric
     */
    public MetricID(String name) {
        this(name, null);
    }

    /**
     * Constructs a MetricID with the given metric name and {@link Tag}s.
     * If global tags are available then they will be appended to this MetricID
     *
     * @param name the name of the metric
     * @param tags the tags associated with this metric
     */
    public MetricID(String name, Tag... tags) {
        this.name = name;
        try {
            Config config = ConfigProvider.getConfig();
            Optional<String> globalTags = config.getOptionalValue(GLOBAL_TAGS_VARIABLE, String.class);
            globalTags.ifPresent(this::parseGlobalTags);

            // for application servers with multiple applications deployed, distinguish metrics from different applications by adding the "_app" tag
            Optional<String> applicationName = config.getOptionalValue(APPLICATION_NAME_VARIABLE, String.class);
            applicationName.ifPresent(appName -> {
                if (!appName.isEmpty()) {
                    addTag(new Tag(APPLICATION_NAME_TAG, appName));
                }
            });
        }
        catch(NoClassDefFoundError | IllegalStateException | ExceptionInInitializerError e) {
            // MP Config is probably not available, so just go on
        }

        addTags(tags);
    }

    /**
     * Returns the Metric name associated with this MetricID
     *
     * @return the Metric name associated with this MetricID
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the underlying map containing the tags.
     *
     * @return a {@link Map} of tags
     */
    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    /**
     * Gets the list of tags as a single String in the format 'key="value",key2="value2",...'
     *
     * @return a String containing the tags
     */
    public String getTagsAsString() {
        return this.tags.entrySet().stream().map(e -> e.getKey() + "=\"" + e.getValue() + "\"").collect(Collectors.joining(","));
    }

    /**
     * Gets the list of tags as a list of {@link Tag} objects
     *
     * @return a a list of Tag objects
     */
    public List<Tag> getTagsAsList() {
        List<Tag> list = new ArrayList<>();
        this.tags.forEach((key, value) -> list.add(new Tag(key, value)));
        return Collections.unmodifiableList(list);
    }

    /**
     * Gets the list of tags as an array of {@link Tag} objects.
     *
     * @return An array of tags
     */
    public Tag[] getTagsAsArray() {
        Tag[] result = new Tag[tags.size()];
        int i = 0;
        for (Entry<String, String> entry : tags.entrySet()) {
            result[i] = new Tag(entry.getKey(), entry.getValue());
            i++;
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MetricID)) {
            return false;
        }
        MetricID that = (MetricID) o;
        return Objects.equals(this.name, that.getName()) && Objects.equals(this.tags, that.getTags());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(name, tags);
    }

    @Override
    public String toString() {
        return "MetricID{" +
            "name='" + name + '\'' +
            ", tags=[" + getTagsAsString() +
            "]}";
    }

    /**
     * Add multiple {@link Tag} objects
     * This method will call {@link #addTag(Tag)} on each tag.
     * If a key already exists then it will be overwritten.
     *
     * @param tagArray an array of {@link Tag} objects
     */
    private void addTags(Tag[] tagArray) {
        if (tagArray == null || tagArray.length == 0) {
            return;
        }
        Stream.of(tagArray).forEach(this::addTag);
    }

    /**
     * Adds a singular {@link Tag} object into the MetricID.
     * If the tag key/name already exists then it will be overwritten with
     * the new value.
     *
     * @param tag the {@link Tag} object
     */
    private void addTag(Tag tag) {
        if (tag == null || tag.getTagName() == null || tag.getTagValue() == null) {
            return;
        }
        tags.put(tag.getTagName(), tag.getTagValue());
    }

    /**
     * Parses the global tags retrieved from environment variable {@code MP_METRICS_TAGS}.
     *
     * @param globalTags the string of global tags retrieved from MP_METRICS_TAGS
     * @throws IllegalArgumentException if the global tags list does not adhere to
     *             the appropriate format.
     */
    private void parseGlobalTags(String globalTags) throws IllegalArgumentException {
        if (globalTags == null || globalTags.length() == 0) {
            return;
        }
        String[] kvPairs = globalTags.split("(?<!\\\\),");
        for (String kvString : kvPairs) {

            if (kvString.length() == 0) {
                throw new IllegalArgumentException(GLOBAL_TAG_MALFORMED_EXCEPTION);
            }

            String[] keyValueSplit = kvString.split("(?<!\\\\)=");

            if (keyValueSplit.length != 2 || keyValueSplit[0].length() == 0 || keyValueSplit[1].length() == 0) {
                throw new IllegalArgumentException(GLOBAL_TAG_MALFORMED_EXCEPTION);
            }

            String key = keyValueSplit[0];
            String value = keyValueSplit[1];

            if (!key.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                throw new IllegalArgumentException("Invalid Tag name. Tag names must match the following regex "
                                                   + "[a-zA-Z_][a-zA-Z0-9_]*");
            }
            value = value.replace("\\,", ",");
            value = value.replace("\\=", "=");
            tags.put(key, value);
        }
    }

    /**
     * Compares two MetricID objects through the following steps:
     * <br>
     * <ol>
     * <li>
     * Compares the names of the two MetricIDs lexicographically.
     * </li>
     * <li>
     * If the names are equal: Compare the number of tags.
     * </li>
     * <li>
     * If the tag lengths are equal: Compare the Tags (sorted by the Tag's key value)
     * <ul>
     * <li>
     * a) Compare the Tag names/keys lexicographically
     * </li>
     * <li>
     * b) If keys are equal, compare the Tag values lexicographically
     * </li>
     * </ul>
     * </li>
     * </ol>
     *
     * @param other the other MetricID
     */
    @Override
    public int compareTo(MetricID other) {
        int compareVal = this.name.compareTo(other.getName());
        if (compareVal == 0) {
            compareVal = this.tags.size() - other.getTags().size();
            if (compareVal == 0) {
                Iterator<Entry<String, String>> thisIterator = tags.entrySet().iterator();
                Iterator<Entry<String, String>> otherIterator = other.getTags().entrySet().iterator();
                while (thisIterator.hasNext() && otherIterator.hasNext()) {
                    Entry<String, String> thisEntry = thisIterator.next();
                    Entry<String, String> otherEntry = otherIterator.next();
                    compareVal = thisEntry.getKey().compareTo(otherEntry.getKey());
                    if (compareVal != 0) {
                        return compareVal;
                    }
                    else {
                        compareVal = thisEntry.getValue().compareTo(otherEntry.getValue());
                        if (compareVal != 0) {
                            return compareVal;
                        }
                    }
                }
            }
        }
        return compareVal;
    }
}
