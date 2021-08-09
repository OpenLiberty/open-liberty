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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.naming.NameAlreadyBoundException;

import org.junit.Test;

import com.ibm.ws.jndi.WSName;

public class ContextNode_Test extends ContextNode_Fixture {

    @Test
    public void testAutoBindSimple() throws Exception {
        root.autoBind(new WSName("x"), "hello");
        assertChildren("a,x", root);
        assertEquals("hello", root.lookup(new WSName("x")));
    }

    @Test(expected = NameAlreadyBoundException.class)
    public void testBindSimpleFail() throws Exception {
        root.autoBind(new WSName("a"), "hello");
    }

    @Test(expected = NameAlreadyBoundException.class)
    public void testAutoBindMultiFailContext() throws Exception {
        root.autoBind(new WSName("m/p/j"), "hello");
        root.autoBind(new WSName("m/p/j"), "fellow");
        root.autoBind(new WSName("m/p"), "ahem");
    }

    @Test
    public void testAutoBindMultiSuccess() throws Exception {
        root.autoBind(new WSName("m/p/j"), "kyle");
        root.autoBind(new WSName("m/p/j"), "stan");
        root.autoBind(new WSName("m/p/j"), "cartman");
        String result = (String) root.lookup(new WSName("m/p/j"));
        assertEquals("cartman", result);
    }

    @Test(expected = NameAlreadyBoundException.class)
    public void testBindMultiFailure() throws Exception {
        root.autoBind(new WSName("m/p/j"), "kyle");
        root.bind(new WSName("m/p/j"), "stan");
    }

    @Test
    public void testUnbind() throws Exception {
        root.autoBind(new WSName("m/p/j"), "kyle");
        root.autoBind(new WSName("m/p/j"), "stan");
        root.autoBind(new WSName("m/p/j"), "cartman");
        String result = (String) root.lookup(new WSName("m/p/j"));
        assertEquals("cartman", result);
        root.ensureNotBound(new WSName("m/p/j"), "kyle");
        root.ensureNotBound(new WSName("m/p/j"), "cartman");
        result = (String) root.lookup(new WSName("m/p/j"));
        assertEquals("stan", result);
    }

    @Test
    public void testAutoBindDeep() throws Exception {
        root.autoBind(new WSName("x/y"), "hello");
        assertChildren("a,x", root);
        assertChildren("y", root.lookup(new WSName("x")));
        assertEquals("hello", root.lookup(new WSName("x/y")));
    }

    @Test(expected = NameAlreadyBoundException.class)
    public void testAutoBindDeepFail() throws Exception {
        root.autoBind(new WSName("a/o"), "hello");
    }

    @Test
    public void testAutoBindDeeper() throws Exception {
        root.autoBind(new WSName("x/y/z"), "hello");
        assertChildren("a,x", root);
        assertChildren("y", root.lookup(new WSName("x")));
        assertChildren("z", root.lookup(new WSName("x/y")));
        assertEquals("hello", root.lookup(new WSName("x/y/z")));
    }

    @Test(expected = NameAlreadyBoundException.class)
    public void testAutoBindDeeperFail() throws Exception {
        root.autoBind(new WSName("a/b/o"), "hello");
    }

    @Test
    public void testAutoBindCleanup() throws Exception {
        root.autoBind(new WSName("x"), "hello");
        root.unbind(new WSName("x"));
        assertChildren("a", root);
    }

    @Test
    public void testAutoBindMultiCleanup() throws Exception {
        root.autoBind(new WSName("x"), "kenny");
        root.autoBind(new WSName("x"), "butters");
        root.unbind(new WSName("x"));
        assertChildren("a", root);
    }

    @Test
    public void testAutoBindMultiCleanup2() throws Exception {
        root.autoBind(new WSName("x"), "kenny");
        root.autoBind(new WSName("x"), "butters");
        root.ensureNotBound(new WSName("x"), "kenny");
        root.ensureNotBound(new WSName("x"), "butters");
        assertChildren("a", root);
    }

    @Test
    public void testAutoBindMultiCleanupPartial() throws Exception {
        root.autoBind(new WSName("x"), "kenny");
        root.autoBind(new WSName("x"), "butters");
        root.ensureNotBound(new WSName("x"), "kenny");
        String result = (String) root.lookup(new WSName("x"));
        assertEquals("butters", result);
        assertChildren("a,x", root);
        root.ensureNotBound(new WSName("x"), "butters");
        assertChildren("a", root);
    }

    @Test
    public void testAutoBindCleanupDeep() throws Exception {
        root.autoBind(new WSName("x/y"), "hello");
        root.unbind(new WSName("x/y"));
        assertChildren("a", root);
    }

    @Test
    public void testAutoBindCleanupDeep2() throws Exception {
        root.autoBind(new WSName("x/y"), "hello");
        root.ensureNotBound(new WSName("x/y"), "hello");
        assertChildren("a", root);
    }

    @Test
    public void testLookupEmptyName() throws Exception {
        assertEquals(root, root.lookup(new WSName()));
    }

    @Test
    public void testSameKeyDiffValue() throws Exception {
        root.autoBind(new WSName("x"), "tolkein");
        root.autoBind(new WSName("x"), "garrison");
        root.ensureNotBound(new WSName("x"), "garrison");
        String str = (String) root.lookup(new WSName("x"));
        assertNotNull("Lookup returned null", str);
        assertEquals("tolkein", str);
        root.ensureNotBound(new WSName("x"), "tolkein");
        assertChildren("a", root);
    }

    @Test
    public void testSameKeySameValue() throws Exception {
        for (int i = 0; i < 100; i++) {
            root.autoBind(new WSName("x"), "tolkein");
        }
        for (int i = 0; i < 100; i++) {
            root.autoBind(new WSName("y"), "garrison");
        }
        root.ensureNotBound(new WSName("y"), "garrison");
        root.ensureNotBound(new WSName("x"), "tolkein");
        assertChildren("a", root);
    }

    @Test(expected = NameAlreadyBoundException.class)
    public void testBindSameKeyDiffValue() throws Exception {
        root.bind(new WSName("x"), "tolkein");
        root.bind(new WSName("x"), "garrison");
    }

    @Test(expected = NameAlreadyBoundException.class)
    public void testBindSameKeySameValue() throws Exception {
        for (int i = 0; i < 100; i++) {
            root.bind(new WSName("x"), "tolkein");
        }
    }

    @Test
    public void testAutoBindCleanupDeeper() throws Exception {
        root.autoBind(new WSName("x/y/z"), "hello");
        root.unbind(new WSName("x/y/z"));
        assertChildren("a", root);
    }

    @Test
    public void testAutoBindCleanupDeeper2() throws Exception {
        root.autoBind(new WSName("x/y/z"), "hello");
        root.ensureNotBound(new WSName("x/y/z"), "hello");
        assertChildren("a", root);
    }

}
