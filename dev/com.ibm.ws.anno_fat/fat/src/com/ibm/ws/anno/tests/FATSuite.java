/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012, 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.tests;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.anno.tests.caching.AbsoluteOrderingTest;
import com.ibm.ws.anno.tests.caching.BasicAnnoCacheUsageTest;

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
                
              	BasicAnnoCacheUsageTest.class,
              	AbsoluteOrderingTest.class,
              	MetadataCompleteTest.class,
              	MetadataCompleteMissingServletsTest.class,
                JandexAppDefaultAppMgrDefaultTest.class,
                JandexAppDefaultAppMgrTrueTest.class,
                JandexAppFalseAppMgrFalseTest.class,
                JandexAppFalseAppMgrTrueTest.class,
                JandexAppTrueAppMgrFalseTest.class,
                JandexAppTrueAppMgrTrueTest.class
})

public class FATSuite {

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }
}
