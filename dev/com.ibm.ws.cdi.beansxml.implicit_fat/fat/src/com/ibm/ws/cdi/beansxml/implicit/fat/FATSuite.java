/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
package com.ibm.ws.cdi.beansxml.implicit.fat;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchive.ImplicitBeanArchiveTest;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchivesDisabled.ImplicitBeanArchivesDisabledTest;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitEJB.ImplicitEJBTest;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitWar.ImplicitWarTest;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitWarLibJars.ImplicitWarLibJarsTest;
import com.ibm.ws.cdi.beansxml.implicit.fat.noAnnotations.ImplicitBeanArchiveNoAnnotationsTest;
import com.ibm.ws.fat.util.FatLogHandler;

/**
 * Tests specific to cdi-1.2
 */
@RunWith(Suite.class)
@SuiteClasses({
                ImplicitBeanArchiveNoAnnotationsTest.class,
                ImplicitBeanArchivesDisabledTest.class,
                ImplicitEJBTest.class,
                ImplicitBeanArchiveTest.class,
                ImplicitWarLibJarsTest.class,
                ImplicitWarTest.class,
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
