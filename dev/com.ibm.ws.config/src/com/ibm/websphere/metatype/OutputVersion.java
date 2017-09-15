/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.metatype;

/**
 *
 */
public enum OutputVersion {
    v1("1"), v2("2");

    private String value;

    private OutputVersion(String val) {
        value = val;
    }

    @Override
    public String toString() {
        return value;
    }

    public static OutputVersion getEnum(String value) {
        if (value == null || value.length() == 0) {
            return v1; //default to v1 if not specified
        }

        for (OutputVersion v : values()) {
            if (v.value.equals(value)) {
                return v;
            }
        }

        throw new IllegalArgumentException(value);
    }
}