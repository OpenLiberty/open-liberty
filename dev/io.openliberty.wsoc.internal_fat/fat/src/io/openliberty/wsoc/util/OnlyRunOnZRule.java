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
package io.openliberty.wsoc.util;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Rule to run on z/OS.
 */
public class OnlyRunOnZRule implements TestRule {

    /** This constant is exposed to any test code to use. It is <code>true</code> iff the FAT is running on z/OS. */
    public static final boolean IS_RUNNING_ON_ZOS = (System.getProperty("os.name").toLowerCase().indexOf("z/os") > -1)
                                                    || (System.getProperty("os.name").toLowerCase().indexOf("os/390") > -1);

    @Override
    public Statement apply(final Statement statement, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (IS_RUNNING_ON_ZOS) {
                    statement.evaluate();
                } else {
                    Log.info(description.getTestClass(), description.getMethodName(), "Test class or method is skipped due to run on z/OS rule");
                }
            }
        };
    }
}
