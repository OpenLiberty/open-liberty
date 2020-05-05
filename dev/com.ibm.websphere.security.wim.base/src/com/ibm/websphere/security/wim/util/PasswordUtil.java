/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim.util;

import java.io.UnsupportedEncodingException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.wsspi.security.wim.exception.WIMSystemException;

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
     * @throws WIMSystemException If there was an {@link UnsupportedEncodingException} exception.
     */
    @Sensitive
    public static byte[] getByteArrayPassword(@Sensitive String password) throws WIMSystemException {
        try {
            if (password != null) {
                return password.getBytes("UTF-8");
            } else {
                return null;
            }
        } catch (java.io.UnsupportedEncodingException e) {
            if (tc.isErrorEnabled()) {
                Tr.error(tc, WIMMessageKey.GENERIC, WIMMessageHelper.generateMsgParms(e.toString()));
            }
            throw new WIMSystemException(WIMMessageKey.GENERIC, Tr.formatMessage(
                                                                                 tc,
                                                                                 WIMMessageKey.GENERIC,
                                                                                 WIMMessageHelper.generateMsgParms(e.toString())));
        }
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
