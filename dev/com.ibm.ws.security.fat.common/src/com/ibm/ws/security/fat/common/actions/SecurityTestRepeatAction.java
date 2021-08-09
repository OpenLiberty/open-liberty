/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.actions;

import componenttest.rules.repeater.RepeatTestAction;

public class SecurityTestRepeatAction implements RepeatTestAction {

    protected String nameExtension = "default";

    public SecurityTestRepeatAction(String inNameExtension) {

        nameExtension = inNameExtension;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setup() throws Exception {
        nameExtension = "default";
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.rules.repeater.RepeatTestAction#getID()
     */
    @Override
    public String getID() {
        if (nameExtension != null) {
            return nameExtension;
        } else {
            return "default";
        }
    }
}

/*
 * To use, insert one of the following into your class - by doing so, each test case will have the string appended to its name
 * 
 * @ClassRule
 * public static RepeatTests r = RepeatTests.with(new SecurityTestRepeatAction(<ConstantClassName>.<ConstantName>));
 * 
 * @ClassRule
 * public static RepeatTests r = RepeatTests.with(new SecurityTestRepeatAction("someString"));
 *
 */