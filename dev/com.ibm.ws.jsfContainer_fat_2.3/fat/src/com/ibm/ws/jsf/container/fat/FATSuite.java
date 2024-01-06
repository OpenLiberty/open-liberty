/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat;

import java.io.File;
import java.util.Locale;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.jsf.container.fat.tests.CDIFlowsTests;
import com.ibm.ws.jsf.container.fat.tests.ClassloadingTest;
import com.ibm.ws.jsf.container.fat.tests.ErrorPathsTest;
import com.ibm.ws.jsf.container.fat.tests.JSF22BeanValidationTests;
import com.ibm.ws.jsf.container.fat.tests.JSF22FlowsTests;
import com.ibm.ws.jsf.container.fat.tests.JSF22StatelessViewTests;
import com.ibm.ws.jsf.container.fat.tests.JSF23CDIGeneralTests;
import com.ibm.ws.jsf.container.fat.tests.JSF23WebSocketTests;
import com.ibm.ws.jsf.container.fat.tests.JSFContainerTest;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;

@RunWith(Suite.class)
@SuiteClasses({
                JSF22FlowsTests.class,
                CDIFlowsTests.class,
                JSFContainerTest.class,
                JSF22StatelessViewTests.class,
                JSF22BeanValidationTests.class,
                ErrorPathsTest.class,
                ClassloadingTest.class,
                JSF23CDIGeneralTests.class,
                JSF23WebSocketTests.class
})

public class FATSuite {

    @ClassRule
    public static RepeatTests repeat;

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    static {
        if (JavaInfo.JAVA_VERSION >= 11) {
            // Repeating the full FAT for multiple features may exceed the 3 hour limit on Fyre Windows.
            // Skip the EE9 repeat on the windows platform when not running locally.
            if (isWindows && !FATRunner.FAT_TEST_LOCALRUN) {
                repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                                .andWith(FeatureReplacementAction.EE10_FEATURES());
            } else {
                repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                                .andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly())
                                .andWith(FeatureReplacementAction.EE10_FEATURES());
            }

        } else {
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE9_FEATURES());
        }
    }

    public static final String MOJARRA_API_IMP = "publish/files/mojarra/javax.faces-2.3.9.jar";
    public static final String MOJARRA_API_IMP_40 = "publish/files/mojarra40/jakarta.faces-4.0.0.jar";
    public static final String MYFACES_API = "publish/files/myfaces/myfaces-api-2.3.10.jar";
    public static final String MYFACES_IMP = "publish/files/myfaces/myfaces-impl-2.3.10.jar";
    public static final String MYFACES_IMP_40 = "publish/files/myfaces40//myfaces-impl-4.0.0-RC4.jar";
    // For ErrorPathsTest#testBadImplVersion_MyFaces Test (apps need the correct api since the tests checks for a bad implementation)
    public static final String MYFACES_API_30 = "publish/files/myfaces30/myfaces-api-3.0.2.jar";
    public static final String MYFACES_API_40 = "publish/files/myfaces40/myfaces-api-4.0.0-RC4.jar";

    public static WebArchive addMojarra(WebArchive app) throws Exception {
        if (JakartaEEAction.isEE10OrLaterActive()) {
            return app.addAsLibraries(new File("publish/files/mojarra40/").listFiles());
        } else if (JakartaEEAction.isEE9Active()) {
            return app.addAsLibraries(new File("publish/files/mojarra30/").listFiles());
        }
        return app.addAsLibraries(new File("publish/files/mojarra/").listFiles());
    }

    public static WebArchive addMyFaces(WebArchive app) throws Exception {
        if (JakartaEEAction.isEE10OrLaterActive()) {
            return app.addAsLibraries(new File("publish/files/myfaces40/").listFiles());
        } else if (JakartaEEAction.isEE9Active()) {
            return app.addAsLibraries(new File("publish/files/myfaces30/").listFiles()).addAsLibraries(new File("publish/files/myfaces-libs/").listFiles());
        }
        return app.addAsLibraries(new File("publish/files/myfaces/").listFiles()).addAsLibraries(new File("publish/files/myfaces-libs/").listFiles());
    }

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
