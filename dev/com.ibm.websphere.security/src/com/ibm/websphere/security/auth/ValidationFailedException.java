/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

    private static final long serialVersionUID = -5920252763989441670L;

    /**
     * <p>
     * A default constructor.
     * </p>
     */
    public ValidationFailedException() {
        super();
    }

    /**
     * <p>
     * A constructor that accepts an error message. The error message can be retrieved
     * using the getMessage() API.
     * </p>
     *
     * @param str An error message.
     */
    public ValidationFailedException(String str) {
        super(str);
    }

    /**
     * <p>
     * A constructor that accepts a Throwable. The Throwable can be retrieved
     * using the getExceptions() or getCause() API.
     * </p>
     *
     * @param t Any exception type that extends Throwable.
     */
    public ValidationFailedException(Throwable t) {
        super(t);
    }

    /**
     * <p>
     * A constructor accepts an error message and original exception. The exception
     * will be added to an ArrayList and other exceptions may be added along the way.
     * The error message can be retrieved using the getMessage() API.
     * </p>
     *
     * @param str An error message.
     * @param t Any exception type that extends Throwable.
     */
    public ValidationFailedException(String str, Throwable t) {
        super(str, t);
    }
}
