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
 * Service provider does not support the request operation, e.g., PATCH.
 */
public class NotImplementedException extends SCIMException {

    private static final long serialVersionUID = 3289063968186049112L;

    public NotImplementedException(String msg) {
        super(501, null, msg);
    }
}
