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
 * One or more of the attribute values are already in use or are reserved.
 */
public class UniquenessException extends SCIMException {
    private static final long serialVersionUID = -1927663170021818129L;

    public UniquenessException(String msg) {
        super(409, "uniqueness", msg);
    }
}
