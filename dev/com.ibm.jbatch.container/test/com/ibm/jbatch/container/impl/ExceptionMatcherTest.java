/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.impl;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ExceptionMatcherTest {
    
    @SuppressWarnings("serial")
    private class ChildNullPointerException extends NullPointerException {}
	
	@Test
	public void testExceptionMatcherOneExclude() {		
		Set<String> _skipIncludeExceptions = new HashSet<String>();
        Set<String> _skipExcludeExceptions = new HashSet<String>();

        // Are skippable exceptions
        _skipIncludeExceptions.add("java.lang.Exception");
        _skipIncludeExceptions.add("java.lang.NullPointerException");
        _skipIncludeExceptions.add("java.lang.IndexOutOfBoundsException");
        _skipIncludeExceptions.add("java.lang.StringIndexOutOfBoundsException");
        
        // Not a skippable exception
        _skipExcludeExceptions.add("java.lang.RuntimeException");

        // Create the ExceptionMatcher Object
        ExceptionMatcher expMatcher = new ExceptionMatcher(_skipIncludeExceptions, _skipExcludeExceptions);

        // Test that the object is skipping exceptions or not correctly
        RuntimeException re = new RuntimeException();
        assertEquals(false, expMatcher.isSkippableOrRetryable(re));
         
        IndexOutOfBoundsException ioobe = new IndexOutOfBoundsException();
        assertEquals(true, expMatcher.isSkippableOrRetryable(ioobe));
        
        NullPointerException npe = new NullPointerException();
        assertEquals(true, expMatcher.isSkippableOrRetryable(npe));
        
        ChildNullPointerException cnpe = new ChildNullPointerException();
        assertEquals(true, expMatcher.isSkippableOrRetryable(cnpe));
        
	}
	
	@Test
	public void testExceptionMatcherMultipleExcludes() {		
		Set<String> _skipIncludeExceptions = new HashSet<String>();
        Set<String> _skipExcludeExceptions = new HashSet<String>();

        // Are skippable exceptions
        _skipIncludeExceptions.add("java.lang.Exception");
        _skipIncludeExceptions.add("java.lang.NullPointerException");
        _skipIncludeExceptions.add("java.lang.StringIndexOutOfBoundsException");
        
        // Not a skippable exception
        _skipExcludeExceptions.add("java.lang.RuntimeException");
        _skipExcludeExceptions.add("java.lang.IndexOutOfBoundsException");

        // Create the ExceptionMatcher Object
        ExceptionMatcher expMatcher = new ExceptionMatcher(_skipIncludeExceptions, _skipExcludeExceptions);

        // Test that the object is skipping exceptions or not correctly
        RuntimeException re = new RuntimeException();
        assertEquals(false, expMatcher.isSkippableOrRetryable(re));
         
        IndexOutOfBoundsException ioobe = new IndexOutOfBoundsException();
        assertEquals(false, expMatcher.isSkippableOrRetryable(ioobe));
        
        NullPointerException npe = new NullPointerException();
        assertEquals(true, expMatcher.isSkippableOrRetryable(npe));
        
        ChildNullPointerException cnpe = new ChildNullPointerException();
        assertEquals(true, expMatcher.isSkippableOrRetryable(cnpe));
        
	}
	
}
