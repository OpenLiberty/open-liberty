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
import com.ibm.websphere.ras.TraceComponent;

public class TokenValidationException extends Exception {

    public static final TraceComponent tc = Tr.register(TokenValidationException.class);

    private static final long serialVersionUID = 1L;

    public TokenValidationException(String clientId, String nlsMessage) {
        super(Tr.formatMessage(tc, "TOKEN_VALIDATION_EXCEPTION", clientId, nlsMessage));
    }

}
