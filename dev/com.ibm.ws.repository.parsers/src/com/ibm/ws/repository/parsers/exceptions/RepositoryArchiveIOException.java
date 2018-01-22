/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

package com.ibm.ws.repository.parsers.exceptions;

import java.io.File;

public class RepositoryArchiveIOException extends RepositoryArchiveException {

    private static final long serialVersionUID = -7601011242428876009L;

    public RepositoryArchiveIOException(String message, File archive) {
        super(message, archive);
    }

    public RepositoryArchiveIOException(String message, File archive, Throwable cause) {
        super(message, archive, cause);
    }
}
