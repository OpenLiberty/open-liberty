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
package componenttest.exception;

/**
 * This exception is thrown to indicate that the expected string could not be
 * found in the logs
 */
public class NoStringFoundInLogException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoStringFoundInLogException() {
        super();
    }

    public NoStringFoundInLogException(String message) {
        super(message);
    }

    public NoStringFoundInLogException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoStringFoundInLogException(Throwable cause) {
        super(cause);
    }

}
