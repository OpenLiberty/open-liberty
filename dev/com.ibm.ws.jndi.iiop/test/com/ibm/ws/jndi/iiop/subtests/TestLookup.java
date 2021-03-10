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
import static com.ibm.ws.jndi.iiop.TestFacade.jndiContext;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import javax.naming.Context;
import javax.naming.NameNotFoundException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.jndi.iiop.JndiUtil;
import com.ibm.ws.jndi.iiop.TestFacade;

/** Not to be run except as part of a test suite */
@RunWith(Parameterized.class)
public class TestLookup {
    private static final Object[][] TEST_PARAMETERS =
    {
     { JndiUtil.USE_STRING_NAMES },
     { JndiUtil.USE_COMPOUND_NAMES }
    };

    @Parameters
    public static List<Object[]> parameters() {
        return Arrays.asList(TEST_PARAMETERS);
    }

    private static final String TEST_PREFIX = TestLookup.class.getSimpleName();
    private static final String NONEXISTENT = TEST_PREFIX + "non-existent";
    private static final String ROOT_OBJECT = TEST_PREFIX + "rootLevelObject";
    private static final String CTX_LEVEL_1 = TEST_PREFIX + "subcontext";
    private static final String CTX_LEVEL_2 = CTX_LEVEL_1 + "/subsubcontext";
    private static final String NSTD_OBJECT = CTX_LEVEL_1 + "/nestedObject";
    private static final String DEEP_OBJECT = CTX_LEVEL_2 + "/deeplyNestedObject";

    private final JndiUtil jndiUtil;

    @BeforeClass
    public static void setup() throws Exception {
        // will fail if containing suite is not run first
        bindCosNamingContext(CTX_LEVEL_1);
        bindCosNamingContext(CTX_LEVEL_2);
        bindCosNamingObject(ROOT_OBJECT);
        bindCosNamingObject(NSTD_OBJECT);
        bindCosNamingObject(DEEP_OBJECT);

    }

    public TestLookup(JndiUtil jndiUtil) throws Exception {
        this.jndiUtil = jndiUtil;
    }

    @Test(expected = NameNotFoundException.class)
    public void testLookupAbsentBinding() throws Exception {
        jndiUtil.lookup(jndiContext(), NONEXISTENT);
    }

    @Test
    public void testLookupObject() throws Exception {
        jndiUtil.lookupObject(jndiContext(), ROOT_OBJECT);
    }

    @Test
    public void testLookupNestedObject() throws Exception {
        jndiUtil.lookupObject(jndiContext(), NSTD_OBJECT);
    }

    @Test
    public void testLookupDeeplyNestedObject() throws Exception {
        jndiUtil.lookupObject(jndiContext(), DEEP_OBJECT);
    }

    @Test
    public void testLookupRootContext() throws Exception {
        Context ctx1 = jndiUtil.lookupContext(jndiContext(), "");
        Context ctx2 = jndiUtil.lookupContext(jndiContext(), "");
        assertThat(ctx1, not(sameInstance(ctx2)));
        assertThat(ctx1, equalTo(ctx2));
        jndiUtil.lookupObject(ctx1, ROOT_OBJECT);
        jndiUtil.lookupObject(ctx2, ROOT_OBJECT);
    }

    @Test
    public void testLookupNestedContext() throws Exception {
        Context ctx1 = jndiUtil.lookupContext(jndiContext(), CTX_LEVEL_1);
        Context ctx2 = jndiUtil.lookupContext(jndiContext(), CTX_LEVEL_1);
        assertThat(ctx1, not(sameInstance(ctx2)));
        assertThat(ctx1, equalTo(ctx2));
        jndiUtil.lookupObject(ctx1, "nestedObject");
        jndiUtil.lookupObject(ctx2, "nestedObject");
    }

    @Test
    public void testLookupDeeplyNestedContext() throws Exception {
        Context ctx1 = jndiUtil.lookupContext(jndiContext(), CTX_LEVEL_2);
        Context ctx2 = jndiUtil.lookupContext(jndiContext(), CTX_LEVEL_2);
        assertThat(ctx1, not(sameInstance(ctx2)));
        assertThat(ctx1, equalTo(ctx2));
        jndiUtil.lookupObject(ctx1, "deeplyNestedObject");
        jndiUtil.lookupObject(ctx2, "deeplyNestedObject");
    }
}
