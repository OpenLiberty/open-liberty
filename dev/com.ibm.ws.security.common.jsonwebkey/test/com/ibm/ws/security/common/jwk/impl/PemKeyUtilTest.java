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
package com.ibm.ws.security.common.jwk.impl;

import static org.junit.Assert.*;

import java.security.PublicKey;

import org.junit.Ignore;
import org.junit.Test;

public class PemKeyUtilTest {

    public static final String PEM_KEY_TEXT = "-----BEGIN PUBLIC KEY-----\n"
            + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0440JtmhlywtkMvR6tTM\n"
            + "s0U6e9Ja4xXj5+q+joWdT2xCHt91Ck9+5C5WOaRTco4CPFMBxoUPi1jktW5c+Oyk\n"
            + "nOIACXu6grXexarFQLjsREE+dkDVrMu75f7Gb9/lC7mrVM73118wnMP2u5MOQIoX\n"
            + "OqqC1y1gaoJaLp/OjTiJGCm4uxzubzUPN5IDAFaTfK+QErhtcGeBDwWjvikGfUfX\n"
            + "+WVq74DOoggLiGbB4jsT8iVXEm53JcoEY8nVr2ygr92TuU1+xLAGisjRSYJVe7V1\n"
            + "tpdRG1CiyCIkqhDFfFBGhFnWlu4gKMiT0KToA9GJfOuCz67XZEAhQYizcXbn1uxa\n"
            + "OQIDAQAB\n"
            + "-----END PUBLIC KEY-----";
    
    private String pemKeyTextWithBadBeginHeader = "-----BEGIN BAD KEY-----\n"
            + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0440JtmhlywtkMvR6tTM\n"
            + "s0U6e9Ja4xXj5+q+joWdT2xCHt91Ck9+5C5WOaRTco4CPFMBxoUPi1jktW5c+Oyk\n"
            + "nOIACXu6grXexarFQLjsREE+dkDVrMu75f7Gb9/lC7mrVM73118wnMP2u5MOQIoX\n"
            + "OqqC1y1gaoJaLp/OjTiJGCm4uxzubzUPN5IDAFaTfK+QErhtcGeBDwWjvikGfUfX\n"
            + "+WVq74DOoggLiGbB4jsT8iVXEm53JcoEY8nVr2ygr92TuU1+xLAGisjRSYJVe7V1\n"
            + "tpdRG1CiyCIkqhDFfFBGhFnWlu4gKMiT0KToA9GJfOuCz67XZEAhQYizcXbn1uxa\n"
            + "OQIDAQAB\n"
            + "-----END PUBLIC KEY-----";
    
    private String pemKeyTextWithBadEndHeader = "-----BEGIN PUBLIC KEY-----\n"
            + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0440JtmhlywtkMvR6tTM\n"
            + "s0U6e9Ja4xXj5+q+joWdT2xCHt91Ck9+5C5WOaRTco4CPFMBxoUPi1jktW5c+Oyk\n"
            + "nOIACXu6grXexarFQLjsREE+dkDVrMu75f7Gb9/lC7mrVM73118wnMP2u5MOQIoX\n"
            + "OqqC1y1gaoJaLp/OjTiJGCm4uxzubzUPN5IDAFaTfK+QErhtcGeBDwWjvikGfUfX\n"
            + "+WVq74DOoggLiGbB4jsT8iVXEm53JcoEY8nVr2ygr92TuU1+xLAGisjRSYJVe7V1\n"
            + "tpdRG1CiyCIkqhDFfFBGhFnWlu4gKMiT0KToA9GJfOuCz67XZEAhQYizcXbn1uxa\n"
            + "OQIDAQAB\n"
            + "-----END BAD KEY-----";
    
    @Test
    public void testParse() throws Exception {
        PublicKey publicKey = PemKeyUtil.getPublicKey(PEM_KEY_TEXT);
        
        assertNotNull("There must be a public key.", publicKey);
    }
    
    @Test(expected=Exception.class)
    public void testParse_BadBeginHeader() throws Exception {
        PublicKey publicKey = PemKeyUtil.getPublicKey(pemKeyTextWithBadBeginHeader);
    }
    
    @Ignore
    @Test(expected=Exception.class)
    public void testParse_BadEndHeader() throws Exception {
        PublicKey publicKey = PemKeyUtil.getPublicKey(pemKeyTextWithBadEndHeader);
        assertNull("There must not be a public key.", publicKey);
    }

}