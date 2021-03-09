/* ***************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ***************************************************************************/
package com.ibm.ws.jndi.iiop.subtests;

import static com.ibm.ws.jndi.iiop.TestFacade.bindCosNamingContext;
import static com.ibm.ws.jndi.iiop.TestFacade.bindCosNamingObject;
import static com.ibm.ws.jndi.iiop.TestFacade.createTestableImpl;
import static com.ibm.ws.jndi.iiop.TestFacade.createTestableStub;
import static com.ibm.ws.jndi.iiop.TestFacade.jndiContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.directory.SchemaViolationException;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.jndi.iiop.JndiBindOperation;
import com.ibm.ws.jndi.iiop.JndiUtil;
import com.ibm.ws.jndi.iiop.TestFacade;
import com.ibm.ws.jndi.iiop.Testable;

/** Not to be run except as part of a test suite */
@RunWith(Parameterized.class)
public class TestBindAndRebind {
    private static final Object[][] TEST_PARAMETERS =
    {
     { JndiUtil.USE_STRING_NAMES, JndiBindOperation.BIND },
     { JndiUtil.USE_STRING_NAMES, JndiBindOperation.REBIND },
     { JndiUtil.USE_COMPOUND_NAMES, JndiBindOperation.BIND },
     { JndiUtil.USE_COMPOUND_NAMES, JndiBindOperation.REBIND }
    };

    @Parameters
    public static List<Object[]> parameters() {
        return Arrays.asList(TEST_PARAMETERS);
    }

    private static final String PREFIX = TestBindAndRebind.class.getSimpleName();
    private static final String BAD_OBJECT = PREFIX + "BadObject";
    private static final String GOOD_OBJECT = PREFIX + "GoodObject";
    private static final String ROOT_OBJECT = PREFIX + "RootObject";
    private static final String CTX_LEVEL_1 = PREFIX;
    private static final String CTX_LEVEL_2 = CTX_LEVEL_1 + "/subcontext";
    private static final String NSTD_OBJECT = CTX_LEVEL_1 + "/NestedObject";
    private static final String DEEP_OBJECT = CTX_LEVEL_2 + "/DeeplyNestedObject";

    static Testable bindableObject;
    static Testable imbindableObject;

    @BeforeClass
    public static void setup() throws Exception {
        bindCosNamingContext(CTX_LEVEL_1);
        bindCosNamingContext(CTX_LEVEL_2);
        bindCosNamingObject(ROOT_OBJECT);
        bindCosNamingObject(DEEP_OBJECT);
        bindCosNamingObject(NSTD_OBJECT);
        bindableObject = createTestableStub(GOOD_OBJECT);
        imbindableObject = createTestableImpl(BAD_OBJECT);
    }

    @Rule
    public final TestName NAMER = new TestName();
    private final JndiUtil jndiUtil;
    private final JndiBindOperation bindOp;

    public TestBindAndRebind(JndiUtil jndiUtil, JndiBindOperation bindOp) throws Exception {
        this.jndiUtil = jndiUtil;
        this.bindOp = bindOp;
    }

    private String makeUnique(String name) {
        return name + ":" + jndiUtil + ":" + bindOp;
    }

    @Test
    public void testBindNewLocation() throws Exception {
        jndiUtil.bindObject(jndiContext(), bindOp, makeUnique(ROOT_OBJECT), bindableObject);
    }

    @Test
    public void testBindExistingLocation() throws Exception {
        try {
            jndiUtil.bindObject(jndiContext(), bindOp, ROOT_OBJECT, bindableObject);
            assertTrue(bindOp.canOverWrite());
        } catch (NameAlreadyBoundException e) {
            assertFalse(bindOp.canOverWrite());
        }
    }

    @Test(expected = SchemaViolationException.class)
    public void testBindUnbindableObject() throws Exception {
        jndiUtil.bindObject(jndiContext(), bindOp, imbindableObject.getName(), imbindableObject);
    }

    @Test
    public void testBindNestedNewLocation() throws Exception {
        jndiUtil.bindObject(jndiContext(), bindOp, makeUnique(NSTD_OBJECT), bindableObject);
    }

    @Test
    public void testBindNestedExistingLocation() throws Exception {
        try {
            jndiUtil.bindObject(jndiContext(), bindOp, NSTD_OBJECT, bindableObject);
            assertTrue(bindOp.canOverWrite());
        } catch (NameAlreadyBoundException e) {
            assertFalse(bindOp.canOverWrite());
        }
    }

    @Test
    public void testBindDeepNewLocation() throws Exception {
        jndiUtil.bindObject(jndiContext(), bindOp, makeUnique(DEEP_OBJECT), bindableObject);
    }

    @Test
    public void testBindDeepExistingLocation() throws Exception {
        try {
            jndiUtil.bindObject(jndiContext(), bindOp, DEEP_OBJECT, bindableObject);
            assertTrue(bindOp.canOverWrite());
        } catch (NameAlreadyBoundException e) {
            assertFalse(bindOp.canOverWrite());
        }
    }

    @Test
    public void testBindIntoNonExistentContext() throws Exception {
        testBindIntoNonExistentContext(null, PREFIX + "nonExistentContext");
    }

    @Test
    public void testBindIntoNestedNonExistentContext() throws Exception {
        testBindIntoNonExistentContext(null, PREFIX + "nonExistentContext", "A");
    }

    @Test
    public void testBindIntoDeepNonExistentContext() throws Exception {
        testBindIntoNonExistentContext(null, PREFIX + "nonExistentContext", "A", "B");
    }

    @Test
    public void testBindIntoNestedNonExistentSubcontext() throws Exception {
        testBindIntoNonExistentContext(CTX_LEVEL_1, PREFIX + "nonExistentContext");
    }

    @Test
    public void testBindIntoDeepNonExistentSubcontext() throws Exception {
        testBindIntoNonExistentContext(CTX_LEVEL_2, PREFIX + "nonExistentContext");
    }

    private void testBindIntoNonExistentContext(final String goodContext, final String badContext, final String... badContexts) throws Exception {
        String name = goodContext == null ? "" : (goodContext + "/");
        name += badContext + "/";
        for (String c : badContexts)
            name += c + "/";
        name += "obj";
        String expectedRemainingName = badContext;
        try {
            jndiUtil.bindObject(jndiContext(), bindOp, name, bindableObject);
            fail("Should have thrown an exception");
        } catch (NameNotFoundException e) {
            Name remainingName = e.getRemainingName();
            assertNotNull(remainingName);
            assertEquals(expectedRemainingName, remainingName.toString());
        }
    }
}