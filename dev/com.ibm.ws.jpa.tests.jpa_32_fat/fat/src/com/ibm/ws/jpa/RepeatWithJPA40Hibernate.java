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
 * Repeat phase: run the JPA 3.2 test suite against the JPA 4.0 persistence
 * feature (persistence-4.0) with Hibernate 8 as the provider.
 */
public class RepeatWithJPA40Hibernate extends JakartaEE12Action {
    public static final String ID = "JPA40_HIBERNATE8";

    public RepeatWithJPA40Hibernate() {
        withID(ID);
        withTestMode(TestMode.FULL);
    }

    @Override
    public String toString() {
        return "JPA 3.2 apps on persistence-4.0 with Hibernate 8";
    }

    @Override
    public void setup() throws Exception {
        // Override the jakarta.persistence import version to [4.0,5) so the
        // transformed WARs resolve against the JPA 4.0 API bundle instead of
        // the JPA 3.2 API bundle (which is [3.2,4) by default in JakartaEE12Action).
        withLocalVersionTransformAppend("jpa40-versions.properties");
        super.setup();
        FATSuite.repeatPhase = "hibernate40-cfg.xml";
        FATSuite.provider = JPAPersistenceProvider.HIBERNATE;
    }
}
