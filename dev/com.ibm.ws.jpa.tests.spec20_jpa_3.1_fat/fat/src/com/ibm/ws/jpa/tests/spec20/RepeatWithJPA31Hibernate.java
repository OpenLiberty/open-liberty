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

package com.ibm.ws.jpa.tests.spec20;

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE10Action;

public class RepeatWithJPA31Hibernate extends JakartaEE10Action {
    public static final String ID = "JPA31_HIBERNATE";

    /**
     * Restrict Hibernate tests to run on FULL mode
     */
    public RepeatWithJPA31Hibernate() {
        // Used in componenttest.rules.repeater.RepeatTestAction.isEnabled() to determine if the test should run
        withTestMode(TestMode.FULL);
    }

    @Override
    public String toString() {
        return "Switch to JPA Container 3.1 feature and use Hibernate for JPA persistence provider";
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        FATSuite.repeatPhase = "hibernate31-cfg.xml";
        FATSuite.provider = JPAPersistenceProvider.HIBERNATE;
    }

    @Override
    public boolean isEnabled() {
        if (!Boolean.getBoolean("jpa.enable.repeat.hibernate"))
            return false;

        return super.isEnabled();
    }
}
