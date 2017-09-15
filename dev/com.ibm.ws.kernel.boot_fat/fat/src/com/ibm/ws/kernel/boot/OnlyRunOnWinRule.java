/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Rule to run on Windows.
 */
public class OnlyRunOnWinRule implements TestRule {

    /** This constant is exposed to any test code to use. It is <code>true</code> iff the FAT is running on z/OS. */
    public static final boolean IS_RUNNING_ON_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");

    @Override
    public Statement apply(final Statement statement, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (IS_RUNNING_ON_WINDOWS) {
                    statement.evaluate();
                } else {
                    Log.info(description.getTestClass(), description.getMethodName(), "Test class or method is skipped due to run on Windows rule");
                }
            }
        };
    }
}
