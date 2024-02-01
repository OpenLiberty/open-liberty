/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.jpa;

import componenttest.rules.repeater.JakartaEE11Action;
import componenttest.rules.repeater.RepeatTestAction;

public class RepeatWithJPA32 extends JakartaEE11Action implements RepeatTestAction {
    public static final String ID = "JPA32";

//     @Override
//     public boolean isEnabled() {
//         return true;
//     }

    @Override
    public String toString() {
        return "JPA 3.2";
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        FATSuite.repeatPhase = "jpa32.xml";
    }

}
