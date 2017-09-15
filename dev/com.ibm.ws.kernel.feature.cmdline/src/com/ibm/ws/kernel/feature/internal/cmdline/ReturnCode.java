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
package com.ibm.ws.kernel.feature.internal.cmdline;

import com.ibm.ws.kernel.boot.cmdline.ExitCode;

/**
 *
 */
public enum ReturnCode implements ExitCode {
    OK(0),
    // Jump a few numbers for error return codes
    USER_FEATURE_REPO_TYPE_INVALID(36),
    BAD_ARGUMENT(20),
    RUNTIME_EXCEPTION(21),
    ALREADY_EXISTS(22),
    BAD_FEATURE_DEFINITION(23),
    MISSING_CONTENT(24),
    IO_FAILURE(25),
    PRODUCT_EXT_NOT_FOUND(26),
    PRODUCT_EXT_NOT_DEFINED(27),
    PRODUCT_EXT_NO_FEATURES_FOUND(28),
    NOT_VALID_FOR_CURRENT_PRODUCT(29);

    final int val;

    ReturnCode(int val) {
        this.val = val;
    }

    @Override
    public int getValue() {
        return val;
    }
}
