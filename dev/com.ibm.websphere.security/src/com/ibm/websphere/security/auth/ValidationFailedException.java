/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.auth;

/**
 *
 */
public class ValidationFailedException extends InvalidTokenException {

    private static final long serialVersionUID = -5920252763989441670L; //@vj1: Take versioning into account if incompatible changes are made to this class

    public ValidationFailedException() {
        super();
    }

    public ValidationFailedException(String debug_message) {
        super(debug_message);
    }

    public ValidationFailedException(Throwable t) {
        super(t);
    }

    public ValidationFailedException(String debug_message, Throwable t) {
        super(debug_message, t);
    }
}
