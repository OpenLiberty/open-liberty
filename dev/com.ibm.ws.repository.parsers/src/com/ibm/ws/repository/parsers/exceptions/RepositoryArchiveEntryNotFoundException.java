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

public class RepositoryArchiveEntryNotFoundException extends RepositoryArchiveInvalidEntryException {

    private static final long serialVersionUID = 5182741767440154685L;

    public RepositoryArchiveEntryNotFoundException(String message, File archive, String entryPath) {
        super(message, archive, entryPath);
    }

    public RepositoryArchiveEntryNotFoundException(String message, File archive, String entryPath, Throwable cause) {
        super(message, archive, entryPath, cause);
    }

}
