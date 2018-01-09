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

public class EmptyAction implements RepeatTestAction {

    public static final String ID = "NO_MODIFICATION_ACTION";

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

    @Override
    public String getID() {
        return ID;
    }

}
