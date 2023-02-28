/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package com.ibm.ws.jpa.tests.container.checkpoint;

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
        FATSuite.provider = JPAPersistenceProvider.DEFAULT;
    }

//    // Overriding this method will disable Jakarta EE9 transformer
//    @Override
//    public String getID() {
//        return ID;
//    }
}
