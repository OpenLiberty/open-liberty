/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import org.apache.commons.codec.binary.Base64;

/**
 * Some utility functions that allow us to use both Base64 methods out of both
 * com.ibm.ws.security.oauth20.util.Base64 and
 * org.apache.commons.codec.binary.Base64 ;
 */
public class ApacheJsonUtils {

    static public final String DELIMITER = ".";

    public static String fromBase64ByteToJsonString(byte[] source) {
        return new String(Base64.decodeBase64(source));
    }

    public static String fromBase64StringToJsonString(String source) {
        return new String(Base64.decodeBase64(source));
    }

    public ApacheJsonUtils() {
    };
}
