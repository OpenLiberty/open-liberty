/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2014, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install;

/**
 * This exception indicates that an exception occurs during reapply fixes.
 */
public class ReapplyFixException extends InstallException {

    private static final long serialVersionUID = 5252063204794357335L;

    /**
     * Create Reapply Fix Exception with a Throwable and return code.
     *
     * @param message Exception message
     * @param cause Throwable cause of exception
     * @param rc Return Code
     */
    public ReapplyFixException(String message, Throwable cause, int rc) {
        super(message, cause, rc);
    }

    /**
     * Create Reapply Fix Exception with message only.
     *
     * @param message Exception message
     * @param runtimeException
     */
    public ReapplyFixException(String message) {
        super(message);
    }
}
