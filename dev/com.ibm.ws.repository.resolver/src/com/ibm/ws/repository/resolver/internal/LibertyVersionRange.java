/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resolver.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>This represents a liberty version range in the form:</p>
 * <pre>version-range ::= interval | atleast
 * interval ::= ( '[' | '(' ) floor ',' ceiling ( ']' | ')' )
 * atleast ::= libertyVersion
 * floor ::= libertyVersion
 * ceiling ::= libertyVersion</pre>
 * <p>Note that although this syntax supports square and curved opening and closing brackets this is not recorded in the object so it is always assumed they are inclusive. (It
 * wasn't needed at the time of writing).
 */
public class LibertyVersionRange {

    private final LibertyVersion minVersion;
    private final LibertyVersion maxVersion;

    /*
     * Gets the "8.5.4.3" and "9.7.6.2" parts out of the forms:
     * [8.5.4.3,9.7.6.2]
     * (8.5.4.3,9.7.6.2)
     */
    private final static Pattern VERSION_RANGE_PATTERN = Pattern.compile("[\\[\\(]([^,]*),(.*)[\\]\\)]");

    /**
     * Attempts to parse the supplied version range string into a Liberty version range containing just numbers. If it fails to do so it will return null.
     * 
     * @param versionRangeString The String to parse
     * @return The {@link LibertyVersionRange} or <code>null</code> if the string is in the wrong form.
     */
    public static LibertyVersionRange valueOf(String versionRangeString) {
        if (versionRangeString == null) {
            return null;
        }
        Matcher versionRangeMatcher = VERSION_RANGE_PATTERN.matcher(versionRangeString);
        if (versionRangeMatcher.matches()) {
            // Have a min and max so parse both
            LibertyVersion minVersion = LibertyVersion.valueOf(versionRangeMatcher.group(1));
            LibertyVersion maxVersion = LibertyVersion.valueOf(versionRangeMatcher.group(2));

            // Make sure both were valid versions
            if (minVersion != null && maxVersion != null) {
                return new LibertyVersionRange(minVersion, maxVersion);
            } else {
                return null;
            }
        } else {
            // It's not a range so see if it's a single version
            LibertyVersion minVersion = LibertyVersion.valueOf(versionRangeString);
            if (minVersion != null) {
                return new LibertyVersionRange(minVersion, null);
            } else {
                return null;
            }
        }
    }

    /**
     * @param minVersion
     * @param maxVersion
     */
    public LibertyVersionRange(LibertyVersion minVersion, LibertyVersion maxVersion) {
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
    }

    /**
     * @return the minVersion
     */
    public LibertyVersion getMinVersion() {
        return minVersion;
    }

    /**
     * @return the maxVersion
     */
    public LibertyVersion getMaxVersion() {
        return maxVersion;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((maxVersion == null) ? 0 : maxVersion.hashCode());
        result = prime * result + ((minVersion == null) ? 0 : minVersion.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LibertyVersionRange other = (LibertyVersionRange) obj;
        if (maxVersion == null) {
            if (other.maxVersion != null)
                return false;
        } else if (!maxVersion.equals(other.maxVersion))
            return false;
        if (minVersion == null) {
            if (other.minVersion != null)
                return false;
        } else if (!minVersion.equals(other.minVersion))
            return false;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (maxVersion != null) {
            return "[" + minVersion.toString() + "," + maxVersion.toString() + "]";
        } else {
            return minVersion.toString();
        }
    }

}
