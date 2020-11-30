/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.common.util;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Junit Rule for repeating tests.
 * 
 * The Rule merely sets up a framework for running repeated tests.
 * The user must supply a Callback object to handle any changes to
 * be made before/after each repetition. The Callback object also
 * decides when to stop repeating the test.
 * 
 */
public class RepeatTestRule implements TestRule {

    /**
     * Callback object that gets called before and after all repetitions
     * and before/after each individual repetition.
     */
    public static abstract class Callback {
        /**
         * Called once, before before the first repetition.
         */
        public void beforeAll() throws Exception {}

        /**
         * Called before each repetition.
         */
        public void beforeEach() throws Exception {}

        /**
         * Called after each repetition.
         */
        public void afterEach() throws Exception {}

        /**
         * Called after the last repetition.
         */
        public void afterAll() throws Exception {}

        /**
         * Called between repetitions.
         * 
         * @return true to do another repetition; false to terminate repetitions.
         */
        public boolean doRepeat() throws Exception {
            return false;
        }
    }

    public static void log(String method, String msg) {
        Log.info(RepeatTestRule.class, method, msg);
    }

    private final Callback callback;

    /**
     * CTOR.
     * 
     * @param callback
     */
    public RepeatTestRule(Callback callback) {
        this.callback = callback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Statement apply(Statement base, Description desc) {
        return new RepeatTestRuleStatement(base);
    }

    /**
     * Statement class - performs the before/after operations around a
     * call to the base Statement's evaulate() method (which runs the test).
     */
    protected class RepeatTestRuleStatement extends Statement {

        /**
         * A reference to the Statement that this Statement wraps around.
         */
        private final Statement base;

        /**
         * CTOR.
         * 
         * @param base The Statement that this Statement wraps around.
         */
        public RepeatTestRuleStatement(Statement base) {
            this.base = base;
        }

        /**
         * This method is called by the test runner in order to execute the test.
         * 
         * Before/After logic is embedded here around a call to base.evaluate(),
         * which processes the Statement chain (for any other @Rules that have been
         * applied) until at last the test method is executed.
         * 
         */
        @Override
        public void evaluate() throws Throwable {

            callback.beforeAll();

            try {
                do {
                    callback.beforeEach();
                    try {
                        base.evaluate();
                    } finally {
                        callback.afterEach();
                    }
                } while (callback.doRepeat());

            } finally {
                callback.afterAll();
            }

        }
    }

}
