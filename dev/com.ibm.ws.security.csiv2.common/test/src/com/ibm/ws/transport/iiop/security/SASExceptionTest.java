/*
 * Copyright 2014, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.transport.iiop.security;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class SASExceptionTest {

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.SASException#getErrorToken()}.
     */
    @Test
    public void testGetErrorToken() {
        SASException sasException = new SASException(1);
        assertNotNull("There must be an error token.", sasException.getErrorToken());
    }
}
