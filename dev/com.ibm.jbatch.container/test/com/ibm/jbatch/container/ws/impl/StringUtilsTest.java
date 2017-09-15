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
package com.ibm.jbatch.container.ws.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

public class StringUtilsTest {
    
    /**
     * A @Rule executes before and after each test (see SharedOutputManager.apply()).
     * The SharedOutputManager Rule captures and restores (i.e. collects and purges) 
     * output streams (stdout/stderr) before and after each test.  The output is dumped
     * if and only if the test failed.
     */
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    
    @Test
    public void testIsEmpty() {
        assertTrue( StringUtils.isEmpty("") );
        assertTrue( StringUtils.isEmpty("  ") );
        assertTrue( StringUtils.isEmpty(null) );
        assertFalse( StringUtils.isEmpty("x") );
    }
    
    @Test
    public void testExceptionToString() {
        
        Exception e = new IllegalArgumentException("My illegal argument");
        
        String etos = StringUtils.exceptionToString(e);
        
        log("testExceptionToString: " + etos);
        
        assertTrue( etos.contains("My illegal argument") );
        assertTrue( etos.contains("at " + this.getClass().getName() + ".testExceptionToString" ) );
    }
    
    @Test
    public void testEnquote() {
        assertEquals("\"hello\"", StringUtils.enquote("hello"));
        assertEquals("\"\"hello\"\"", StringUtils.enquote("\"hello\""));
        assertEquals("\"\"", StringUtils.enquote(""));
        assertEquals("\"null\"", StringUtils.enquote(null));
    }
    
    @Test
    public void testTrimPrefix() {
        assertEquals("llo", StringUtils.trimPrefix("hello", "he"));
        assertEquals("hello", StringUtils.trimPrefix("hello", "bye"));
        assertEquals("hello", StringUtils.trimPrefix("hello", ""));
        assertEquals("", StringUtils.trimPrefix("hello", "hello"));
    }
    
    @Test
    public void testJoin() {
        
        assertEquals( "a,b,c,d", StringUtils.join( Arrays.asList("a", "b", "c", "d"), ",") );
        assertEquals( "a", StringUtils.join( Arrays.asList("a"), ",") );
        assertEquals( "", StringUtils.join( new ArrayList<String>(), "," ) );
        assertEquals( "", StringUtils.join( null, "," ) );
                
    }
    
    @Test
    public void testPlatformPath() {
        assertEquals("a" + File.separator + "b" + File.separator + "c", 
                     StringUtils.platformPath("a", "b", "c") );
    }
    
    @Test
    public void testNormalizePath() {
        // Just verify that String.replaceAll works how I think it works...
        assertEquals("some/path/part.1.log", StringUtils.normalizePath("some\\path\\part.1.log"));
        assertEquals("some/path/part.1.log", StringUtils.normalizePath("some/path/part.1.log"));
        assertEquals("part.1.log", StringUtils.normalizePath("part.1.log"));
        assertNull(StringUtils.normalizePath(null));
    }
    
    @Test
    public void testFirstNonNull() {
        
        assertEquals("blah", StringUtils.firstNonNull(null, null, "blah") );
        assertEquals("blah", StringUtils.firstNonNull("blah", null) );
        assertEquals("blah", StringUtils.firstNonNull(null, "blah") );
        assertNull( StringUtils.firstNonNull(null, null) );
    }
    
    @Test
    public void testFirstNonEmpty() {
        
        assertEquals("blah", StringUtils.firstNonEmpty(null, "", "blah") );
        assertEquals("blah", StringUtils.firstNonEmpty("blah", null) );
        assertEquals("blah", StringUtils.firstNonEmpty("  ", "blah") );
        assertNull( StringUtils.firstNonEmpty(null, null) );
    }
    
    @Test
    public void testTrimSuffix() {
        
        assertEquals("blah", StringUtils.trimSuffix("blah.war", ".war"));
        assertEquals("blah", StringUtils.trimSuffix("blah", ".war"));
    }
    
    @Test
    public void testTrimSuffixes() {
        
        assertEquals("blah", StringUtils.trimSuffixes("blah.war", ".jar", ".war"));
        assertEquals("blah", StringUtils.trimSuffixes("blah.jar", ".jar", ".war"));
        assertEquals("blah", StringUtils.trimSuffixes("blah.war.jar", ".jar", ".war"));
    }
    
    @Test
    public void testTrimQuotes() {
        
        assertEquals("blah", StringUtils.trimQuotes("blah"));
        assertEquals("blah", StringUtils.trimQuotes("\"blah\""));
    }
    
    @Test
    public void testAreEqual() {
        assertTrue( StringUtils.areEqual(null, null) );
        assertTrue( StringUtils.areEqual("", "") );
        assertTrue( StringUtils.areEqual("hello", "hello") );
        assertFalse( StringUtils.areEqual(null, "") );
        assertFalse( StringUtils.areEqual("hello", "goodbye") );
        assertFalse( StringUtils.areEqual("hello", "") );
    }
    
    @Test
    public void testAreEqualList() {
        assertTrue( StringUtils.areEqual( Arrays.asList( (String) null, null, null ) ) );
        assertTrue( StringUtils.areEqual( null ) );
        assertTrue( StringUtils.areEqual( new ArrayList<String>() ) );
        
        assertTrue( StringUtils.areEqual( Arrays.asList( "", "", "" ) ) );
        assertTrue( StringUtils.areEqual( Arrays.asList( "hello" ) ) );
        assertTrue( StringUtils.areEqual( Arrays.asList( "hello", "hello", "hello", "hello" ) ) );
        assertFalse( StringUtils.areEqual( Arrays.asList( null, "hello", "hello" ) ) );
        assertFalse( StringUtils.areEqual( Arrays.asList( "", "hello", "hello" ) ) );
        assertFalse( StringUtils.areEqual( Arrays.asList( "goodbye", "hello", "blah" ) ) );
    }
    
    protected static void log(String msg) {
        System.out.println(StringUtilsTest.class.getName() + ": " + msg);
    }
        

}
