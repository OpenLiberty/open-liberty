/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a server configuration document for Open Liberty
 */
@XmlRootElement(name = "server")
public class OpenLibertyServerConfiguration extends ServerConfiguration {

    public OpenLibertyServerConfiguration() {
        super();
    }

    @XmlElement(name = "jmsActivationSpec")
    private ConfigElementList<JMSActivationSpec> jmsActivationSpecs;

    @Override
    public ConfigElementList<JMSActivationSpec> getJMSActivationSpecs() {
        if (this.jmsActivationSpecs == null)
            this.jmsActivationSpecs = new ConfigElementList<JMSActivationSpec>();
        return this.jmsActivationSpecs;
    }

    private List<Field> getAllXmlElements() {
        List<Field> xmlElements = new ArrayList<Field>();
        for (Field field : OpenLibertyServerConfiguration.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(XmlElement.class))
                xmlElements.add(field);
        }
        return xmlElements;
    }

    @SuppressWarnings("unchecked")
    @Override
    public OpenLibertyServerConfiguration clone() throws CloneNotSupportedException {
        OpenLibertyServerConfiguration clone = (OpenLibertyServerConfiguration) super.clone();

        for (Field field : getAllXmlElements()) {
            try {
                Object val = field.get(this);
                if (val instanceof ConfigElementList) {
                    field.set(clone, ((ConfigElementList<ConfigElement>) val).clone());
                } else if (val != null) {
                    field.set(clone, ((ConfigElement) val).clone());
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (RuntimeException e) {
                throw new RuntimeException("Error on field: " + field);
            }
        }
        return clone;
    }

    @Override
    public boolean equals(Object otherConfig) {
        if (!super.equals(otherConfig)) {
            return false;
        }

        // Consider server configurations equal if their XmlElements match up
        for (Field field : getAllXmlElements()) {
            try {
                Object thisVal = field.get(this);
                Object otherVal = field.get(otherConfig);
                if (!(thisVal == null ? otherVal == null : thisVal.equals(otherVal)))
                    return false;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    @Override
    protected void fieldsToString(StringBuilder builder) {
        super.fieldsToString(builder);
        String nl = System.getProperty("line.separator");
        for (Field field : getAllXmlElements()) {
            try {
                builder.append(field.get(this).toString());
                builder.append(nl);
            } catch (Exception ignore) {
            }
        }
    }
}
