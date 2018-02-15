/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.utility.cmdline;

import com.ibm.ws.kernel.boot.cmdline.ExitCode;

/**
 * This API contains return code values used on exit.
 */
public enum ReturnCode implements ExitCode {
    OK(0),
    // Jump a few numbers for error return codes
    BAD_ARGUMENT(20),
    RUNTIME_EXCEPTION(21),
    ALREADY_EXISTS(22),
    BAD_FEATURE_DEFINITION(23),
    MISSING_CONTENT(24),
    IO_FAILURE(25),
    PRODUCT_EXT_NOT_FOUND(26),
    PRODUCT_EXT_NOT_DEFINED(27),
    PRODUCT_EXT_NO_FEATURES_FOUND(28),
    NOT_VALID_FOR_CURRENT_PRODUCT(29),
    REPO_PROPS_VALIDATION_FAILED(30),
    CONNECTION_FAILED(33),
    REPOSITORY_NOT_FOUND(34),
    USER_ABORT(35),
    USER_FEATURE_REPO_TYPE_INVALID(36),
    BAD_CONNECTION_FOUND(1000);

    final int val;

    /**
     *
     * @param val
     */
    ReturnCode(int val) {
        this.val = val;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getValue() {
        return val;
    }
}
