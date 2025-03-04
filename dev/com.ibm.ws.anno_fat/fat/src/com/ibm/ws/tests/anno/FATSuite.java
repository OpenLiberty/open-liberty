/*******************************************************************************
 * Copyright (c) 2012,2025 IBM Corporation and others.
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
package com.ibm.ws.tests.anno;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.tests.anno.caching.AppReplaceTest;
import com.ibm.ws.tests.anno.caching.CacheEnablementTest;
import com.ibm.ws.tests.anno.caching.FragmentOrderTest;
import com.ibm.ws.tests.anno.caching.LooseConfigTest;
import com.ibm.ws.tests.anno.caching.MetadataCompleteTest;
import com.ibm.ws.tests.anno.caching.MetadataIncompleteTest;
import com.ibm.ws.tests.anno.jandex.JandexAppDefaultAppMgrDefaultTest;
import com.ibm.ws.tests.anno.jandex.JandexAppDefaultAppMgrTrueTest;
import com.ibm.ws.tests.anno.jandex.JandexAppFalseAppMgrFalseTest;
import com.ibm.ws.tests.anno.jandex.JandexAppFalseAppMgrTrueTest;
import com.ibm.ws.tests.anno.jandex.JandexAppTrueAppMgrFalseTest;
import com.ibm.ws.tests.anno.jandex.JandexAppTrueAppMgrTrueTest;

/**
 * Servlet 4.0 Tests
 *
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
    // Jandex tests
    JandexAppDefaultAppMgrDefaultTest.class,
    JandexAppDefaultAppMgrTrueTest.class,
    JandexAppFalseAppMgrFalseTest.class,
    JandexAppFalseAppMgrTrueTest.class,
    JandexAppTrueAppMgrFalseTest.class,
    JandexAppTrueAppMgrTrueTest.class,

    // Annotation caching tests
    CacheEnablementTest.class,
    MetadataCompleteTest.class,
    MetadataIncompleteTest.class,
    FragmentOrderTest.class,
    LooseConfigTest.class,
    AppReplaceTest.class
    // BigAppTest.class
})

public class FATSuite {
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }
}
