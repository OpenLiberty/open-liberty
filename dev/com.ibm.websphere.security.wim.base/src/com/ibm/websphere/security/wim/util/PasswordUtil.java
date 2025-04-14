/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
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
package com.ibm.websphere.security.wim.util;

import java.nio.charset.StandardCharsets;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * The utility which provides helper functions related with password.
 *
 */
public class PasswordUtil {

    private static final TraceComponent tc = Tr.register(PasswordUtil.class);

    /**
     * Gets the byte array of the given password from using UTF-8 encoding.
     *
     * @param password the string of the password to encode.
     * @return the byte array representation of the text string
     */
    @Sensitive
    public static byte[] getByteArrayPassword(@Sensitive String password) {
        return password == null ? null : password.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Erase the password byte array by setting its elements to zero.
     * For security reason, all password byte array should be erased before the references to it is dropped.
     *
     * @param pwdBytes The password byte array to be erased.
     */
    @Trivial
    public static void erasePassword(@Sensitive byte[] pwdBytes) {
        if (pwdBytes != null) {
            for (int i = 0; i < pwdBytes.length; i++) {
                pwdBytes[i] = 0x00;
            }
        }

    }
}
