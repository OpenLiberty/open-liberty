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
 * The attempted modification is not compatible with the target attribute's
 * mutability or current state (e.g., modification of an "immutable" attribute
 * with an existing value).
 */
public class MutabilityException extends SCIMException {

    private static final long serialVersionUID = 8298420250436704987L;

    public MutabilityException(String msg) {
        super(400, "mutability", msg);
    }
}
