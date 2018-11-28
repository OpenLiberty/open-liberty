/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
