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
package io.openliberty.microprofile.openapi20.fat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.FeatureSet.FeatureSetBuilder;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatActions;
import componenttest.rules.repeater.RepeatTests;

public class OpenApiActions {

    public static final String MP_OPENAPI_31_ID = MicroProfileActions.MP50_ID + "_MpOpenApi_31";

    public static final FeatureSet MP_OPENAPI_31 = new FeatureSetBuilder(MicroProfileActions.MP50).removeFeature("mpOpenAPI-3.0")
                                                                                                  .addFeature("mpOpenAPI-3.1")
                                                                                                  .build(MP_OPENAPI_31_ID);

    private static final Set<FeatureSet> ALL;

    static {
        Set<FeatureSet> allSets = new HashSet<>(MicroProfileActions.ALL);
        allSets.add(MP_OPENAPI_31);
        ALL = Collections.unmodifiableSet(allSets);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode. The others will be run in FULL.
     *
     * @param server The server to repeat on
     * @param firstFeatureSet The first FeatureSet
     * @param otherFeatureSets The other FeatureSets
     * @return a RepeatTests instance
     */
    public static RepeatTests repeat(String server, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return RepeatActions.repeat(server, TestMode.FULL, ALL, firstFeatureSet, otherFeatureSets);
    }

}
