/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.tests.embeddable;

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE8FeatureReplacementAction;

public class RepeatWithJPA22 extends EE8FeatureReplacementAction {
    public static final String ID = "JPA22";

    /**
     * Allow the default repeat action to run on LITE mode
     */
    public RepeatWithJPA22() {
        // Used in componenttest.rules.repeater.RepeatTestAction.isEnabled() to determine if the test should run
        withTestMode(TestMode.LITE);
    }

    @Override
    public String toString() {
        return "JPA 2.2";
    }

    @Override
    public void setup() throws Exception {
        FATSuite.repeatPhase = "jpa22-cfg.xml";
        FATSuite.provider = JPAPersistenceProvider.DEFAULT;
    }

    @Override
    public String getID() {
        return ID;
    }
}
