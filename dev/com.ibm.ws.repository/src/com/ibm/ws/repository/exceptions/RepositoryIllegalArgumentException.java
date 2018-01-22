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

public class RepositoryIllegalArgumentException extends RepositoryException {

    /**  */
    private static final long serialVersionUID = 8048008267474528329L;

    public RepositoryIllegalArgumentException() {
        super();
    }

    public RepositoryIllegalArgumentException(String message) {
        super(message);
    }

    public RepositoryIllegalArgumentException(Throwable cause) {
        super(cause);
    }

    public RepositoryIllegalArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public Throwable getCause() {
        return super.getCause();
    }

}