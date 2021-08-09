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
package com.ibm.ws.logging.internal.impl;

import java.util.Comparator;

/**
 * A comparator for "natural" sort order: number sequences are sorted by
 * their numeric value.
 */
public class NaturalComparator implements Comparator<String> {
    public static final Comparator<String> instance = new NaturalComparator();

    private NaturalComparator() {}

    @Override
    public int compare(String s1, String s2) {
        boolean num = false;
        int numCompare = 0;
        for (int i = 0;;) {
            int cp1 = i >= s1.length() ? -1 : s1.codePointAt(i);
            int cp2 = i >= s2.length() ? -1 : s2.codePointAt(i);

            if (cp1 >= '0' && cp1 <= '9') {
                if (cp2 >= '0' && cp2 <= '9') {
                    // If the numbers have already miscompared, then keep
                    // the miscomparison.  e.g., "20" is greater than "12"
                    // even though 0<2 at i=1.
                    if (numCompare == 0) {
                        numCompare = cp1 - cp2;
                    }

                    num = true;
                    i++;
                    continue;
                }

                if (num) {
                    // s1 has a number with more digits, so it's "greater".
                    return 1;
                }
            } else if (cp2 >= '0' && cp2 <= '9') {
                if (num) {
                    // s1 has a number with fewer digits, so it's "less".
                    return -1;
                }
            }

            if (numCompare != 0) {
                // Both strings had numbers with the same quantity of
                // digits, but there was a miscomparison.
                return numCompare;
            }
            num = false;

            // Check for end of string.
            if (cp1 == -1) {
                return cp2 == -1 ? 0 : -1;
            }
            if (cp2 == -1) {
                return 1;
            }

            // Check for non-digit character mismatch.
            if (cp1 != cp2) {
                return cp1 - cp2;
            }

            i += Character.charCount(cp1);
        }
    }
}
