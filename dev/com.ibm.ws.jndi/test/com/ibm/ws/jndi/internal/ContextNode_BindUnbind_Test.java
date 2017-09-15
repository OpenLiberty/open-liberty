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
import static com.ibm.ws.jndi.internal.ContextNode_Fixture.BindType.BIND;
import static com.ibm.ws.jndi.internal.ContextNode_Fixture.BindType.CONTEXT;
import static com.ibm.ws.jndi.internal.ContextNode_Fixture.BindType.REBIND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test the behaviour of the {@link ContextNode} conforms to the expected behaviour
 * of the JNDI {@link Context} interface. The method signatures are not identical
 * because this is an underlying implementation object, but it is relied on to
 * implement the interface so that the caller does not handle the exceptions at all - they are passed straight through.
 * <p>
 * To reduce the test code to a manageable level, some re-use is effected here.
 * This makes use of JUnit's parameterised tests and {@link ExpectedException} using the {@link Rule} annotation. For example, a single test attempts to bind
 * into an occupied slot. When we are using {@link Context#bind(String, Object)} or {@link Context#createSubcontext(String)} or other forms of those
 * methods, this test should expect an exception. However, if we are using {@link Context#rebind(String, Object)} the operation should succeed.
 * <p>
 * When debugging, note that the test name has an index as a suffix.
 * To decode this index into something useful, look at the {@link #getVariations()} method.
 */

@RunWith(Parameterized.class)
public class ContextNode_BindUnbind_Test extends ContextNode_Fixture {
    /////////////
    // CONFIG //
    ///////////
    @Parameters
    public static Collection<Object[]> getVariations() {
        Object[][] arr =
        {
         // These tests are parameterised, using JUnit's
         // parameterised test runner.  The parameters are 
         // laid out in the rows in the table below.
         // Sadly, the names of the tests just use the index
         // to append to each test method, like so:
         // ---> "testBindDeep[0]"
         // which can be a little obscure. Use the leftmost
         // column in the table to decode the test names.
         //_________________________________
         // Name Pattern   ||   BindType  
         //=================================
         /* test...[0] --> || */{ CONTEXT },
         /* test...[1] --> || */{ BIND },
         /* test...[2] --> || */{ REBIND },
         /* test...[3] --> || */{ CONTEXT },
         /* test...[4] --> || */{ BIND },
         /* test...[5] --> || */{ REBIND },
         /* test...[6] --> || */{ CONTEXT },
         /* test...[7] --> || */{ BIND },
         /* test...[8] --> || */{ REBIND },
        };
        return Arrays.asList(arr);
    }

    /** Initialise test case using one row of parameter data from {@link #getVariations()}. */
    public ContextNode_BindUnbind_Test(BindType bindType) {
        super(bindType);
    }

    ////////////////////////////
    // TEST SIMPLE USE CASES //
    //////////////////////////

    @Test
    public void testBind() throws Exception {
        bindAndCheckNew(root, "z");
        assertChildren("a,z", root);
    }

    @Test
    public void testBindDeep() throws Exception {
        bindAndCheckNew(root, "a/B");
        assertChildren("B,b,o", a);
    }

    @Test
    public void testBindDeeper() throws Exception {
        bindAndCheckNew(root, "a/b/C");
        assertChildren("C,c,o", ab);
    }

    @Test
    public void testUnbindNonExistent() throws Exception {
        Object removed = unbind(root, "x");
        assertNull("Should return null", removed);
    }

    @Test
    public void testUnbindEmptyContext() throws Exception {
        ContextNode removed = (ContextNode) unbind(root, "a/b/c");
        Assert.assertEquals("Should have removed a/b/c", "a/b/c", removed);
    }

    ////////////////////////////////////////////////////////////
    // TEST COMPLEX CASES (sometimes valid, sometimes error) //
    //////////////////////////////////////////////////////////

    @Test
    public void testBindIntoExistingSlot() throws Exception {
        ContextNode removed = (ContextNode) bindExisting(root, "a");
        assertEquals("Should have removed node a", "a", removed);
    }

    @Test
    public void testBindIntoDeepExistingSlot() throws Exception {
        ContextNode removed = (ContextNode) bindExisting(root, "a/b");
        assertEquals("Should have removed node a", "a/b", removed);
    }

    @Test
    public void testBindIntoDeeperExistingSlot() throws Exception {
        ContextNode removed = (ContextNode) bindExisting(root, "a/b/c");
        assertEquals("Should have removed node a", "a/b/c", removed);
    }

    @Test
    public void testUnbindObject() throws Exception {
        Object removed = unbindObject(root, "a/o");
        assertEquals("Should have removed AN OBJECT", "AN OBJECT", removed);
    }

    @Test
    public void testUnbindDeepObject() throws Exception {
        Object removed = unbindObject(root, "a/b/o");
        assertEquals("Should have remove ANOTHER OBJECT", "ANOTHER OBJECT", removed);
    }

    @Test
    public void testUnbindNonEmptyContext() throws Exception {
        ContextNode removed = (ContextNode) unbindNonEmptyContext(root, "a/b");
        assertEquals("Should have removed a/b", "a/b", removed);
    }

    //////////////////////////////
    // TEST SIMPLE ERROR CASES //
    ////////////////////////////

    @Test(expected = InvalidNameException.class)
    public void testBindWithEmptyString() throws Exception {
        bind(root, "");
    }

    @Test(expected = NameNotFoundException.class)
    public void testBindIntoNonExistentParentByString() throws Exception {
        bind(root, "x/y");
    }

    @Test(expected = NameNotFoundException.class)
    public void testBindIntoDeeplyNonExistentParentByString() throws Exception {
        bind(root, "a/b/c/d/e");
    }

    @Test(expected = NameNotFoundException.class)
    public void testBindIntoNonExistentGrandParentByString() throws Exception {
        bind(root, "a/b/c/d/e/f");
    }

}
