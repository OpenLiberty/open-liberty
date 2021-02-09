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
public class DontRunWithWebServerRule implements TestRule {

    /*
     * (non-Javadoc)
     * 
     * @see org.junit.rules.TestRule#apply(org.junit.runners.model.Statement, org.junit.runner.Description)
     */
    @Override
    public Statement apply(Statement statement, Description arg1) {
        return new WebServerStatement(statement);
    }

    private static class WebServerStatement extends Statement {

        private final Statement statement;
        private boolean WebServerPresent = false;

        /**
         * @param statement
         */
        public WebServerStatement(Statement statement) {
            this.statement = statement;

            WebServerPresent = false;
            try {
                WebServerPresent = WebServerControl.isWebserverInFront();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        /*
         * (non-Javadoc)
         * 
         * @see org.junit.runners.model.Statement#evaluate()
         */
        @Override
        public void evaluate() throws Throwable {
            if (!WebServerPresent) {
                statement.evaluate();
            }
            else {
                // Do nothing
                // This will have the effect of causing no tests to be run
            }
        }

    }

}
