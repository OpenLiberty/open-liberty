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
package com.ibm.websphere.security.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * Simple test to verify exception behaviour.
 */
public class CredentialDestroyedExceptionTest {

    /**
     * Test method for {@link com.ibm.websphere.security.auth.CredentialDestroyedException#CredentialDestroyedException(java.lang.String)}.
     */
    @Test
    public void constructorString() {
        assertNotNull("Constructor(String) should succeed",
                      new CredentialDestroyedException("msg"));
    }

    /**
     * Test method for {@link com.ibm.websphere.security.auth.CredentialDestroyedException#CredentialDestroyedException(java.lang.Throwable)}.
     */
    @Test
    public void constructorCause() {
        Exception cause = new Exception("Cause");
        CredentialDestroyedException ex = new CredentialDestroyedException(cause);
        assertNotNull("Constructor(Throwable) should succeed", ex);
        assertSame("Cause should be set", cause, ex.getCause());
    }

    /**
     * Test method for {@link com.ibm.websphere.security.auth.CredentialDestroyedException#CredentialDestroyedException(java.lang.String, java.lang.Throwable)}.
     */
    @Test
    public void constructorStringCause() {
        Exception cause = new Exception("Cause");
        CredentialDestroyedException ex = new CredentialDestroyedException("msg", cause);
        assertNotNull("Constructor(String,Throwable) should succeed", ex);
        assertEquals("Should see expected message", "msg", ex.getMessage());
        assertSame("Cause should be set", cause, ex.getCause());
    }

}
