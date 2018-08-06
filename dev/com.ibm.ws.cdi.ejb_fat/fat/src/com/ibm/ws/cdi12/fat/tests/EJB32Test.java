/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;

/**
 * This test requires the ejb-3.2 feature. I started with it merged into EjbTimerTest, but
 * that test depends on ejbLite-3.2, and/or there's something funny about the way it uses
 * SHARED_SERVER... either way, EjbTimerTest hard to add new tests to.
 */
// Skip this class for EE8 features (cdi-2.0) because Weld tightened up its EJB checks and we get the following error:
// WELD-000088: Observer method must be static or local business method:  [EnhancedAnnotatedMethodImpl] public com.ibm.ws.cdi12test.remoteEjb.ejb.TestObserver.observeRemote(@Observes EJBEvent)
@SkipForRepeat(SkipForRepeat.EE8_FEATURES)
@RunWith(FATRunner.class)
public class EJB32Test extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12EJB32FullServer", EJB32Test.class);

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
        return ShrinkWrap.create(WebArchive.class,
                                 "ejbMisc.war").addClass("com.ibm.ws.cdi12test.remoteEjb.web.AServlet").addClass("com.ibm.ws.cdi12test.remoteEjb.ejb.TestObserver").addClass("com.ibm.ws.cdi12test.remoteEjb.api.RemoteInterface").addClass("com.ibm.ws.cdi12test.remoteEjb.api.EJBEvent").add(new FileAsset(new File("test-applications/ejbMisc.war/resources/WEB-INF/beans.xml")),
                                                                                                                                                                                                                                                                                               "/WEB-INF/beans.xml");
    }

    @Test
    public void testRemoteEJBsWorkWithCDI() throws Exception {
        verifyResponse("/ejbMisc/AServlet", "observed=true");
    }

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }
}
