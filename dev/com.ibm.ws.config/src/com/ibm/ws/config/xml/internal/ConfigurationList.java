/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.admin.ConfigID;

/**
 * This class is entirely private to BaseConfiguration
 */
@Trivial
class ConfigurationList<T extends ConfigElement> {

    private final List<T> configElements;
    private Boolean hasId;

    public ConfigurationList() {
        configElements = new ArrayList<T>();
        hasId = null;
    }

    /**
     * @param configElement
     */
    public void add(T configElement) {
        configElements.add(configElement);
        hasId = null;
    }

    public void add(ConfigurationList<T> value) {
        configElements.addAll(value.configElements);
        hasId = null;
    }

    public void remove(ConfigurationList<T> value) {
        configElements.removeAll(value.configElements);
        hasId = null;
    }

    public boolean remove(String id) {
        boolean removed = false;
        if (id == null) {
            removed = (configElements.size() > 0);
            configElements.clear();
        } else {
            Iterator<T> iter = configElements.iterator();
            while (iter.hasNext()) {
                T element = iter.next();
                if (id.equals(element.getId())) {
                    iter.remove();
                    removed = true;
                }
            }
        }

        hasId = null;
        return removed;
    }

    public boolean isEmpty() {
        return configElements.isEmpty();
    }

    public boolean hasId() {
        // if one "id" attribute is found, assume all elements represent factory instances
        if (hasId == null) {
            hasId = hasElementWithId();
        }
        return hasId;
    }

    private boolean hasElementWithId() {
        for (ConfigElement configElement : configElements) {
            if (configElement.getId() != null) {
                return true;
            }
        }
        return false;
    }

    private static String generateId(int index) {
        return "default-" + index;
    }

    /**
     * Collects elements into Lists based on their ID. If an ID is not specified, the defaultId will be used.
     * If the defaultId is null, an id will be generated.
     * 
     * @param map
     * @param defaultId
     * @return
     */
    public Map<ConfigID, List<T>> collectElementsById(Map<ConfigID, List<T>> map, String defaultId, String pid) {
        if (map == null) {
            map = new HashMap<ConfigID, List<T>>();
        }
        int index = 0;
        for (T configElement : configElements) {
            String id = configElement.getId();

            if (id == null) {
                if (defaultId != null) {
                    id = defaultId;
                } else {
                    id = generateId(index++);
                }
            }

            // Create a new config ID based on the old one, but using the generated ID if necessary
            ConfigID configID = configElement.getConfigID();
            configID = new ConfigID(configID.getParent(), pid, id, configID.getChildAttribute());

            List<T> elements = map.get(configID);
            if (elements == null) {
                elements = new ArrayList<T>();
                map.put(configID, elements);
            }
            elements.add(configElement);
        }
        return map;
    }

    public List<T> collectElementsWithId(String id, List<T> elements) {
        if (elements == null) {
            elements = new ArrayList<T>();
        }
        int index = 0;
        for (T configElement : configElements) {
            String elementId = configElement.getId();
            if (elementId == null) {
                elementId = generateId(index++);
            }
            if (elementId.equals(id)) {
                elements.add(configElement);
            }
        }
        return elements;
    }

    public List<T> collectElements(List<T> elements) {
        if (elements == null) {
            elements = new ArrayList<T>();
        }
        elements.addAll(configElements);
        return elements;
    }

}
