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

public class IDTokenValidationFailedException extends JWTTokenValidationFailedException {

    private static final TraceComponent tc = Tr.register(IDTokenValidationFailedException.class, TraceConstants.TRACE_GROUP,
                                                         TraceConstants.MESSAGE_BUNDLE);

    private static final long serialVersionUID = 1L;

    public IDTokenValidationFailedException(String message) {
        super(message);
    }

    public IDTokenValidationFailedException(String message, Exception e) {
        super(message, e);
    }

    public static IDTokenValidationFailedException format(String msgKey, Object... objs) {
        return format(tc, msgKey, objs);
    }

    public static IDTokenValidationFailedException format(TraceComponent tc, String msgKey, Object... objs) {
        String message = Tr.formatMessage(tc, msgKey, objs);
        return new IDTokenValidationFailedException(message);
    }

}
