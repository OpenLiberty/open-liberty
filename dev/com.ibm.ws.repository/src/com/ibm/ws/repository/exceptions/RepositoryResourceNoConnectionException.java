/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.exceptions;

/**
 * This is thrown when an operation is attempted on a resource that requires a connection and the resource does not have a connection.
 */
public class RepositoryResourceNoConnectionException extends RepositoryResourceException {

    private static final long serialVersionUID = 1;

    public RepositoryResourceNoConnectionException(String message, String resourceId) {
        super(message, resourceId);
    }

}
