/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * A parsed OpenAPI spec version. May represent a two-digit (e.g. 3.0) or three-digit (e.g. 3.0.3) version.
 * <p>
 * Both {@code equals} and {@code compareTo} treat {@code 3.0} as less than {@code 3.0.0}.
 */
public class OpenAPIVersion implements Comparable<OpenAPIVersion> {
    /** Matches a.b or a.b.c where a, b and c are integers. After matching, a, b and c are in groups 1-3. */
    private static final Pattern VERSION_PATTERN = Pattern.compile("([0-9]+)\\.([0-9]+)(?:\\.([0-9]+))?");

    private int major, minor, patch;
    private String versionString;

    /**
     * Attempt to parse a string into an OpenAPI version
     *
     * @param versionString the string to parse
     * @return an {@code OpenAPIVersion} if the string represents a valid version, otherwise an empty {@code Optional}
     */
    @FFDCIgnore(NumberFormatException.class)
    public static Optional<OpenAPIVersion> parse(String versionString) {
        if (versionString == null) {
            return Optional.empty();
        }

        versionString = versionString.trim();

        Matcher matcher = VERSION_PATTERN.matcher(versionString);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String major = matcher.group(1);
        String minor = matcher.group(2);
        String patch = matcher.group(3);

        try {
            return Optional.of(new OpenAPIVersion(Integer.parseInt(major),
                                                  Integer.parseInt(minor),
                                                  patch == null ? -1 : Integer.parseInt(patch)));
        } catch (NumberFormatException e) {
            // We've already checked each component is an integer, but it could be too large for an int
            return Optional.empty();
        }
    }

    public OpenAPIVersion(int major, int minor) {
        this(major, minor, -1);
    }

    public OpenAPIVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        versionString = major + "." + minor + (patch == -1 ? "" : "." + patch);
    }

    /**
     * Gets the major version number.
     *
     * @return the major version
     */
    public int getMajor() {
        return major;
    }

    /**
     * Gets the minor version number.
     *
     * @return the minor version
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Gets the patch version number.
     *
     * @return the patch version number, or {@code -1} for a two-digit version
     */
    public int getPatch() {
        return patch;
    }

    /**
     * Returns the string form of this OpenAPI version, suitable for passing to {@link OpenAPI#setOpenapi(String)}
     */
    @Override
    public String toString() {
        return versionString;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OpenAPIVersion other = (OpenAPIVersion) obj;
        return major == other.major && minor == other.minor && patch == other.patch;
    }

    @Override
    public int compareTo(OpenAPIVersion o) {
        int compare = 0;
        compare = Integer.compare(major, o.major);
        if (compare == 0) {
            compare = Integer.compare(minor, o.minor);
        }
        if (compare == 0) {
            // Note patch == -1 for a two-digit version
            // Therefore 3.0 < 3.0.0
            // Having an ordering here is necessary to be consistent with equals
            compare = Integer.compare(patch, o.patch);
        }
        return compare;
    }
}