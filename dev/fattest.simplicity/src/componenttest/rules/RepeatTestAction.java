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

}
