/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa;

import java.io.File;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.rules.repeater.RepeatTestAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class RepeatWithJPA20 implements RepeatTestAction {
    public static final String ID = "JPA20";

    @Override
    public boolean isEnabled() {
        LibertyServer server = LibertyServerFactory.getLibertyServer("JPAServer");

        File jpa20Feature = new File(server.getInstallRoot() + "/lib/features/com.ibm.websphere.appserver.jpa-2.0.mf");
        Log.info(getClass(), "isEnabled", "Does the jpa-2.0 feature exist? " + jpa20Feature.exists());
        return jpa20Feature.exists();
    }

    @Override
    public String toString() {
        return "Set JPA feature to 2.0 version";
    }

    @Override
    public void setup() throws Exception {
        FATSuite.repeatPhase = "jpa20.xml";
    }

    @Override
    public String getID() {
        return ID;
    }
}
