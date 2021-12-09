/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Test WSUtil.resolveURI.
 */
public class WSUtilTest {
    /**
     * Negative test.  Ensure that URI resolution fails when too
     * many parent directories are specified.  Ensure that the correct
     * exception text is received.
     */
    @Test
    public void resolveURI_invalidArgument_tooManyParents() {
        // note three parents, but only two directories deep
        String badURI = "/dir1/dir2/../../../blah";

        try {
            WSUtil.resolveURI(badURI);
            fail("Did not throw IllegalArgumentException for invalid URI parameter");

        } catch ( IllegalArgumentException ex ) {
            // Start: Issue 17123:
            //
            // The text of the IllegalArgumentException may only display
            // a generic error message.  The detailed error message which
            // was displayed is not allowed.
            // 

            String actual = ex.getMessage();

            // String expected =
            //     "is invalid because it contains more references to parent" +
            //     " directories (\"..\") than is possible.";
            // assertTrue( "Did not contain expected exception text indicating more parents than directory depth",
            //             actual.contains(expected) );

            assertFalse( "Missing message", (actual == null) );

            String expected = "Non-valid URI.";

            assertTrue( "Does not contain generic exception text", actual.contains(expected) );
            assertFalse( "Contains disallowed precise message", actual.contains("parent") );
            assertFalse( "Contains disallowed precise message", actual.contains("..") );
        }
    }

    // TFB: TODO:
    //
    // These results are per the current implementation.
    //
    // A number of the results, marked with '**', are questionable.
    // '****' marks particularly strange results.
    //
    // Two kinds of strange results are obtained: Cases where a trailing
    // slash is removed, and cases where a leading slash is added.
    //
    // The removal of a trailing slash would be less questionable if all
    // cases removed the trailing slash.

    public static final String[][] RESOLVE_URI_DATA = {
        { "/", "/" },        
        { "\\", "/" },
        
        { "servlet", "servlet" },

        { "/servlet",  "/servlet" },
        { "\\servlet", "/servlet" },
        { "//servlet", "/servlet" },

        { "servlet/",  "servlet/" },
        { "servlet\\", "servlet/" },
        { "servlet//", "/servlet" }, //****
        
        { "/servlet/",   "/servlet/" },
        { "\\servlet\\", "/servlet/" },
        { "//servlet/",  "/servlet" }, //**
        { "/servlet//",  "/servlet" }, //**
        { "//servlet//", "/servlet" }, //**

        { "servlet/snoop",  "servlet/snoop" },
        { "servlet\\snoop", "servlet/snoop" },
        { "servlet//snoop", "/servlet/snoop" }, //****

        { "/servlet/snoop",   "/servlet/snoop" },
        { "\\servlet\\snoop", "/servlet/snoop" },
        { "//servlet/snoop",  "/servlet/snoop" },
        { "/servlet//snoop",  "/servlet/snoop" },
        { "//servlet//snoop", "/servlet/snoop" },

        { "servlet/snoop/",   "servlet/snoop/" },
        { "servlet\\snoop\\", "servlet/snoop/" },
        { "servlet//snoop/",  "/servlet/snoop" }, //**
        { "servlet/snoop//",  "/servlet/snoop" }, //****
        { "servlet//snoop//", "/servlet/snoop" }, //****

        { "/servlet/snoop/",    "/servlet/snoop/" },
        { "\\servlet\\snoop\\", "/servlet/snoop/" },
        { "//servlet/snoop/",   "/servlet/snoop" }, //**
        { "/servlet//snoop/",   "/servlet/snoop" }, //**
        { "/servlet/snoop//",   "/servlet/snoop" }, //**
        { "//servlet//snoop/",  "/servlet/snoop" }, //**
        { "//servlet/snoop//",  "/servlet/snoop" }, //**
        { "/servlet//snoop//",  "/servlet/snoop" }, //**
        { "//servlet//snoop//", "/servlet/snoop" }, //**   

        { "/hello/./dog/", "/hello/dog" },
        { "/hello/../dog/", "/dog" },
        { "/hello/dog/../../etc", "/etc" },

        { "?", "?" },
        { "hello?", "hello?" },
        { "?dog", "?dog" },
        { "hello?dog", "hello?dog" },        
        
        { "/hello//dog/?hello=/../5", "/hello/dog?hello=/../5" },
        { "/hello/dog/?hello=/../5", "/hello/dog/?hello=/../5" },
        { "todd?hello=\\string", "todd?hello=\\string" },
        { "?hello=/../5", "?hello=/../5" },
        
        { "/hello\\\\there\\////dog?query=5", "/hello/there/dog?query=5" }
    };

    /**
     * Test resolution of various URIs.
     */
    @Test
    public void testResolveUris() {
        List<String> failures = null;

        for ( String[] uriData : RESOLVE_URI_DATA ) {
            String unresolved = uriData[0];
            String expectedResolved = uriData[1];
            String actualResolved;
            Throwable caughtFailure;
            
            try {
                actualResolved = WSUtil.resolveURI(unresolved);
                caughtFailure = null;
            } catch ( Throwable th ) {
                actualResolved = null;
                caughtFailure = th;
            }

            String result = "Resolve [ " + unresolved + " ] expecting [ " + expectedResolved + " ]";
            boolean failed;
            
            if ( caughtFailure != null ) {
                if ( failures == null ) {
                    failures = new ArrayList<String>(1);
                }
                result += ": Exception [ " + caughtFailure + " ]";
                failed = true;

            } else {
                if ( actualResolved == null ) {
                    result += ": Obtained null";
                    failed = true;
                } else if ( !actualResolved.equals(expectedResolved) ) {
                    result += ": Obtained [ " + actualResolved + " ]";
                    failed = true;                    
                } else {
                    failed = false;                    
                }
            }
            
            System.out.println( ( failed ? "Failed: " : "Success: ") + result );

            if ( failed ) {
                if ( failures == null ) {
                    failures = new ArrayList<String>(1);
                    failures.add(result);
                }
            }
        }

        if ( failures != null ) {
            fail("Resolution failures [ " + failures + " ]");
        }
    }
    
}
