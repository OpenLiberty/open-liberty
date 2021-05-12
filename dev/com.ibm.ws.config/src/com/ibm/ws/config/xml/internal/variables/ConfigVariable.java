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
package com.ibm.ws.config.xml.internal.variables;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.config.xml.internal.XMLConfigParser.MergeBehavior;

/**
 *
 */
public class ConfigVariable extends AbstractLibertyVariable {

    private final String name;
    private final String value;
    private final String defaultValue;
    private final MergeBehavior mergeBehavior;
    private final String location;
    private final boolean sensitive;

    public ConfigVariable(String name, @Sensitive String value, String variableDefault, MergeBehavior mb, String l, boolean isSensitive) {
        this.name = name;
        this.value = value;
        this.defaultValue = variableDefault;
        this.mergeBehavior = mb;
        this.location = l;
        this.sensitive = isSensitive;
    }

    @Override
    public String getName() {
        return name;
    }

    @Sensitive
    @Override
    public String getValue() {
        return value;
    }

    @Sensitive
    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    public MergeBehavior getMergeBehavior() {
        return this.mergeBehavior;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ConfigVariable[");
        builder.append("name=").append(name).append(", ");
        builder.append("value=").append(getObscuredValue()).append(", ");
        builder.append("defaultValue=").append(getObscuredDefaultValue()).append(", ");
        builder.append("source=").append(Source.XML_CONFIG);
        builder.append("]");
        return builder.toString();
    }

    public String getDocumentLocation() {
        return location;
    }

    @Override
    public boolean isSensitive() {
        return this.sensitive;
    }

    @Override
    public Source getSource() {
        return Source.XML_CONFIG;
    }
}
