/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.exceptions;

import com.ibm.ws.repository.transport.exceptions.BadVersionException;

/**
 * Use this when we pull invalid objects out of the repository. Objects are invalid if,
 * for instance, they fail a VersionableContent.verify() check.
 *
 */
public class RepositoryBadDataException extends RepositoryResourceException {

    private static final long serialVersionUID = 1L;
    private final BadVersionException badVersion;

    public RepositoryBadDataException(String message, String resource, BadVersionException bvx) {
        super(message, resource);
        initCause(bvx);
        badVersion = bvx;
    }

    @Override
    public Throwable getCause() {
        return super.getCause();
    }

    public String getMinVersion() {
        return badVersion.getMinVersion();
    }

    public String getMaxVersion() {
        return badVersion.getMaxVersion();
    }

    public String getBadVersion() {
        return badVersion.getBadVersion();
    }

}
