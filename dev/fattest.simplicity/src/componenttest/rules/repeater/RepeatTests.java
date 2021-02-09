/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import java.util.ArrayList;
import java.util.List;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Used as a JUnit <code>@ClassRule</code> to repeat all tests with the
 * specified actions.
 *
 * <b>NOTE: Order is significant when building the object! Tests will be
 * run in the order of the builder methods.</b>
 *
 * For example, if a FAT was written with Java EE 7 features initially,
 * and we want to repeat the tests with Java EE 8 equivalent features,
 * do the following:
 *
 * <pre>
 * <code>
 * @ClassRule
 * public static RepeatTests r =
 *     RepeatTests.withoutModification()
 *                .andWith( FeatureReplacementAction.EE8_FEATURES() );
 * </code>
 * </pre>
 */
public class RepeatTests extends ExternalResource {
    private static final Class<? extends RepeatTests> c = RepeatTests.class;
    
    /** Property used to limit repeat actions to a single specified repeat action. */
    public static final String REPEAT_ONLY_PROPERTY_NAME = "fat.test.repeat.only";

    /**
     * A single specified repeat action which is to be run.  When null,
     * run all enabled repeat actions which were specified using a class rule. 
     */
    public static final String REPEAT_ONLY = System.getProperty(REPEAT_ONLY_PROPERTY_NAME);

    static {
        Log.info(c, "<clinit>",
            "Repeat tests only with action (" + REPEAT_ONLY_PROPERTY_NAME + "): " + REPEAT_ONLY);
    }

    /**
     * Enablement helper: Tell if tests should be run with a specified
     * repeat action.
     * 
     * If a specific repeat-only repeat action was specified using property
     * {@link #REPEAT_ONLY_PROPERTY_NAME}, then run only the repeat action
     * which has that ID.
     * 
     * Of no specific repeat only repeat action was specfied, run actions
     * which are enabled.  See {@link RepeatTestAction#isEnabled()}.
     * 
     * @param action The action which is to be tested.
     *
     * @return True or false telling if tests should be run using the repeat
     *     action.
     */
    private static boolean shouldRun(RepeatTestAction action) {
        if ( REPEAT_ONLY == null ) {
            return action.isEnabled();
        } else {
            return action.getID().equals(REPEAT_ONLY);
        }
    }
    
    /**
     * Repeat tests factory method:  Create a repeat tests widget which
     * has a single "no-modification" repeat action.  Other repeat actions
     * may be added to the test widget.
     * 
     * @return A repeat tests widget with a single "no-modification" repeat action.
     */
    public static RepeatTests withoutModification() {
        return new RepeatTests().andWithoutModification();
    }

    /**
     * Repeat tests factory method: Create a repeat tests widget which has a
     * single repeat tests action.  Other repeat actions may be added to the
     * test widget.
     * 
     * @return A repeat tests widget with the single specified repeat action.
     */
    public static RepeatTests with(RepeatTestAction action) {
        return new RepeatTests().andWith(action);
    }

    /**
     * Private constructor: Use one of {@link #withoutModification()} or
     * {@link #andWith(RepeatTestAction)} to create a new repeat tests
     * widget.
     */
    private RepeatTests() {
        this.actions = new ArrayList<>();
    }

    /**
     * The repeat actions of this test widget.  Current factory methods
     * immediately add a single test action to this collection.
     */
    private final List<RepeatTestAction> actions;

    /**
     * Add the no-modification action to this test widget's action.
     * 
     * @return This repeat tests widget.
     */
    public RepeatTests andWithoutModification() {
        actions.add(EmptyAction.NO_MODIFICATION_ACTION);
        return this;
    }

    /**
     * Add a single repeat action to this test widget's actions.
     * 
     * @return This repeat tests widget.
     */
    public RepeatTests andWith(RepeatTestAction action) {
        actions.add(action);
        return this;
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new RepeatStatement(statement, actions);
    }

    private static class RepeatStatement extends Statement {
        private static Class<? extends RepeatStatement> innerC = RepeatStatement.class;

        private final Statement statement;
        private final List<? extends RepeatTestAction> actions;

        private RepeatStatement(Statement statement, List<RepeatTestAction> actions) {
            this.statement = statement;
            this.actions = actions;
        }

        @Override
        public void evaluate() throws Throwable {
            String methodName = "evaluate";

            Log.info(innerC, methodName, "Repeating tests with " + actions.size() + " repeat actions:");
            for ( int actionNo = 0; actionNo < actions.size(); actionNo++ ) {
                Log.info(innerC, methodName, "  [" + actionNo + "] " + actions.get(actionNo));
            }

            ArrayList<Throwable> errors = new ArrayList<>();

            for ( RepeatTestAction action : actions ) {
                if ( !shouldRun(action) ) {
                    Log.info(innerC, methodName, "Skip repeat tests using: " + action);
                    continue;
                }
                
                Log.info(innerC, methodName, "Repeat tests using: " + action);

                try {
                    action.evaluate(statement); // throws Throwable

                } catch ( Throwable t ) {
                    Log.info(innerC, methodName,
                        "Failure of repeat tests using: " + action +
                        ": " + t.getMessage());

                    // Contrary to the javadoc for @ClassRule, a class statement may
                    // throw an exception.
                    // Catch it to ensure we still run all repeats.
                    errors.add(t);
                }
            }

            MultipleFailureException.assertEmpty(errors);
        }
    }
}