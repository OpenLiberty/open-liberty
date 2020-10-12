/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

public interface RepeatTestAction {
    /**
     * Invoked by the FAT framework to test if the action should be applied or not.
     *
     * A RepeatTestAction which is disabled should log a message indicating why
     * the test is disabled.
     *
     * @return True or false telling if this action should be applied.
     */
    boolean isEnabled();

    /**
     * Invoked by the FAT framework to perform setup steps before repeating the tests.
     *
     * @throws Exception Thrown in case of an exceptional failure of the test setup.
     */
    void setup() throws Exception;

    /**
     * Used to identify the RepeatTestAction and used in conjunction with @SkipForRepeat.
     *
     * @return The identifier of this test action.
     */
    String getID();
}
