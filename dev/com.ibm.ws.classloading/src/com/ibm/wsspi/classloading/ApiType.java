/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.classloading;

import java.util.EnumSet;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * An Api Type is a grouping of packages that can be accessed by applications.
 * The grouping represents a coarse-grained control structure for use with JEE.
 * It is not needed for OSGi artefacts since they have a finer-grained control
 * structure built in already.
 */

@Trivial
public enum ApiType {
    /** The standard APIs for any specifications supported by Liberty, e.g. javax.servlet.http */
    SPEC("spec"),
    /** Proprietary APIs provided by IBM features. */
    IBMAPI("ibm-api"),
    /** Proprietary APIs provided by non-IBM features. */
    API("api"),
    /** Proprietary APIs from third-party libraries which can be used in conjunction with features, e.g. the Wink APIs for use with Liberty's JAX-RS support. */
    THIRDPARTY("third-party"),
    /** Stable APIs -- These are third party APIs we have enough confidence in to expose by default, but are not fully spec APIs */
    STABLE("stable");

    // OSGi APIs are marked spec:api, which is obviously not in this enum.
    // This means that OSGi application API will not be visible to EE applications.

    private final String attributeName;

    private ApiType(String attributeName) {
        this.attributeName = attributeName;
    }

    public static ApiType fromString(String value) {
        if (value != null) {
            value = value.trim();
            for (ApiType t : ApiType.values()) {
                if (t.attributeName.equals(value)) {
                    return t;
                }
            }
        } else {
            // if no type is specified, default to "api"
            return ApiType.API;
        }
        return null;
    }

    /** Convert one or more comma-and-space-delimited api type strings into a single set of types */
    public static EnumSet<ApiType> createApiTypeSet(String... apiTypes) {
        EnumSet<ApiType> set = EnumSet.noneOf(ApiType.class);
        if (apiTypes != null)
            for (String types : apiTypes)
                if (types != null)
                    for (String stype : types.split("[ ,]+")) {
                        ApiType type = ApiType.fromString(stype);
                        if (type != null) {
                            set.add(type);
                        }
                    }
        return set;
    }

    @Override
    public String toString() {
        return attributeName;
    }
}
