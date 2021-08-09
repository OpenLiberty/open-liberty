/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ras;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;

/**
 * The ProtectedString class wraps a String to protect sensitive strings from
 * accidentally being printed in trace, FFDC, or via {@code toString()}.
 * Instances of this class are immutable.
 * 
 * <p>This class does not provide a constructor that takes a String, and it does
 * not have a method to convert the contents to a String. This is deliberate to
 * encourage the caller to never store passwords in a String, which might be
 * accidentally logged in trace or FFDC. In particular, callers must avoid
 * storing a String even indirectly, such as in a Map. Additionally, callers
 * must be careful to avoid accidentally logging in trace or FFDC
 * the {@code char[]} used to construct the instance or that is returned
 * from {@link #getChars}.
 * 
 * <p>This implementation does not protect against the string being visible in
 * clear text in JVM memory dumps. This class does not provide additional
 * protection against the string being accessible via reflection, so callers
 * should be careful to use private accessibility.
 */
public final class ProtectedString implements Traceable, FFDCSelfIntrospectable {
    /**
     * Salt to be used when hashing - different for each class loader used so
     * that a dictionary attack will not work. (A dictionary attack is one where
     * you duplicate the code in this class and then hash every word in a
     * dictionary to see if that password appears in the trace or ffdc file)
     */
    private static final byte[] SALT;
    static {
        SecureRandom sr = new SecureRandom();
        SALT = new byte[12];
        sr.nextBytes(SALT);
    }

    /** A password object that holds null */
    public static final ProtectedString NULL_PROTECTED_STRING = new ProtectedString((char[]) null);

    /** A password object that holds the equivalent of the empty string */
    public static final ProtectedString EMPTY_PROTECTED_STRING = new ProtectedString(new char[] {});

    /**
     * The actual password - stored as a character array - ideally passwords
     * should NEVER appear as a string so that they do not get interned
     */
    @Sensitive
    private final char[] _password;

    /**
     * A traceable string that will be the same for identical passwords (in the
     * same app server run
     */
    private String _traceableString = null;

    /**
     * Construct a ProtectedString (typically a password) from an array of
     * characters. The characters will not be revealed to trace or ffdc by this
     * class
     * 
     * @param chars
     *            The password to be protected
     */
    public ProtectedString(@Sensitive char[] password) {
        if (password != null) {
            if (password.length != 0) {
                _password = new char[password.length];
                System.arraycopy(password, 0, _password, 0, password.length);
            } else {
                _password = password; // Can safely use the empty array
            }
        } else {
            _password = null;
        }
    }

    /**
     * Return the protected password (Note: it is then the job of the caller to
     * prevent its copies reaching trace, ffdc or converting it to a string
     * 
     * @return char[] The protected password
     */
    @Sensitive
    public char[] getChars() {
        if (_password != null) {
            if (_password.length != 0) {
                char[] result = new char[_password.length];
                System.arraycopy(_password, 0, result, 0, _password.length);
                return result;
            } else {
                return _password; // Can safely return the empty array - caller
                // can't change it!
            }
        } else {
            return null;
        }
    }

    /**
     * Convert the password to a string, revealing only if it is null or
     * non-null. In particular note that it is NOT the password.
     * 
     * @return String A string representation of the password that can be used
     *         in trace etc.
     */
    @Override
    public String toString() {
        if (_password != null) {
            return "*****";
        } else {
            return "null";
        }
    }

    /**
     * Convert the password to a string for tracing purposes. This provides a
     * string that, for the same password, will be the same string, but will be
     * different for different password (well, almost certainly different)
     * and/or different class loaders (of Password). The password cannot be
     * deduced from the trace string
     * 
     * @return String A string for the password for trace purposes
     */
    @Override
    public String toTraceString() {
        if (_traceableString == null) {
            if (_password != null) {
                try {
                    MessageDigest digester = MessageDigest.getInstance("SHA-512");
                    digester.update(SALT);

                    for (char c : _password) {
                        digester.update((byte) ((c & 0xFF00) >> 8));
                        digester.update((byte) ((c & 0x00FF)));
                    }

                    byte[] hash = digester.digest();
                    StringBuilder sb = new StringBuilder();

                    // Throw away the high nibbles of each byte to increase
                    // security and reduce the length of the trace string
                    for (byte b : hash) {
                        int i = b & 0x0F;
                        sb.append(Integer.toHexString(i));
                    }
                    _traceableString = sb.toString();
                } catch (NoSuchAlgorithmException nsae) {
                    // No FFDC Code needed - fall back on the toString implementation
                    _traceableString = toString();
                }
            } else {
                _traceableString = ""; /* not just null :-) */
            }
        }

        return _traceableString;
    }

    /**
     * Provide details on the state of this object to ffdc, hiding the actual
     * contents of the password
     * 
     * @return String[] An array of strings to be added to the ffdc log
     */
    @Override
    public String[] introspectSelf() {
        return new String[] { "_password = " + toString(), "_traceablePassword = " + toTraceString() };
    }

    /**
     * Determine if this password is the same as another object
     * 
     * NOTE: As with all equals() methods, this implementation obeys the requirements
     * of java.lang.Object.equals(). In particular that requires that if a.equals(b), then
     * b.equals(a). That means that we only check against other ProtectedString objects. If
     * we returned true for any String that was passed in, we would then need to modify java.lang.String's
     * implementation so that it returned true when passed in the correct ProtectedString....
     * 
     * @param o
     *            The other object
     * @return boolean true if the other object is a Password and is the same of
     *         this one
     */
    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o == this)
            return true;

        if (o instanceof ProtectedString) {
            ProtectedString other = (ProtectedString) o;
            return Arrays.equals(_password, other._password);
        } else {
            return false;
        }
    }

    /**
     * return a hash code for this Password
     * 
     * @return int The hash code of this password
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(_password);
    }

    /**
     * Return true if password is either null or has no characters
     * (use in situations where some kind of password is required)
     * 
     * @return true if password is null or has no characters.
     */
    public boolean isEmpty() {
        return _password == null || _password.length == 0;
    }
}
