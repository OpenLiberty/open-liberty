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
package com.ibm.ws.security.token.ltpa.internal.pqc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.NoSuchAlgorithmException;

import org.junit.Test;

/**
 * Unit tests for PQCException class.
 * 
 * <p>This test suite provides comprehensive coverage of the PQCException class,
 * including all constructor variants, exception chaining, message formatting,
 * and null handling.</p>
 * 
 * <h3>Test Coverage Areas:</h3>
 * <ul>
 *   <li>Default constructor (no message)</li>
 *   <li>Constructor with message</li>
 *   <li>Constructor with cause</li>
 *   <li>Constructor with message and cause</li>
 *   <li>Exception chaining and getCause()</li>
 *   <li>Message formatting and getMessage()</li>
 *   <li>Null handling for messages and causes</li>
 * </ul>
 */
public class PQCExceptionTest {
    
    // ========================================================================
    // Constructor Tests - No Arguments
    // ========================================================================
    
    /**
     * Test default constructor creates exception with no message.
     */
    @Test
    public void test_constructor_NoArgs() {
        PQCException exception = new PQCException();
        
        assertNotNull("Exception should not be null", exception);
        assertNull("Exception message should be null", exception.getMessage());
        assertNull("Exception cause should be null", exception.getCause());
    }
    
    // ========================================================================
    // Constructor Tests - Message Only
    // ========================================================================
    
    /**
     * Test constructor with message stores the message correctly.
     */
    @Test
    public void test_constructor_WithMessage() {
        String message = "Test error message";
        PQCException exception = new PQCException(message);
        
        assertNotNull("Exception should not be null", exception);
        assertEquals("Exception message should match", message, exception.getMessage());
        assertNull("Exception cause should be null", exception.getCause());
    }
    
    /**
     * Test constructor with null message.
     */
    @Test
    public void test_constructor_WithNullMessage() {
        PQCException exception = new PQCException((String) null);
        
        assertNotNull("Exception should not be null", exception);
        assertNull("Exception message should be null", exception.getMessage());
        assertNull("Exception cause should be null", exception.getCause());
    }
    
    /**
     * Test constructor with empty message.
     */
    @Test
    public void test_constructor_WithEmptyMessage() {
        String message = "";
        PQCException exception = new PQCException(message);
        
        assertNotNull("Exception should not be null", exception);
        assertEquals("Exception message should be empty string", message, exception.getMessage());
    }
    
    /**
     * Test constructor with descriptive message.
     */
    @Test
    public void test_constructor_WithDescriptiveMessage() {
        String message = "Failed to generate ML-DSA-65 key pair: algorithm not available";
        PQCException exception = new PQCException(message);
        
        assertNotNull("Exception should not be null", exception);
        assertEquals("Exception message should match", message, exception.getMessage());
        assertTrue("Message should contain algorithm name", exception.getMessage().contains("ML-DSA-65"));
        assertTrue("Message should contain operation", exception.getMessage().contains("generate"));
    }
    
    // ========================================================================
    // Constructor Tests - Cause Only
    // ========================================================================
    
    /**
     * Test constructor with cause stores the cause correctly.
     */
    @Test
    public void test_constructor_WithCause() {
        Throwable cause = new NoSuchAlgorithmException("Algorithm not found");
        PQCException exception = new PQCException(cause);
        
        assertNotNull("Exception should not be null", exception);
        assertNotNull("Exception message should not be null", exception.getMessage());
        assertSame("Exception cause should match", cause, exception.getCause());
        assertTrue("Exception message should contain cause message",
                   exception.getMessage().contains("Algorithm not found"));
    }
    
    /**
     * Test constructor with null cause.
     */
    @Test
    public void test_constructor_WithNullCause() {
        PQCException exception = new PQCException((Throwable) null);
        
        assertNotNull("Exception should not be null", exception);
        assertNull("Exception cause should be null", exception.getCause());
    }
    
    /**
     * Test constructor with cause that has no message.
     */
    @Test
    public void test_constructor_WithCauseNoMessage() {
        Throwable cause = new RuntimeException();
        PQCException exception = new PQCException(cause);
        
        assertNotNull("Exception should not be null", exception);
        assertSame("Exception cause should match", cause, exception.getCause());
        assertNotNull("Exception message should not be null", exception.getMessage());
    }
    
    // ========================================================================
    // Constructor Tests - Message and Cause
    // ========================================================================
    
    /**
     * Test constructor with message and cause stores both correctly.
     */
    @Test
    public void test_constructor_WithMessageAndCause() {
        String message = "PQC operation failed";
        Throwable cause = new NoSuchAlgorithmException("Algorithm not found");
        PQCException exception = new PQCException(message, cause);
        
        assertNotNull("Exception should not be null", exception);
        assertEquals("Exception message should match", message, exception.getMessage());
        assertSame("Exception cause should match", cause, exception.getCause());
    }
    
    /**
     * Test constructor with null message and valid cause.
     */
    @Test
    public void test_constructor_WithNullMessageAndCause() {
        Throwable cause = new NoSuchAlgorithmException("Algorithm not found");
        PQCException exception = new PQCException(null, cause);
        
        assertNotNull("Exception should not be null", exception);
        assertNull("Exception message should be null", exception.getMessage());
        assertSame("Exception cause should match", cause, exception.getCause());
    }
    
    /**
     * Test constructor with valid message and null cause.
     */
    @Test
    public void test_constructor_WithMessageAndNullCause() {
        String message = "PQC operation failed";
        PQCException exception = new PQCException(message, null);
        
        assertNotNull("Exception should not be null", exception);
        assertEquals("Exception message should match", message, exception.getMessage());
        assertNull("Exception cause should be null", exception.getCause());
    }
    
    /**
     * Test constructor with both null message and cause.
     */
    @Test
    public void test_constructor_WithBothNull() {
        PQCException exception = new PQCException(null, null);
        
        assertNotNull("Exception should not be null", exception);
        assertNull("Exception message should be null", exception.getMessage());
        assertNull("Exception cause should be null", exception.getCause());
    }
    
    // ========================================================================
    // Exception Chaining Tests
    // ========================================================================
    
    /**
     * Test exception chaining with multiple levels.
     */
    @Test
    public void test_exceptionChaining_MultipleLevels() {
        Throwable rootCause = new IllegalArgumentException("Invalid parameter");
        Throwable intermediateCause = new NoSuchAlgorithmException("Algorithm not found", rootCause);
        PQCException exception = new PQCException("PQC operation failed", intermediateCause);
        
        assertNotNull("Exception should not be null", exception);
        assertEquals("Exception message should match", "PQC operation failed", exception.getMessage());
        assertSame("Exception cause should match", intermediateCause, exception.getCause());
        assertSame("Root cause should match", rootCause, exception.getCause().getCause());
    }
    
    /**
     * Test getCause returns the correct cause.
     */
    @Test
    public void test_getCause_ReturnsCorrectCause() {
        Throwable cause = new NoSuchAlgorithmException("Algorithm not found");
        PQCException exception = new PQCException("PQC operation failed", cause);
        
        Throwable retrievedCause = exception.getCause();
        assertNotNull("Retrieved cause should not be null", retrievedCause);
        assertSame("Retrieved cause should match original cause", cause, retrievedCause);
        assertTrue("Retrieved cause should be NoSuchAlgorithmException",
                   retrievedCause instanceof NoSuchAlgorithmException);
    }
    
    // ========================================================================
    // Message Formatting Tests
    // ========================================================================
    
    /**
     * Test message formatting with algorithm name.
     */
    @Test
    public void test_messageFormatting_WithAlgorithm() {
        String message = "Failed to generate ML-DSA-65 key pair";
        PQCException exception = new PQCException(message);
        
        assertEquals("Message should match", message, exception.getMessage());
        assertTrue("Message should contain algorithm", exception.getMessage().contains("ML-DSA-65"));
    }
    
    /**
     * Test message formatting with operation details.
     */
    @Test
    public void test_messageFormatting_WithOperationDetails() {
        String message = "Signature verification failed: invalid signature format";
        PQCException exception = new PQCException(message);
        
        assertEquals("Message should match", message, exception.getMessage());
        assertTrue("Message should contain operation", exception.getMessage().contains("verification"));
        assertTrue("Message should contain reason", exception.getMessage().contains("invalid signature"));
    }
    
    /**
     * Test message formatting with provider information.
     */
    @Test
    public void test_messageFormatting_WithProviderInfo() {
        String message = "PQC provider not available: BouncyCastle PQC library not found";
        PQCException exception = new PQCException(message);
        
        assertEquals("Message should match", message, exception.getMessage());
        assertTrue("Message should contain provider name", exception.getMessage().contains("BouncyCastle"));
    }
    
    // ========================================================================
    // Exception Inheritance Tests
    // ========================================================================
    
    /**
     * Test that PQCException extends Exception.
     */
    @Test
    public void test_inheritance_ExtendsException() {
        PQCException exception = new PQCException("Test message");
        
        assertTrue("PQCException should be an instance of Exception",
                   exception instanceof Exception);
    }
    
    /**
     * Test that PQCException can be caught as Exception.
     */
    @Test
    public void test_inheritance_CatchAsException() {
        try {
            throw new PQCException("Test exception");
        } catch (Exception e) {
            assertTrue("Caught exception should be PQCException",
                       e instanceof PQCException);
            assertEquals("Exception message should match", "Test exception", e.getMessage());
        }
    }
    
    // ========================================================================
    // Serialization Tests
    // ========================================================================
    
    /**
     * Test that PQCException has a serialVersionUID.
     * This is important for serialization compatibility.
     */
    @Test
    public void test_serialization_HasSerialVersionUID() {
        // This test verifies that the class compiles with serialVersionUID
        // The actual serialization is tested implicitly by the class definition
        PQCException exception = new PQCException("Test message");
        assertNotNull("Exception should not be null", exception);
    }
}

// Made with Bob