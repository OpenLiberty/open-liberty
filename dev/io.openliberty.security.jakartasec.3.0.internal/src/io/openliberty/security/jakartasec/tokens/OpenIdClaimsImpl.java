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

import java.io.Serializable;
import java.util.Map;

import jakarta.security.enterprise.identitystore.openid.OpenIdClaims;

/**
 * This class inherits the default implementations from OpenIdClaims.
 */
public class OpenIdClaimsImpl extends ClaimsImpl implements OpenIdClaims, Serializable {

    private static final long serialVersionUID = 1L;

    public OpenIdClaimsImpl(Map<String, Object> claimsMap) {
        super(claimsMap);
    }

}
