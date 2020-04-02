/*
 * =============================================================================
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.ws.transport.iiop.open_fat;

import com.ibm.websphere.simplicity.ShrinkHelper;
import junit.framework.AssertionFailedError;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(Suite.class)
@SuiteClasses({
                IORTest.class,
})
public class FATSuite {

    private static final JavaArchive INTERFACES_JAR;
    private static final JavaArchive TEST_CORBA_EJB_JAR;
    private static final EnterpriseArchive TEST_CORBA_EAR;
    private static final WebArchive TEST_CORBA_WEB_WAR;

    static {
        try {
            INTERFACES_JAR = ShrinkHelper.buildJavaArchive("Interfaces.jar", "shared");
            System.out.println(INTERFACES_JAR);

            TEST_CORBA_EJB_JAR = ShrinkHelper.buildJavaArchive("TestCorbaEjb.jar", "ejb");
            ShrinkHelper.addDirectory(TEST_CORBA_EJB_JAR, "test-applications/TestCorbaEjb.jar/resources");
            System.out.println(TEST_CORBA_EJB_JAR);

            TEST_CORBA_EAR = ShrinkWrap.create(EnterpriseArchive.class, "TestCorba.ear");
            TEST_CORBA_EAR.addAsModule(TEST_CORBA_EJB_JAR);
            TEST_CORBA_EAR.addAsLibraries(INTERFACES_JAR);
            ShrinkHelper.addDirectory(TEST_CORBA_EJB_JAR, "test-applications/TestCorba.ear/resources");

            TEST_CORBA_WEB_WAR = ShrinkHelper.buildDefaultApp("TestCorbaWeb.war", "servlet");
            TEST_CORBA_WEB_WAR.addAsLibrary(INTERFACES_JAR);
            ShrinkHelper.addDirectory(TEST_CORBA_WEB_WAR, "test-applications/TestCorbaWeb.war/resources");
        } catch (Exception e) {
            throw (AssertionFailedError)new AssertionFailedError("Could not assemble test applications").initCause(e);
        }
    }

    static final List<? extends Archive<?>> SERVER_APPS = Collections.unmodifiableList(Arrays.asList(TEST_CORBA_WEB_WAR, TEST_CORBA_EAR));
}