/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static org.junit.Assert.assertFalse;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Re-purpose all the inherited tests to use {@link ShadowClassLoader}s, which should effect the same semantics.
 */
public class ParentFirstParentLastShadowTest extends ParentFirstParentLastTest {
    @BeforeClass
    public static void createShadowClassLoaders() throws Exception {
        parentFirstLoader = cls.getShadowClassLoader(parentFirstLoader);
        parentLastLoader = cls.getShadowClassLoader(parentLastLoader);
    }

    @Rule
    public TestRule checkAfterEveryTestThatNoClassesWereLoaded = new TestRule() {
        @Override
        public Statement apply(final Statement stmt, Description desc) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    stmt.evaluate();
                    checkNoClassesWereLoaded();
                }
            };
        }
    };

    public void checkNoClassesWereLoaded() throws Exception {
        assertFalse("No classes should have been loaded by the AppClassLoaders", outputManager.checkForTrace("LOAD"));
    }
}
