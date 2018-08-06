/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.wsspi.anno.classsource;

public class ClassSource_Exception extends Exception {
    private static final long serialVersionUID = 1L;

    public static final String CLASS_NAME = "ClassSource_Exception";

    //

    public ClassSource_Exception(String message) {
        super(message);
    }

    public ClassSource_Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
