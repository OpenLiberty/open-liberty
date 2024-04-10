/*******************************************************************************
 * Copyright (c) 2010, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

/**
 * One "Subsystem-Content" element, parsed from a feature manifest.
 */
public class KernelContentElement {
    protected final static String TYPE = "type=";
    protected final static String VERSION = "version=";
    protected final static String LOCATION = "location:=";
    protected final static String START_PHASE = "start-phase:=";

    /**
     * Create a subsystem element parsed from a line, which may be either
     * from a manifest, or from the kernel resolver cache.
     *
     * Leading and trailing spaces must have been trimmed from the element line.
     * (Spaces may appear before and after attributes within the element line.)
     *
     * Each line must have information for exactly one constituent, which is
     * identified by symbolic name. Values for the constituent must appear
     * together on a single line. Values for multiple constituents must not
     * placed together on a line.
     *
     * The line is minimally parsed and validated. Spaces MUST NOT be placed
     * between the part tags and their values. (For example, "version =3.0"
     * will not be detected as a version, because of the space before the equals.)
     *
     * The first part of the element line must be the symbolic name. Other
     * parts may appear in any order.
     *
     * Part values may have leading and trailing quotes. These are removed.
     *
     * The element line may have a trailing comma. This is removed before parsing
     * the element line.
     *
     * For example:
     *
     * <pre>
     * org.apache.aries.util; version="[1,1.0.100)"; type="boot.jar",
     * com.ibm.wsspi.org.osgi.cmpn; location="dev/spi/spec/"; version="[5.0, 5.1)"; start-phase:=BOOTSTRAP
     * </pre>
     *
     * @param line Text for a manifest element.
     */
    protected KernelContentElement(String line) {
        if (line.endsWith(",")) {
            line = line.substring(0, line.length() - 1);
        }

        String[] parts = line.split(";");

        String parsedSymbolicName = null;
        String parsedType = null;
        String parsedVRange = null;
        String parsedLocation = null;
        String parsedStartPhase = null;

        if (parts.length > 1) {
            parsedSymbolicName = parts[0].trim();
            for (int partNo = 1; partNo < parts.length; partNo++) {
                String part = parts[partNo].trim();
                if (part.startsWith(TYPE)) {
                    parsedType = KernelUtils.stripQuotes(part.substring(TYPE.length()));
                } else if (part.startsWith(VERSION)) {
                    parsedVRange = KernelUtils.stripQuotes(part.substring(VERSION.length()));
                } else if (part.startsWith(LOCATION)) {
                    parsedLocation = KernelUtils.stripQuotes(part.substring(LOCATION.length()));
                } else if (part.startsWith(START_PHASE)) {
                    parsedStartPhase = KernelUtils.stripQuotes(part.substring(START_PHASE.length()));
                } else {
                    // An unused attribute; ignore it.
                    //
                    // This means the attribute will not be preserved
                    // by the element print string.
                }
            }
        }

        this.symbolicName = parsedSymbolicName;
        this.type = parsedType;
        this.location = parsedLocation;
        this.vrangeString = parsedVRange;
        this.startPhase = parsedStartPhase;
    }

    //

    private final String symbolicName;
    private final String type;
    private final String location;
    private final String vrangeString;
    private final String startPhase;

    public String getSymbolicName() {
        return symbolicName;
    }

    public String getType() {
        return type;
    }

    public String getLocation() {
        return location;
    }

    public String getRange() {
        return vrangeString;
    }

    public String getStartPhase() {
        return startPhase;
    }

    /**
     * Answer the element as it appears in the manifest, but with
     * unused attributes removed.
     *
     * Used attributes are the symbolic name, version range, location,
     * type, and start phase.
     *
     * @return The element print string, in the format as the element
     *         appears in the manifest.
     */
    @Override
    public String toString() {
        int len = symbolicName.length();
        if (vrangeString != null) {
            len += appendLen(VERSION, vrangeString);
        }
        if (location != null) {
            len += appendLen(LOCATION, location);
        }
        if (type != null) {
            len += appendLen(TYPE, type);
        }
        if (startPhase != null) {
            len += appendLen(START_PHASE, startPhase);
        }

        StringBuilder builder = new StringBuilder(len);

        builder.append(symbolicName);
        if (vrangeString != null) {
            append(builder, VERSION, vrangeString);
        }
        if (location != null) {
            append(builder, LOCATION, location);
        }
        if (type != null) {
            append(builder, TYPE, type);
        }
        if (startPhase != null) {
            append(builder, START_PHASE, startPhase);
        }

        return builder.toString();
    }

    /**
     * Tell the length added by the attribute using manifest
     * attribute formatting.
     *
     * See {@link #append(StringBuilder, String, String)}.
     *
     * The tag must include the appropriate assignment characters
     * (usually "=" or ":=").
     *
     * The attribute must not be the first value of the builder.
     *
     * @param tag   That attribute name, including assignment characters.
     * @param value The attribute value.
     *
     * @return The length added by the attribute.
     */
    private static int appendLen(String tag, String value) {
        // ;tag"value"
        return 1 + tag.length() + 1 + value.length() + 1;
    }

    /**
     * Append a value to the string builder, using manifest
     * attribute formatting.
     *
     * The tag must include the appropriate assignment characters
     * (usually "=" or ":=").
     *
     * The attribute must not be the first value of the builder.
     *
     * @param builder The builder which is to receive the attribute.
     * @param tag     That attribute name, including assignment characters.
     * @param value   The attribute value.
     */
    private static void append(StringBuilder builder, String tag, String value) {
        // ;tag"value"
        builder.append(';');
        builder.append(tag);
        builder.append('"');
        builder.append(value);
        builder.append('"');
    }
}
