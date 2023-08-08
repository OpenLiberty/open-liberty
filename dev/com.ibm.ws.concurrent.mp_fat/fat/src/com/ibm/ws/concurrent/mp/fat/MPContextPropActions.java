/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
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
package com.ibm.ws.concurrent.mp.fat;

import java.util.Arrays;
import java.util.List;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatActions;
import componenttest.rules.repeater.RepeatTests;

public class MPContextPropActions {

    public static final String CTX10_ID = "MP_CONTEXT_PROP_10";
    public static final String CTX12_ID = "MP_CONTEXT_PROP_12";

    public static final FeatureSet CTX10 = MicroProfileActions.MP32.addFeature("mpContextPropagation-1.0").build(CTX10_ID);
    public static final FeatureSet CTX12 = MicroProfileActions.MP40.addFeature("mpContextPropagation-1.2").build(CTX12_ID);

    //All MicroProfile CTX FeatureSets - must be descending order
    private static final FeatureSet[] ALL_CTX_SETS_ARRAY = { CTX12, CTX10 };
    private static final List<FeatureSet> ALL = Arrays.asList(ALL_CTX_SETS_ARRAY);

    public static RepeatTests repeat(String server, FeatureSet firstVersion, FeatureSet... otherVersions) {
        return RepeatActions.repeat(server, TestMode.LITE, ALL, firstVersion, otherVersions);
    }

}
