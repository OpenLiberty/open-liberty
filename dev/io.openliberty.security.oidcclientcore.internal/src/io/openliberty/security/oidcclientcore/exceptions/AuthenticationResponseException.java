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
package io.openliberty.security.oidcclientcore.exceptions;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class AuthenticationResponseException extends Exception {

    public static final TraceComponent tc = Tr.register(AuthenticationResponseException.class);

    private static final long serialVersionUID = 1L;

    public static enum ValidationResult {
        INVALID_RESULT, NOT_VALIDATED_RESULT
    }

    private final ValidationResult result;
    private final String clientId;
    private final String nlsMessage;

    public AuthenticationResponseException(ValidationResult result, String clientId, String nlsMessage) {
        this.result = result;
        this.clientId = clientId;
        this.nlsMessage = nlsMessage;
    }

    public ValidationResult getValidationResult() {
        return result;
    }

    @Override
    public String getMessage() {
        return Tr.formatMessage(tc, "AUTHENTICATION_RESPONSE_ERROR", clientId, nlsMessage);
    }

}
