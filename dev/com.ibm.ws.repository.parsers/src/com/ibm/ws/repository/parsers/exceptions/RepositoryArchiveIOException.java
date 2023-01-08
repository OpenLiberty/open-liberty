/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
