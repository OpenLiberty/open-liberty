/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.microprofile.openapi.fat.filter.FilterConfigTest;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestFive;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestFour;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestOne;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestSix;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestThree;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestTwo;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                ApplicationProcessorTest.class,
                OpenAPIValidationTestOne.class,
                OpenAPIValidationTestTwo.class,
                OpenAPIValidationTestThree.class,
                OpenAPIValidationTestFour.class,
                OpenAPIValidationTestFive.class,
                OpenAPIValidationTestSix.class,
                FilterConfigTest.class,
                ProxySupportTest.class,
                EndpointAvailabilityTest.class,
                UICustomizationTest.class
})
public class FATSuite {
    private static final String[] ALL_VERSIONS = { "1.0", "1.1", "2.0" };

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(MP_OPENAPI("1.1")).andWith(MP_OPENAPI("2.0"));

    static FeatureReplacementAction MP_OPENAPI(String version) {
        return MP_OPENAPI(new FeatureReplacementAction(), version);
    }

    static FeatureReplacementAction MP_OPENAPI(FeatureReplacementAction action, String version) {
        return use(action, "mpOpenAPI", version).withID("mpOpenAPI-" + version);
    }

    private static FeatureReplacementAction use(FeatureReplacementAction action, String featureName, String version) {
        return use(action, featureName, version, ALL_VERSIONS);
    }

    private static FeatureReplacementAction use(FeatureReplacementAction action, String featureName, String version, String... versionsToRemove) {
        action = action.addFeature(featureName + "-" + version);
        for (String remove : versionsToRemove) {
            if (!version.equals(remove)) {
                action = action.removeFeature(featureName + "-" + remove);
            }
        }
        return action;
    }
}
