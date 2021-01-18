/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.custom.junit.runner.TestModeFilter;

/**
 * The trivial repeat action.  This repeat action runs tests as-is, with
 * no particular modifications away from the default test configuration.
 */
public class EmptyAction implements RepeatTestAction {
    private static final Class<? extends EmptyAction> c = EmptyAction.class;

    /** Singleton no-modification repeat action. */
    public static final RepeatTestAction NO_MODIFICATION_ACTION = new EmptyAction();

    /** The ID of no-modification repeat actions. */
    public static final String ID = "NO_MODIFICATION_ACTION";

    /**
     * Answer the ID of this repeat action.
     * 
     * See {@link #ID}.
     * 
     * @return the ID of this repeat action.
     */
    @Override
    public String getID() {
        return ID;
    }

    /**
     * Answer a print string for this repeat action.
     * 
     * @return A print string for this repeat action.
     */
    @Override
    public String toString() {
        return "No modifications repeat test action";
    }

    /**
     * Do nothing.  This repeat action runs tests as-is.
     */
    @Override
    public void setup() {
        // Empty
    }

    /**
     * Default mode in which to run the empty action.  This
     * setting enables the empty action to run in all framework
     * modes.
     */
    private TestMode testRunMode = TestMode.LITE;

    /**
     * Set the test mode of this empty action to {@link TestMode#FULL}.
     *
     * The setting enables the tests to run in {@link TestModel#FULL}
     * or greater.
     * 
     * @return This empty action.
     */
    public RepeatTestAction fullFATOnly() {
        this.testRunMode = TestMode.FULL;
        return this;
    }

    /**
     * Tell if this repeat action is enabled.  This is based on the test
     * mode which is set for this action, and on the framework test mode.
     * 
     * This empty repeat action is enabled only if the test mode is less
     * than or equal to the framework test mode.
     * 
     * @return True or false telling if this repeat action is enabled.
     */
    @Override
    public boolean isEnabled() {
      if ( TestModeFilter.FRAMEWORK_TEST_MODE.compareTo(testRunMode) < 0 ) {
          Log.info(c, "isEnabled",
              "Skipping action '" + toString() + "' because the test mode " + testRunMode +
              " is not valid for framework mode " + TestModeFilter.FRAMEWORK_TEST_MODE);
          return false;
      }
      return true;
    }

    /**
     * Evaluate the statement in the context of this repeat action.
     * 
     * Do activate the repeat action, but do no particular setup.
     */
    @Override
    public void evaluate(Statement statement) throws Throwable {
        RepeatTestFilter.activateRepeatAction( getID() );                        

        try {
            // setup(); Not done by this action.
            statement.evaluate();

        } finally {
            RepeatTestFilter.deactivateRepeatAction();
        }
    }
}
