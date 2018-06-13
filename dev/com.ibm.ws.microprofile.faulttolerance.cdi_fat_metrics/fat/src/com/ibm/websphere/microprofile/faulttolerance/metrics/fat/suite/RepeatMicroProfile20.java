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
public class RepeatMicroProfile20 extends FeatureReplacementAction {

    static final String[] MP20_FEATURES_ARRAY = { "mpConfig-1.3", "mpFaultTolerance-1.1", "servlet-4.0", "cdi-2.0", "appSecurity-3.0" };
    static final Set<String> MP20_FEATURE_SET = new HashSet<>(Arrays.asList(MP20_FEATURES_ARRAY));

    public RepeatMicroProfile20(String server) {
        super(RepeatMicroProfile13.MP13_FEATURE_SET, MP20_FEATURE_SET);
        forceAddFeatures(true);
        withID("MICROPROFILE20");
        forServers(server);
    }

}
