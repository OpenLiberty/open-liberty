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
 * The specified SCIM protocol version is not supported (see Section 3.13).
 */
public class InvalidVersionException extends SCIMException {

    private static final long serialVersionUID = -8391381416886717795L;

    public InvalidVersionException(String msg) {
        super(400, "invalidVers", msg);
    }
}
