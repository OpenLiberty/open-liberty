/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.validation;

import com.ibm.ws.ui.persistence.InvalidPOJOException;

/**
 *
 */
public class InvalidToolException extends InvalidPOJOException {
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public InvalidToolException(String message) {
        super(message);
    }

}
