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

import com.ibm.ws.ui.internal.rest.exceptions.MethodNotSupportedException;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;

/**
 *
 */
public class MethodNotSupportedExceptionTest {

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.exceptions.MethodNotSupportedException#MethodNotSupportedException()}.
     */
    @Test
    public void MethodNotSupportedException() {
        RESTException e = new MethodNotSupportedException();
        assertEquals("Exception status should be 405",
                     405, e.getStatus());
        assertNull("Status-only constructor should have no content type",
                   e.getContentType());
        assertNull("Status-only constructor should have no payload",
                   e.getPayload());
    }

}
