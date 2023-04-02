/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.utility;

/**
 *
 */
public enum SpringBootUtilityReturnCodes {
    OK(0),
    ERR_GENERIC(1),
    ERR_APP_NOT_FOUND(2),
    ERR_APP_DEST_IS_DIR(3),
    ERR_LIB_DEST_IS_FILE(4),
    ERR_MAKE_DIR(5);

    final int rc;

    private SpringBootUtilityReturnCodes(int val) {
        rc = val;
    }

    int getReturnCode() {
        return rc;
    }

}
