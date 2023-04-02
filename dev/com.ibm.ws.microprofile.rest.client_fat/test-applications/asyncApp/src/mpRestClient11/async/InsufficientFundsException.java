/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient11.async;


@SuppressWarnings("serial")
public class InsufficientFundsException extends Exception {

    /**
     * 
     */
    public InsufficientFundsException() {
    }

    /**
     * @param message
     */
    public InsufficientFundsException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public InsufficientFundsException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public InsufficientFundsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
