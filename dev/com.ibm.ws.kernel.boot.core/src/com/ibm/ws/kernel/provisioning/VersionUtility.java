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
package com.ibm.ws.kernel.provisioning;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 *
 */
public class VersionUtility {
    public static final Version VERSION_1_0 = new Version("1.0.0");

    // These are annoying to label correctly, which is why they are private.
    // The first set of ranges we look for will tend to look the same.
    // When API versions start bumping, we may get more: we don't want an infinite list here, 
    // we only want the ones that show up in a heap analysis because we use them most of the time...
    private static final VersionRange EMPTY_RANGE = new VersionRange("0.0.0");
    private static final VersionRange INITIAL_RANGE = new VersionRange("[1.0.0,1.0.100)");

    /**
     * Convert a string into a Version, reusing common Version
     * objects if we can.
     * 
     * @param str String to convert to a Version
     * @return Version
     */
    public static Version stringToVersion(String str) {
        if (str == null || str.isEmpty() || str.equals("0")) {
            return Version.emptyVersion;
        }

        if (str.equals("1") || str.equals("1.0") || str.equals("1.0.0"))
            return VERSION_1_0;

        return new Version(str);
    }

    /**
     * Convert a string into a VersionRange, reusing common VersionRange
     * objects if we can.
     * 
     * @param str String to convert to a VersionRange
     * @return VersionRange
     */
    public static final VersionRange stringToVersionRange(String str) {
        if (str == null || str.isEmpty() || "0".equals(str))
            return EMPTY_RANGE;

        if ("[1,1.0.100)".equals(str) || "[1.0,1.0.100)".equals(str) || "[1.0.0,1.0.100)".equals(str))
            return INITIAL_RANGE;

        return new VersionRange(str);
    }
}
