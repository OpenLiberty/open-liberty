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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * errno/errno2 for __passwd service.
 *
 * http://www-01.ibm.com/support/knowledgecenter/SSLTBW_1.12.0/com.ibm.zos.r12.bpxbd00/rpassw.htm%23rpassw
 * If unsuccessful, __passwd() returns -1 and sets errno to one of the following values:
 *
 * Error Code Description
 * ---------- -----------
 * EACCES The oldpass is not authorized.
 * EINVAL The username, oldpass, newpass, or applid argument is invalid.
 * EMVSERR The specified function is not supported in an address space where a load was done from an uncontrolled library.
 * EMVSEXPIRE The oldpass has expired and no newpass has been provided.
 * EMVSPASSWORD The newpass is not valid, or does not meet the installation-exit requirements.
 * EMVSSAF2ERR Internal processing error.
 * EMVSSAFEXTRERR An internal SAF/RACF extract error has occurred. A possible reason is that the username access has been revoked.
 * errno2 contains the BPX1PWD reason code. For more information, see z/OS UNIX System Services Programming: Assembler
 * Callable Services Reference.
 * ESRCH The username provided is not defined to the security product or does not have an OMVS segment defined.
 *
 * http://www-01.ibm.com/support/knowledgecenter/SSLTBW_1.13.0/com.ibm.zos.r13.bpxa800/errno.htm%23errno
 * 111 006F EACCES Permission is denied.
 * 121 0079 EINVAL The parameter is incorrect.
 * 157 009D EMVSERR A MVS environmental or internal error has occurred.
 * 168 00A8 EMVSEXPIRE The password for the specified resource has expired.
 * 169 00A9 EMVSPASSWORD The new password specified is not valid.
 * 164 00A4 EMVSSAF2ERR SAF/RACF error.
 * 163 00A3 EMVSSAFEXTRERR SAF/RACF extract error.
 * 143 008F ESRCH No such process or thread exists.
 *
 */
public class PasswdResult {

    /**
     * Errno mapping
     */
    public enum Errno {
        EACCES(111),
        EINVAL(121),
        EMVSERR(157),
        EMVSEXPIRE(168),
        EMVSPASSWORD(169),
        EMVSSAF2ERR(164),
        EMVSSAFEXTRERR(163),
        ESRCH(143),
        UNKNOWN(-1);

        public final int errno;

        private Errno(int errno) {
            this.errno = errno;
        }

        public static Errno forErrno(int errno) {
            for (Errno e : values()) {
                if (e.errno == errno) {
                    return e;
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * Room for two ints: errno and errno2
     */
    private final byte[] raw = new byte[8];

    /**
     * errno and errno2
     */
    private int errno;
    private int errno2;

    /**
     * flag indicates whether the errnos have already been parsed
     */
    private boolean areErrnosParsed = false;

    /**
     * @return the raw byte buffer for passing errnos back from native.
     */
    public byte[] getBytes() {
        return raw;
    }

    /**
     * Parse the return/reason codes.
     */
    private void parseErrnos() {
        if (!areErrnosParsed) {
            IntBuffer ibuff = ByteBuffer.wrap(getBytes()).asIntBuffer();

            errno = ibuff.get();
            errno2 = ibuff.get();

            areErrnosParsed = true;
        }
    }

    /**
     * @return true if this is a "normal" auth failure (bad password, etc).
     */
    public boolean isNormalAuthFailure() {

        switch (Errno.forErrno(getErrno())) {
            case EACCES:
            case EINVAL:
            case EMVSEXPIRE:
            case EMVSPASSWORD:
            case ESRCH:
                return true;
            case EMVSERR:
                return false;
        }

        // Return "true" for errno==0 too.
        return (getErrno() == 0) ? true : false;
    }

    /**
     * @return __passwd errno
     */
    public int getErrno() {
        parseErrnos();
        return errno;
    }

    /**
     * @return __passwd errno2
     */
    public int getErrno2() {
        parseErrnos();
        return errno2;
    }

    /**
     * @return String message containing errno info.
     */
    public String getMessage() {
        return String.format("Unix System Service __passwd failed with errno %d (%s) and errno2 x%x",
                             getErrno(),
                             Errno.forErrno(getErrno()),
                             getErrno2());
    }

    /**
     * @return String message containing errno info.
     */
    public String getMessage(String userId) {
        return String.format("Unix System Service __passwd failed for user %s with errno %d (%s) and errno2 x%x",
                             userId,
                             getErrno(),
                             Errno.forErrno(getErrno()),
                             getErrno2());
    }
}