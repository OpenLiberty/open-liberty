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
package componenttest.custom.junit.runner;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import componenttest.topology.utils.JavaMTUtils;

/**
 * Rule to test the all the criteria for running the FATs in multitenancy mode.
 */
public class OnlyRunInIBMJava7ForMultitenancyRule implements TestRule {
    private static final Class<?> c = OnlyRunInIBMJava7ForMultitenancyRule.class;

    @Override
    public Statement apply(Statement statement, Description arg1) {
        return new Java7MutlitenancyStatement(statement);
    }

    private static class Java7MutlitenancyStatement extends Statement {

        private final Statement statement;

        public Java7MutlitenancyStatement(Statement statement) {
            this.statement = statement;
        }

        @Override
        public void evaluate() throws Throwable {

            if (JavaMTUtils.checkSupportedEnvForMultiTenancy())
                statement.evaluate(); // Run the test
            else {
                // Skip the test
            }
        }

    }
}
