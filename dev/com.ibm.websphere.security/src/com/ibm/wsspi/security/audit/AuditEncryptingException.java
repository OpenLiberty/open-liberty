/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.wsspi.security.audit;

/**
 *
 */
public class AuditEncryptingException extends java.lang.Exception {
    public AuditEncryptingException() {
        super();
    }

    public AuditEncryptingException(String message) {
        super(message);
    }

    public AuditEncryptingException(Exception e) {
        super(e);
    }

    public AuditEncryptingException(String message, Exception e) {
        super(message, e);
    }
}
