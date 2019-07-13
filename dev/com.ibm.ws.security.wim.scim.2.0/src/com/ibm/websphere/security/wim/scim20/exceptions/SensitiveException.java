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
 * The specified request cannot be completed, due to the passing of sensitive
 * (e.g., personal) information in a request URI. For example, personal
 * information SHALL NOT be transmitted over request URIs. See Section 7.5.2.
 */
public class SensitiveException extends SCIMException {

    private static final long serialVersionUID = 6592680438161054541L;

    public SensitiveException(String msg) {
        super(403, "sensitive", msg);
    }
}
