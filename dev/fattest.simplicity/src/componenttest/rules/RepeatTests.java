/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules;

import java.util.ArrayList;
import java.util.List;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Used as a JUnit <code>@ClassRule</code> to repeat all tests with the specified actions.
 * <br>
 * <b>NOTE: Order is significant when building the object! Tests will be run in the order of the builder methods.</b>
 * <br>
 * For example, if a FAT was written with Java EE 7 features initially, and we want to repeat
 * the tests with Java EE 8 equivalent features, do the following:
 *
 * <pre>
 * <code>@ClassRule
 * public static RepeatTests r = new RepeatTests().withoutModification()
 *                                                .with(FeatureReplacementAction.EE8);
 * </code>
 * </pre>
 */
public class RepeatTests extends ExternalResource {

    private static final RepeatTestAction NO_MODIFICATION_ACTION = new RepeatTestAction() {
        @Override
        public void setup() {}

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public String toString() {
            return "No modifications";
        }
    };

    private final List<RepeatTestAction> actions = new ArrayList<>();

    public RepeatTests() {}

    /**
     * Adds an iteration of test execution without making any modifications
     */
    public RepeatTests withoutModification() {
        actions.add(NO_MODIFICATION_ACTION);
        return this;
    }

    /**
     * Adds an iteration of test execution, where the action.setup() is called before repeating the tests.
     */
    public RepeatTests with(RepeatTestAction action) {
        actions.add(action);
        return this;
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new CompositeRepeatTestActionStatement(actions, statement);
    }

    private static class CompositeRepeatTestActionStatement extends Statement {
        private static Class<?> c = CompositeRepeatTestActionStatement.class;

        private final Statement statement;
        private final List<RepeatTestAction> actions;

        private CompositeRepeatTestActionStatement(List<RepeatTestAction> actions, Statement statement) {
            this.statement = statement;
            this.actions = actions;
        }

        @Override
        public void evaluate() throws Throwable {
            final String m = "evaluate";
            for (RepeatTestAction action : actions) {
                if (action.isEnabled()) {
                    Log.info(c, m, "===================================");
                    Log.info(c, m, "");
                    Log.info(c, m, "Running tests with action: " + action);
                    Log.info(c, m, "");
                    Log.info(c, m, "===================================");
                    action.setup();
                    statement.evaluate();
                }
            }
        }
    }
}