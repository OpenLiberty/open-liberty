/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.digest;

import java.io.IOException;

public enum Assert {;
    private static final Checksums CHECKSUMS = Checksums.getInstance();

    public static void assertMatch(Class<?> a, Class<?> b) {
        try {
            String actual = CHECKSUMS.forClass(a);
            String expected = CHECKSUMS.forClass(b);
            org.junit.Assert.assertEquals(actual, expected);
        } catch (IOException e) {
            e.printStackTrace();
            org.junit.Assert.fail("An exception is caught " + e);
        }
    }

    public static void assertNoMatch(Class<?> a, Class<?> b) {
        try {
            String actual = CHECKSUMS.forClass(a);
            String expected = CHECKSUMS.forClass(b);
            org.junit.Assert.assertFalse(actual.equals(expected));
        } catch (IOException e) {
            e.printStackTrace();
            org.junit.Assert.fail("An exception is caught " + e);
        }
    }
}
