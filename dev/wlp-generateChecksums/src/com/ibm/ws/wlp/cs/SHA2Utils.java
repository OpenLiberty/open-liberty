/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.wlp.cs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.ibm.ws.common.crypto.CryptoUtils;

public class SHA2Utils {

    protected static MessageDigest messagedigest = null;

    static {
        try {
            messagedigest = MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA_256);
        } catch (NoSuchAlgorithmException e) {
            //should not happen
        }
    }

    public static String getFileSHA2String(String str) {
        messagedigest.update(str.getBytes());
        return byteArrayToHexString(messagedigest.digest());
    }

    public static String getFileSHA2String(File file) throws IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(file);

            byte[] buffer = new byte[1024];
            int numRead = 0;
            while ((numRead = fis.read(buffer)) > 0) {
                messagedigest.update(buffer, 0, numRead);
            }
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                }
            }
        }

        return byteArrayToHexString(messagedigest.digest());
    }

    public static String byteArrayToHexString(byte[] byteArray) {

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            String hex = Integer.toHexString(0xff & byteArray[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();

    }

}
