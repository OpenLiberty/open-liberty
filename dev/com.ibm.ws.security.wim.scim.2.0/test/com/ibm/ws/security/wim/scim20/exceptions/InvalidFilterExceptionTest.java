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

package com.ibm.ws.security.wim.scim20.exceptions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.websphere.security.wim.scim20.exceptions.InvalidFilterException;
import com.ibm.ws.security.wim.scim20.model.ErrorImpl;

public class InvalidFilterExceptionTest {
    @Test
    public void asJson() throws Exception {
        String msg = "Invalid filter was provided.";
        InvalidFilterException e = new InvalidFilterException(msg);

        String expected = "";
        expected += "{\n";
        expected += "  \"schemas\" : [ \"" + ErrorImpl.SCHEMA_URI + "\" ],\n";
        expected += "  \"detail\" : \"" + msg + "\",\n";
        expected += "  \"scimType\" : \"invalidFilter\",\n";
        expected += "  \"status\" : 400\n";
        expected += "}";
        assertEquals(expected, e.asJson());
    }
}
