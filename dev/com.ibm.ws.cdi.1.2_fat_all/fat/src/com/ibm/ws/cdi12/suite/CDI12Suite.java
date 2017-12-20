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

import org.junit.BeforeClass;
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

import com.ibm.ws.cdi12.fat.tests.EJB32Test;
import com.ibm.ws.fat.util.FatLogHandler;

/**
 * Tests that do not yet work on CDI-2.0
 */
@RunWith(Suite.class)
@SuiteClasses({

                EJB32Test.class,  //[ERROR   ] CWWKZ0002E: An exception occurred while starting the application ejbMisc. The exception message was: com.ibm.ws.container.service.state.StateChangeException: org.jboss.weld.exceptions.DefinitionException: WELD-000088: Observer method must be static or local business method:  [EnhancedAnnotatedMethodImpl] public com.ibm.ws.cdi12test.remoteEjb.ejb.TestObserver.observeRemote(@Observes EJBEvent) 	at com.ibm.ws.cdi12test.remoteEjb.ejb.TestObserver.observeRemote(TestObserver.java:0)

// This error might be a legitimtae change in the spec, and that will mean the fix is note this as an aloud error. However I think the error is thrown as part of the server.xmls being refreshed and the test framework might or might not associate it with EJB32Test. If allowing the error doesn't appear to fix it, consider moving EJB32Test to use LibertyServer.
               

})
public class CDI12Suite {
    
    private static final Logger LOG = Logger.getLogger(FATSuite.class.getName());
    

}
