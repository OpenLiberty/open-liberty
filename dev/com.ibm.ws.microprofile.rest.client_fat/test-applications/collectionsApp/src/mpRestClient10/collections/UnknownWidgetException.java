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
package mpRestClient10.collections;

public class UnknownWidgetException extends Exception {
    private static final long serialVersionUID = 1L;

    public UnknownWidgetException() {
        super();
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public UnknownWidgetException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * @param message
     * @param cause
     */
    public UnknownWidgetException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     */
    public UnknownWidgetException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public UnknownWidgetException(Throwable cause) {
        super(cause);
    }

}
