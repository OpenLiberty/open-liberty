/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ras;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import test.LoggingTestUtils;

/**
 * <p>
 * This is a base class for testing the PasswordImpl class.
 * </p>
 * 
 * <p>
 * SIB build component: sib.security.impl unittests
 * </p>
 * 
 * @author timoward
 * @version 1.0
 * @since 1.0
 */
public class ProtectedStringTest {
    /** The serial version UID for this class */
    private static final long serialVersionUID = 7357505859619169423L;

    static {
        LoggingTestUtils.ensureLogManager();
    }

    @Test
    public void testNULLPASSWORD() {
        assertNull("NULL_PROTECTED_STRING.getChars() isn't null!", ProtectedString.NULL_PROTECTED_STRING.getChars());
    }

    @Test
    public void testEMPTYPASSWORD() {
        assertEquals("EMPTY_PROTECTED_STRING.getPasswordChars().size() isn't 0", 0, ProtectedString.EMPTY_PROTECTED_STRING.getChars().length);
    }

    /**
     * Test the toTraceString method with the standard JCE Provider
     */
    @Test
    public void testTraceString() {

        final char[] password1 = "pa55w0rd1".toCharArray();
        final char[] password2 = "pa55w0rd2".toCharArray();

        ProtectedString p1 = new ProtectedString(password1);
        ProtectedString p2 = new ProtectedString(password2);
        ProtectedString p3 = new ProtectedString(password1);

        assertEquals("Call to toTraceString() gave different results for the same object.", p1.toTraceString(), p1.toTraceString());
        assertEquals("Call to toTraceString() gave different results for the same object.", p2.toTraceString(), p2.toTraceString());
        assertEquals("Obfuscation of password produced different results for the same password. " + p1.toTraceString() + ", " + p3.toTraceString(), p1.toTraceString(),
                     p3.toTraceString());
        assertFalse("Obfuscation of password produced the same result for the different passwords. " + p1.toTraceString() + ", " + p2.toTraceString(),
                    p1.toTraceString().equals(p2.toTraceString()));
        assertFalse("Obfuscation of password produced the plain text password!", p1.toTraceString().equals(password1));
        assertFalse("Obfuscation of password produced the plain text password!", p2.toTraceString().equals(password2));
    }

    @Test
    public void testGetProtectedString() {
        final char[] password = "pa55w0rd".toCharArray();

        ProtectedString p = new ProtectedString(password);

        assertArrayEquals("Call to getProtectedString didn't return the right character array", password, p.getChars());
    }

    @Test
    public void testToString() {
        ProtectedString p = new ProtectedString("p4ssword".toCharArray());

        assertEquals("Didn't star out non-null password", "*****", p.toString());
        assertEquals("Didn't star out non-null password", "*****", ProtectedString.EMPTY_PROTECTED_STRING.toString());
        assertEquals("Didn't return \"null\" for null password", "null", ProtectedString.NULL_PROTECTED_STRING.toString());
    }

    @Test
    public void testEquals() {
        ProtectedString p1 = new ProtectedString("one".toCharArray());
        ProtectedString p2 = new ProtectedString("two".toCharArray());
        ProtectedString p11 = new ProtectedString("one".toCharArray());

        assertEquals(p1, p1);
        assertEquals(p1, p11);
        assertEquals(p11, p1);

        assertFalse(p1.equals(null));
        assertFalse(p1.equals(p2));
        assertFalse(p2.equals(p1));

        assertFalse(p1.equals("one"));
    }
}
