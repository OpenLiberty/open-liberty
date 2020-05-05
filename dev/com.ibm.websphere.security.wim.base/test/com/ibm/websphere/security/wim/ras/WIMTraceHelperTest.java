/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim.ras;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WIMTraceHelperTest {

    @Test
    public void testPrintPrimitiveObjects() {
        assertEquals("[-2, 0, 2]", WIMTraceHelper.printPrimitiveArray(new short[] { -2, 0, 2 }));
        assertEquals("[false, true, false]", WIMTraceHelper.printPrimitiveArray(new boolean[] { false, true, false }));
        assertEquals("[-3.14, 0.12345, 1337.0]", WIMTraceHelper.printPrimitiveArray(new double[] { -3.14, 0.12345, 1337.0 }));
        assertEquals("[-2, 0, 2]", WIMTraceHelper.printPrimitiveArray(new byte[] { -2, 0, 2 }));
        assertEquals("[Z, &, 6]", WIMTraceHelper.printPrimitiveArray(new char[] { 'Z', '&', '6' }));
        assertEquals("[-2, 0, 2]", WIMTraceHelper.printPrimitiveArray(new int[] { -2, 0, 2 }));
        assertEquals("[-2, 0, 2]", WIMTraceHelper.printPrimitiveArray(new long[] { -2L, 0, 2L }));
        assertEquals("[-2.0E10, 0.0, 2.0E10]", WIMTraceHelper.printPrimitiveArray(new float[] { -2e10f, 0, 2e10f }));
    }
}
