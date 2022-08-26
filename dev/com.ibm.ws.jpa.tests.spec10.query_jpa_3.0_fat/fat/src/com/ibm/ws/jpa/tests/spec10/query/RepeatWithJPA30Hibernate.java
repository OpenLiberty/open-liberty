/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.tests.spec10.query;

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;

public class RepeatWithJPA30Hibernate extends JakartaEE9Action {
    public static final String ID = "JPA30_HIBERNATE";

    /**
     * Restrict Hibernate tests to run on FULL mode
     */
    public RepeatWithJPA30Hibernate() {
        // Used in componenttest.rules.repeater.RepeatTestAction.isEnabled() to determine if the test should run
        withTestMode(TestMode.FULL);
    }

    @Override
    public String toString() {
        return "Switch to JPA Container 3.0 feature and use Hibernate for JPA persistence provider";
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        FATSuite.repeatPhase = "hibernate30-cfg.xml";
        FATSuite.provider = JPAPersistenceProvider.HIBERNATE;
    }

//    // Overriding this method will disable Jakarta EE9 transformer
//    @Override
//    public String getID() {
//        return ID;
//    }

    @Override
    public boolean isEnabled() {
        // Disable testing against Hibernate for time constraints
        if (!Boolean.getBoolean("jpa.enable.repeat.hibernate"))
            return false;

        return super.isEnabled();
    }
}
