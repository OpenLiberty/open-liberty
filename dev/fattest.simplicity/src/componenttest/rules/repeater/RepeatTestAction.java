/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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

public interface RepeatTestAction {

    /**
     * Invoked by the FAT framework to test if the action should be applied or not.
     * If a RepeatTestAction is disabled, it ought to log a message indicating why.
     */
    public boolean isEnabled();

    /**
     * Invoked by the FAT framework to perform setup steps before repeating the tests.
     */
    public void setup() throws Exception;

    /**
     * Used to identify the RepeatTestAction and used in conjunction with @SkipForRepat
     */
    public String getID();

    /**
     * Invoked by the FAT framework to perform cleanup steps before ending test repetition.
     * If clean up is needed to undo the setup changes after running the action then override this method.
     */
    default public void cleanup() throws Exception {}
}