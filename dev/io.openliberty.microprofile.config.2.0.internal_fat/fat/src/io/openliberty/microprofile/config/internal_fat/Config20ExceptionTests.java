/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.ValidConverter;

@RunWith(FATRunner.class)
public class Config20ExceptionTests extends FATServletClient {

    public static final String BAD_OBSERVER_APP_NAME = "badObserverApp";
    public static final String BROKEN_INJECTION_APP_NAME = "brokenInjectionApp";

    public static final String SERVER_NAME = "Config20ExceptionServer";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.LATEST);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive badObserverWar = ShrinkWrap.create(WebArchive.class, BAD_OBSERVER_APP_NAME + ".war")
                        .addPackages(true, "io.openliberty.microprofile.config.internal_fat.apps.badobserver");

        WebArchive brokenInjectionWar = ShrinkWrap.create(WebArchive.class, BROKEN_INJECTION_APP_NAME + ".war")
                        .addPackages(true, "io.openliberty.microprofile.config.internal_fat.apps.brokenInjection")
                        .addAsServiceProvider(Converter.class, ValidConverter.class);

        // The 2 wars should throw deployment exceptions, hence don't validate.
        ShrinkHelper.exportDropinAppToServer(server, badObserverWar, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);
        ShrinkHelper.exportDropinAppToServer(server, brokenInjectionWar, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);

        server.startServer();

    }

    @Test
    public void testBadObserver() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("SRCFG02000: No Config Value exists for required property DOESNOTEXIST");
        assertNotNull("error not found", errors);
        assertTrue("error not found: " + errors.size(), errors.size() > 0);
    }

    @Test
    public void testMethodUnnamed() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("SRCFG02002: Could not find default name for .*io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.ConfigUnnamedMethodInjectionBean.*setSimpleKey6");
        assertNotNull("error not found", errors);
        assertTrue("error not found: " + errors.size(), errors.size() > 0);
    }

    @Test
    public void testConstructorUnnamed() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("SRCFG02002: Could not find default name for .*io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.ConfigUnnamedConstructorInjectionBean");
        assertNotNull("error not found", errors);
        assertTrue("error not found: " + errors.size(), errors.size() > 0);
    }

    @Test
    public void testNonExistantKey() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("SRCFG02000: No Config Value exists for required property io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.MissingConfigPropertyBean.nonExistantKey");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @Test
    public void testNonExistantKeyWithCustomConverter() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("SRCFG02000: No Config Value exists for required property io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.MissingConfigPropertyBean.undefinedKeyWithConverter");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @Test
    public void testConverterMissing() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("SRCFG02006: The property noConverterKey cannot be converted to class io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.TypeWithNoConverter");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKZ0002E", "CWMCG5005E");
        //CWWKZ0002E: An exception occurred while starting the application badObserverApp/brokenInjectionApp
        //CWMCG5005E: The InjectionPoint dependency was not resolved for the Observer method: private static final void com.ibm.ws.microprofile.config14.test.apps.badobserver.TestObserver.observerMethod(java.lang.Object,java.lang.String).
    }

}
