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

public class VerificationKeyException extends Exception {

    public static final TraceComponent tc = Tr.register(VerificationKeyException.class);

    private static final long serialVersionUID = 1L;

    private final String clientId;
    private final String nlsMessage;

    public VerificationKeyException(String clientId, String nlsMessage) {
        this.clientId = clientId;
        this.nlsMessage = nlsMessage;
    }

    @Override
    public String getMessage() {
        return Tr.formatMessage(tc, "VERIFICATION_KEY_ERROR", clientId, nlsMessage);
    }

}
