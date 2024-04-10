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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Implementation of cached kernel bundle elements.
 */
public class KernelBundleElementImpl implements KernelBundleElement {
    /**
     * Standard constructor: Create a cache element for a bundle element.
     *
     * The cache element is assigned a null best fit file.
     *
     * @param cache   The cache that will hold the new element.
     * @param element The content element for this cache element.
     *
     * @throws IOException If the value read from the cache is an invalid start phase
     */
    public KernelBundleElementImpl(KernelResolverCache cache,
                                   KernelContentElement element) throws IOException {

        this(cache, element, null);
    }

    /**
     * Deserialization constructor: Create a cache element from a line of
     * a persisted resolver cache.
     *
     * @param cache   The cache that will hold the new element.
     * @param element The content element for this cache element.
     *
     * @throws IOException If the value read from the cache is an invalid start phase
     */
    KernelBundleElementImpl(KernelResolverCache cache, String cacheLine) throws IOException {
        this(cache, parseContentElement(cacheLine), parseBestMatch(cacheLine));
    }

    /**
     * Deserialization helper: Parse a content element from a line from
     * a persisted resolver cache.
     *
     * @param cacheLine A line from a persisted resolver cache.
     *
     * @return A content element parsed from the line.
     */
    private static KernelContentElement parseContentElement(String cacheLine) {
        int index = cacheLine.indexOf('|');
        return new KernelContentElement(cacheLine.substring(0, index));
    }

    /**
     * Deserialization helper: Parse a best fit file from a line from
     * a persisted resolver cache.
     *
     * @param cacheLine A line from a persisted resolver cache.
     *
     * @return A best fit file from the line. Null if no best fit
     *         value is present.
     */
    private static File parseBestMatch(String cacheLine) {
        int index = cacheLine.indexOf('|');
        if (index == -1) {
            return null; // Unexpected
        } else if (index == cacheLine.length() - 1) {
            return null; // No best match was set.
        } else {
            return new File(cacheLine.substring(index + 1));
        }
    }

    /**
     * Parse a start phase persisted value. This is the print string
     * of the parse phase enumeration. See {@link KernelStartLevel}.
     *
     * @param startPhase A start phase persisted value.
     *
     * @return The integer value of the enumerated value matching the
     *         start phase.
     *
     * @throws IOException Thrown if the persisted value matches none
     *                         of the start level enumeration values.
     */
    private static int parseStartPhase(String startPhase) throws IOException {
        if (startPhase == null) {
            return KernelStartLevel.ACTIVE.startLevel;
        } else {
            try {
                KernelStartLevel level = KernelStartLevel.valueOf(startPhase);
                return level.startLevel;
            } catch (IllegalArgumentException e) {
                throw new IOException("Kernel cache start phase is not valid [ " + startPhase + " ]");
            }
        }
    }

    /**
     * Core constructor: Create a bundle cache element for a content element
     *
     * @param cache         The cache which will hold the new bundle cache element.
     * @param element       The content element for of this cache element.
     * @param bestMatchFile The best match file of this cache element.
     *
     * @throws IOException Thrown if the start phase of the content element is
     *                         not a valid start phase value. See {@link KernelStartLevel}.
     */
    public KernelBundleElementImpl(KernelResolverCache cache,
                                   KernelContentElement element,
                                   File bestMatchFile) throws IOException {
        this.cache = cache;
        this.element = element;
        this.bestMatch = bestMatchFile;

        this.startLevel = parseStartPhase(element.getStartPhase());
    }

    /**
     * Answer the symbolic name from the content element plus the
     * version range. For example:
     *
     * <pre>
     * symbolicName;version="[1.0, 2.0]"
     * </pre>
     *
     * @return The content element symbolic name plus the version range.
     */
    @Override
    public String toNameVersionString() {
        return element.getSymbolicName() + ";" +
               KernelContentElement.VERSION + '"' + element.getRange() + '"';
    }

    //

    private final KernelResolverCache cache;

    public KernelResolverCache getCache() {
        return cache;
    }

    //

    private final KernelContentElement element;

    public KernelContentElement getElement() {
        return element;
    }

    @Override
    public String getSymbolicName() {
        return element.getSymbolicName();
    }

    @Override
    public String getLocation() {
        return element.getLocation();
    }

    @Override
    public String getRangeString() {
        return element.getRange();
    }

    //

    private final int startLevel;

    @Override
    public int getStartLevel() {
        return startLevel;
    }

    //

    private File bestMatch;

    @Override
    public File getCachedBestMatch() {
        return bestMatch;
    }

    /**
     * Set the best match file of this content element.
     *
     * As a side effect, mark the enclosing cache as dirty.
     *
     * @param bestMatch The best match file.
     */
    @Override
    public void setBestMatch(File bestMatch) {
        this.bestMatch = bestMatch;

        cache.setDirty();
    }

    //

    /**
     * Write this bundle element.
     *
     * Write the print string of the enclosed content element,
     * plus '|', plus, if available, the absolute path of the
     * best match file.
     *
     * @param writer A print writer.
     */
    public void write(PrintWriter writer) {
        writer.write(element.toString());
        writer.write('|');
        if (bestMatch != null) {
            writer.write(bestMatch.getAbsolutePath());
        }
    }

    /**
     * Answer the print string of this bundle element.
     *
     * Use the manifest format, except, only values which
     * were retained from the manifest are present.
     *
     * Print string is the composite of the content element
     * print string plus, if set, the best match print string.
     *
     * @return The print string of this bundle element.
     */
    @Override
    public String toString() {
        String printString = element.toString();
        if (bestMatch != null) {
            printString += ";path=\"" + bestMatch.getAbsolutePath() + '"';
        }
        return printString;
    }
}
