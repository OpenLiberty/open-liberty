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
package componenttest.exception;

public class UnavailableDatabaseException extends Exception {

    public UnavailableDatabaseException() {
        super();
    }

    public UnavailableDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnavailableDatabaseException(String message) {
        super(message);

    }

    public UnavailableDatabaseException(Throwable cause) {
        super(cause);
    }

    private static final long serialVersionUID = 1L;
}
