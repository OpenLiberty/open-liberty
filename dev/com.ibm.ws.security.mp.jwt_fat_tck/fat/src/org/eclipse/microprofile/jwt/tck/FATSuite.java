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

import org.eclipse.microprofile.jwt.tck.container.jaxrs.ClaimValueInjectionTest;
import org.eclipse.microprofile.jwt.tck.container.jaxrs.InvalidTokenTest;
import org.eclipse.microprofile.jwt.tck.container.jaxrs.JsonValueInjectionTest;
import org.eclipse.microprofile.jwt.tck.container.jaxrs.ProviderInjectionTest;
import org.eclipse.microprofile.jwt.tck.container.jaxrs.RolesAllowedTest;
import org.eclipse.microprofile.jwt.tck.container.jaxrs.UnsecuredPingTest;
import org.eclipse.microprofile.jwt.tck.util.TokenUtilsTest;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

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

    @BeforeClass
    public static void setUp() throws Exception {
        // use our cxf jax-rs thin client
        System.setProperty("javax.ws.rs.client.ClientBuilder", "org.apache.cxf.jaxrs.client.spec.ClientBuilderImpl");
    }

}
