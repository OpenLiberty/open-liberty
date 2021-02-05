/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal.utils;

import org.junit.Assert;
import org.junit.Test;
import org.opensaml.xml.signature.SignatureConstants;

/**
 * Unit test the {@link SignatureMethods} class.
 */
public class SignatureMethodsTest {

    private static final int ALGO_ID_SIGNATURE_RSA_SHA1_VALUE = 1;
    private static final int ALGO_ID_SIGNATURE_RSA_SHA256_VALUE = 256;
    private static final int ALGO_ID_SIGNATURE_RSA_SHA384_VALUE = 384;
    private static final int ALGO_ID_SIGNATURE_RSA_SHA512_VALUE = 512;

    /**
     * Test if @link {@link SignatureMethods#toInteger(String)} return the
     * correct representation of the provided method. Fails if it does not
     * return the expected value.
     */
    @Test
    public void toIntegerShouldReturnIntegerRepresentationOfMethod() {
        Assert.assertEquals(ALGO_ID_SIGNATURE_RSA_SHA1_VALUE,
                            SignatureMethods.toInteger(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1));
        Assert.assertEquals(ALGO_ID_SIGNATURE_RSA_SHA256_VALUE,
                            SignatureMethods.toInteger(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256));
        Assert.assertEquals(ALGO_ID_SIGNATURE_RSA_SHA384_VALUE,
                            SignatureMethods.toInteger(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA384));
        Assert.assertEquals(ALGO_ID_SIGNATURE_RSA_SHA512_VALUE,
                            SignatureMethods.toInteger(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA512));
    }

    /**
     * Test if @link {@link SignatureMethods#toInteger(String)} return 0 if it
     * couldn't found the representation of the method. Fails if it does not
     * return the expected value.
     */
    @Test
    public void toIntegerShouldReturnZeroIfMethodIsNotFound() {
        Assert.assertEquals(0, SignatureMethods.toInteger(SignatureConstants.ALGO_ID_MAC_HMAC_NOT_RECOMMENDED_MD5));
    }

}
