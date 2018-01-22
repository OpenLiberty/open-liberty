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

public class RepositoryResourceCreationException extends RepositoryResourceException {

    private static final long serialVersionUID = -6700922237337244301L;

    public RepositoryResourceCreationException(String message, String resourceId) {
        super(message, resourceId);
    }

    public RepositoryResourceCreationException(String message, String resourceId, Throwable cause) {
        super(message, resourceId, cause);
    }

}
