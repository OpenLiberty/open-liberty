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
 * The specified filter syntax was invalid, or the specified attribute and
 * filter comparison combination is not supported.
 */
public class InvalidFilterException extends SCIMException {

    private static final long serialVersionUID = -4872756520888058312L;

    public InvalidFilterException(String msg) {
        super(400, "invalidFilter", msg);
    }
}
