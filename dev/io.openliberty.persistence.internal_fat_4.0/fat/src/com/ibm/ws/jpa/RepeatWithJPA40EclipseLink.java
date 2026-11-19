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
 * Runs the FAT suite once: {@code persistenceContainer-4.0} with EclipseLink 5.
 * Marked FULL so it runs only in the full CI pipeline pass.
 */
public class RepeatWithJPA40EclipseLink extends JakartaEE12Action {
    public static final String ID = "JPA_CONTAINER40_ECLIPSELINK";

    public RepeatWithJPA40EclipseLink() {
        withID(ID);
        withTestMode(TestMode.FULL);
    }

    @Override
    public String toString() {
        return "persistenceContainer-4.0 + EclipseLink 5";
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        FATSuite.repeatPhase = "eclipselink40-cfg.xml";
        FATSuite.provider = JPAPersistenceProvider.ECLIPSELINK;
    }
}
