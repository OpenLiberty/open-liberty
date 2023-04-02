/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.websphere.metatype;

/**
 *
 */
public enum SchemaVersion {

    // If new versions are added, for example V2, be consistent and make it V2_0("2.0").  The version
    // representation here is, unfortunately, not consistent with the "OutputVersion", but we can't do
    // anything about that now.  Note getNormalizedVersion() adds ".0" when the version does not
    // contain a ".".  This is to make, for example, v1 = v1_0, or "1" = "1.0".
    v1_0("1.0"), v1_1("1.1");

    private String value;

    private SchemaVersion(String version) {
        value = getNormalizedVersion(version);
    }

    @Override
    public String toString() {
        return value;
    }

    public static SchemaVersion getEnum(String version) {

        String normalizedVersion = getNormalizedVersion(version);

        for (SchemaVersion v : values()) {
            if (v.value.equals(normalizedVersion)) {
                return v;
            }
        }

        throw new IllegalArgumentException(version);
    }

    public static boolean isValid(String version) {

        try {
            getEnum(version);
        } catch (IllegalArgumentException iae) {
            return false;
        }
        return true;
    }

    /**
     * Strips spaces and ".0" from version parameter.
     * Default to version "1" if version parameter is null or "";
     */
    public static String getNormalizedVersion(String version) {

        if (version == null) {
            version = "1.0";
        } else {
            version = version.trim();
            if (version.length() == 0) {
                version = "1.0";
            }
        }

        if (!version.contains(".")) {
            return version.concat(".0");
        }
        return version;
    }
}
