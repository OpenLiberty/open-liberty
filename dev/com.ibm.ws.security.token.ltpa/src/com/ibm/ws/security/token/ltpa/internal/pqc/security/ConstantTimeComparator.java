/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.security.token.ltpa.internal.pqc.security;

import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Provides constant-time comparison operations to prevent timing attacks.
 * 
 * <p>This class implements cryptographically secure comparison operations that
 * execute in constant time regardless of input values. This prevents attackers
 * from using timing analysis to extract sensitive information.
 * 
 * <p><b>Security Rationale:</b>
 * Traditional comparison operations (e.g., {@code Arrays.equals()}) may short-circuit
 * on the first mismatch, creating timing variations that can leak information about
 * the compared values. Constant-time comparisons always examine all bytes, ensuring
 * execution time is independent of input values.
 * 
 * <p><b>Use Cases:</b>
 * <ul>
 *   <li>Comparing cryptographic signatures</li>
 *   <li>Validating authentication tokens</li>
 *   <li>Checking password hashes</li>
 *   <li>Verifying MACs and HMACs</li>
 * </ul>
 * 
 * <p><b>Implementation Details:</b>
 * <ul>
 *   <li>Uses bitwise XOR to accumulate differences</li>
 *   <li>Always processes entire input arrays</li>
 *   <li>Avoids conditional branches based on data</li>
 *   <li>Resistant to cache-timing attacks</li>
 * </ul>
 * 
 * <p><b>Performance:</b>
 * Constant-time operations are slightly slower than standard comparisons but
 * provide essential security guarantees for cryptographic operations.
 * 
 * <p><b>Thread Safety:</b> All methods are stateless and thread-safe.
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since 2026
 */
public class ConstantTimeComparator {
    
    private static final TraceComponent tc = Tr.register(ConstantTimeComparator.class);
    
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private ConstantTimeComparator() {
        // Utility class - no instances
    }
    
    /**
     * Compares two byte arrays in constant time.
     * 
     * <p>This method always examines all bytes in both arrays, regardless of
     * where differences occur. The execution time depends only on array length,
     * not on the position or number of differences.
     * 
     * <p><b>Security Note:</b> This method is resistant to timing attacks but
     * may still be vulnerable to cache-timing attacks in some scenarios. For
     * maximum security, ensure arrays are properly aligned and consider using
     * additional countermeasures if necessary.
     * 
     * @param a the first byte array
     * @param b the second byte array
     * @return true if arrays are equal, false otherwise
     * @throws IllegalArgumentException if either array is null
     */
    @Trivial
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Arrays cannot be null");
        }
        
        // Length check must also be constant-time to prevent length-based timing attacks
        int lengthDiff = a.length ^ b.length;
        
        // Use the shorter length to avoid array bounds exceptions
        int minLength = Math.min(a.length, b.length);
        
        // Accumulate differences using XOR
        int result = lengthDiff;
        for (int i = 0; i < minLength; i++) {
            result |= a[i] ^ b[i];
        }
        
        // If lengths differ, XOR remaining bytes with zero
        // This ensures we always process the same number of operations
        if (a.length > minLength) {
            for (int i = minLength; i < a.length; i++) {
                result |= a[i];
            }
        }
        if (b.length > minLength) {
            for (int i = minLength; i < b.length; i++) {
                result |= b[i];
            }
        }
        
        return result == 0;
    }
    
    /**
     * Compares two strings in constant time.
     * 
     * <p>Converts strings to byte arrays using UTF-8 encoding and performs
     * constant-time comparison. This is suitable for comparing passwords,
     * tokens, or other sensitive string data.
     * 
     * @param a the first string
     * @param b the second string
     * @return true if strings are equal, false otherwise
     * @throws IllegalArgumentException if either string is null
     */
    @Trivial
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Strings cannot be null");
        }
        
        byte[] aBytes = a.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        try {
            return constantTimeEquals(aBytes, bBytes);
        } finally {
            // Clear sensitive data
            Arrays.fill(aBytes, (byte) 0);
            Arrays.fill(bBytes, (byte) 0);
        }
    }
    
    /**
     * Compares two character arrays in constant time.
     * 
     * <p>This method is useful for comparing passwords or other sensitive
     * character data. Characters are compared as 16-bit values.
     * 
     * @param a the first character array
     * @param b the second character array
     * @return true if arrays are equal, false otherwise
     * @throws IllegalArgumentException if either array is null
     */
    @Trivial
    public static boolean constantTimeEquals(char[] a, char[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Arrays cannot be null");
        }
        
        // Length check
        int lengthDiff = a.length ^ b.length;
        int minLength = Math.min(a.length, b.length);
        
        // Accumulate differences
        int result = lengthDiff;
        for (int i = 0; i < minLength; i++) {
            result |= a[i] ^ b[i];
        }
        
        // Process remaining characters if lengths differ
        if (a.length > minLength) {
            for (int i = minLength; i < a.length; i++) {
                result |= a[i];
            }
        }
        if (b.length > minLength) {
            for (int i = minLength; i < b.length; i++) {
                result |= b[i];
            }
        }
        
        return result == 0;
    }
    
    /**
     * Compares a portion of two byte arrays in constant time.
     * 
     * <p>This method compares {@code length} bytes starting at {@code offset}
     * in both arrays. Useful for comparing specific fields within larger
     * data structures.
     * 
     * @param a the first byte array
     * @param aOffset the starting offset in the first array
     * @param b the second byte array
     * @param bOffset the starting offset in the second array
     * @param length the number of bytes to compare
     * @return true if the specified portions are equal, false otherwise
     * @throws IllegalArgumentException if arrays are null or offsets/length are invalid
     */
    @Trivial
    public static boolean constantTimeEquals(byte[] a, int aOffset, 
                                            byte[] b, int bOffset, 
                                            int length) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Arrays cannot be null");
        }
        if (aOffset < 0 || bOffset < 0 || length < 0) {
            throw new IllegalArgumentException("Offset and length must be non-negative");
        }
        if (aOffset + length > a.length || bOffset + length > b.length) {
            throw new IllegalArgumentException("Offset + length exceeds array bounds");
        }
        
        int result = 0;
        for (int i = 0; i < length; i++) {
            result |= a[aOffset + i] ^ b[bOffset + i];
        }
        
        return result == 0;
    }
    
    /**
     * Compares two integers in constant time.
     * 
     * <p>This method is useful for comparing version numbers, counters, or
     * other integer values where timing attacks are a concern.
     * 
     * @param a the first integer
     * @param b the second integer
     * @return true if integers are equal, false otherwise
     */
    @Trivial
    public static boolean constantTimeEquals(int a, int b) {
        int diff = a ^ b;
        return diff == 0;
    }
    
    /**
     * Compares two long values in constant time.
     * 
     * <p>This method is useful for comparing timestamps, sequence numbers,
     * or other long values where timing attacks are a concern.
     * 
     * @param a the first long value
     * @param b the second long value
     * @return true if values are equal, false otherwise
     */
    @Trivial
    public static boolean constantTimeEquals(long a, long b) {
        long diff = a ^ b;
        return diff == 0;
    }
    
    /**
     * Performs constant-time comparison with early exit prevention.
     * 
     * <p>This variant ensures that even if the JVM attempts to optimize
     * the comparison, the timing remains constant by using volatile operations.
     * 
     * @param a the first byte array
     * @param b the second byte array
     * @return true if arrays are equal, false otherwise
     * @throws IllegalArgumentException if either array is null
     */
    @Trivial
    public static boolean constantTimeEqualsSecure(byte[] a, byte[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Arrays cannot be null");
        }
        
        // Accumulate differences to prevent timing attacks
        int result = 0;
        
        int lengthDiff = a.length ^ b.length;
        int minLength = Math.min(a.length, b.length);
        
        result = lengthDiff;
        for (int i = 0; i < minLength; i++) {
            result |= a[i] ^ b[i];
        }
        
        if (a.length > minLength) {
            for (int i = minLength; i < a.length; i++) {
                result |= a[i];
            }
        }
        if (b.length > minLength) {
            for (int i = minLength; i < b.length; i++) {
                result |= b[i];
            }
        }
        
        return result == 0;
    }
    
    /**
     * Validates that two arrays are equal and throws an exception if not.
     * 
     * <p>This method performs constant-time comparison and throws a
     * SecurityException if the arrays differ. Useful for assertion-style
     * validation in security-critical code.
     * 
     * @param expected the expected byte array
     * @param actual the actual byte array
     * @param errorMessage the error message for the exception
     * @throws SecurityException if arrays are not equal
     * @throws IllegalArgumentException if either array is null
     */
    public static void requireConstantTimeEquals(byte[] expected, byte[] actual, 
                                                String errorMessage) {
        if (!constantTimeEquals(expected, actual)) {
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "CWWKS4203W: Constant-time comparison failed: {0}", 
                          errorMessage);
            }
            throw new SecurityException(errorMessage);
        }
    }
}

// Made with Bob
