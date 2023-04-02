/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.common.jwt.exceptions;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class SigningKeyNotSpecifiedException extends Exception {

    private static final long serialVersionUID = 1L;

    private static final TraceComponent tc = Tr.register(SigningKeyNotSpecifiedException.class);

    private final String algHeader;

    public SigningKeyNotSpecifiedException(String algHeader) {
        this.algHeader = algHeader;
    }

    @Override
    public String getMessage() {
        return Tr.formatMessage(tc, "SIGNING_KEY_NOT_SPECIFIED", algHeader);
    }

}
