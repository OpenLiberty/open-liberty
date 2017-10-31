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
public class AuditDecryptionException extends java.lang.Exception {
    public AuditDecryptionException() {
        super();
    }

    public AuditDecryptionException(String message) {
        super(message);
    }

    public AuditDecryptionException(Exception e) {
        super(e);
    }

    public AuditDecryptionException(String message, Exception e) {
        super(message, e);
    }
}
