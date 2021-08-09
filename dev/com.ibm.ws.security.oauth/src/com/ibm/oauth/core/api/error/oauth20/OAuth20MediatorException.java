/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.api.error.oauth20;

import java.util.Locale;

/**
 * Represents a mediation exception in an OAuth request.
 * The component consumer that implements OAuth20Mediator throws this
 * exception if an error occurs during Mediator processing.
 */

public class OAuth20MediatorException extends OAuth20Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a OAuth20MediatorException.
     * 
     * @param msg the error message.
     * @param cause the root cause.
     */
    public OAuth20MediatorException(String msg, Throwable cause) {
        /*
         * SERVER_ERROR is not valid for token and resource end point but it
         * best describes this exception. Component consumers can always handle
         * this exception differently for each end point in order to be protocol
         * compliant. Typically it will be the consumer's themselves who have
         * caused this in the first place due to their mediator
         * implementation(s).
         */
        super(SERVER_ERROR, msg, cause);
    }

    @Override
    public String formatSelf(Locale locale, String encoding) {
        // Do not translate mediator exception messages.
        return getMessage();
    }
}
