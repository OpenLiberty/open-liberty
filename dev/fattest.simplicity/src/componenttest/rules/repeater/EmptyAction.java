/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;

public class EmptyAction implements RepeatTestAction {

    private static final Class<?> c = EmptyAction.class;

    public static final String ID = "NO_MODIFICATION_ACTION";
    private TestMode testRunMode = TestMode.LITE;

    @Override
    public void setup() {}

    @Override
    public boolean isEnabled() {
      if (TestModeFilter.FRAMEWORK_TEST_MODE.compareTo(testRunMode) < 0) {
          Log.info(c, "isEnabled", "Skipping action '" + toString() + "' because the test mode " + testRunMode +
                                   " is not valid for current mode " + TestModeFilter.FRAMEWORK_TEST_MODE);
          return false;
      }
      return true;
    }

    public RepeatTestAction fullFATOnly() {
        this.testRunMode = TestMode.FULL;
        return this;
    }

    @Override
    public String toString() {
        return "No modifications";
    }

    @Override
    public String getID() {
        return ID;
    }

}
