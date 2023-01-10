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

public class TokenRequestException extends Exception {

    public static final TraceComponent tc = Tr.register(TokenRequestException.class);

    private static final long serialVersionUID = 1L;

    private final String clientId;
    private final String nlsMessage;

    public TokenRequestException(String clientId, String nlsMessage) {
        this.clientId = clientId;
        this.nlsMessage = nlsMessage;
    }

    public TokenRequestException(String clientId, String nlsMessage, Throwable cause) {
        super(cause);
        this.clientId = clientId;
        this.nlsMessage = nlsMessage;
    }

    @Override
    public String getMessage() {
        return Tr.formatMessage(tc, "TOKEN_REQUEST_ERROR", clientId, nlsMessage);
    }

}
