/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient11.cdiPropsAndProviders;

/**
 *
 */
public class MyException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public MyException() {
        super();
    }

    /**
     * @param message
     */
    public MyException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public MyException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public MyException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public MyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
