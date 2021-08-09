/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BaseDefinition {

    private final String id;
    private String name;
    private String description;
    private final Map<String, Map<String, String>> extensions;

    public BaseDefinition(String id) {
        this.id = id;
        this.extensions = new HashMap<String, Map<String, String>>();
    }

    @Override
    public String toString() {
        return super.toString() + '[' + name + ']';
    }

    public String getID() {
        return id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setExtensionAttribute(String schema, String name, String value) {
        Map<String, String> map = extensions.get(schema);
        if (map == null) {
            map = new HashMap<String, String>();
            extensions.put(schema, map);
        }
        map.put(name, value);
    }

    public String removeExtensionAttribute(String schema, String name) {
        Map<String, String> map = extensions.get(schema);
        return (map != null) ? map.remove(name) : null;
    }

    public Map<String, String> getExtensionAttributes(String schema) {
        return extensions.get(schema);
    }

    public String getExtensionAttribute(String schema, String name) {
        Map<String, String> attributes = extensions.get(schema);
        return (attributes != null) ? attributes.get(name) : null;
    }

    public Set<String> getExtensionUris() {
        return extensions.keySet();
    }

}
