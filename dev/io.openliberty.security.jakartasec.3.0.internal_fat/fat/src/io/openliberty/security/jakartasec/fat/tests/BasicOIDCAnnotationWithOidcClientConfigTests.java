/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.tests;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.sharedTests.BasicOIDCAnnotationTests;

/**
 * Tests appSecurity-5.0
 */
@RunWith(FATRunner.class)
public class BasicOIDCAnnotationWithOidcClientConfigTests extends BasicOIDCAnnotationTests {

    protected static Class<?> thisClass = BasicOIDCAnnotationWithOidcClientConfigTests.class;

    @Server("jakartasec-3.0_fat." + withOidcClientConfig + ".jwt.rp")
    public static LibertyServer rpJwtServer;
    @Server("jakartasec-3.0_fat." + withOidcClientConfig + ".opaque.rp")
    public static LibertyServer rpOpaqueServer;

    @ClassRule
    public static RepeatTests r = createTokenTypeRepeats(withOidcClientConfig);

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(thisClass, "setUp", "starting setup");

        baseSetup(rpJwtServer, rpOpaqueServer);

    }

}
