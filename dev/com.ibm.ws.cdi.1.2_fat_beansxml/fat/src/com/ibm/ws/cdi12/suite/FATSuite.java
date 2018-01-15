/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.suite;

import java.nio.file.Files; 
import java.nio.file.StandardCopyOption; 
import java.nio.file.attribute.FileAttribute; 
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import com.ibm.ws.cdi12.fat.tests.AfterTypeDiscoveryTest;
import com.ibm.ws.cdi12.fat.tests.BeanDiscoveryModeNoneTest;
import com.ibm.ws.cdi12.fat.tests.ClassExclusionTest;
import com.ibm.ws.cdi12.fat.tests.DisablingBeansXmlValidationTest;
import com.ibm.ws.cdi12.fat.tests.EmptyCDITest;
import com.ibm.ws.cdi12.fat.tests.EnablingBeansXmlValidationTest;
import com.ibm.ws.cdi12.fat.tests.MultipleBeansXmlTest;
import com.ibm.ws.cdi12.fat.tests.WebBeansBeansXmlInWeldTest;
import com.ibm.ws.cdi12.fat.tests.implicit.ImplicitBeanArchiveNoAnnotationsTest; 
import com.ibm.ws.cdi12.fat.tests.implicit.ImplicitBeanArchiveTest; 
import com.ibm.ws.cdi12.fat.tests.implicit.ImplicitBeanArchivesDisabledTest; 
import com.ibm.ws.cdi12.fat.tests.implicit.ImplicitEJBTest;
import com.ibm.ws.cdi12.fat.tests.implicit.ImplicitWarLibJarsTest; 
import com.ibm.ws.cdi12.fat.tests.implicit.ImplicitWarTest; 
import com.ibm.ws.fat.util.FatLogHandler;

/**
 * Tests specific to cdi-1.2
 */
@RunWith(Suite.class)
@SuiteClasses({
                AfterTypeDiscoveryTest.class,
                BeanDiscoveryModeNoneTest.class, 
                ClassExclusionTest.class,
                DisablingBeansXmlValidationTest.class,
                EmptyCDITest.class,
                EnablingBeansXmlValidationTest.class,
                MultipleBeansXmlTest.class,
                WebBeansBeansXmlInWeldTest.class,
                ImplicitBeanArchiveNoAnnotationsTest.class,
                ImplicitBeanArchivesDisabledTest.class,
                ImplicitEJBTest.class,
                ImplicitBeanArchiveTest.class,
                ImplicitWarLibJarsTest.class,
                ImplicitWarTest.class,
})
public class FATSuite {
    
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE8_FEATURES());

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
