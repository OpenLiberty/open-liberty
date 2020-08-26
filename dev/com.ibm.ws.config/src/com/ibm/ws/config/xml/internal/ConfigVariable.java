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

import java.util.regex.Pattern;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.config.xml.internal.XMLConfigParser.MergeBehavior;

/**
 *
 */
class ConfigVariable {

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

    public String getName() {
        return name;
    }

    @Sensitive
    public String getValue() {
        return value;
    }

    @Sensitive
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
        builder.append("value=").append(getObscuredValue(name, value));
        builder.append("]");
        return builder.toString();
    }

    final Pattern obscuredValuePattern = Pattern.compile("(\\{aes\\}|\\{xor\\}).*");
    final String OBSCURED_VALUE = "*****";
    final String ENCRYPTION_KEY = "wlp.password.encryption.key";

    protected String getObscuredValue(String name, @Sensitive Object o) {
        if (isSensitive())
            return OBSCURED_VALUE;

        if (ENCRYPTION_KEY.equals(name))
            return OBSCURED_VALUE;

        String value = String.valueOf(o);
        if (obscuredValuePattern.matcher(value).matches())
            return OBSCURED_VALUE;
        return value;
    }

    public String getDocumentLocation() {
        return location;
    }

    public boolean isSensitive() {
        return this.sensitive;
    }
}
