/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.cmdline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tools for reading the manifest of the command line jar
 */
public class LaunchManifest {

    static final class RequiredBundle
    {
        private final String symbolicName;
        private final Map<String, String> attributes;

        public RequiredBundle(String name, Map<String, String> attributeMap)
        {
            symbolicName = name;
            attributes = attributeMap;
        }

        public String getSymbolicName()
        {
            return symbolicName;
        }

        public String getAttribute(String name)
        {
            return attributes.get(name);
        }

        @Override
        public String toString()
        {
            return symbolicName + ":" + attributes;
        }
    }

    static List<RequiredBundle> parseRequireBundle(String header)
    {
        header = header + ",";
        List<RequiredBundle> bundles = new ArrayList<RequiredBundle>();
        List<String> symbolicNames = new ArrayList<String>();
        List<String> attributes = new ArrayList<String>();
        boolean quoted = false;
        boolean symbolicNameAllowed = true;
        int strLen = header.length();
        for (int i = 0, pos = 0; i < strLen; i++) {
            char c = header.charAt(i);
            if (c == ';') {
                String str = header.substring(pos, i);
                pos = i + 1;
                if (str.contains(":=")) {
                    symbolicNameAllowed = false;
                } else if (str.contains("=")) {
                    attributes.add(str);
                    symbolicNameAllowed = false;
                } else if (symbolicNameAllowed) {
                    symbolicNames.add(str);
                }
            } else if (c == ',' && !!!quoted) {
                String str = header.substring(pos, i);
                pos = i + 1;
                if (str.contains(":=")) {
                    symbolicNameAllowed = false;
                } else if (str.contains("=")) {
                    attributes.add(str);
                    symbolicNameAllowed = false;
                } else if (symbolicNameAllowed) {
                    symbolicNames.add(str);
                }

                symbolicNameAllowed = true;
                quoted = false;

                Map<String, String> attributeMap = new HashMap<String, String>();
                for (String attrib : attributes) {
                    int index = attrib.indexOf('=');
                    String key = attrib.substring(0, index).trim();
                    String value = attrib.substring(index + 1).trim();
                    if (value.charAt(0) == '\"') {
                        value = value.substring(1, value.length() - 1);
                    }
                    attributeMap.put(key, value);
                }

                for (String name : symbolicNames) {
                    bundles.add(new RequiredBundle(name.trim(), attributeMap));
                }
                symbolicNames.clear();
                attributes.clear();
            } else if (c == '\"') {
                quoted = !quoted;
            }
        }

        return bundles;
    }

    static List<String> parseHeaderList(String header) {
        List<String> headerList = new ArrayList<String>();
        if (null != header) {
            Collections.addAll(headerList, header.split(","));
        }

        return headerList;
    }
}
