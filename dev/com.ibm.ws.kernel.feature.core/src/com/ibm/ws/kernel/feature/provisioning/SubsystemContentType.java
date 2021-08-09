/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.provisioning;

import java.util.Locale;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Type values for the Subsystem-Content header in a feature manifest.
 * Specifies the type of content to be provisioned.
 * 
 * Notes from infocenter (subset):
 * osgi.bundle - This is the default value and indicates an OSGi bundle.
 * osgi.subsystem.feature - This value indicates that the feature should be provisioned.
 * These features need to use the name specified in Subsystem-SymbolicName.
 * 
 */
@Trivial
public enum SubsystemContentType {
    FEATURE_TYPE("osgi.subsystem.feature"),
    BUNDLE_TYPE("osgi.bundle"),
    JAR_TYPE("jar"),
    FILE_TYPE("file"),
    BOOT_JAR_TYPE("boot.jar"), // kernel use only
    CHECKSUM("checksum"),
    UNKNOWN("?");

    private final String value;

    SubsystemContentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "type=" + value;
    }

    public static SubsystemContentType fromString(String s) {
        if (s == null || s.isEmpty())
            return BUNDLE_TYPE;

        String compare = s.trim().toLowerCase(Locale.ENGLISH);
        for (SubsystemContentType t : SubsystemContentType.values()) {
            if (compare.equals(t.getValue()))
                return t;
        }

        return UNKNOWN;
    }

}