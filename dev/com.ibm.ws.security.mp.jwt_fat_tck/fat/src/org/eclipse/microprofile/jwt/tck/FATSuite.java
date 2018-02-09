/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.microprofile.jwt.tck;

import java.util.HashSet;

import org.eclipse.microprofile.jwt.tck.container.jaxrs.ClaimValueInjectionTest;
import org.eclipse.microprofile.jwt.tck.container.jaxrs.InvalidTokenTest;
import org.eclipse.microprofile.jwt.tck.container.jaxrs.JsonValueInjectionTest;
import org.eclipse.microprofile.jwt.tck.container.jaxrs.ProviderInjectionTest;
import org.eclipse.microprofile.jwt.tck.container.jaxrs.RolesAllowedTest;
import org.eclipse.microprofile.jwt.tck.container.jaxrs.UnsecuredPingTest;
import org.eclipse.microprofile.jwt.tck.util.TokenUtilsTest;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                TokenUtilsTest.class,
                UnsecuredPingTest.class,
                JsonValueInjectionTest.class,
                ProviderInjectionTest.class,
                ClaimValueInjectionTest.class,
                InvalidTokenTest.class,
                RolesAllowedTest.class
})
public class FATSuite {

    // our default config pulls in cdi 1.2, but we need to check 2.0.
    // here's an easy way to do that.
    static HashSet<String> addfeatures = new HashSet<String>();
    static HashSet<String> removefeatures = new HashSet<String>();
    static {
        addfeatures.add("cdi-2.0");
        addfeatures.add("jaxrs-2.1");
        removefeatures.add("jaxrs-2.0");

    }

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction(removefeatures, addfeatures));

    @BeforeClass
    public static void setUp() throws Exception {
        // use our cxf jax-rs thin client
        //System.setProperty("javax.ws.rs.client.ClientBuilder", "org.apache.cxf.jaxrs.client.spec.ClientBuilderImpl");
    }

}
