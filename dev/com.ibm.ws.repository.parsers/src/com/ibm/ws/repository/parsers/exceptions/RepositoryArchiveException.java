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

import com.ibm.ws.repository.exceptions.RepositoryException;

public class RepositoryArchiveException extends RepositoryException {

    private static final long serialVersionUID = 688268214806214239L;

    private final File archive;

    public RepositoryArchiveException(String message, File archive) {
        super(message);
        this.archive = archive;
    }

    public RepositoryArchiveException(String message, File archive, Throwable cause) {
        super(message, cause);
        this.archive = archive;
    }

    public File getArchive() {
        return this.archive;
    }
}
