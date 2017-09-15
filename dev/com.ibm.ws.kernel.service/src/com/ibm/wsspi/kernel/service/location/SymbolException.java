/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.location;

/**
 *
 */
public class SymbolException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Create an unchecked exception with the specified localized
     * message for the invalid path.
     * 
     * @param string
     *            exception message
     */
    public SymbolException(String string) {
        super(string);
    }
}
