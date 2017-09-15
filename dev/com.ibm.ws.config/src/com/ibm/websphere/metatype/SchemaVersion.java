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
public enum SchemaVersion {
    v1_0("1.0"), v1_1("1.1");

    private String value;

    private SchemaVersion(String val) {
        value = val;
    }

    @Override
    public String toString() {
        return value;
    }

    public static SchemaVersion getEnum(String value) {
        if (value == null || value.length() == 0) {
            return v1_0; //default to v1 if not specified
        }

        for (SchemaVersion v : values()) {
            if (v.value.equals(value)) {
                return v;
            }
        }

        throw new IllegalArgumentException(value);
    }
}
