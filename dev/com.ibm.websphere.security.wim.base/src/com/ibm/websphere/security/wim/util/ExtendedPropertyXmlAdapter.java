/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.ibm.websphere.security.wim.util.ExtendedPropertyXmlAdapter.MapElement;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.PersonAccount;

/**
 * An {@link XmlAdapter} to marshal and unmarshal the extended property maps for {@link PersonAccount} and {@link Group}.
 * This class will handle a Map of non-container value as well as a Map containing List values.
 */
public class ExtendedPropertyXmlAdapter extends XmlAdapter<MapElement[], Map<String, Object>> {

    @Override
    public Map<String, Object> unmarshal(MapElement[] elements) throws Exception {
        /*
         * Don't unmarshal empty array of MapElements.
         */
        if (elements == null || elements.length == 0) {
            return null;
        }

        Map<String, Object> r = new HashMap<String, Object>();
        for (MapElement element : elements) {
            if (element.values != null) {
                r.put(element.key, element.values);
            } else {
                r.put(element.key, element.value);
            }
        }
        return r;
    }

    @Override
    public MapElement[] marshal(Map<String, Object> value) throws Exception {
        /*
         * Don't marshal empty map.
         */
        if (value == null || value.isEmpty()) {
            return null;
        }

        MapElement[] mapElements = new MapElement[value.size()];
        int i = 0;
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            if (entry.getValue() instanceof List) {
                mapElements[i++] = new MapElement(entry.getKey(), null, (List<?>) entry.getValue());
            } else {
                mapElements[i++] = new MapElement(entry.getKey(), entry.getValue(), null);
            }
        }

        return mapElements;
    }

    public static class MapElement {
        public String key;

        public Object value;

        public List<?> values;

        private MapElement() {}

        private MapElement(String key, Object value, List<?> values) {
            this.key = key;
            this.value = value;
            this.values = values;
        }
    }
}
