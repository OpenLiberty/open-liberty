/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.interfaces;

import java.lang.reflect.Type;

/**
 * A value and the id of its source
 */
public class SourcedPropertyValue {

    private final Object value;
    private final Type type;
    private final String source;
    private final String tostring;

    public SourcedPropertyValue(Object value, Type type, String source) {
        this.value = value;
        this.type = type;
        this.source = source;
        this.tostring = value + "(" + source + ")";
    }

    /**
     * Get the actual value
     *
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Get the type of the value
     * 
     * @return
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the ID of the source that provided the value
     *
     * @return the originating source ID
     */
    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return tostring;
    }
}
