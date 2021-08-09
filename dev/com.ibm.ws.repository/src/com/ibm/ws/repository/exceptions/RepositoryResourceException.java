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

public abstract class RepositoryResourceException extends RepositoryException {

    private static final long serialVersionUID = -210111935495059084L;

    private final String resourceId;

    protected RepositoryResourceException(String message, String resourceId) {
        super(message);
        this.resourceId = resourceId;
    }

    protected RepositoryResourceException(String message, String resourceId, Throwable cause) {
        super(message, cause);
        this.resourceId = resourceId;
    }

    @Override
    public Throwable getCause() {
        return super.getCause();
    }

    public String getResourceId() {
        return this.resourceId;
    }
}
