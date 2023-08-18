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

import java.util.ArrayList;
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

    private static final List<FeatureSet> ALL;

    static {
        //The list of all features must be in decending order
        ALL = new ArrayList<>(MicroProfileActions.ALL);
        //put CTX12 just before MP40
        ALL.add(ALL.indexOf(MicroProfileActions.MP40), CTX12);
        //put CTX10 just before MP32
        ALL.add(ALL.indexOf(MicroProfileActions.MP32), CTX10);
    }

    public static RepeatTests repeat(String server, FeatureSet firstVersion, FeatureSet... otherVersions) {
        return RepeatActions.repeat(server, TestMode.LITE, ALL, firstVersion, otherVersions);
    }

}
