/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.jpa.tests.spec10.injection.dpu;

import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.RepeatTestAction;

public class RepeatWithJPA31 extends JakartaEE10Action implements RepeatTestAction {
    public static final String ID = "JPA31";

//     @Override
//     public boolean isEnabled() {
//         return true;
//     }

    @Override
    public String toString() {
        return "JPA 3.1";
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        FATSuite.repeatPhase = "jpa31.xml";
    }
}
