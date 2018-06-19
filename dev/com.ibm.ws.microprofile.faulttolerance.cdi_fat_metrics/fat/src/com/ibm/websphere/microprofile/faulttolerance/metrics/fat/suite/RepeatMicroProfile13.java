/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.suite;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import componenttest.rules.repeater.FeatureReplacementAction;

/**
 *
 */
public class RepeatMicroProfile13 extends FeatureReplacementAction {

    static final String[] MP13_FEATURES_ARRAY = { "mpConfig-1.2", "mpFaultTolerance-1.0", "servlet-3.1", "cdi-1.2", "appSecurity-2.0" };
    static final Set<String> MP13_FEATURE_SET = new HashSet<>(Arrays.asList(MP13_FEATURES_ARRAY));

    public RepeatMicroProfile13(String server) {
        super(RepeatMicroProfile20.MP20_FEATURE_SET, MP13_FEATURE_SET);
        forceAddFeatures(true);
        withID("MICROPROFILE13");
        forServers(server);
    }

}
