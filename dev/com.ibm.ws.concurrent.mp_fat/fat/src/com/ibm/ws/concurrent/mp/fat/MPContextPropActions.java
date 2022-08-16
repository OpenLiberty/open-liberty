/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.fat;

import java.util.HashSet;
import java.util.Set;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;

public class MPContextPropActions {

    public static final String CTX10_ID = "MP_CONTEXT_PROP_10";
    public static final String CTX12_ID = "MP_CONTEXT_PROP_12";

    public static final FeatureSet CTX10 = MicroProfileActions.MP32.addFeature("mpContextPropagation-1.0").build(CTX10_ID);
    public static final FeatureSet CTX12 = MicroProfileActions.MP40.addFeature("mpContextPropagation-1.2").build(CTX12_ID);

    public static final Set<FeatureSet> ALL;
    static {
        ALL = new HashSet<>(MicroProfileActions.ALL);
        ALL.add(CTX10);
        ALL.add(CTX12);
    }

    public static RepeatTests repeat(String server, FeatureSet firstVersion, FeatureSet... otherVersions) {
        return MicroProfileActions.repeat(server, TestMode.LITE, ALL, firstVersion, otherVersions);
    }

}
