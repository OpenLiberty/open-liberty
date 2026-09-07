/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE12Action;

/**
 * Runs the FAT suite once: {@code persistence-4.0} with Hibernate 8 as the default provider.
 * Marked LITE so it runs in the standard CI pipeline pass.
 */
public class RepeatWithJPA40 extends JakartaEE12Action {
    public static final String ID = "JPA40";

    public RepeatWithJPA40() {
        withID(ID);
        withTestMode(TestMode.LITE);
    }

    @Override
    public String toString() {
        return "JPA 4.0 + Hibernate 8";
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        FATSuite.repeatPhase = "persistence40-cfg.xml";
        FATSuite.provider = JPAPersistenceProvider.HIBERNATE;
    }
}
