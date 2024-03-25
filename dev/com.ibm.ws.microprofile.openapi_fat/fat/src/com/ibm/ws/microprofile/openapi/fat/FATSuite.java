/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.microprofile.openapi.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.microprofile.openapi.fat.annotations.AnnotationProcessingTest;
import com.ibm.ws.microprofile.openapi.fat.config.OpenAPIConfigTest;
import com.ibm.ws.microprofile.openapi.fat.filter.FilterConfigTest;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestFive;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestFour;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestOne;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestSix;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestThree;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestTwo;

import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
    AnnotationProcessingTest.class,
    ApplicationProcessorServletTest.class,
    ApplicationProcessorTest.class,
    ContentTypeTest.class,
    OpenAPIConfigTest.class,
    OpenAPIValidationTestOne.class,
    OpenAPIValidationTestTwo.class,
    OpenAPIValidationTestThree.class,
    OpenAPIValidationTestFour.class,
    OpenAPIValidationTestFive.class,
    OpenAPIValidationTestSix.class,
    FilterConfigTest.class,
    ProxySupportTest.class,
    EndpointAvailabilityTest.class,
    UICustomizationTest.class,
    OpenAPICorsTest.class
})

public class FATSuite {
    public static RepeatTests defaultRepeat(String serverName) {
        return MicroProfileActions.repeat(serverName,
            MicroProfileActions.MP61, // mpOpenAPI-3.1, LITE
            MicroProfileActions.MP50, // mpOpenAPI-3.0, FULL
            MicroProfileActions.MP41, // mpOpenAPI-2.0, FULL
            MicroProfileActions.MP33, // mpOpenAPI-1.1, FULL
            MicroProfileActions.MP22);// mpOpenAPI-1.0, FULL
    }

    static {
        /*
         * Set property to allow the use of the 'Origin' header in CORS tests This
         * property is read one time only when the 'HttpURLConnection' class is first
         * instantiated and cannot be changed. Setting this property before the tests
         * run ensures that the value is not defaulted to 'False' when non-CORS tests
         * run
         */
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }
}
