/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class SignatureAlgorithmDoesNotMatchHeaderException extends Exception {

    private static final TraceComponent tc = Tr.register(SignatureAlgorithmDoesNotMatchHeaderException.class);

    private static final long serialVersionUID = -4376636239462726717L;

    private final String expectedSignatureAlgorithm;
    private final String sigAlgInHeader;

    public SignatureAlgorithmDoesNotMatchHeaderException(String expectedSignatureAlgorithm, String sigAlgInHeader) {
        this.expectedSignatureAlgorithm = expectedSignatureAlgorithm;
        this.sigAlgInHeader = sigAlgInHeader;
    }

    @Override
    public String getMessage() {
        return Tr.formatMessage(tc, "EXPECTED_SIG_ALG_DOES_NOT_MATCH_HEADER", sigAlgInHeader, expectedSignatureAlgorithm);
    }

}
