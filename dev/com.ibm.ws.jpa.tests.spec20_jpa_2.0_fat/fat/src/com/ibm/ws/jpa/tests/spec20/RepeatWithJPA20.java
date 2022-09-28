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

import java.io.File;

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;

import componenttest.common.apiservices.Bootstrap;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE6FeatureReplacementAction;

public class RepeatWithJPA20 extends EE6FeatureReplacementAction {
    public static final String ID = "JPA20";

    /**
     * Allow the default repeat action to run on LITE mode
     */
    public RepeatWithJPA20() {
        // Used in componenttest.rules.repeater.RepeatTestAction.isEnabled() to determine if the test should run
        withTestMode(TestMode.LITE);
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

    @Override
    public boolean isEnabled() {
        try {
            Bootstrap b = Bootstrap.getInstance();
            String installRoot = b.getValue("libertyInstallPath");
            File jpa20Feature = new File(installRoot + "/lib/features/com.ibm.websphere.appserver.jpa-2.0.mf");
            return jpa20Feature.exists() && super.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }
}
