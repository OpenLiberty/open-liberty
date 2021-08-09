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

package com.ibm.ws.security.wim.scim20.exceptions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.websphere.security.wim.scim20.exceptions.AuthorizationException;
import com.ibm.ws.security.wim.scim20.model.ErrorImpl;

public class AuthorizationExceptionTest {
    @Test
    public void asJson() throws Exception {
        String msg = "Forbidden.";
        AuthorizationException e = new AuthorizationException(msg, null);

        String expected = "";
        expected += "{\n";
        expected += "  \"schemas\" : [ \"" + ErrorImpl.SCHEMA_URI + "\" ],\n";
        expected += "  \"detail\" : \"" + msg + "\",\n";
        expected += "  \"status\" : 403\n";
        expected += "}";
        assertEquals(expected, e.asJson());
    }
}
