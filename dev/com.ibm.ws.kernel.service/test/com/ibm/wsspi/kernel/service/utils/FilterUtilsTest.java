/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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
package com.ibm.wsspi.kernel.service.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test {@link FilterUtils#createPropertyFilter(String, String)} and
 * {@link FilterUtils#createValidPropertyFilter(String, String)}.
 */
public class FilterUtilsTest {
    
    /**
     * Valid name value pair test cases.
     * 
     * Includes a null value case, an empty value case,
     * simple valid pair cases, and escaped value cases for
     * all special characters, including multiple special
     * characters.
     * 
     * {@link FilterUtils#VALUE_SPECIAL_CHARS}.
     */
    public static final String[][] TEST_PARMS_VALID = {
        { "(name=)",                   "name", null },
        { "(name=)",                   "name", "" },
        { "(name=v)",                  "name", "v" },
        { "(name=value)",              "name", "value" },
        { "(name=\\\\value)",          "name", "\\value" },
        { "(name=value\\\\)",          "name", "value\\" },
        { "(name=v\\\\a\\*l\\(u\\)e)", "name", "v\\a*l(u)e" }
    };
    
    public static final String JUNK_VALUE = "junk";
    
    /**
     * Non-valid name value pair test cases.
     * 
     * Includes an empty name case, and cases for each forbidden
     * name character, included cases of an embedded forbidden character.
     * 
     * {@link FilterUtils#FORBIDDEN_NAME_CHARS}.
     */
    
    public static final String[][] TEST_PARMS_NON_VALID = {
        { "(=junk)",  "",  JUNK_VALUE },
        { "(==junk)", "=", JUNK_VALUE },
        { "(>=junk)", ">", JUNK_VALUE },
        { "(<=junk)", "<", JUNK_VALUE },
        { "(~=junk)", "~", JUNK_VALUE },
        { "((=junk)", "(", JUNK_VALUE },
        { "()=junk)", ")", JUNK_VALUE },
        { "(valid=nonValid=junk)",  "valid=nonValid",  JUNK_VALUE },
        { "(valid(nonValid)=junk)", "valid(nonValid)", JUNK_VALUE },
        { "(~nonValid=junk)",       "~nonValid",       JUNK_VALUE },
    };
    
    /**
     * Test valid filter name-value pairs. Verify that the expected
     * filter value is produced, including expected escaping of value
     * special characters.
     * 
     * See the parameters array {@link #TEST_PARMS_VALID} for cases.
     * 
     * Validation of the name is not performed.
     */    
    @Test
    public void testValidFilters() {
        for ( String[] parms : TEST_PARMS_VALID ) {
            String expected = parms[0];
            String name = parms[1];
            String value = parms[2];
            
            System.out.println("Name (valid) [ " + name + " ]; value [ " + value + " ]");
            System.out.println("Expected [ " + expected + " ]");
            
            String filter0 = FilterUtils.createPropertyFilter(name, value);
            System.out.println("Actual (no validation) [ " + filter0 + " ]");            
            Assert.assertEquals(expected, filter0); 
            
            String filter1 = FilterUtils.createValidPropertyFilter(name, value);
            System.out.println("Actual (validation) [ " + filter1 + " ]");                        
            Assert.assertEquals(expected, filter1);
        }
    }

    /**
     * Test non-valid filter name-value pairs.
     * 
     * Validation is performed on name values. See the parameters array
     * {@link #TEST_PARMS_NON_VALID} for cases.
     */
    @Test
    public void testNonValidFilters() {    
        for ( String[] parms : TEST_PARMS_NON_VALID ) {
            String expected = parms[0];
            String name = parms[1];
            String value = parms[2];
            
            System.out.println("Name (non-valid) [ " + name + " ]; value [ " + value + " ]");
            System.out.println("Expected [ " + expected + " ]");
            
            String filter0 = FilterUtils.createPropertyFilter(name, value);
            System.out.println("Actual (no validation) [ " + filter0 + " ]");            
            Assert.assertEquals(expected, filter0);

            try {
                String filter1 = FilterUtils.createValidPropertyFilter(name, value);
                Assert.fail("Unexpected success [ " + name + " ]: [ " + filter1 + " ]");
            } catch ( IllegalArgumentException e ) {
                // Expected
            }
        }
    }
}
