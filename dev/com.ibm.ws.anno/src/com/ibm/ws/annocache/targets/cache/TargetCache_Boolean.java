/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.cache;

/**
 * Wrapper for a boolean value.
 *
 * Not thread safe.
 */
public class TargetCache_Boolean {
    public static final boolean DEFAULT_VALUE = false;

    public TargetCache_Boolean() {
        this(DEFAULT_VALUE);
    }

    public TargetCache_Boolean(boolean value) {
        this.value = value;
    }

    //

    private boolean value;

    public boolean getValue() {
        return value;
    }

    public boolean consumeValue() {
        boolean oldValue = value;
        value = DEFAULT_VALUE;
        return oldValue;
    }

    public boolean setValue(boolean newValue) {
        boolean oldValue = value;
        value = newValue;
        return oldValue;
    }
}
