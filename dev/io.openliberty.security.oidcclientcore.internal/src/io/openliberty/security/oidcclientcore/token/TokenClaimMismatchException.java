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
package io.openliberty.security.oidcclientcore.token;

import com.ibm.websphere.ras.Tr;

public class TokenClaimMismatchException extends TokenValidationException {

    private static final long serialVersionUID = 3348448258753761187L;

    public TokenClaimMismatchException(String clientId, String claimValue, String claimName, String expectedClaimValue) {
        super(clientId, Tr.formatMessage(tc, "TOKEN_CLAIM_VALUE_MISMATCH", claimValue, claimName, expectedClaimValue));
    }

}
