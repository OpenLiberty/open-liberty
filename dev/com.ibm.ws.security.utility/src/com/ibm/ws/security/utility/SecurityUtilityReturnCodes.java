/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility;

/**
 *
 */
public enum SecurityUtilityReturnCodes {
    OK(0),
    ERR_GENERIC(1),

    ERR_SERVER_NOT_FOUND(2),
    ERR_CLIENT_NOT_FOUND(3),
    ERR_PATH_CANNOT_BE_CREATED(4),
    ERR_FILE_EXISTS(5);

    final int rc;

    private SecurityUtilityReturnCodes(int val) {
        rc = val;
    }

    int getReturnCode() {
        return rc;
    }

}
