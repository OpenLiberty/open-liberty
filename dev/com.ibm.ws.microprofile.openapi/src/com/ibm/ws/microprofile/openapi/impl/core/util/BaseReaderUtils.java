/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl.core.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;

/**
 * The <code>BaseReaderUtils</code> class is utility class which helps read annotations to the OpenApi.
 */
public final class BaseReaderUtils {

    private BaseReaderUtils() {

    }

    /**
     * Collects extensions.
     *
     * @param extensions is an array of extensions
     * @return the map with extensions
     */
    public static Map<String, Object> parseExtensions(Extension[] extensions) {
        final Map<String, Object> map = new HashMap<String, Object>();
        for (Extension extension : extensions) {
            final String name = extension.name();
            final String key = name.length() > 0 ? StringUtils.prependIfMissing(name, "x-") : name;

            /*
             * for (ExtensionProperty property : extension.properties()) {
             * final String propertyName = property.name();
             * final String propertyValue = property.value();
             * if (StringUtils.isNotBlank(propertyName) && StringUtils.isNotBlank(propertyValue)) {
             * if (key.isEmpty()) {
             * map.put(StringUtils.prependIfMissing(propertyName, "x-"), propertyValue);
             * } else {
             * Object value = map.get(key);
             * if (value == null || !(value instanceof Map)) {
             * value = new HashMap<String, Object>();
             * map.put(key, value);
             * }
             *
             * @SuppressWarnings("unchecked")
             * final Map<String, Object> mapValue = (Map<String, Object>) value;
             * mapValue.put(propertyName, propertyValue);
             * }
             * }
             * }
             */
        }

        return map;
    }
}
