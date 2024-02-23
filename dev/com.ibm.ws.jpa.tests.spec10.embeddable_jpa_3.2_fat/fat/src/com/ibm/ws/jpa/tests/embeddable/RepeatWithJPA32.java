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

package com.ibm.ws.jpa.tests.embeddable;

import com.ibm.ws.jpa.tests.embeddable.tests.AbstractFATSuite;
import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE11Action;

public class RepeatWithJPA32 extends JakartaEE11Action {
    public static final String ID = "JPA32";

    /**
     * Allow the default repeat action to run on LITE mode
     */
    public RepeatWithJPA32() {
        // Used in componenttest.rules.repeater.RepeatTestAction.isEnabled() to determine if the test should run
        withTestMode(TestMode.LITE);
    }

    @Override
    public String toString() {
        return "JPA 3.2";
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        AbstractFATSuite.repeatPhase = "jpa32-cfg.xml";
        AbstractFATSuite.provider = JPAPersistenceProvider.DEFAULT;
    }
}
