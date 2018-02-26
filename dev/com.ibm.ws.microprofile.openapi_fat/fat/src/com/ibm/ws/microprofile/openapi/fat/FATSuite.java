/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestFive;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestFour;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestOne;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestSix;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestThree;
import com.ibm.ws.microprofile.openapi.validation.fat.OpenAPIValidationTestTwo;

@RunWith(Suite.class)
@SuiteClasses({
                ApplicationProcessorTest.class,
                OpenAPIValidationTestOne.class,
                OpenAPIValidationTestTwo.class,
                OpenAPIValidationTestThree.class,
                OpenAPIValidationTestFour.class,
                OpenAPIValidationTestFive.class,
                OpenAPIValidationTestSix.class,
                ProxySupportTest.class,
                EndpointAvailabilityTest.class,
                UICustomizationTest.class
})
public class FATSuite {}
