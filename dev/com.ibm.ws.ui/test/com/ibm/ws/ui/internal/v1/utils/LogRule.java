/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.ui.internal.v1.utils;

import java.io.PrintStream;
import java.lang.reflect.Method;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * This somehow is setting up the console output so that it can be read by the unit tests
 */
public class LogRule implements TestRule {

    private final TestRule outerRule;

    public LogRule(TestRule outerRule) {
        this.outerRule = outerRule;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.junit.rules.TestRule#apply(org.junit.runners.model.Statement, org.junit.runner.Description)
     */
    @Override
    public Statement apply(final Statement stmt, final Description desc) {
        try {
            final Method m = outerRule.getClass().getDeclaredMethod("apply", Statement.class, Description.class);
            m.setAccessible(true);
            final Statement innerRule = new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    PrintStream systemOut = System.out;
                    PrintStream systemErr = System.err;
                    if (systemOut != null) {
                        System.setOut(systemOut);
                        systemOut = null;
                    }
                    if (systemErr != null) {
                        System.setErr(systemErr);
                        systemErr = null;
                    }

                }
            };
            return (Statement) m.invoke(outerRule, innerRule, desc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
