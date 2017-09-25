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
package com.ibm.ws.microprofile.config.archaius.impl;

import com.ibm.ws.microprofile.config.interfaces.SourcedValue;

/**
 * A value and the id of its source
 */
public class CachedCompositeValue<T> implements SourcedValue<T> {

    private final T value;
    private final String source;
    private final String tostring;

    public CachedCompositeValue(T value, String source) {
        this.value = value;
        this.source = source;
        this.tostring = value + "(" + source + ")";
    }

    @Override
    public T getValue() {
        return value;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return tostring;
    }
}
