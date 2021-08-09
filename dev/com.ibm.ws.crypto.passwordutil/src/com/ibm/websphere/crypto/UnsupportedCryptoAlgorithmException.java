/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.crypto;

/**
 * Exception thrown when a requested crypto algorithm (ie. "xor") is unknown
 * by the runtime conversion code.
 */
public class UnsupportedCryptoAlgorithmException extends Exception {
    private static final long serialVersionUID = -1038233301900299114L;

    /**
     * Constructor.
     */
    public UnsupportedCryptoAlgorithmException() {
        super();
    }

}
