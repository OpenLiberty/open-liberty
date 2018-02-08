/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015,2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install;

/**
 * This class is used to create an object of type Version.
 * This class also contains methods used to extract Version properties and parameters.
 */
public class Version {

    private final int major;
    private final int minor;
    private final int micro;
    private String qualifier = "";
    private int hashCode = 0;

    /**
     * Constructor Method for Version
     * 
     * @param major
     * @param minor
     * @param micro
     * @param qualifier
     */
    public Version(int major, int minor, int micro, String qualifier) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        if (qualifier != null)
            this.qualifier = qualifier;
    }

    /**
     * @param o - input object
     * @return true if current object equals the Object o
     *         If o is not of type Version, it compares the object's parameters individually
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Version))
            return false;
        Version v = (Version) o;
        return major == v.major &&
               minor == v.minor &&
               micro == v.micro &&
               qualifier.equals(v.qualifier);
    }

    /**
     * Compares the current version with the parameter Version v
     * 
     * @param v - Version object
     * @return: the difference between the object parameter's values (eg.this.major - v.major)
     */
    public int compareTo(Version v) {
        if (this == v)
            return 0;
        int diff = major - v.major;
        if (diff != 0)
            return diff;
        diff = minor - v.minor;
        if (diff != 0)
            return diff;
        diff = micro - v.micro;
        if (diff != 0)
            return diff;
        return qualifier.compareTo(v.qualifier);
    }

    /**
     * Creates a new object of type Version
     * This method extracts the values for the object parameters from vStr
     * 
     * @param vStr - string input
     */
    public static Version createVersion(String vStr) {
        if (vStr == null || vStr.isEmpty())
            return null;
        int major = 0;
        int minor = 0;
        int micro = 0;
        String qualifier = "";
        String v[] = vStr.split("\\.", 4);
        try {
            if (v.length > 0) {
                major = Integer.valueOf(v[0]).intValue();
            }
            if (v.length > 1) {
                minor = Integer.valueOf(v[1]).intValue();
            }
            if (v.length > 2) {
                micro = Integer.valueOf(v[2]).intValue();
            }
            if (v.length > 3) {
                qualifier = v[3];
            }
            return new Version(major, minor, micro, qualifier);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * Computes the hasCode using the Object's parameters
     */
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = ((major * 100 + minor) * 100 + micro) * 100;
            if (qualifier != null)
                hashCode += qualifier.hashCode() % 100;
        }
        return hashCode;
    }

    /**
     * {@inheritDoc}
     * Converts the Object's paramters into a string sequence
     * 
     * @return: String with object parameters separated with a "."
     */
    @Override
    public String toString() {
        return major + "." + minor + "." + micro + (qualifier.isEmpty() ? "" : "." + qualifier);
    }
}
