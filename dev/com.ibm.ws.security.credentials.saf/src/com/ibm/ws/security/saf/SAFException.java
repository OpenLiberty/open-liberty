/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saf;

/**
 * SAFException is the Java representation of a native SAF service failure.
 * The exception message contains information about the SAF failure, including
 * the service that failed, the SAF router return code, the SAF product return
 * code, and the SAF product reason code.
 */
public class SAFException extends Exception {

    SAFServiceResult safServiceResult = null;
    String message;

    /**
     * CTOR.
     */
    public SAFException(SAFServiceResult safServiceResult) {
        this.safServiceResult = safServiceResult;
    }

    /**
     * CTOR.
     */
    public SAFException(String msg, Throwable ex) {
        super(msg, ex);
        this.message = msg;
    }

    /**
     * CTOR.
     */
    public SAFException(String msg) {
        super(msg);
        this.message = msg;
    }

    /**
     * Determine if the SAF service failure is severe. If the SAFException does not
     * represent a SAF service failure, then this method will return true.
     */
    public boolean isSevere() {
        if (safServiceResult != null)
            return safServiceResult.isSevere();
        return true;
    }

    /**
     * Determine if the SAF service failure is a password expired error. If the SAFException does not
     * represent a SAF service failure, then this method will return false.
     */
    public boolean isPasswordExpiredError() {
        if (safServiceResult != null)
            return safServiceResult.isPasswordExpiredError();
        return false;
    }

    /**
     * Determine if the SAF service failure is a user revoked error. If the SAFException does not
     * represent a SAF service failure, then this method will return false.
     */
    public boolean isUserRevokedError() {
        if (safServiceResult != null)
            return safServiceResult.isUserRevokedError();
        return false;
    }

    /**
     * Determine if the SAF service failure is a Penalty box error. If the SAFException does not
     * represent a Penalty Box failure, then this method will return false.
     */
    public boolean isPenaltyBoxError() {
        if (safServiceResult != null)
            return safServiceResult.isPenaltyBoxError();
        return false;
    }

    @Override
    public String getMessage() {
        if (safServiceResult != null)
            return safServiceResult.getMessage();
        return message;
    }

    /**
     * Log the SAFServiceResult failure if it represents an unexpected failure.
     * If this SAFException does not represent a SAFServiceResult, then this
     * method is a NO-OP.
     */
    public void logIfUnexpected() {
        if (safServiceResult != null)
            safServiceResult.logIfUnexpected();
    }

    /**
     * Getter for SAFServiceResult
     *
     * @return
     */
    public SAFServiceResult getSAFServiceResult() {
        return safServiceResult;
    }

}
