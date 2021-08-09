/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.token;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class JWTTokenValidationFailedException extends Exception {

    public static final String ACCESS_DENIED = "access_denied";

    private static final TraceComponent tc = Tr.register(JWTTokenValidationFailedException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static final long serialVersionUID = 1L;

    public JWTTokenValidationFailedException(String message) {
        super(message, null);
    }

    public JWTTokenValidationFailedException(String message, Exception e) {
        super(message, e);
    }

    public static JWTTokenValidationFailedException format(String msgKey, Object... objs) {
        return format(tc, msgKey, objs);
    }

    public static JWTTokenValidationFailedException format(TraceComponent tc, String msgKey, Object[] objs) {
        String message = Tr.formatMessage(tc, msgKey, objs);
        return new JWTTokenValidationFailedException(message);
    }

    /**
     * @return the error response associated with this OAuth 2.0 exception. These
     *         errors correspond to the mandated error field in OAuth 2.0 protocol.
     */
    public String getError() {
        return ACCESS_DENIED;
    }

}
