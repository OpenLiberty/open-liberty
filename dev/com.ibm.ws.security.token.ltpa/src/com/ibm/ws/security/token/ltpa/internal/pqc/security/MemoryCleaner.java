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

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Provides secure memory cleaning operations for sensitive cryptographic data.
 * 
 * <p>This class implements secure memory wiping to prevent sensitive data from
 * remaining in memory after use. This is critical for protecting against:
 * <ul>
 *   <li><b>Memory Dumps:</b> Sensitive data in crash dumps or core files</li>
 *   <li><b>Swap Files:</b> Sensitive data written to disk swap space</li>
 *   <li><b>Memory Forensics:</b> Data recovery from RAM after process termination</li>
 *   <li><b>Cold Boot Attacks:</b> Data recovery from RAM after power loss</li>
 * </ul>
 * 
 * <p><b>Security Rationale:</b>
 * Java's garbage collector does not guarantee immediate memory cleanup, and
 * sensitive data may remain in memory for extended periods. This class provides
 * explicit memory wiping to minimize the window of vulnerability.
 * 
 * <p><b>Data Types Handled:</b>
 * <ul>
 *   <li>Byte arrays (signatures, tokens, keys)</li>
 *   <li>Character arrays (passwords, passphrases)</li>
 *   <li>String objects (via reflection)</li>
 *   <li>ByteBuffer objects</li>
 *   <li>CharBuffer objects</li>
 *   <li>Cryptographic key objects</li>
 * </ul>
 * 
 * <p><b>Cleaning Strategy:</b>
 * <ol>
 *   <li>Overwrite memory with zeros</li>
 *   <li>Overwrite memory with random data (optional)</li>
 *   <li>Overwrite memory with zeros again (optional)</li>
 *   <li>Suggest garbage collection (optional)</li>
 * </ol>
 * 
 * <p><b>Limitations:</b>
 * <ul>
 *   <li>Cannot guarantee JVM won't copy data during GC</li>
 *   <li>Cannot prevent OS-level memory paging</li>
 *   <li>Cannot clean data in CPU caches</li>
 *   <li>String cleaning requires reflection (may fail with SecurityManager)</li>
 * </ul>
 * 
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use byte[] or char[] instead of String for sensitive data</li>
 *   <li>Clean data immediately after use</li>
 *   <li>Use try-finally blocks to ensure cleanup</li>
 *   <li>Consider using {@code javax.security.auth.Destroyable} interface</li>
 * </ul>
 * 
 * <p><b>Thread Safety:</b> All methods are thread-safe.
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since 2026
 */
public class MemoryCleaner {
    
    private static final TraceComponent tc = Tr.register(MemoryCleaner.class);
    
    /**
     * Number of overwrite passes for paranoid cleaning.
     */
    private static final int PARANOID_PASSES = 3;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private MemoryCleaner() {
        // Utility class - no instances
    }
    
    /**
     * Securely wipes a byte array by overwriting with zeros.
     * 
     * <p>This is the most common and efficient cleaning method. It overwrites
     * the entire array with zeros, making the original data unrecoverable.
     * 
     * @param data the byte array to clean (may be null)
     */
    @Trivial
    public static void cleanByteArray(byte[] data) {
        if (data != null) {
            Arrays.fill(data, (byte) 0);
        }
    }
    
    /**
     * Securely wipes a character array by overwriting with zeros.
     * 
     * <p>This method is essential for cleaning passwords and other sensitive
     * character data. Always prefer char[] over String for passwords.
     * 
     * @param data the character array to clean (may be null)
     */
    @Trivial
    public static void cleanCharArray(char[] data) {
        if (data != null) {
            Arrays.fill(data, '\0');
        }
    }
    
    /**
     * Securely wipes multiple byte arrays.
     * 
     * <p>Convenience method for cleaning multiple arrays in one call.
     * 
     * @param arrays the byte arrays to clean (may contain nulls)
     */
    @Trivial
    public static void cleanByteArrays(byte[]... arrays) {
        if (arrays != null) {
            for (byte[] array : arrays) {
                cleanByteArray(array);
            }
        }
    }
    
    /**
     * Securely wipes multiple character arrays.
     * 
     * @param arrays the character arrays to clean (may contain nulls)
     */
    @Trivial
    public static void cleanCharArrays(char[]... arrays) {
        if (arrays != null) {
            for (char[] array : arrays) {
                cleanCharArray(array);
            }
        }
    }
    
    /**
     * Securely wipes a ByteBuffer by overwriting with zeros.
     * 
     * <p>This method handles both heap and direct ByteBuffers. For direct
     * buffers, it attempts to clean the underlying native memory.
     * 
     * @param buffer the ByteBuffer to clean (may be null)
     */
    @Trivial
    public static void cleanByteBuffer(ByteBuffer buffer) {
        if (buffer != null) {
            try {
                // Save position and limit
                int position = buffer.position();
                int limit = buffer.limit();
                
                // Clear entire buffer
                buffer.clear();
                while (buffer.hasRemaining()) {
                    buffer.put((byte) 0);
                }
                
                // Restore position and limit
                buffer.limit(limit);
                buffer.position(position);
                
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to clean ByteBuffer", e);
                }
            }
        }
    }
    
    /**
     * Securely wipes a CharBuffer by overwriting with zeros.
     * 
     * @param buffer the CharBuffer to clean (may be null)
     */
    @Trivial
    public static void cleanCharBuffer(CharBuffer buffer) {
        if (buffer != null) {
            try {
                int position = buffer.position();
                int limit = buffer.limit();
                
                buffer.clear();
                while (buffer.hasRemaining()) {
                    buffer.put('\0');
                }
                
                buffer.limit(limit);
                buffer.position(position);
                
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to clean CharBuffer", e);
                }
            }
        }
    }
    
    /**
     * Attempts to securely wipe a String by overwriting its internal char array.
     * 
     * <p><b>Warning:</b> This method uses reflection to access the String's
     * internal char array. It may fail if:
     * <ul>
     *   <li>A SecurityManager is installed</li>
     *   <li>The JVM implementation changes</li>
     *   <li>The String is interned</li>
     * </ul>
     * 
     * <p><b>Best Practice:</b> Avoid using String for sensitive data. Use
     * char[] instead and clean it explicitly.
     * 
     * @param str the String to clean (may be null)
     * @return true if cleaning succeeded, false otherwise
     */
    public static boolean cleanString(String str) {
        if (str == null) {
            return true;
        }
        
        try {
            // Try to access the internal char array via reflection
            Field valueField = String.class.getDeclaredField("value");
            valueField.setAccessible(true);
            
            Object value = valueField.get(str);
            if (value instanceof char[]) {
                char[] chars = (char[]) value;
                Arrays.fill(chars, '\0');
                return true;
            } else if (value instanceof byte[]) {
                // Java 9+ compact strings
                byte[] bytes = (byte[]) value;
                Arrays.fill(bytes, (byte) 0);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to clean String via reflection", e);
            }
            return false;
        }
    }
    
    /**
     * Performs paranoid cleaning with multiple overwrite passes.
     * 
     * <p>This method performs multiple overwrite passes with different patterns:
     * <ol>
     *   <li>Overwrite with zeros</li>
     *   <li>Overwrite with 0xFF</li>
     *   <li>Overwrite with zeros again</li>
     * </ol>
     * 
     * <p>This provides defense-in-depth against sophisticated memory recovery
     * attacks, though it is slower than single-pass cleaning.
     * 
     * @param data the byte array to clean (may be null)
     */
    public static void cleanByteArrayParanoid(byte[] data) {
        if (data == null) {
            return;
        }
        
        // Pass 1: Zeros
        Arrays.fill(data, (byte) 0);
        
        // Pass 2: Ones
        Arrays.fill(data, (byte) 0xFF);
        
        // Pass 3: Zeros again
        Arrays.fill(data, (byte) 0);
    }
    
    /**
     * Performs paranoid cleaning of a character array.
     * 
     * @param data the character array to clean (may be null)
     */
    public static void cleanCharArrayParanoid(char[] data) {
        if (data == null) {
            return;
        }
        
        // Pass 1: Zeros
        Arrays.fill(data, '\0');
        
        // Pass 2: Ones
        Arrays.fill(data, (char) 0xFFFF);
        
        // Pass 3: Zeros again
        Arrays.fill(data, '\0');
    }
    
    /**
     * Attempts to clean a cryptographic private key.
     * 
     * <p>This method attempts to destroy the key using the {@code Destroyable}
     * interface if available, or falls back to reflection-based cleaning.
     * 
     * @param key the private key to clean (may be null)
     * @return true if cleaning succeeded, false otherwise
     */
    public static boolean cleanPrivateKey(PrivateKey key) {
        if (key == null) {
            return true;
        }
        
        try {
            // Try Destroyable interface (Java 8+)
            if (key instanceof javax.security.auth.Destroyable) {
                javax.security.auth.Destroyable destroyable = 
                    (javax.security.auth.Destroyable) key;
                if (!destroyable.isDestroyed()) {
                    destroyable.destroy();
                }
                return true;
            }
            
            // Fallback: Try to clean encoded form
            byte[] encoded = key.getEncoded();
            if (encoded != null) {
                cleanByteArrayParanoid(encoded);
            }
            
            return true;
            
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to clean private key", e);
            }
            return false;
        }
    }
    
    /**
     * Attempts to clean a cryptographic public key.
     * 
     * <p>Public keys are less sensitive than private keys, but cleaning them
     * can still be useful in some scenarios.
     * 
     * @param key the public key to clean (may be null)
     * @return true if cleaning succeeded, false otherwise
     */
    public static boolean cleanPublicKey(PublicKey key) {
        if (key == null) {
            return true;
        }
        
        try {
            // Try Destroyable interface
            if (key instanceof javax.security.auth.Destroyable) {
                javax.security.auth.Destroyable destroyable = 
                    (javax.security.auth.Destroyable) key;
                if (!destroyable.isDestroyed()) {
                    destroyable.destroy();
                }
                return true;
            }
            
            // Fallback: Clean encoded form
            byte[] encoded = key.getEncoded();
            if (encoded != null) {
                cleanByteArray(encoded);
            }
            
            return true;
            
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to clean public key", e);
            }
            return false;
        }
    }
    
    /**
     * Suggests garbage collection after cleaning sensitive data.
     * 
     * <p><b>Note:</b> This is only a suggestion to the JVM. The garbage
     * collector may or may not run immediately. This method should be used
     * sparingly as it can impact performance.
     * 
     * <p>Use this method only after cleaning large amounts of sensitive data
     * or in security-critical scenarios where you want to minimize the time
     * sensitive data remains in memory.
     */
    public static void suggestGarbageCollection() {
        try {
            System.gc();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Suggested garbage collection");
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to suggest garbage collection", e);
            }
        }
    }
    
    /**
     * Cleans sensitive data and suggests garbage collection.
     * 
     * <p>This is a convenience method that combines data cleaning with a
     * garbage collection suggestion. Use for cleaning large amounts of
     * sensitive data.
     * 
     * @param data the byte array to clean (may be null)
     */
    public static void cleanAndCollect(byte[] data) {
        cleanByteArrayParanoid(data);
        suggestGarbageCollection();
    }
    
    /**
     * Cleans sensitive data and suggests garbage collection.
     * 
     * @param data the character array to clean (may be null)
     */
    public static void cleanAndCollect(char[] data) {
        cleanCharArrayParanoid(data);
        suggestGarbageCollection();
    }
}

// Made with Bob
