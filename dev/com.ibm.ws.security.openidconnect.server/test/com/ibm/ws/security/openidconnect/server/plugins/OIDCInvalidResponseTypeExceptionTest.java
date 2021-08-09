/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.plugins;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;

public class OIDCInvalidResponseTypeExceptionTest {

    @Test
    public void testContruction1() {
        String responseType = "bad";
        OIDCInvalidResponseTypeException rte = new OIDCInvalidResponseTypeException(OAuth20Exception.UNSUPPORTED_RESPONSE_TPE, "response_type " + responseType, null);
        assertEquals("error type should be " + OAuth20Exception.UNSUPPORTED_RESPONSE_TPE, OAuth20Exception.UNSUPPORTED_RESPONSE_TPE, rte.getError());
    }
}
