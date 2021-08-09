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
package com.ibm.ws.app.manager.springboot.container.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

/**
 * A generic server configuration element. Most entries in server.xml can safely
 * extend this class.
 */
public class ConfigElement implements Cloneable {

    private Map<QName, Object> extraAttributes;

    @SuppressWarnings("unused")
    private List<Element> extraElements;

    public static String XML_ATTRIBUTE_NAME_ID = "id";
    private String id;

    /**
     * @return the unique identifier of this configuration element
     */
    public String getId() {
        return this.id;
    }

    /**
     * @param id the unique identifier of this configuration element
     */
    public void setId(String id) {
        this.id = getValue(id);
    }

    /**
     * Sets an attribute on this element that does not have an explicit setter.
     *
     * @param name  the name
     * @param value the value
     */
    public void setExtraAttribute(String name, String value) {
        if (extraAttributes == null) {
            extraAttributes = new HashMap<QName, Object>();
        }
        if (value == null) {
            extraAttributes.remove(new QName(null, name));
        } else {
            extraAttributes.put(new QName(null, name), value);
        }
    }

    /**
     * Sometimes, the JACL representation of a null or empty value includes
     * quotation marks. Calling this method will parse away the extra JACL
     * syntax and return a real or null value
     *
     * @param value
     *                  an unchecked input value
     * @return the real value described by the input value
     */
    public static String getValue(String value) {
        if (value == null) {
            return null;
        }
        String v = removeQuotes(value.trim()).trim();
        if (v.isEmpty()) {
            return null;
        }
        return v;
    }

    /**
     * Removes leading and trailing quotes from the input argument, if they
     * exist.
     *
     * @param arg
     *                The argument you want to parse
     * @return The argument, without leading and trailing quotes
     */
    public static String removeQuotes(String arg) {
        if (arg == null) {
            return null;
        }
        int length = arg.length();
        if (length > 1 && arg.startsWith("\"") && arg.endsWith("\"")) {
            return arg.substring(1, length - 1);
        }
        return arg;
    }

    /**
     * Uses the server configuration to resolve a variable into a string.
     * For example, with this in server config: {@code <variable name="key" value="val"/>}
     *
     * Calling <code> expandVariable(config, "${key}"); </code> would return "val"
     *
     * @param config The server configuration
     * @param value  The raw value of a variable with brackets
     * @return The value of the expanded string.
     */
    public static String expand(ServerConfiguration config, String value) {
        if (value == null)
            return null;

        if (!value.startsWith("${") || !value.endsWith("}"))
            return value;

        String variableName = value.substring(2, value.length() - 1);
        Variable var = config.getVariables().getBy("name", variableName);
        return var.getValue();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        Class<?> clazz = this.getClass();
        StringBuilder buf = new StringBuilder(clazz.getSimpleName()).append('{');
        buf.append("id=\"" + (getId() == null ? "" : getId()) + "\" ");
        buf.append("}");
        return buf.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ConfigElement))
            return false;
        return this.toString().equals(o.toString());
    }
}
