/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package componenttest.rules.repeater;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.RepeatTestFilter;

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
 * public static RepeatTests r = RepeatTests.withoutModification()
 *                                                .andWith(FeatureReplacementAction.EE8_FEATURES());
 * </code>
 * </pre>
 */
public class RepeatTests extends ExternalResource {

    /**
     * Adds an iteration of test execution without making any modifications
     */
    public static RepeatTests withoutModification() {
        return new RepeatTests().andWithoutModification();
    }

    /**
     * Adds an iteration of test execution without making any modifications, but only run in FULL mode
     */
    public static RepeatTests withoutModificationInFullMode() {
        return new RepeatTests().andWithoutModificationInFullMode();
    }

    /**
     * Adds an iteration of test execution, where the action.setup() is called before repeating the tests.
     */
    public static RepeatTests with(RepeatTestAction action) {
        return new RepeatTests().andWith(action);
    }

    private static final RepeatTestAction NO_MODIFICATION_ACTION = new EmptyAction();

    private final List<RepeatTestAction> actions = new ArrayList<>();

    private RepeatTests() {
        // private ctor, require static entry point
    }

    /**
     * Adds an iteration of test execution without making any modifications
     */
    public RepeatTests andWithoutModification() {
        actions.add(NO_MODIFICATION_ACTION);
        return this;
    }

    /**
     * Adds an iteration of test execution without making any modifications, but run in FULL mode
     */
    public RepeatTests andWithoutModificationInFullMode() {
        actions.add(new EmptyAction().fullFATOnly());
        return this;
    }

    /**
     * Adds an iteration of test execution, where the action.setup() is called before repeating the tests.
     */
    public RepeatTests andWith(RepeatTestAction action) {
        actions.add(action);
        return this;
    }

    /**
     * Removes an iteration of test execution of the previous <code>and*</code> call if the passed-in supplier
     * returns false. If true or if no test executions have been added, this method will have no effect.
     */
    public RepeatTests onlyIf(Supplier<Boolean> check) {
        int size = actions.size();
        if (size > 0 && !check.get()) {
            actions.remove(size - 1);
        }
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
            ArrayList<Throwable> errors = new ArrayList<>();

            Log.info(c, m, "All tests attempt to run " + actions.size() + " times:");
            for (int i = 0; i < actions.size(); i++)
                Log.info(c, m, "  [" + i + "] " + actions.get(i));

            for (RepeatTestAction action : actions) {
                try {
                    RepeatTestFilter.activateRepeatAction(action);
                    if (shouldRun(action)) {
                        Log.info(c, m, "===================================");
                        Log.info(c, m, "");
                        Log.info(c, m, "Running tests with action: " + action);
                        Log.info(c, m, "");
                        Log.info(c, m, "===================================");
                        action.setup();
                        statement.evaluate();
                        action.cleanup();
                    } else {
                        Log.info(c, m, "===================================");
                        Log.info(c, m, "");
                        Log.info(c, m, "Skipping tests with action: " + action);
                        Log.info(c, m, "");
                        Log.info(c, m, "===================================");
                    }
                } catch (Throwable t) {
                    // Contrary to the javadoc for @ClassRule, a class statement may throw an exception
                    // Catch it to ensure we still run all repeats
                    errors.add(t);
                } finally {
                    RepeatTestFilter.deactivateRepeatAction();
                }
            }

            MultipleFailureException.assertEmpty(errors);
        }

        private static boolean shouldRun(RepeatTestAction action) {
            String repeatOnly = System.getProperty("fat.test.repeat.only"); // If current action matches
            String repeatAny = System.getProperty("fat.test.repeat.any"); // If any action matches
            if (repeatOnly == null && repeatAny == null) {
                return action.isEnabled();
            } else {
                if (repeatOnly != null) {
                    // Note: If the user has requested this specific action, we ignore the isEnabled() flag
                    return action.getID().equals(repeatOnly);
                } else { // repeatAny != null
                    boolean repeatNotAny = false;
                    if (repeatAny.startsWith("!")) {
                        repeatNotAny = true;
                        repeatAny = repeatAny.substring(1, repeatAny.length());
                    }

                    if (repeatNotAny) {
                        // Note: If the user has requested an action NOT be any of the
                        //       active actions, we ignore isEnabled() flag.
                        return !RepeatTestFilter.isRepeatActionActive(repeatAny);
                    } else {
                        // Note: If the user has requested any of the active actions, we ignore isEnabled() flag.
                        return RepeatTestFilter.isRepeatActionActive(repeatAny);
                    }
                }
            }
        }
    }
}