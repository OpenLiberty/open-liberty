/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.common.jwt.exceptions;

public class SignatureAlgorithmDoesNotMatchHeaderException extends Exception {

    private static final long serialVersionUID = -4376636239462726717L;

    private final String signatureAlgorithm;
    private final String sigAlgInHeader;

    public SignatureAlgorithmDoesNotMatchHeaderException(String signatureAlgorithm, String sigAlgInHeader) {
        this.signatureAlgorithm = signatureAlgorithm;
        this.sigAlgInHeader = sigAlgInHeader;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public String getSigAlgInHeader() {
        return sigAlgInHeader;
    }

}
