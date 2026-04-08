/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.internal.tests;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.authorization.internal.tests.factories.AbstractClassPolicyConfigFactory;
import io.openliberty.security.authorization.internal.tests.factories.AbstractClassPolicyFactory;
import io.openliberty.security.authorization.internal.tests.factories.BothExpectedCtorsPolicyConfigFactory;
import io.openliberty.security.authorization.internal.tests.factories.BothExpectedCtorsPolicyFactory;
import io.openliberty.security.authorization.internal.tests.factories.DecorateCtorOnlyPolicyConfigFactory;
import io.openliberty.security.authorization.internal.tests.factories.DecorateCtorOnlyPolicyFactory;
import io.openliberty.security.authorization.internal.tests.factories.DefaultCtorOnlyPolicyConfigFactory;
import io.openliberty.security.authorization.internal.tests.factories.DefaultCtorOnlyPolicyFactory;
import io.openliberty.security.authorization.internal.tests.factories.ExceptionCtorPolicyConfigFactory;
import io.openliberty.security.authorization.internal.tests.factories.ExceptionCtorPolicyFactory;
import io.openliberty.security.authorization.internal.tests.factories.NoPublicCtorPolicyConfigFactory;
import io.openliberty.security.authorization.internal.tests.factories.NoPublicCtorPolicyFactory;
import io.openliberty.security.authorization.internal.tests.factories.NotExtendPolicyConfigFactory;
import io.openliberty.security.authorization.internal.tests.factories.NotExtendPolicyFactory;
import io.openliberty.security.authorization.internal.tests.factories.WrongCtorPolicyConfigFactory;
import io.openliberty.security.authorization.internal.tests.factories.WrongCtorPolicyFactory;

/**
 * This test validates that the correct info and error messages as well as FFDCs come out when
 * creating PolicyConfigurationFactory and PolicyFactory objects defined in web.xml for applications.
 */
@RunWith(FATRunner.class)
public class FactoryMessagesTests {

    @Server("FactoryMessagesServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    private void testRunner(String appName, String exceptionText, String policyConfigClass, String policyClass) throws Exception {
        RemoteFile messagesLog = server.getDefaultLogFile();
        server.setMarkToEndOfLog(messagesLog);
        WebArchive war = ShrinkWrap.create(WebArchive.class, appName + ".war");
        war.addPackages(true, DecorateCtorOnlyPolicyConfigFactory.class.getPackage());
        File webXMLFile = new File("lib/LibertyFATTestFiles/", appName + "-web.xml");
        if (!webXMLFile.exists()) {
            throw new FileNotFoundException("expected " + appName + "-web.xml file not found");
        }
        war.setWebXML(webXMLFile);

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
        assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0001I: .*" + appName, 30000, messagesLog));
        try {
            if (exceptionText != null) {
                assertNotNull(server.waitForStringInLogUsingMark("CWWKS2865E.*PolicyConfigurationFactory.*" + policyConfigClass + ".*" + appName + ".*" + exceptionText,
                                                                 30000, messagesLog));
                assertNotNull(server.waitForStringInLogUsingMark("CWWKS2865E.*PolicyFactory.*" + policyClass + ".*" + appName + ".*" + exceptionText, 30000, messagesLog));
            } else {
                assertNotNull(server.waitForStringInLogUsingMark("CWWKS2866I.*PolicyConfigurationFactory.*" + policyConfigClass + ".*" + appName, 30000,
                                                                 messagesLog));
                assertNotNull(server.waitForStringInLogUsingMark("CWWKS2866I.*PolicyFactory.*" + policyClass + ".*" + appName, 30000, messagesLog));
            }
            server.setMarkToEndOfLog(messagesLog);
        } finally {
            server.removeDropinsApplications(appName + ".war");
        }
        if (exceptionText == null) {
            assertNotNull(server.waitForStringInLogUsingMark("CWWKS2867I.*PolicyConfigurationFactory.*" + policyConfigClass + ".*" + appName, 30000,
                                                             messagesLog));
            assertNotNull(server.waitForStringInLogUsingMark("CWWKS2867I.*PolicyFactory.*" + policyClass + ".*" + appName, 30000, messagesLog));
        }
    }

    /**
     * This test validates the error when the defined security module classes are abstract
     */
    @Test
    @ExpectedFFDC(value = "java.lang.InstantiationException")
    public void testAbstractClass() throws Exception {
        testRunner("abstractclass", InstantiationException.class.getName(), AbstractClassPolicyConfigFactory.class.getName(), AbstractClassPolicyFactory.class.getName());
    }

    /**
     * This test validates the error when the defined security module classes do not exist
     */
    @Test
    @ExpectedFFDC(value = "java.lang.ClassNotFoundException")
    public void testClassNotFound() throws Exception {
        testRunner("classnotfound", ClassNotFoundException.class.getName(), "not.found.MyPolicyConfigurationFactory", "not.found.MyPolicyFactory");
    }

    /**
     * This test validates the error when the defined security module constructor throws an exception
     */
    @Test
    @ExpectedFFDC(value = "java.lang.reflect.InvocationTargetException")
    public void testExceptionThrownInConstuctor() throws Exception {
        testRunner("exceptionctor", InvocationTargetException.class.getName(), ExceptionCtorPolicyConfigFactory.class.getName(), ExceptionCtorPolicyFactory.class.getName());
    }

    /**
     * This test validates the error when the defined security module classes are not public
     */
    @Test
    @ExpectedFFDC(value = "java.lang.IllegalAccessException")
    public void testNonPublicClass() throws Exception {
        testRunner("nonpublicclass", IllegalAccessException.class.getName(), "io.openliberty.security.authorization.internal.tests.factories.NonPublicClassPolicyConfigFactory",
                   "io.openliberty.security.authorization.internal.tests.factories.NonPublicClassPolicyFactory");
    }

    /**
     * This test validates the error when the defined security module classes do not have any public constructors
     */
    @Test
    @ExpectedFFDC(value = "java.lang.IllegalAccessException")
    public void testNoPublicConstructor() throws Exception {
        testRunner("nopublicctor", IllegalAccessException.class.getName(), NoPublicCtorPolicyConfigFactory.class.getName(), NoPublicCtorPolicyFactory.class.getName());
    }

    /**
     * This test validates the error when the defined security module classes do not extends PolicyConfigurationFactory and PolicyFactory
     */
    @Test
    @ExpectedFFDC(value = "java.lang.ClassCastException")
    public void testNotExtendSuperClass() throws Exception {
        testRunner("notextend", ClassCastException.class.getName(), NotExtendPolicyConfigFactory.class.getName(), NotExtendPolicyFactory.class.getName());
    }

    /**
     * Test test validates what happens if there are security modules that do not have the expected constructor signatures
     */
    @Test
    @ExpectedFFDC(value = "java.lang.IllegalArgumentException")
    public void testWrongConstructorArguments() throws Exception {
        testRunner("wrongctor", IllegalArgumentException.class.getName(), WrongCtorPolicyConfigFactory.class.getName(), WrongCtorPolicyFactory.class.getName());
    }

    /**
     * This test validates the error when the defined security module classes extend the wrong class type and only default constructor is defined
     */
    @Test
    @ExpectedFFDC(value = "java.lang.ClassCastException")
    public void testWrongSuperClass() throws Exception {
        testRunner("wrongtype", ClassCastException.class.getName(), DefaultCtorOnlyPolicyFactory.class.getName(), DefaultCtorOnlyPolicyConfigFactory.class.getName());
    }

    /**
     * This test validates the error when the defined security module classes extend the wrong class type and both default constructor and
     * decorate constructor are defined. The default constructor will still be called because the single argument signature has the wrong class type.
     */
    @Test
    @ExpectedFFDC(value = "java.lang.ClassCastException")
    public void testWrongSuperClass2() throws Exception {
        testRunner("wrongtype2", ClassCastException.class.getName(), BothExpectedCtorsPolicyFactory.class.getName(), BothExpectedCtorsPolicyConfigFactory.class.getName());
    }

    /**
     * This test validates the error when the defined security module classes extend the wrong class type and only the
     * decorate constructor is defined. The liberty code will end up not finding any constructors and get a different exception
     */
    @Test
    @ExpectedFFDC(value = "java.lang.IllegalArgumentException")
    public void testWrongSuperClass3() throws Exception {
        testRunner("wrongtype3", IllegalArgumentException.class.getName(), DecorateCtorOnlyPolicyFactory.class.getName(), DecorateCtorOnlyPolicyConfigFactory.class.getName());
    }

    @Test
    public void testBothCtors() throws Exception {
        testRunner("bothctors", null, BothExpectedCtorsPolicyConfigFactory.class.getName(), BothExpectedCtorsPolicyFactory.class.getName());
    }

    @Test
    public void testDecoratorCtor() throws Exception {
        testRunner("decoratorctor", null, DecorateCtorOnlyPolicyConfigFactory.class.getName(), DecorateCtorOnlyPolicyFactory.class.getName());
    }

    @Test
    public void testDefaultCtor() throws Exception {
        testRunner("defaultctor", null, DefaultCtorOnlyPolicyConfigFactory.class.getName(), DefaultCtorOnlyPolicyFactory.class.getName());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKS2865E");
    }
}
