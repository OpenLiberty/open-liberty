/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jaxrs21.fat.security.annotations.JAXRS21SecurityAnnotationsTest;
import com.ibm.ws.jaxrs21.fat.security.annotations.JAXRS21SecurityAnnotationsTestRolesAsGroups;
import com.ibm.ws.jaxrs21.fat.security.ssl.JAXRS21SecuritySSLTest;
import com.ibm.ws.jaxrs21.fat.securitycontext.JAXRS21SecurityContextTest;
import com.ibm.ws.jaxrs21.fat.uriInfo.UriInfoTest;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                JAXRS21SecurityAnnotationsTest.class,
                JAXRS21SecurityAnnotationsTestRolesAsGroups.class,
                JAXRS21SecurityContextTest.class,
                JAXRS21SecuritySSLTest.class,
                UriInfoTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
        .andWith(new FeatureReplacementAction("jaxrs-2.1", "jaxrs-3.0").withID("RESTEasy"));
}
