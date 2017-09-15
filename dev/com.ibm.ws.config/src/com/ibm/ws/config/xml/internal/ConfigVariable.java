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

import com.ibm.ws.config.xml.internal.XMLConfigParser.MergeBehavior;

/**
 *
 */
class ConfigVariable {

    private final String name;
    private final String value;
    private final MergeBehavior mergeBehavior;
    private final String location;

    public ConfigVariable(String name, String value, MergeBehavior mb, String l) {
        this.name = name;
        this.value = value;
        this.mergeBehavior = mb;
        this.location = l;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public MergeBehavior getMergeBehavior() {
        return this.mergeBehavior;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ConfigVariable[");
        builder.append("name=").append(name).append(", ");
        builder.append("value=").append(value);
        builder.append("]");
        return builder.toString();
    }

    /**
     * @return
     */
    public String getDocumentLocation() {
        return location;
    }
}
