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

package com.ibm.ws.jpa.tests.spec20;

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE7FeatureReplacementAction;

public class RepeatWithJPA21OpenJPA extends EE7FeatureReplacementAction {
    public static final String ID = "JPA21_OPENJPA";

    /**
     * Restrict OpenJPA tests to run on FULL mode
     */
    public RepeatWithJPA21OpenJPA() {
        // Used in componenttest.rules.repeater.RepeatTestAction.isEnabled() to determine if the test should run
        withTestMode(TestMode.FULL);
    }

    @Override
    public String toString() {
        return "Switch to JPA Container 2.1 feature and use OpenJPA for JPA persistence provider";
    }

    @Override
    public void setup() throws Exception {
        FATSuite.repeatPhase = "openjpa21-cfg.xml";
        FATSuite.provider = JPAPersistenceProvider.DEFAULT;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public boolean isEnabled() {
        if (!Boolean.getBoolean("jpa.enable.repeat.openjpa"))
            return false;

        return super.isEnabled();
    }
}
