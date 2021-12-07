/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * An <code>SpiType</code> is a grouping of SPI packages that can be accessed directly 
 * by libraries, only. The grouping represents a coarse-grained control structure for 
 * use by feature service providers and basic server extensions (BELLs).
 */

@Trivial
public enum SpiType {
    /** SPI provided by features */
    SPI("spi");

    private static final TraceComponent tc = Tr.register(SpiType.class,"ClassLoadingService", "com.ibm.ws.classloading.internal.resources.ClassLoadingServiceMessages");

    private final String attributeName;

    private SpiType(String attributeName) {
        this.attributeName = attributeName;
    }

    public static SpiType fromString(String value) {
        if (value != null) {
            value = value.trim();
            for (SpiType t : SpiType.values()) {
                if (t.attributeName.equals(value)) {
                    return t;
                }
            }
        }
        // Return null for invalid values rather than IllegalArgumentException
        return null;
    }

    /** Convert spi type string into a single set of type */
    public static EnumSet<SpiType> createSpiTypeSet(final String spiType) {
        EnumSet<SpiType> result = EnumSet.noneOf(SpiType.class);
        StringBuffer initialtypes = new StringBuffer();
        if (spiType != null
                && !"".equals(spiType)) {  // Empty string is a valid configuration
            initialtypes.append("\"" + spiType + "\"");
            SpiType type = SpiType.fromString(spiType);
            if (type != null) {
                result.add(type);
            } else {
                if (tc.isErrorEnabled()) {
                    Tr.error(tc, "cls.library.config.typo", spiType, initialtypes, EnumSet.allOf(SpiType.class));
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return attributeName;
    }
}
