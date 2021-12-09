/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.fat.tests;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;

/**
 * Tests specific to cdi-1.2
 */
@RunWith(Suite.class)
@SuiteClasses({
                AfterTypeDiscoveryTest.class,
                BeanDiscoveryModeNoneTest.class,
                ClassExclusionTest.class,
                CustomerProvidedXMLParserFactoryTest.class,
                DisablingBeansXmlValidationTest.class,
                EmptyCDITest.class,
                EnablingBeansXmlValidationTest.class,
                EnablingBeansXmlValidationTestEE7.class,
                MultipleBeansXmlTest.class,
                WebBeansBeansXmlInWeldTest.class,
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
