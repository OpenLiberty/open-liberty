/* ============================================================================
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ============================================================================
 */
package testjms.web.impl;

abstract class JmsTestsBase {
    protected static final byte[] testBytes = { Byte.MIN_VALUE, -5, 3, Byte.MAX_VALUE };
    protected static final char[] testChars = { Character.MIN_VALUE, '\0', '£', Character.MAX_VALUE };
    protected static final double[] testDoubles = { Double.NEGATIVE_INFINITY, Double.MIN_VALUE, Double.NaN, -5.0D, 3.0D, Double.MAX_VALUE, Double.POSITIVE_INFINITY };
    protected static final float[] testFloats = { Float.NEGATIVE_INFINITY, Float.MIN_VALUE, Float.NaN, -5.0F, 3.0F, Float.MAX_VALUE, Float.POSITIVE_INFINITY };
    protected static final int[] testInts = { Integer.MIN_VALUE, -5, 3, Integer.MAX_VALUE };
    protected static final long[] testLongs = { Long.MIN_VALUE, -5L, 3L, Long.MAX_VALUE };
    protected static final short[] testShorts = { Short.MIN_VALUE, -5, 3, Short.MAX_VALUE };
    protected static final int[] testCodePoints = { Character.MIN_CODE_POINT, '\0', '£', Character.MIN_SUPPLEMENTARY_CODE_POINT, (Character.MAX_CODE_POINT + Character.MIN_SUPPLEMENTARY_CODE_POINT) / 2, Character.MAX_CODE_POINT };
    protected static final String testString;

    static {
        final StringBuilder sb = new StringBuilder(2 * testCodePoints.length);
        for (int cp: testCodePoints) {
            sb.append(Character.toChars(cp));
        }
        testString = sb.toString();
    }

    private final ConnectionFactoryType cfType;
    protected JmsTestsBase(ConnectionFactoryType cfType) {
        this.cfType = cfType;
    }

    protected final JmsTestFramework createTestFramework() throws Exception {
        return new JmsTestFramework(cfType);
    }
}
// A dummy change
