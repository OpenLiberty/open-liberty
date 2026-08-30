/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.saf.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.junit.Test;

/**
 *
 */
public class PasswdResultTest {

    /**
     *
     */
    @Test
    public void testEACCES() {

        PasswdResult passwdResult = buildPasswdResult(111, 0);

        assertEquals(111, passwdResult.getErrno());
        assertEquals(0, passwdResult.getErrno2());

        assertTrue(passwdResult.isNormalAuthFailure());
        assertEquals("Unix System Service __passwd failed with errno 111 (EACCES) and errno2 x0", passwdResult.getMessage());
    }

    /**
     *
     */
    @Test
    public void testEINVAL() {

        PasswdResult passwdResult = buildPasswdResult(121, 0x1234);

        assertEquals(121, passwdResult.getErrno());
        assertEquals(0x1234, passwdResult.getErrno2());

        assertTrue(passwdResult.isNormalAuthFailure());
        assertEquals("Unix System Service __passwd failed with errno 121 (EINVAL) and errno2 x1234", passwdResult.getMessage());
    }

    /**
     *
     */
    @Test
    public void testEMVSERR() {

        PasswdResult passwdResult = buildPasswdResult(157, 0x3456);

        assertEquals(157, passwdResult.getErrno());
        assertEquals(0x3456, passwdResult.getErrno2());

        assertFalse(passwdResult.isNormalAuthFailure());
        assertEquals("Unix System Service __passwd failed for user BLAHBLAH with errno 157 (EMVSERR) and errno2 x3456", passwdResult.getMessage("BLAHBLAH"));
    }

    /**
     *
     */
    @Test
    public void testEMVSEXPIRE() {

        PasswdResult.Errno errno = PasswdResult.Errno.EMVSEXPIRE;
        PasswdResult passwdResult = buildPasswdResult(errno.errno, 0x3456);

        assertEquals(errno.errno, passwdResult.getErrno());
        assertEquals(0x3456, passwdResult.getErrno2());

        assertTrue(passwdResult.isNormalAuthFailure());
        assertEquals("Unix System Service __passwd failed with errno " + errno.errno + " (" + errno + ") and errno2 x3456", passwdResult.getMessage());
    }

    /**
     *
     */
    @Test
    public void testEMVSPASSWORD() {

        PasswdResult.Errno errno = PasswdResult.Errno.EMVSPASSWORD;
        PasswdResult passwdResult = buildPasswdResult(errno.errno, 0x3456);

        assertEquals(errno.errno, passwdResult.getErrno());
        assertEquals(0x3456, passwdResult.getErrno2());

        assertTrue(passwdResult.isNormalAuthFailure());
        assertEquals("Unix System Service __passwd failed with errno " + errno.errno + " (" + errno + ") and errno2 x3456", passwdResult.getMessage());
    }

    /**
     *
     */
    @Test
    public void testEMVSSAF2ERR() {

        PasswdResult.Errno errno = PasswdResult.Errno.EMVSSAF2ERR;
        PasswdResult passwdResult = buildPasswdResult(errno.errno, 0x3456);

        assertEquals(errno.errno, passwdResult.getErrno());
        assertEquals(0x3456, passwdResult.getErrno2());

        assertFalse(passwdResult.isNormalAuthFailure());
        assertEquals("Unix System Service __passwd failed with errno " + errno.errno + " (" + errno + ") and errno2 x3456", passwdResult.getMessage());
    }

    /**
     *
     */
    @Test
    public void testEMVSSAFEXTRERR() {

        PasswdResult.Errno errno = PasswdResult.Errno.EMVSSAFEXTRERR;
        PasswdResult passwdResult = buildPasswdResult(errno.errno, 0x3456);

        assertEquals(errno.errno, passwdResult.getErrno());
        assertEquals(0x3456, passwdResult.getErrno2());

        assertFalse(passwdResult.isNormalAuthFailure());
        assertEquals("Unix System Service __passwd failed with errno " + errno.errno + " (" + errno + ") and errno2 x3456", passwdResult.getMessage());
    }

    /**
     *
     */
    @Test
    public void testESRCH() {

        PasswdResult.Errno errno = PasswdResult.Errno.ESRCH;
        PasswdResult passwdResult = buildPasswdResult(errno.errno, 0x3456);

        assertEquals(errno.errno, passwdResult.getErrno());
        assertEquals(0x3456, passwdResult.getErrno2());

        assertTrue(passwdResult.isNormalAuthFailure());
        assertEquals("Unix System Service __passwd failed with errno " + errno.errno + " (" + errno + ") and errno2 x3456", passwdResult.getMessage());
    }

    /**
     *
     */
    @Test
    public void testUNKNOWN() {

        PasswdResult passwdResult = buildPasswdResult(777, 0x3456);

        assertEquals(777, passwdResult.getErrno());
        assertEquals(0x3456, passwdResult.getErrno2());

        assertFalse(passwdResult.isNormalAuthFailure());
        assertEquals("Unix System Service __passwd failed with errno 777 (UNKNOWN) and errno2 x3456", passwdResult.getMessage());
    }

    /**
     *
     */
    @Test
    public void testNoError() {

        PasswdResult passwdResult = buildPasswdResult(0, 0);

        assertEquals(0, passwdResult.getErrno());
        assertEquals(0, passwdResult.getErrno2());

        assertTrue(passwdResult.isNormalAuthFailure());
        assertEquals("Unix System Service __passwd failed with errno 0 (UNKNOWN) and errno2 x0", passwdResult.getMessage());
    }

    /**
     * @return a PasswdResult object whose raw bytes contain the given errnos.
     */
    private static PasswdResult buildPasswdResult(int errno, int errno2) {
        PasswdResult passwdResult = new PasswdResult();
        ByteBuffer bb = ByteBuffer.wrap(passwdResult.getBytes());
        bb.putInt(errno);
        bb.putInt(errno2);
        return passwdResult;
    }
}
