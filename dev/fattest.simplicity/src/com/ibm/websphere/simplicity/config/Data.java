/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Represents the <data> element in server.xml
 */
public class Data extends ConfigElement {

    // attributes

    private String createTables;
    private String dropTables;
    private String logValues;

    // nested elements

    @XmlElement(name = "logValues")
    private Set<String> logValuesList;

    public String getCreateTables() {
        return createTables;
    }

    public String getDropTables() {
        return dropTables;
    }

    public String getLogValues() {
        return logValues;
    }

    public Set<String> getLogValuesElements() {
        if (logValuesList == null) {
            logValuesList = new TreeSet<String>();
        }
        return logValuesList;
    }

    @XmlAttribute
    public void setCreateTables(String value) {
        createTables = value;
    }

    @XmlAttribute
    public void setDropTables(String value) {
        dropTables = value;
    }

    @XmlAttribute
    public void setLogValues(String value) {
        logValues = value;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        if (createTables != null)
            buf.append("createTables=").append(createTables).append(' ');
        if (dropTables != null)
            buf.append("dropTables=").append(dropTables).append(' ');
        if (logValues != null)
            buf.append("logValues=").append(logValues).append(' ');
        if (logValuesList != null)
            buf.append("logValues subelements: ").append(logValuesList).append(' ');
        buf.append('}');
        return buf.toString();
    }
}
