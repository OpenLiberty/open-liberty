/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install;

import java.util.ArrayList;
import java.util.List;

/**
 * This exception indicates that there is an installation exception.
 */
public class InstallException extends Exception {

    private static final long serialVersionUID = -2397755301510300348L;
    public static final int BAD_ARGUMENT = 20; // same as ReturnCode.BAD_ARGUMENT
    public static final int RUNTIME_EXCEPTION = 21; // same as ReturnCode.RUNTIME_EXCEPTION
    public static final int ALREADY_EXISTS = 22; // same as ReturnCode.ALREADY_EXISTS
    public static final int BAD_FEATURE_DEFINITION = 23; // same as ReturnCode.BAD_FEATURE_DEFINITION
    public static final int MISSING_CONTENT = 24; // same as ReturnCode.MISSING_CONTENT
    public static final int IO_FAILURE = 25; // same as ReturnCode.IO_FAILURE
    public static final int NOT_VALID_FOR_CURRENT_PRODUCT = 29; // same as ReturnCode.NOT_VALID_FOR_CURRENT_PRODUCT
    public static final int CONNECTION_FAILED = 33; // same as ReturnCode.CONNECTION_FAILED for installUtility
    public static final int USER_FEATURE_REPO_TYPE_INVALID = 36; //same as ReturnCode.USER_FEATURE_REPO_TYPE_INVALID for installUtility

    int rc = RUNTIME_EXCEPTION;
    List<Object> data = new ArrayList<Object>();

    /**
     * Creates an Install Exception with a cause and return code.
     *
     * @param message Exception message
     * @param cause Exception cause
     * @parm rc Return code
     */
    public InstallException(String message, Throwable cause, int rc) {
        super(message, cause);
        this.rc = rc;
    }

    /**
     * Creates an Install Exception with a message only.
     *
     * @param message Exception message
     */
    public InstallException(String message) {
        super(message);
    }

    /**
     * Creates an Install Exception with a return code.
     *
     * @param message Exception message
     * @parm rc Return Code
     */
    public InstallException(String message, int rc) {
        super(message);
        this.rc = rc;
    }

    /**
     * Gets the return code.
     *
     * @return the rc
     */
    public int getRc() {
        return rc;
    }

    /**
     * Gets the associated data.
     *
     * @return the Data
     */
    public List<Object> getData() {
        return data;
    }

    /**
     * @param Data the Data to set
     */
    public void setData(Object... objects) {
        for (Object s : objects) {
            this.data.add(s);
        }
    }

}
