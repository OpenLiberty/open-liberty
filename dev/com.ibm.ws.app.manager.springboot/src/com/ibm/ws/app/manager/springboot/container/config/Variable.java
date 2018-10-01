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

/**
 * {@code <variable>} element in server.xml
 */
public class Variable extends ConfigElement {

    public final static String XML_ATTRIBUTE_NAME_NAME = "name";
    private String name;

    public final static String XML_ATTRIBUTE_NAME_VALUE = "value";
    private String value;

    public Variable() {}

    public Variable(String name, String value) {
        setName(name);
        setValue(value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns a string representing this {@code <variable>} element
     *
     * @return String representing this {@code <variable>} element
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Variable{");
        if (getId() != null)
            buf.append("id=\"").append(getId()).append("\" ");
        if (name != null)
            buf.append("name=\"").append(name).append("\" ");
        if (value != null)
            buf.append("value=\"").append(value).append("\" ");
        buf.append("}");
        return buf.toString();
    }
}
