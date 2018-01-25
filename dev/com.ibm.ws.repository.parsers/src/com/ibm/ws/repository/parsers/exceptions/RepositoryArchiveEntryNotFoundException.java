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

public class RepositoryArchiveEntryNotFoundException extends RepositoryArchiveInvalidEntryException {

    private static final long serialVersionUID = 5182741767440154685L;

    public RepositoryArchiveEntryNotFoundException(String message, File archive, String entryPath) {
        super(message, archive, entryPath);
    }

    public RepositoryArchiveEntryNotFoundException(String message, File archive, String entryPath, Throwable cause) {
        super(message, archive, entryPath, cause);
    }

}
