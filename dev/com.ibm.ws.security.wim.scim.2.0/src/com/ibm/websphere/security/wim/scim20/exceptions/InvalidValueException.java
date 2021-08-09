/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.security.wim.scim20.exceptions;

/**
 * A required value was missing, or the value specified was not compatible with
 * the operation or attribute type (see Section 2.2 of [RFC7643]), or resource
 * schema (see Section 4 of [RFC7643]).
 */
public class InvalidValueException extends SCIMException {

    private static final long serialVersionUID = -1712217056199657063L;

    public InvalidValueException(String msg) {
        super(400, "invalidValue", msg);
    }
}
