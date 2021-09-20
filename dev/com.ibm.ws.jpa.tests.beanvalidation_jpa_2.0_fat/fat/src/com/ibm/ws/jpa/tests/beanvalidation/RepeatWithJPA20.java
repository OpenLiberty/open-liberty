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

import java.io.File;

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;

import componenttest.common.apiservices.Bootstrap;
import componenttest.rules.repeater.RepeatTestAction;

/**
 *
 */
public class RepeatWithJPA20 implements RepeatTestAction {
    public static final String ID = "JPA20";

    @Override
    public boolean isEnabled() {
        try {
            Bootstrap b = Bootstrap.getInstance();
            String installRoot = b.getValue("libertyInstallPath");
            File jpa20Feature = new File(installRoot + "/lib/features/com.ibm.websphere.appserver.jpa-2.0.mf");
            return jpa20Feature.exists();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "JPA 2.0";
    }

    @Override
    public void setup() throws Exception {
        FATSuite.repeatPhase = "jpa20-cfg.xml";
        FATSuite.provider = JPAPersistenceProvider.DEFAULT;
    }

    @Override
    public String getID() {
        return ID;
    }
}
