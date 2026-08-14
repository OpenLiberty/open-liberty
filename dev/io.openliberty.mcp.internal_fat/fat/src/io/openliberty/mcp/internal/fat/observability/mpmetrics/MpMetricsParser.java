/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.observability.mpmetrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for MicroProfile Metrics output in Prometheus text format.
 * Parses metric lines containing metric names, optional tags, and values.
 */
public class MpMetricsParser {

    private final List<MetricEntry> metrics;

    private MpMetricsParser(List<MetricEntry> metrics) {
        this.metrics = List.copyOf(metrics);
    }

    /**
     * Parses MicroProfile Metrics output and creates a parser instance.
     *
     * @param output the metrics output string in Prometheus text format, with one metric per line
     * @return a new MpMetricsParser instance containing all parsed metrics
     */
    public static MpMetricsParser parse(String output) {
        List<MetricEntry> metrics = new ArrayList<>();

        // split on each new line
        for (String rawLine : output.split("\\R")) {
            String line = rawLine.trim();
            // skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            MetricEntry metric = parseMetricLine(line);
            if (metric != null) {
                metrics.add(metric);
            }
        }

        return new MpMetricsParser(metrics);
    }

    /**
     * Filters a list of MetricEntry objects to return only those that match all expected tags.
     *
     * @param metrics the list of MetricEntry objects to filter
     * @param expectedTags a map of tag names to expected values that must all match
     * @return an immutable list containing only the MetricEntry objects that have all the expected tags
     */
    public static List<MetricEntry> getMetricsByTags(List<MetricEntry> metrics, Map<String, String> expectedTags) {
        List<MetricEntry> result = new ArrayList<>();
        for (MetricEntry metric : metrics) {
            if (metric.hasTags(expectedTags)) {
                result.add(metric);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Filters metrics by exact name match.
     *
     * @param metrics the list of MetricEntry objects to filter
     * @param metricName the exact metric name to match
     * @return an immutable list containing only the MetricEntry objects with the specified name
     */
    public static List<MetricEntry> filterMetricsByName(List<MetricEntry> metrics, String metricName) {
        List<MetricEntry> result = new ArrayList<>();
        for (MetricEntry metric : metrics) {
            if (metric.getName().equals(metricName)) {
                result.add(metric);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Filters metrics by partial name match.
     *
     * @param metrics the list of MetricEntry objects to filter
     * @param metricName the substring to search for in metric names
     * @return an immutable list of metrics whose names contain the specified substring
     */
    public static List<MetricEntry> filterMetricsByNameContains(List<MetricEntry> metrics, String metricName) {
        List<MetricEntry> result = new ArrayList<>();
        for (MetricEntry metric : metrics) {
            if (metric.getName().contains(metricName)) {
                result.add(metric);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Gets all parsed metrics from the MicroProfile Metrics output
     *
     * @return an immutable list of parsed metric entries
     */
    public List<MetricEntry> getMetrics() {
        return metrics;
    }

    /**
     * Parses a single metric line in Prometheus text format.
     * Expected format: metric_name{tag1="value1",tag2="value2"} numeric_value
     *
     * @param line the metric line to parse
     * @return a MetricEntry object, or null if the line cannot be parsed
     */
    private static MetricEntry parseMetricLine(String line) {
        int valueSeparator = line.lastIndexOf(' ');
        if (valueSeparator < 0) {
            return null;
        }

        String metricPart = line.substring(0, valueSeparator).trim();
        String valuePart = line.substring(valueSeparator + 1).trim();

        double value = Double.parseDouble(valuePart);

        String metricName;
        Map<String, String> tags = new LinkedHashMap<>();

        int openBrace = metricPart.indexOf('{');
        if (openBrace >= 0) {
            int closeBrace = metricPart.lastIndexOf('}');
            metricName = metricPart.substring(0, openBrace);
            String tagSection = metricPart.substring(openBrace + 1, closeBrace);
            tags.putAll(parseTags(tagSection));
        } else {
            metricName = metricPart;
        }

        return new MetricEntry(metricName, tags, value);
    }

    /**
     * Parses the tag section of a metric line.
     * Expected format: tag1="value1",tag2="value2"
     *
     * @param tagSection the tag section string (content between curly braces)
     * @return a map of tag names to tag values
     */
    private static Map<String, String> parseTags(String tagSection) {
        Map<String, String> tags = new LinkedHashMap<>();
        if (tagSection.isEmpty()) {
            return tags;
        }

        for (String pair : splitTagPairs(tagSection)) {
            if (pair.isBlank()) {
                continue;
            }
            int equalsIndex = pair.indexOf('=');
            if (equalsIndex < 0) {
                continue;
            }
            String key = pair.substring(0, equalsIndex);
            String value = pair.substring(equalsIndex + 1);
            tags.put(key, unquote(value));
        }
        return tags;
    }

    /**
     * Splits tag pairs by commas, respecting quoted values that may contain commas.
     *
     * @param tagSection the tag section string to split
     * @return a list of individual tag pair strings
     */
    private static List<String> splitTagPairs(String tagSection) {
        List<String> pairs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < tagSection.length(); i++) {
            char c = tagSection.charAt(i);
            if (c == '"' && (i == 0 || tagSection.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }

            if (c == ',' && !inQuotes) {
                pairs.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        if (!current.isEmpty()) {
            pairs.add(current.toString());
        }

        return pairs;
    }

    /**
     * Removes surrounding quotes from a tag value and unescapes internal quotes.
     *
     * @param value the potentially quoted string
     * @return the unquoted and unescaped string
     */
    private static String unquote(String value) {
        String result = value.trim();
        if (result.startsWith("\"") && result.endsWith("\"") && result.length() >= 2) {
            result = result.substring(1, result.length() - 1);
        }
        return result.replace("\\\"", "\"");
    }

    /**
     * Represents a single parsed metric entry with name, tags, and value.
     */
    public static final class MetricEntry {
        private final String name;
        private final Map<String, String> tags;
        private final double value;

        private MetricEntry(String name, Map<String, String> tags, double value) {
            this.name = name;
            this.tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
            this.value = value;
        }

        /**
         * Gets the metric name.
         *
         * @return the metric name
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the metric tags.
         *
         * @return an immutable map of tag names to tag values
         */
        public Map<String, String> getTags() {
            return tags;
        }

        /**
         * Gets the metric value.
         *
         * @return the numeric metric value
         */
        public double getValue() {
            return value;
        }

        /**
         * Checks if this metric has all the specified tags with matching values.
         *
         * @param expectedTags a map of tag names to expected values
         * @return true if all expected tags are present with matching values, false otherwise
         */
        public boolean hasTags(Map<String, String> expectedTags) {
            for (Map.Entry<String, String> entry : expectedTags.entrySet()) {
                if (!entry.getValue().equals(tags.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
    }
}
