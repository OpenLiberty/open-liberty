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
package io.openliberty.microprofile.telemetry.internal_fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                Telemetry10.class
})
public class FATSuite {

    private static final FeatureSet MP50_MPTELEMETRY = MicroProfileActions.MP50.addFeature("mpTelemetry-1.0").build(MicroProfileActions.MP50_ID + "_MPTELEMETRY");

    @ClassRule
    public static final RepeatTests r = MicroProfileActions.repeat(null, MicroProfileActions.MP60, MP50_MPTELEMETRY);

}
