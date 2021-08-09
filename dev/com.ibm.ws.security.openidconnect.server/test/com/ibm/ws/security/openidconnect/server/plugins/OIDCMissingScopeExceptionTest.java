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

/**
 *
 */
public class OIDCMissingScopeExceptionTest {

    @Test
    public void testContruction1() {
        OIDCMissingScopeException mse = new OIDCMissingScopeException(OAuth20Exception.INVALID_SCOPE, "When calling response_type id_token, one of the scopes has to be 'openid'", null);
        assertEquals("Error type should be an " + OAuth20Exception.INVALID_SCOPE, OAuth20Exception.INVALID_SCOPE, mse.getError());
    }
}
