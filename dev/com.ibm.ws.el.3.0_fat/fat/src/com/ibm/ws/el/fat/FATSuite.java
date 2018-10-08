/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.el.fat;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.el.fat.tests.ELDummyTest;
import com.ibm.ws.el.fat.tests.ELLambdaExpHttpUnit;
import com.ibm.ws.el.fat.tests.ELServerHttpUnit;
import com.ibm.ws.el.fat.tests.ELServerTest;
import com.ibm.ws.fat.util.FatLogHandler;

/**
 * EL 3.0 Tests
 *
 * The tests for both features should be included in this test component.
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
 *
 * For additional information see:
 *
 * http://was.pok.ibm.com/xwiki/bin/view/Liberty/Test-FAT
 */
@RunWith(Suite.class)
@SuiteClasses({
                ELDummyTest.class,
                ELServerHttpUnit.class,
                ELServerTest.class,
                ELLambdaExpHttpUnit.class
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
