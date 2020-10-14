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
package com.ibm.ws.security.credentials.saf.internal;

/**
 * Loose representation of a RACO via several layers of indirection.
 */
public class SAFCredentialToken {

    /**
     * Credential token used by native code. This maps to a native RegistryToken.
     * It contains an indirect reference to the RACO.
     */
    private final byte[] nativeToken;

    /**
     * The native credential token in String form, marshaled as hexadecimal chars.
     */
    private String key = null;

    /**
     * The time this SAFCredentialToken was created. This time is used by the
     * token-map reaper which goes thru and deletes old tokens from the map
     * (to avoid storage build-up).
     */
    private final long creationTime;

    /**
     * This flag is set as soon as the SAFCredential associated with this token has
     * been set into a Subject (see SAFCredentialsServiceImpl.setCredential). The
     * flag is used by the token-map reaper, which avoids deleting native tokens
     * prematurely before they've been set into the Subject (otherwise setCredential
     * will fail to find the native token and thus fail to put a SAFCredential
     * in the Subject, which will thus fail any SAF authz against that Subject).
     */
    private boolean isSubjectPopulated = false;

    /**
     * CTOR.
     *
     * @param token The native token created during a call to initACEE.
     */
    public SAFCredentialToken(byte[] token) {
        this.nativeToken = token;
        this.creationTime = System.nanoTime();
    }

    /**
     * Return the native token wrapped by this SAFCredentialToken.
     *
     * Note that we prevent mutation of the token by returning a copy.
     */
    public byte[] getBytes() {
        byte[] retMe = new byte[nativeToken.length];
        System.arraycopy(nativeToken, 0, retMe, 0, retMe.length);
        return retMe;
    }

    /**
     * Return the native token as a hexadecimal string.
     */
    public String getKey() {
        if (key == null) {
            key = toHexString(nativeToken);
        }
        return key;
    }

    /**
     * Converts a byte array to a hexadecimal string.
     */
    public static String toHexString(byte[] b) {
        final String digits = "0123456789abcdef";
        StringBuffer result = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            result.append(digits.charAt((b[i] >> 4) & 0xf));
            result.append(digits.charAt(b[i] & 0xf));
        }
        return (result.toString());
    }

    /**
     * @param value The new value for the isSubjectPopulated field.
     *
     * @return this
     */
    protected SAFCredentialToken setSubjectPopulated(boolean value) {
        this.isSubjectPopulated = value;
        return this;
    }

    /**
     *
     * @return the time this SAFCredToken was created (in ns)
     */
    protected long getCreationTime() {
        return creationTime;
    }

    /**
     * @return current time - creation time (in ns)
     */
    protected long getAge() {
        return System.nanoTime() - creationTime;
    }

    /**
     * @return
     */
    public boolean isSubjectPopulated() {
        return isSubjectPopulated;
    }

    /**
     * @return stringified
     */
    @Override
    public String toString() {
        return "SAFCredentialToken@"
               + Integer.toHexString(super.hashCode())
               + "{key:" + key + ", creationTime:" + creationTime + ", isSubjectPopulated: " + isSubjectPopulated + "}";
    }
}
