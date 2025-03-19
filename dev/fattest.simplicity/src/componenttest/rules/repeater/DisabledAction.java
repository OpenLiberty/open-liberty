/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package componenttest.rules.repeater;

/**
 * An action that is never enabled
 */
public class DisabledAction implements RepeatTestAction {

    private static final Class<?> c = DisabledAction.class;

    public static final String ID = "DISABLED_ACTION";

    @Override
    public void setup() {}

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String toString() {
        return "Disabled Action";
    }

    @Override
    public String getID() {
        return ID;
    }

}
