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

package com.ibm.ws.jpa.tests.spec10.query;

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE11Action;

public class RepeatWithJPA32Hibernate extends JakartaEE11Action {
    public static final String ID = "JPA32_HIBERNATE";

    /**
     * Restrict Hibernate tests to run on FULL mode
     */
    public RepeatWithJPA32Hibernate() {
        // Used in componenttest.rules.repeater.RepeatTestAction.isEnabled() to determine if the test should run
        withTestMode(TestMode.FULL);
    }

    @Override
    public String toString() {
        return "Switch to JPA Container 3.2 feature and use Hibernate for JPA persistence provider";
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        FATSuite.repeatPhase = "hibernate32-cfg.xml";
        FATSuite.provider = JPAPersistenceProvider.HIBERNATE;
    }

    @Override
    public boolean isEnabled() {
        // Disable testing against Hibernate for time constraints
        if (!Boolean.getBoolean("jpa.enable.repeat.hibernate"))
            return false;

        return super.isEnabled();
    }
}
