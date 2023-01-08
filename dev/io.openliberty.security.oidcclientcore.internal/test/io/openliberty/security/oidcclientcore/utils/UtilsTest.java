/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UtilsTest {

    private final String input = "abcdefghijklmnopqrstuvwxyz10123456789";

    @Test
    public void test_getStrHashCode_nullInput() {
        String strHashCode = Utils.getStrHashCode(null);
        assertEquals("strHashCode is not empty", "", strHashCode);
    }

    @Test
    public void test_getStrHashCode() {
        String strHashCode = Utils.getStrHashCode(input);
        int expectedHashCode = input.hashCode();
        int oldHashCode = 0;
        if (strHashCode.startsWith("n")) {
            String strTmp = "-" + strHashCode.substring(1);
            oldHashCode = Integer.parseInt(strTmp);
        } else {
            String strTmp = strHashCode.substring(1);
            oldHashCode = Integer.parseInt(strTmp);
        }
        assertEquals(expectedHashCode, oldHashCode);
    }

}
