/* ============================================================================
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ============================================================================
 */
package testjms.web.util;

public enum OutputHelper {
    ;

    private static String toHex(String s) {
        final StringBuilder sb = new StringBuilder(2 * s.length());
        for (char c: s.toCharArray()) {
            sb.append(String.format("%04x ", (int)c));
        }
        return sb.toString();
    }

    public static String comparisonFailureDescription(String expected, Object actual) {
        return String.format("%n%s%nCopy:%n%s%ndoes not equal original:%n%s%n%1$s%n", "#########", toHex(actual.toString()), toHex(expected));
    }
}
