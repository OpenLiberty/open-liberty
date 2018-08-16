/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.jwk;

import java.math.BigInteger;

import org.apache.commons.codec.binary.Base64;

public class BigIntegerUtil {
    // This is in use by FAT only at the writing time
    public static BigInteger decode(String s) {
        byte[] bytes = Base64.decodeBase64(s);

        BigInteger bi = new BigInteger(1, bytes);
        return bi;
    }
}
