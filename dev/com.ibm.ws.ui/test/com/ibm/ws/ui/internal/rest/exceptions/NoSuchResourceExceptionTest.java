/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.ui.internal.rest.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.ibm.ws.ui.internal.rest.HTTPConstants;
import com.ibm.ws.ui.internal.rest.exceptions.NoSuchResourceException;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;

/**
 *
 */
public class NoSuchResourceExceptionTest {

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.exceptions.NoSuchResourceException#NoSuchResourceException()}.
     */
    @Test
    public void NoSuchResourceException() {
        RESTException e = new NoSuchResourceException();
        assertEquals("Exception status should be 404",
                     404, e.getStatus());
        assertNull("Status-only constructor should have no content type",
                   e.getContentType());
        assertNull("Status-only constructor should have no payload",
                   e.getPayload());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.exceptions.NoSuchResourceException#NoSuchResourceException(String, Object)}.
     */
    @Test
    public void payloadConstructor() {
        RESTException e = new NoSuchResourceException(HTTPConstants.MEDIA_TYPE_TEXT_PLAIN, "hi!");
        assertEquals("Exception status should be 404",
                     404, e.getStatus());
        assertEquals("Content type was not returned as set",
                     HTTPConstants.MEDIA_TYPE_TEXT_PLAIN, e.getContentType());
        assertEquals("Payload was not returned as set",
                     "hi!", e.getPayload());
    }

}
