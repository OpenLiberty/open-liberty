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
package com.ibm.ws.fat.wc;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.fat.wc.tests.WCContextRootPrecedence;
import com.ibm.ws.fat.wc.tests.WCDummyTest;
import com.ibm.ws.fat.wc.tests.WCEncodingTest;
import com.ibm.ws.fat.wc.tests.WCGetMappingTest;
import com.ibm.ws.fat.wc.tests.WCPushBuilderTest;
import com.ibm.ws.fat.wc.tests.WCServerTest;
import com.ibm.ws.fat.wc.tests.WCServletClarificationTest;
import com.ibm.ws.fat.wc.tests.WCTrailersTest;
import com.ibm.ws.fat.wc.tests.WCAddJspFileTest;

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
 * Alternatively, for a comand line launch, add "-Dfat.test.mode=full".
 *
 * For additional information see:
 *
 * http://was.pok.ibm.com/xwiki/bin/view/Liberty/Test-FAT
 */
@RunWith(Suite.class)
@SuiteClasses({
               WCDummyTest.class,
                WCServerTest.class,
                WCPushBuilderTest.class,
                WCServletClarificationTest.class,
                WCContextRootPrecedence.class,
                WCGetMappingTest.class,
                WCEncodingTest.class,
                WCTrailersTest.class,
                WCAddJspFileTest.class
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
