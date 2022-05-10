/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal.nester;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;

/**
 * This is the client side helper for flat configurations. It is not used in config itself.
 * To use it in your project containing a component with a flattened configuration, include
 * <classpathentry combineaccessrules="false" kind="src" path="/com.ibm.ws.config"/>
 * in the project's .classpath file and
 * Private-Package: \
 * com.ibm.ws.config.xml.internal.nester, \
 * as part of the Private-Package section of the project's bnd.bnd file.
 * 
 * Both methods nest the top level of the supplied map. If you have multiple levels of flattened configuration
 * you will need to call the appropriate methods at each level on the results from the previous level.
 */

/*
 * See also:
 *     open-liberty\dev\com.ibm.ws.javaee.ddmodel.wsbnd\src\
 *         com\ibm\ws\javaee\ddmodel\wsbnd\internal\
 *             NestingUtils.java
 *
 * Which implements the same operations. If any updates are made
 * here, parallel updates should be made to NestingUtils.
 */
public class Nester {

    /**
     * Extracts all sub configurations starting with a specified key
     *
     * @param key key prefix of interest
     * @param map flattened properties
     * @return list of extracted nested configurations for the key
     */
    public static List<Map<String, Object>> nest(String key, Map<String, Object> map) {
        return com.ibm.ws.config.xml.nester.Nester.nest(key, map);
    }

    /**
     * Extracts all sub configurations starting with a specified key
     *
     * @param key key prefix of interest
     * @param map flattened properties
     * @return list of extracted nested configurations for the key
     */
    public static List<Map<String, Object>> nest(String key, Dictionary<String, Object> map) {
        return com.ibm.ws.config.xml.nester.Nester.nest(key, map);
    }

    /**
     * Extracts all sub configurations starting with any of the specified keys
     *
     * @param map  flattened properties
     * @param keys keys of interest
     * @return map keyed by (supplied) key of lists of extracted nested configurations
     */
    public static Map<String, List<Map<String, Object>>> nest(Map<String, Object> map, String... keys) {
        return com.ibm.ws.config.xml.nester.Nester.nest(map, keys);
    }
}
