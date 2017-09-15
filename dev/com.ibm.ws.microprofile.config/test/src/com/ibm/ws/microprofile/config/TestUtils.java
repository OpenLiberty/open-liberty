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
package com.ibm.ws.microprofile.config;

import static org.junit.Assert.fail;

public class TestUtils {

    public static void assertContains(Iterable<String> iterable, String value) {
        StringBuilder strb = new StringBuilder();
        boolean first = true;
        for (String str : iterable) {
            if (str.equals(value)) {
                return;
            } else {
                if (!first) {
                    strb.append(", ");
                } else {
                    first = false;
                }
                strb.append(str);
            }
        }
        fail("Iterable (" + strb + ") did not contain: " + value);
    }

    public static void assertNotContains(Iterable<String> iterable, String value) {
        StringBuilder strb = new StringBuilder();
        boolean contains = false;
        boolean first = true;
        for (String str : iterable) {
            if (str.equals(value)) {
                contains = true;
            }

            if (!first) {
                strb.append(", ");
            } else {
                first = false;
            }
            strb.append(str);
        }
        if (contains) {
            fail("Iterable (" + strb + ") DID contain: " + value);
        }
    }
}
