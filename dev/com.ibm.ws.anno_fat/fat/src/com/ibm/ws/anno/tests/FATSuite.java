/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.anno.tests;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.anno.tests.caching.AbsoluteOrderingTest;
import com.ibm.ws.anno.tests.caching.BasicAnnoCacheUsageTest;
import com.ibm.ws.anno.tests.caching.BigAppTest;
import com.ibm.ws.anno.tests.caching.LooseConfigTest;
import com.ibm.ws.anno.tests.caching.MetadataCompleteTest;
import com.ibm.ws.anno.tests.caching.MetadataCompleteMissingServletsTest;
import com.ibm.ws.anno.tests.jandex.JandexAppDefaultAppMgrDefaultTest;
import com.ibm.ws.anno.tests.jandex.JandexAppDefaultAppMgrTrueTest;
import com.ibm.ws.anno.tests.jandex.JandexAppFalseAppMgrFalseTest;
import com.ibm.ws.anno.tests.jandex.JandexAppFalseAppMgrTrueTest;
import com.ibm.ws.anno.tests.jandex.JandexAppTrueAppMgrFalseTest;
import com.ibm.ws.anno.tests.jandex.JandexAppTrueAppMgrTrueTest;
import com.ibm.ws.fat.util.FatLogHandler;

/**
 * Make sure to add any new test classes to the @SuiteClasses
 * annotation.
 *
 * Make sure to distinguish full mode tests using
 * <code>@Mode(TestMode.FULL)</code>. Tests default to
 * use lite mode (<code>@Mode(TestMode.LITE)</code>).
 *
 * By default only lite mode tests are run. To also run
 * full mode tests a property must be specified to ant:
 *
 * Select the target build file (usually "build-test.xml").
 * Right click and chose "Run As>Ant Build…". Add
 * "fat.test.mode=full" to the properties tab, then launch the
 * build.
 *
 * Alternatively, for a command line launch, add "-Dfat.test.mode=full".
 */
@RunWith(Suite.class)
@SuiteClasses({
    // BigAppTest.class, // This test can take 10 min and is disabled for regular builds.

    //  Annotation caching tests ...
    BasicAnnoCacheUsageTest.class,
    AbsoluteOrderingTest.class,
    LooseConfigTest.class,
    MetadataCompleteTest.class,
    MetadataCompleteMissingServletsTest.class,

    // Jandex tests ...
    JandexAppDefaultAppMgrDefaultTest.class,
    JandexAppDefaultAppMgrTrueTest.class,
    JandexAppFalseAppMgrFalseTest.class,
    JandexAppFalseAppMgrTrueTest.class,
    JandexAppTrueAppMgrFalseTest.class,
    JandexAppTrueAppMgrTrueTest.class
})

public class FATSuite {
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }
}
