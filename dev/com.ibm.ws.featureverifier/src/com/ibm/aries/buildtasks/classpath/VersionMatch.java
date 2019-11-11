/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.aries.buildtasks.classpath;

/**
 * This class matches a version based on a version range on an Import-Package
 */
public class VersionMatch
{
    /** The minimum desired version for the bundle */
    private Version minimumVersion;
    /** The maximum desired version for the bundle */
    private Version maximumVersion;
    /** True if the match is exclusive of the minimum version */
    private boolean minimumExclusive;
    /** True if the match is exclusive of the maximum version */
    private boolean maximumExclusive;

    /**
     * @param versions the version spec
     */
    public VersionMatch(String versions)
    {
        int index;

        if ((versions.startsWith("[") || versions.startsWith("(")) &&
            (versions.endsWith("]") || versions.endsWith(")"))) {
            if (versions.startsWith("["))
                minimumExclusive = false;
            else if (versions.startsWith("("))
                minimumExclusive = true;

            if (versions.endsWith("]"))
                maximumExclusive = false;
            else if (versions.endsWith(")"))
                maximumExclusive = true;

            index = versions.indexOf(',');
            String minVersion = versions.substring(1, index);
            String maxVersion = versions.substring(index + 1, versions.length() - 1);

            try {
                minimumVersion = new Version(minVersion);
                maximumVersion = new Version(maxVersion);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Failed to parse " + versions + ".", nfe);
            }
        } else {
            try {
                minimumVersion = new Version(versions);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Failed to parse " + versions + ".", nfe);
            }
        }
    }

    /**
     * This toString will return a String identical to the one originally used to
     * construct this BundleMatchImpl
     * 
     * @return a string representing this BundleMatchImpl
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        if (maximumVersion == null) {
            builder.append(minimumVersion);
        } else {
            if (minimumExclusive)
                builder.append('(');
            else
                builder.append('[');
            builder.append(minimumVersion);
            builder.append(',');
            builder.append(maximumVersion);
            if (maximumExclusive)
                builder.append(')');
            else
                builder.append(']');
        }

        return builder.toString();
    }

    /**
     * This method checks that the provided version matches the desired version.
     * 
     * @param version the version.
     * @return true if the version matches, false otherwise.
     */
    public boolean matches(Version version)
    {
        boolean result;

        // if max is null then no maximum has been specified.
        if (maximumVersion == null) {
            result = minimumVersion.compareTo(version) <= 0;
        } else {
            int minN = minimumExclusive ? 0 : 1;
            int maxN = maximumExclusive ? 0 : 1;

            result = (minimumVersion.compareTo(version) < minN) &&
                     (version.compareTo(maximumVersion) < maxN);
        }
        return result;
    }
}