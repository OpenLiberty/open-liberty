/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.ConfigUnnamedConstructorInjectionBean;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.ConfigUnnamedMethodInjectionBean;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.converters.BadConverter;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.converters.TypeWithBadConverter;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.converters.TypeWithNoConverter;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.converters.ValidConverter;

@RunWith(FATRunner.class)
public class Config20ExceptionTests extends FATServletClient {

    public static final String BROKEN_INJECTION_APP_NAME = "brokenInjectionApp";

    public static final String SERVER_NAME = "Config20ExceptionServer";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.LATEST);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        PropertiesAsset brokenInjectionConfigSource = new PropertiesAsset()
                        .addProperty("validPrefix.myString", "aString")
                        .addProperty("badConverterKey", "aValue")
                        .addProperty("noConverterKey", "aValue");

        WebArchive brokenInjectionWar = ShrinkWrap.create(WebArchive.class, BROKEN_INJECTION_APP_NAME + ".war")
                        .addPackages(true, "io.openliberty.microprofile.config.internal_fat.apps.brokenInjection")
                        .addAsServiceProvider(Converter.class, ValidConverter.class, BadConverter.class)
                        .addAsResource(brokenInjectionConfigSource, "META-INF/microprofile-config.properties");

        // The war should throw a deployment exception, hence don't validate.
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

    /**
     * Check an appropriate error message occurs when a user tries to Inject a non-existing Config Property without a "name" field into a Method.
     *
     * Should be: java.lang.IllegalStateException: SRCFG02002: Could not find default name for @ConfigProperty InjectionPoint [BackedAnnotatedParameter] Parameter 1 of
     * [BackedAnnotatedMethod] @Inject public io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.ConfigUnnamedMethodInjectionBean.aMethod(@ConfigProperty String)
     */
    @Test
    public void testMethodUnnamed() throws Exception {
        String beanDir = ConfigUnnamedMethodInjectionBean.class.getName();
        List<String> errors = server.findStringsInLogs("SRCFG02002: .*" + beanDir + ".aMethod\\(@ConfigProperty String\\)");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    /**
     * Check an appropriate error message occurs when a user tries to Inject a non-existing Config Property without a "name" field into a Constructor.
     *
     * Should be: java.lang.IllegalStateException: SRCFG02002: Could not find default name for @ConfigProperty InjectionPoint [BackedAnnotatedParameter] Parameter 1 of
     * [BackedAnnotatedConstructor] @Inject public io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.ConfigUnnamedConstructorInjectionBean(@ConfigProperty
     * String)
     */
    @Test
    public void testConstructorUnnamed() throws Exception {
        String beanDir = ConfigUnnamedConstructorInjectionBean.class.getName();
        List<String> errors = server.findStringsInLogs("SRCFG02002: .*" + beanDir + "\\(@ConfigProperty String\\)");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @Test
    public void testNonExistantKey() throws Exception {
        List<String> errors = server.findStringsInLogs("SRCFG02000: No Config Value exists for required property nonExistantKey");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @Test
    public void testNonExistantKeyWithCustomConverter() throws Exception {
        List<String> errors = server.findStringsInLogs("SRCFG02000: No Config Value exists for required property nonExistingKeyWithCustomConverter");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);

        errors = server.findStringsInLogs(ValidConverter.CHECK_STRING); // Confirm the Converter was never entered
        assertNotNull(errors);
        assertTrue(errors.size() == 0);
    }

    @Test
    public void testConverterMissing() throws Exception {
        List<String> errors = server.findStringsInLogs("SRCFG02006: The property noConverterKey cannot be converted to class " + TypeWithNoConverter.class.getName());
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @Test
    public void testBadConverter() throws Exception {
        List<String> errors = server.findStringsInLogs("SRCFG02006: The property badConverterKey cannot be converted to class " + TypeWithBadConverter.class.getName());
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @Test
    public void testBadConfigPropertiesInjection() throws Exception {
        List<String> errors = server.findStringsInLogs("SRCFG00029: Expected an integer value, got \"aString\"");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKZ0002E");
        //CWWKZ0002E: An exception occurred while starting the application brokenInjectionApp
    }

}
