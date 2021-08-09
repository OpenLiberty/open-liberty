/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal;

import static com.ibm.ws.jndi.internal.Assert.assertChildren;
import static com.ibm.ws.jndi.internal.Assert.assertEquals;
import static org.junit.Assert.assertEquals;

import javax.naming.NameNotFoundException;

import org.junit.Test;

import com.ibm.ws.jndi.WSName;

public class ContextNode_Lookup_Test extends ContextNode_Fixture {

    ///////////////////////////
    // TEST BASIC BEHAVIOUR //
    /////////////////////////

    @Test
    public void testStructure() throws Exception {
        assertEquals("Root should have an empty name", "", root);
        assertEquals("a should be called a", "a", lookupContext(root, "a"));
        assertEquals("a/b should be called a/b", "a/b", lookupContext(root, "a/b"));
        assertChildren("a", root);
        assertChildren("b,o", lookupContext(root, "a"));
        assertChildren("c,o", lookupContext(root, "a/b"));
        assertChildren("", lookupContext(root, "a/b/c"));
    }

    ////////////////////////////
    // TEST SIMPLE USE CASES //
    //////////////////////////

    @Test
    public void testDirectLookup() throws Exception {
        assertEquals("AN OBJECT", a.lookup(new WSName("o")));
        assertEquals("ANOTHER OBJECT", lookup(ab, "o"));
    }

    @Test
    public void testDeepLookup() throws Exception {
        assertEquals("AN OBJECT", lookup(root, "a/o"));
    }

    @Test
    public void testDeeperLookup() throws Exception {
        assertEquals("ANOTHER OBJECT", lookup(root, "a/b/o"));
    }

    @Test(expected = NameNotFoundException.class)
    public void testFailedLookup() throws Exception {
        lookup(root, "x");
    }
}
