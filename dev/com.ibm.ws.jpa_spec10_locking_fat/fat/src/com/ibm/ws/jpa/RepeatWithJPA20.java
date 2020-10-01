/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class RepeatWithJPA20 extends FeatureReplacementAction {
    public static final String ID = "JPA20_FEATURES";

    public RepeatWithJPA20() {
        super(EE7FeatureReplacementAction.EE7_FEATURE_SET, featuresToAdd());
        forceAddFeatures(false);
        this.withID(ID);
    }

    private static Set<String> featuresToAdd() {
        Set<String> addFeatures = new HashSet<>(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        addFeatures.remove("jpa-2.1");
        addFeatures.add("jpa-2.0");
        return addFeatures;
    }

    @Override
    public boolean isEnabled() {
        LibertyServer server = LibertyServerFactory.getLibertyServer("EntityLocking");

        File jpa20Feature = new File(server.getInstallRoot() + "/lib/features/com.ibm.websphere.appserver.jpa-2.0.mf");
        Log.info(getClass(), "isEnabled", "Does the jpa-2.0 feature exist? " + jpa20Feature.exists());
        return jpa20Feature.exists();
    }

    @Override
    public String toString() {
        return "Set JPA feature to 2.0 version";
    }
}
