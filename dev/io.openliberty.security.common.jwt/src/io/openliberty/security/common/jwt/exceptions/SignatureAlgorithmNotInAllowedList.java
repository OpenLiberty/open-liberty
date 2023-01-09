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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.common.jwt.exceptions;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class SignatureAlgorithmNotInAllowedList extends Exception {

    private static final TraceComponent tc = Tr.register(SignatureAlgorithmNotInAllowedList.class);

    private static final long serialVersionUID = 5850783893619480595L;

    private final String sigAlgInHeader;
    private final List<String> signatureAlgorithmsAllowed;

    public SignatureAlgorithmNotInAllowedList(String sigAlgInHeader, List<String> signatureAlgorithmsAllowed) {
        this.sigAlgInHeader = sigAlgInHeader;
        this.signatureAlgorithmsAllowed = new ArrayList<>(signatureAlgorithmsAllowed);
    }

    @Override
    public String getMessage() {
        return Tr.formatMessage(tc, "SIG_ALG_IN_HEADER_NOT_ALLOWED", sigAlgInHeader, signatureAlgorithmsAllowed);
    }

}
