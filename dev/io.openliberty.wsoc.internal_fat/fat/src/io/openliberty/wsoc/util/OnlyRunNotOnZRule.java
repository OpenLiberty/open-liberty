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
package io.openliberty.wsoc.util;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 *
 */
public class OnlyRunNotOnZRule implements TestRule {

    /*
     * (non-Javadoc)
     * 
     * @see org.junit.rules.TestRule#apply(org.junit.runners.model.Statement, org.junit.runner.Description)
     */
    @Override
    public Statement apply(Statement statement, Description arg1) {
        return new ZosStatement(statement, arg1.getMethodName());
    }

    private static class ZosStatement extends Statement {

        private final Statement statement;
        private final boolean otherThanZos;
        private boolean isSSCTest = false;

        // 4/9/2015 Current logic is that we only want to run the SSC tests on Z, since the Jetty websocket client on Z is not stable and we don't have
        // access to the jetty code to fix it.

        /**
         * @param statement
         */
        public ZosStatement(Statement statement, String methodName) {
            this.statement = statement;

            String strOsName = System.getProperty("os.name").toLowerCase();
            otherThanZos = !(strOsName.indexOf("z/os") > -1 || strOsName.indexOf("os/390") > -1);
            if (methodName != null) {
                isSSCTest = (methodName.indexOf("SSC") > 0);
            }

        }

        /*
         * (non-Javadoc)
         * 
         * @see org.junit.runners.model.Statement#evaluate()
         */
        @Override
        public void evaluate() throws Throwable {
            if (otherThanZos) {
                // If not on Zos then run the test
                statement.evaluate();
            } else if (isSSCTest) {
                // If on Zos and this is an "SSC" test, then run the test
                statement.evaluate();
            } else {
                // We are on Z, but this is not an SSC test, so don't run the test.
            }
        }

    }

}
