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
 * <p>This stores a liberty product version, this looks very similar to an OSGi version but don't be fooled! It is actually of the form:</p>
 * 
 * <code>d.d.d.d</code>
 * 
 * <p>Where "d" is an integer digit. OSGi versions are of the form:</p>
 * 
 * <code>d.d.d.s</code>
 * 
 * <p>where "s" is a string. This means that 8.5.5.10 is higher than 8.5.5.2 in Liberty but lower in OSGi.</p>
 * 
 * <p>To follow OSGi's definition:</p>
 * 
 * <pre>version ::=
 * major'.'minor'.'micro'.'qualifier
 * major ::= number // See 1.3.2
 * minor ::= number
 * micro ::= number
 * qualifier ::= number</pre>
 */
public class LibertyVersion implements Comparable<LibertyVersion> {

    private final int major;
    private final int minor;
    private final int micro;
    private final int qualifier;
    private final static Pattern VALID_VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)");

    /**
     * Parses the supplied string into a LibertyVersion. Will return <code>null</code> if the string is not a valid version string.
     * 
     * @param versionString The string to parse
     * @return The version
     */
    public static LibertyVersion valueOf(String versionString) {
        if (versionString == null) {
            return null;
        }
        versionString = versionString.trim();
        Matcher versionMatcher = VALID_VERSION.matcher(versionString);
        if (!versionMatcher.matches()) {
            return null;
        }
        return new LibertyVersion(Integer.parseInt(versionMatcher.group(1)), Integer.parseInt(versionMatcher.group(2)), Integer.parseInt(versionMatcher.group(3)), Integer.parseInt(versionMatcher.group(4)));
    }

    /**
     * @param major
     * @param minor
     * @param micro
     * @param qualifier
     */
    private LibertyVersion(int major, int minor, int micro, int qualifier) {
        super();
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.qualifier = qualifier;
    }

    /**
     * Matches the major, minor and micro parts of this version to the other version
     * 
     * @param other The other version, can be <code>null</code> in which case this returns true.
     * @return
     */
    public boolean matchesToMicros(LibertyVersion other) {
        if (other == null) {
            return true;
        }
        return this.major == other.major && this.minor == other.minor && this.micro == other.micro;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(LibertyVersion o) {
        if (this.equals(o)) {
            return 0;
        }
        if (o == null) {
            throw new NullPointerException();
        }
        if (this.major < o.major) {
            return -1;
        } else if (this.major > o.major) {
            return 1;
        }
        if (this.minor < o.minor) {
            return -1;
        } else if (this.minor > o.minor) {
            return 1;
        }
        if (this.micro < o.micro) {
            return -1;
        } else if (this.micro > o.micro) {
            return 1;
        }
        if (this.qualifier < o.qualifier) {
            return -1;
        } else if (this.qualifier > o.qualifier) {
            return 1;
        }

        // Err everything was equal but equals returned false, don't think we can get here but return 0 anyway
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return major + "." + minor + "." + micro + "." + qualifier;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + major;
        result = prime * result + micro;
        result = prime * result + minor;
        result = prime * result + qualifier;
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
        LibertyVersion other = (LibertyVersion) obj;
        if (major != other.major)
            return false;
        if (micro != other.micro)
            return false;
        if (minor != other.minor)
            return false;
        if (qualifier != other.qualifier)
            return false;
        return true;
    }

}
