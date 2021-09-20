/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.tests.beanvalidation;

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTestAction;

/**
 *
 */
public class RepeatWithJPA30Hibernate extends JakartaEE9Action implements RepeatTestAction {
    public static final String ID = "JPA30_HIBERNATE";

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "Switch to JPA Container 3.0 feature and use Hibernate for JPA persistence provider";
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        FATSuite.repeatPhase = "hibernate30-cfg.xml";
        // Set this for Hibernate specific DDL
        FATSuite.provider = JPAPersistenceProvider.HIBERNATE;
    }

//    // Overriding this method will disable Jakarta EE9 transformer
//    @Override
//    public String getID() {
//        return ID;
//    }
}
