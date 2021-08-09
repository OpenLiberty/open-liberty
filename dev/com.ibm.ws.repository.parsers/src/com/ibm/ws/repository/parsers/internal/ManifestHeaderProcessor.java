/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Temporarily copied from aries in order to fix parseFilterList.
 * A patch has been submitted to aries to get this fixed properly. This
 * file should be deleted when possible. See RTC work item:
 * 
 * https://wasrtc.hursley.ibm.com:9443/jazz/resource/itemName/com.ibm.team.workitem.WorkItem/148418
 * 
 */

package com.ibm.ws.repository.parsers.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Traceable;

import org.apache.aries.util.ManifestHeaderUtils;
import org.apache.aries.util.VersionRange;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class ManifestHeaderProcessor
{
    public static final String NESTED_FILTER_ATTRIBUTE = "org.apache.aries.application.filter.attribute";
    private static final Pattern FILTER_ATTR = Pattern.compile("(\\(!)?\\((.*?)([<>]?=)(.*?)\\)\\)?");
    private static final String LESS_EQ_OP = "<=";
    private static final String GREATER_EQ_OP = ">=";
    private static final String EQ_OP = "=";

    /**
     * A GenericMetadata is either a Generic Capability or a Generic Requirement
     */
    public static class GenericMetadata {
        private final String namespace;
        private final Map<String, Object> attributes = new HashMap<String, Object>();
        private final Map<String, String> directives = new HashMap<String, String>();

        public GenericMetadata(String namespace) {
            this.namespace = namespace;
        }

        public String getNamespace() {
            return namespace;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public Map<String, String> getDirectives() {
            return directives;
        }
    }

    /**
     * A simple class to associate two types.
     */
    public static class NameValuePair {
        private String name;
        private Map<String, String> attributes;

        public NameValuePair(String name, Map<String, String> value)
        {
            this.name = name;
            this.attributes = value;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public Map<String, String> getAttributes()
        {
            return attributes;
        }

        public void setAttributes(Map<String, String> value)
        {
            this.attributes = value;
        }

        @Override
        public String toString() {
            return "{" + name.toString() + "::" + attributes.toString() + "}";
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final NameValuePair other = (NameValuePair) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (attributes == null) {
                if (other.attributes != null)
                    return false;
            } else if (!attributes.equals(other.attributes))
                return false;
            return true;
        }
    }

    /**
     * Intended to provide a standard way to add Name/Value's to
     * aggregations of Name/Value's.
     */
    public static interface NameValueCollection {
        /**
         * Add this Name & Value to the collection.
         * 
         * @param n
         * @param v
         */
        public void addToCollection(String n, Map<String, String> v);
    }

    /**
     * Map of Name -> Value.
     */
    public static class NameValueMap extends HashMap<String, Map<String, String>> implements NameValueCollection, Map<String, Map<String, String>> {
        private static final long serialVersionUID = -6446338858542599141L;

        @Override
        public void addToCollection(String n, Map<String, String> v) {
            this.put(n, v);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Map<String, String>> entry : this.entrySet()) {
                if (!first)
                    sb.append(",");
                first = false;
                sb.append(entry.getKey() + "->" + entry.getValue());
            }
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * List of Name/Value
     */
    public static class NameValueList extends ArrayList<NameValuePair> implements NameValueCollection, List<NameValuePair>, Traceable {
        private static final long serialVersionUID = 1808636823825029983L;

        @Override
        public void addToCollection(String n, Map<String, String> v) {
            this.add(new NameValuePair(n, v));
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("{");
            boolean first = true;
            for (NameValuePair nvp : this) {
                if (!first)
                    sb.append(",");
                first = false;
                sb.append(nvp.toString());
            }
            sb.append("}");
            return sb.toString();
        }

        @Override
        public String toTraceString() {
            return toString();
        }
    }

    /**
     * 
     * Splits a delimiter separated string, tolerating presence of non separator commas
     * within double quoted segments.
     * 
     * Eg.
     * com.ibm.ws.eba.helloWorldService;version="[1.0.0, 1.0.0]" &
     * com.ibm.ws.eba.helloWorldService;version="1.0.0"
     * com.ibm.ws.eba.helloWorld;version="2";bundle-version="[2,30)"
     * com.acme.foo;weirdAttr="one;two;three";weirdDir:="1;2;3"
     * 
     * @param value the value to be split
     * @param delimiter the delimiter string such as ',' etc.
     * @return List<String> the components of the split String in a list
     */
    public static List<String> split(String value, String delimiter)
    {
        return ManifestHeaderUtils.split(value, delimiter);
    }

    /**
     * Internal method to parse headers with the format<p>
     * [Name](;[Name])*(;[attribute-name]=[attribute-value])*<br>
     * Eg.<br>
     * rumplestiltskin;thing=value;other=something<br>
     * littleredridinghood
     * bundle1;bundle2;other=things
     * bundle1;bundle2
     * 
     * @param s data to parse
     * @return a list of NameValuePair, with the Name being the name component,
     *         and the Value being a NameValueMap of key->value mappings.
     */
    private static List<NameValuePair> genericNameWithNameValuePairProcess(String s) {
        String name;
        Map<String, String> params = null;
        List<NameValuePair> nameValues = new ArrayList<NameValuePair>();
        List<String> pkgs = new ArrayList<String>();
        int index = s.indexOf(";");
        if (index == -1) {
            name = s;
            params = new HashMap<String, String>();
            pkgs.add(name);
        } else {
            name = s.substring(0, index).trim();
            String tail = s.substring(index + 1).trim();

            pkgs.add(name); // add the first package
            StringBuilder parameters = new StringBuilder();

            // take into consideration of multiple packages separated by ';'
            // while they share the same attributes or directives
            List<String> tailParts = split(tail, ";");
            boolean firstParameter = false;

            for (String part : tailParts) {
                // if it is not a parameter and no parameter appears in front of it, it must a package
                if (!!!(part.contains("="))) {
                    // Need to make sure no parameter appears before the package, otherwise ignore this string
                    // as this syntax is invalid
                    if (!!!(firstParameter))
                        pkgs.add(part);
                } else {
                    if (!!!(firstParameter))
                        firstParameter = true;

                    parameters.append(part + ";");
                }
            }

            if (parameters.length() != 0) {
                //remove the final ';' if there is one
                if (parameters.toString().endsWith(";")) {

                    parameters = parameters.deleteCharAt(parameters.length() - 1);
                }

                params = genericNameValueProcess(parameters.toString());
            }

        }
        for (String pkg : pkgs) {
            nameValues.add(new NameValuePair(pkg, params));
        }

        return nameValues;

    }

    /**
     * Internal method to parse headers with the format<p>
     * [attribute-name]=[attribute-value](;[attribute-name]=[attribute-value])*<br>
     * Eg.<br>
     * thing=value;other=something<br>
     * <p>
     * Note. Directives (name:=value) are represented in the map with name suffixed by ':'
     * 
     * @param s data to parse
     * @return a NameValueMap, with attribute-name -> attribute-value.
     */
    private static Map<String, String> genericNameValueProcess(String s) {
        Map<String, String> params = new HashMap<String, String>();
        List<String> parameters = split(s, ";");
        for (String parameter : parameters) {
            List<String> parts = split(parameter, "=");
            // do a check, otherwise we might get NPE
            if (parts.size() == 2) {
                String second = parts.get(1).trim();
                if (second.startsWith("\"") && second.endsWith("\""))
                    second = second.substring(1, second.length() - 1);

                String first = parts.get(0).trim();

                // make sure for directives we clear out any space as in "directive  :=value"
                if (first.endsWith(":")) {
                    first = first.substring(0, first.length() - 1).trim() + ":";
                }

                params.put(first, second);
            }
        }

        return params;
    }

    /**
     * Parse a generic capability header. For example<br/>
     * com.acme.myns;mylist:List<String>="nl,be,fr,uk";myver:Version=1.3;long:Long="1234";d:Double="3.14";myattr=xyz,
     * com.acme.myns;myattr=abc
     * 
     * @param s The header to be parsed
     * @return A list of GenericMetadata objects each representing an individual capability. The values in the attribute map
     *         are of the specified datatype.
     */
    public static List<GenericMetadata> parseRequirementString(String s) {
        return parseGenericMetadata(s);
    }

    private static List<GenericMetadata> parseGenericMetadata(String s) {
        List<GenericMetadata> capabilities = new ArrayList<GenericMetadata>();

        List<String> entries = split(s, ",");
        for (String e : entries) {
            List<NameValuePair> nvpList = genericNameWithNameValuePairProcess(e);

            for (NameValuePair nvp : nvpList) {
                String namespace = nvp.getName();
                GenericMetadata cap = new GenericMetadata(namespace);
                capabilities.add(cap);

                Map<String, String> attrMap = nvp.getAttributes();
                for (Map.Entry<String, String> entry : attrMap.entrySet()) {
                    String k = entry.getKey();
                    String v = entry.getValue();
                    if (k.contains(":")) {
                        if (k.endsWith(":")) {
                            // a directive
                            cap.getDirectives().put(k.substring(0, k.length() - 1), v);
                        } else {
                            // an attribute with its datatype specified
                            parseTypedAttribute(k, v, cap);
                        }
                    } else {
                        // ordinary (String) attribute
                        cap.getAttributes().put(k, v);
                    }
                }
            }
        }

        return capabilities;
    }

    private static void parseTypedAttribute(String k, String v, GenericMetadata cap) {
        int idx = k.indexOf(':');
        String name = k.substring(0, idx);
        String type = k.substring(idx + 1);

        if (type.startsWith("List<") && type.endsWith(">")) {
            String subtype = type.substring("List<".length(), type.length() - 1).trim();
            List<Object> l = new ArrayList<Object>();
            for (String s : v.split(",")) {
                l.add(getTypedValue(k, subtype, s));
            }
            cap.getAttributes().put(name, l);
        } else {
            cap.getAttributes().put(name, getTypedValue(k, type.trim(), v));
        }
    }

    private static Object getTypedValue(String k, String type, String v) {
        if ("String".equals(type)) {
            return v;
        } else if ("Long".equals(type)) {
            return Long.parseLong(v);
        } else if ("Double".equals(type)) {
            return Double.parseDouble(v);
        } else if ("Version".equals(type)) {
            return Version.parseVersion(v);
        }
        throw new IllegalArgumentException(k + "=" + v);
    }

    /**
     * Parse a version range..
     * 
     * @param s
     * @return VersionRange object.
     * @throws IllegalArgumentException if the String could not be parsed as a VersionRange
     */
    public static VersionRange parseVersionRange(String s) throws IllegalArgumentException {
        return new VersionRange(s);
    }

    /**
     * Parse a version range and indicate if the version is an exact version
     * 
     * @param s
     * @param exactVersion
     * @return VersionRange object.
     * @throws IllegalArgumentException if the String could not be parsed as a VersionRange
     */
    public static VersionRange parseVersionRange(String s, boolean exactVersion) throws IllegalArgumentException {
        return new VersionRange(s, exactVersion);
    }

    private static Map<String, String> parseFilterList(String filter) {

        Map<String, String> result = new HashMap<String, String>();
        Set<String> negatedVersions = new HashSet<String>();
        Set<String> negatedBundleVersions = new HashSet<String>();

        String lowerVersion = null;
        String upperVersion = null;
        String lowerBundleVersion = null;
        String upperBundleVersion = null;

        Matcher m = FILTER_ATTR.matcher(filter);
        while (m.find()) {
            boolean negation = m.group(1) != null;
            String attr = m.group(2);
            String op = m.group(3);
            String value = m.group(4);

            if (Constants.VERSION_ATTRIBUTE.equals(attr)) {
                if (negation) {
                    negatedVersions.add(value);
                } else {
                    if (GREATER_EQ_OP.equals(op)) {
                        lowerVersion = value;
                    } else if (LESS_EQ_OP.equals(op)) {
                        upperVersion = value;
                    } else if (EQ_OP.equals(op)) {
                        lowerVersion = value;
                        upperVersion = value;
                    } else {
                        throw new IllegalArgumentException();
                    }
                }
            } else if (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(attr)) {
                // bundle-version is like version, but may be specified at the
                // same time
                // therefore we have similar code with separate variables
                if (negation) {
                    negatedBundleVersions.add(value);
                } else {
                    if (GREATER_EQ_OP.equals(op))
                        lowerBundleVersion = value;
                    else if (LESS_EQ_OP.equals(op))
                        upperBundleVersion = value;
                    else
                        throw new IllegalArgumentException();
                }
            } else {
                result.put(attr, value);
            }
        }

        if (lowerVersion != null) {
            StringBuilder versionAttr = new StringBuilder(lowerVersion);
            if (upperVersion != null) {
                versionAttr.append(",").append(upperVersion).insert(0,
                                                                    negatedVersions.contains(lowerVersion) ? '(' : '[').append(
                                                                                                                               negatedVersions.contains(upperVersion) ? ')' : ']');
            }

            result.put(Constants.VERSION_ATTRIBUTE, versionAttr.toString());
        }
        // Do it again for bundle-version
        if (lowerBundleVersion != null) {
            StringBuilder versionAttr = new StringBuilder(lowerBundleVersion);
            if (upperBundleVersion != null) {
                versionAttr.append(",").append(upperBundleVersion).insert(0,
                                                                          negatedBundleVersions.contains(lowerBundleVersion) ? '(' : '[')
                                .append(
                                        negatedBundleVersions.contains(upperBundleVersion) ? ')' : ']');
            }

            result.put(Constants.BUNDLE_VERSION_ATTRIBUTE, versionAttr.toString());
        }

        return result;
    }

    public static Map<String, String> parseFilter(String filter)
    {
        Map<String, String> result;
        if (filter.startsWith("(&")) {
            result = parseFilterList(filter.substring(2, filter.length() - 1));
        } else {
            result = parseFilterList(filter);
        }
        return result;
    }
}
