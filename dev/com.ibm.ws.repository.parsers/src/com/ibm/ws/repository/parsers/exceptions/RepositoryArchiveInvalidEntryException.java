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
