/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs2x.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jaxrs2x.fat.helloworld.HelloWorldTest;
import com.ibm.ws.jaxrs2x.fat.jsonp.JaxRsJsonPTest;
import com.ibm.ws.jaxrs2x.fat.multipart.MultipartTest;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
               AlwaysPassesTest.class,
               HelloWorldTest.class,
               MultipartTest.class,
               JaxRsJsonPTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE8_FEATURES().withID("JAXRS-2.1"));

}
