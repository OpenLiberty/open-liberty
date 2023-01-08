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
package io.openliberty.security.jakartasec.tokens;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import jakarta.security.enterprise.identitystore.openid.RefreshToken;

public class RefreshTokenImplTest {

    @Test
    public void testGetToken() {
        String tokenString = "tokenString";
        RefreshToken refreshToken = new RefreshTokenImpl(tokenString);

        assertEquals("The refresh token must be set.", tokenString, refreshToken.getToken());
    }

}
