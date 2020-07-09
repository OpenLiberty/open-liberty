/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.cdi12.fat.tests.CDI12BasicTests;
import com.ibm.ws.cdi12.fat.tests.ClassMaskingTest;
import com.ibm.ws.cdi12.fat.tests.JarInRarTest;
import com.ibm.ws.cdi12.fat.tests.PackagePrivateAccessTest;
import com.ibm.ws.cdi12.fat.tests.RootClassLoaderTest;
import com.ibm.ws.cdi12.fat.tests.SharedLibraryTest;
import com.ibm.ws.cdi12.fat.tests.ValidatorInJarTest;
import com.ibm.ws.cdi12.fat.tests.VisTest;
import com.ibm.ws.cdi12.fat.tests.WarLibsAccessWarBeansTest;

/**
 * Tests specific to cdi-1.2
 */
@RunWith(Suite.class)
@SuiteClasses({
                CDI12BasicTests.class, //basic, lite, EE9, EE7
                ClassMaskingTest.class, //ejbLite, full, EE8
                JarInRarTest.class, //ejb, full, EE7
                PackagePrivateAccessTest.class,
                RootClassLoaderTest.class,
                SharedLibraryTest.class,
                ValidatorInJarTest.class,
                WarLibsAccessWarBeansTest.class,
                VisTest.class
})

public class FATSuite {

//    /**
//     * @see {@link FatLogHandler#generateHelpFile()}
//     */
//    @BeforeClass
//    public static void generateHelpFile() {
//        FatLogHandler.generateHelpFile();
//    }

}
