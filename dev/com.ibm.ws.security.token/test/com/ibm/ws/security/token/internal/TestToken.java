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
package com.ibm.ws.security.token.internal;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Random;

import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.wsspi.security.ltpa.Token;

/**
 * Fake token in place of LTPAToken2 needed for the SSO serialization
 * test. LTPAToken2 serialization is tested separately.
 * Most methods will not be called.
 */
@SuppressWarnings("serial")
public class TestToken implements Token, Serializable {
    private final double random;

    public TestToken() {
        random = new Random().nextDouble();
    }

    @Override
    public boolean isValid() throws InvalidTokenException, TokenExpiredException {
        return true;
    }

    @Override
    public byte[] getBytes() throws InvalidTokenException, TokenExpiredException {
        return null;
    }

    @Override
    public long getExpiration() {
        return 123456789L;
    }

    @Override
    public short getVersion() {
        return 1;
    }

    @Override
    public String[] getAttributes(String key) {
        return null;
    }

    @Override
    public String[] addAttribute(String key, String value) {
        return null;
    }

    @Override
    public Enumeration<?> getAttributeNames() {
        return null;
    }

    @Override
    public Object clone() {
        return null;
    }

    @Override
    public String toString() {
        return "tokenstring: " + random;
    }
}
