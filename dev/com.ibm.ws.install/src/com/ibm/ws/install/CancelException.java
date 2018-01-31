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
package com.ibm.ws.install;

/**
 * This class provides APIs for an Exception caused by a Cancelled Installation.
 */
public class CancelException extends InstallException {

    private static final long serialVersionUID = -487184439522887816L;

    /**
     * This method creates a CancelException
     *
     * @param message Exception message
     * @param rc Return code
     */
    public CancelException(String message, int rc) {
        super(message, rc);
    }

}
