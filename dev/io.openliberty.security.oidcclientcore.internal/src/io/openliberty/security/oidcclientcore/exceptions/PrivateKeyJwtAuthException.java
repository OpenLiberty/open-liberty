/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.exceptions;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class PrivateKeyJwtAuthException extends Exception {

    public static final TraceComponent tc = Tr.register(PrivateKeyJwtAuthException.class);

    private static final long serialVersionUID = 1L;

    private final String configurationId;
    private final String nlsMessage;

    public PrivateKeyJwtAuthException(String configurationId, String nlsMessage) {
        this.configurationId = configurationId;
        this.nlsMessage = nlsMessage;
    }

    @Override
    public String getMessage() {
        return Tr.formatMessage(tc, "PRIVATE_KEY_JWT_AUTH_ERROR", configurationId, nlsMessage);
    }

}
