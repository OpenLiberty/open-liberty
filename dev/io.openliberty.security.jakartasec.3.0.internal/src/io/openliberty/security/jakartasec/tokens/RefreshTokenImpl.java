/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.tokens;

import com.ibm.websphere.ras.annotation.Sensitive;

import jakarta.security.enterprise.identitystore.openid.RefreshToken;

public class RefreshTokenImpl implements RefreshToken {

    private final String tokenString;

    public RefreshTokenImpl(@Sensitive String tokenString) {
        this.tokenString = tokenString;
    }

    @Sensitive
    @Override
    public String getToken() {
        return tokenString;
    }

}
