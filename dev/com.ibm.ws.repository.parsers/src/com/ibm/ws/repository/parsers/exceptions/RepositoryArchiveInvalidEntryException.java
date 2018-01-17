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

public class RepositoryArchiveInvalidEntryException extends RepositoryArchiveException {

    private static final long serialVersionUID = 736980083399631714L;

    private final String entryPath;

    public RepositoryArchiveInvalidEntryException(String message, File archive, String entryPath) {
        super(message, archive);
        this.entryPath = entryPath;
    }

    public RepositoryArchiveInvalidEntryException(String message, File archive, String entryPath, Throwable cause) {
        super(message, archive, cause);
        this.entryPath = entryPath;
    }

    public String getEntryPath() {
        return entryPath;
    }
}
