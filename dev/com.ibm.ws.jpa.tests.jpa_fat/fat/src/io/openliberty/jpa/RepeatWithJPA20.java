/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.jpa;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.rules.repeater.EE6FeatureReplacementAction;
import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE11Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class RepeatWithJPA20 extends FeatureReplacementAction {
    public static final String ID = EE7FeatureReplacementAction.ID + "_JPA20";

    public RepeatWithJPA20() {
        Set<String> addFeatures = new HashSet<>(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        addFeatures.remove("jpa-2.1");
        addFeatures.add("jpa-2.0");

        addFeatures(addFeatures);

        removeFeatures(EE6FeatureReplacementAction.EE6_FEATURE_SET);
        removeFeatures(EE8FeatureReplacementAction.EE8_FEATURE_SET);
        removeFeatures(JakartaEE9Action.EE9_FEATURE_SET);
        removeFeatures(JakartaEE10Action.EE10_FEATURE_SET);
        removeFeatures(JakartaEE11Action.EE11_FEATURE_SET);
        forceAddFeatures(false);
        withID(ID);
    }

    @Override
    public boolean isEnabled() {
        LibertyServer server = LibertyServerFactory.getLibertyServer("ConcurrentEnhancementVerification");

        File jpa20Feature = new File(server.getInstallRoot() + "/lib/features/com.ibm.websphere.appserver.jpa-2.0.mf");
        Log.info(getClass(), "isEnabled", "Does the jpa-2.0 feature exist? " + jpa20Feature.exists());
        return jpa20Feature.exists();
    }
}
