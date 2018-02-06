/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.strategies.writeable;

import org.osgi.framework.Version;

/**
 * This version class extends some of the behaviour of the org.osgi.framework.Version.Version
 * specifically in that allows the two additional uses:
 * <ul>
 * <li>It can accept the "+" character in the qualifier e.g. "8.5.5.5+"
 * <li>It can do a integer based compares on the qualifier field if the String field contains
 * an integer i.e. it correctly identifies that "8.5.5.10" is a higher level than "8.5.5.9" which
 * the original Version class fails to do.
 * </ul>
 */
public class Version4Digit implements Comparable<Version4Digit> {
    private int major = -1;
    private int minor = -1;
    private int micro = -1;
    private String qualifier = null;

    public Version4Digit(int major, int minor, int micro) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.qualifier = "";
    }

    public Version4Digit(int major, int minor, int micro, String s) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.qualifier = s;
    }

    public Version4Digit(Version version) {
        major = version.getMajor();
        minor = version.getMinor();
        micro = version.getMicro();
        qualifier = version.getQualifier();
    }

    public Version4Digit(String versionString) {
        try {
            Version v = new Version(versionString);
            major = v.getMajor();
            minor = v.getMinor();
            micro = v.getMicro();
            qualifier = v.getQualifier();
        } catch (IllegalArgumentException eae) {
            // this isn't a valid OSGI Version but we want to support 1.2.3.4+ Versions
            String[] bits = versionString.split("\\.");
            if (bits.length != 4) {
                throw eae; // throw the original exception
            }
            try {
                major = Integer.parseInt(bits[0]);
                minor = Integer.parseInt(bits[1]);
                micro = Integer.parseInt(bits[2]);
            } catch (NumberFormatException e) {
                throw eae; // throw the original exception
            }
            qualifier = bits[3];
        }

    }

    @Override
    public int compareTo(Version4Digit that) {
        if (equals(that)) {
            return 0;
        }
        if (this.getMajor() > that.getMajor()) {
            return 1;
        } else if (that.getMajor() > this.getMajor()) {
            return -1;
        }

        if (this.getMinor() > that.getMinor()) {
            return 1;
        } else if (that.getMinor() > this.getMinor()) {
            return -1;
        }

        if (this.getMicro() > that.getMicro()) {
            return 1;
        } else if (that.getMicro() > this.getMicro()) {
            return -1;
        }

        // now the tricky bit
        try {
            if (this.getiQualifier() > that.getiQualifier()) {
                return 1;
            } else if (that.getiQualifier() > this.getiQualifier()) {
                return -1;
            }
        } catch (NumberFormatException e) {
            // the 4th field could not be converted to an int so just do a String compare
            return this.qualifier.compareTo(that.qualifier);
        }
        return 0; // equals
    }

    public static Version4Digit getHigherVersion(Version4Digit v1, Version4Digit v2) {
        int i = v1.compareTo(v2);
        if (i > 0) {
            return v1;
        } else if (i < 0) {
            return v2;
        } else {
            return null;
        }
    }

    /**
     * This method can throw a runtime java.lang.NumberFormatException
     *
     * @return the integer form of the qualifier
     */
    private int getiQualifier() {
        return Integer.parseInt(qualifier);
    }

    /**
     * @return the major
     */
    public int getMajor() {
        return major;
    }

    /**
     * @return the minor
     */
    public int getMinor() {
        return minor;
    }

    /**
     * @return the micro
     */
    public int getMicro() {
        return micro;
    }

    /**
     * @return the qualifier
     */
    public String getQualifier() {
        return qualifier;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + major;
        result = prime * result + micro;
        result = prime * result + minor;
        result = prime * result + ((qualifier == null) ? 0 : qualifier.hashCode());
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
        Version4Digit other = (Version4Digit) obj;
        if (major != other.major)
            return false;
        if (micro != other.micro)
            return false;
        if (minor != other.minor)
            return false;
        if (qualifier == null) {
            if (other.qualifier != null)
                return false;
        } else if (!qualifier.equals(other.qualifier))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major);
        sb.append(".");
        sb.append(minor);
        sb.append(".");
        sb.append(micro);
        if (qualifier != null && !(qualifier.equals(""))) {
            sb.append(".");
            sb.append(qualifier);
        }
        return sb.toString();
    }
}
