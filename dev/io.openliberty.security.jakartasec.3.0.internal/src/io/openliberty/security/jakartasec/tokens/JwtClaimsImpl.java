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

import java.io.Serializable;
import java.util.Map;

import jakarta.security.enterprise.identitystore.openid.JwtClaims;

/**
 * This class inherits the default implementations from JwtClaims.
 */
public class JwtClaimsImpl extends ClaimsImpl implements JwtClaims, Serializable {

    private static final long serialVersionUID = 1L;

    public JwtClaimsImpl(Map<String, Object> claimsMap) {
        super(claimsMap);
    }

}
