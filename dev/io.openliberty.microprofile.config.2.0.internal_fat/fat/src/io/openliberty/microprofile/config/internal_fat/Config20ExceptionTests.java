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
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.BadConfigPropertiesBean;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.BadConfigPropertyBean;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.BadConfigPropertyInConstructorBean;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.BadConfigPropertyInMethodBean;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.BadObserverBean;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.MissingPropertyExpressionBean;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.converters.BadConverter;
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
                        .addPackages(true, BadConfigPropertiesBean.class.getPackage())
                        .addAsServiceProvider(Converter.class, ValidConverter.class, BadConverter.class)
                        .addAsResource(brokenInjectionConfigSource, "META-INF/microprofile-config.properties");

        // The war should throw a deployment exception, hence don't validate.
        ShrinkHelper.exportAppToServer(server, brokenInjectionWar, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);

        server.startServer();

    }

    @Test
    public void testBadObserver() throws Exception {
        String beanDir = BadObserverBean.class.getName();
        List<String> errors = server
                        .findStringsInLogs("SRCFG02000: Failed to Inject @ConfigProperty for key DOESNOTEXIST into " + beanDir
                                           + ".observerMethod\\(Object, String\\) since the config property could not be found in any config source");
        assertNotNull("error not found", errors);
        assertTrue("error not found: " + errors.size(), errors.size() > 0);
    }

    /**
     * Check an appropriate error message occurs when a user tries to Inject a non-existing Config Property without a "name" field into a Method.
     *
     * Should be: io.smallrye.config.inject.ConfigException: SRCFG02001: Failed to Inject @ConfigProperty for key null into
     * io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.BadConfigPropertyInConstructorBean(String) SRCFG02002: Could not find default name for @ConfigProperty
     * InjectionPoint [BackedAnnotatedParameter] Parameter 1 of [BackedAnnotatedConstructor] @Inject public
     * io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.BadConfigPropertyInConstructorBean(@ConfigProperty String)
     */
    @Test
    public void testMethodUnnamed() throws Exception {
        String beanDir = BadConfigPropertyInMethodBean.class.getName();
        List<String> errors = server.findStringsInLogs("SRCFG02001: Failed to Inject @ConfigProperty for key null into " + beanDir + ".aMethod\\(String\\) "
                                                       + "SRCFG02002: Could not find default name for @ConfigProperty .*" + beanDir + ".aMethod\\(@ConfigProperty String\\)");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    /**
     * Check an appropriate error message occurs when a user tries to Inject a non-existing Config Property without a "name" field into a Constructor.
     *
     * Should be: io.smallrye.config.inject.ConfigException: SRCFG02001: Failed to Inject @ConfigProperty for key null into
     * io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.BadConfigPropertyInConstructorBean(String) SRCFG02002: Could not find default name for @ConfigProperty
     * InjectionPoint [BackedAnnotatedParameter] Parameter 1 of [BackedAnnotatedConstructor] @Inject public
     * io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.BadConfigPropertyInConstructorBean(@ConfigProperty String)
     */
    @Test
    public void testConstructorUnnamed() throws Exception {
        String beanDir = BadConfigPropertyInConstructorBean.class.getName();
        List<String> errors = server
                        .findStringsInLogs("SRCFG02001: Failed to Inject @ConfigProperty for key null into " + beanDir + "\\(String\\) "
                                           + "SRCFG02002: Could not find default name for @ConfigProperty .*" + beanDir + "\\(@ConfigProperty String\\)");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @Test
    public void testNonExistantKey() throws Exception {
        String beanDir = BadConfigPropertyBean.class.getName();
        List<String> errors = server
                        .findStringsInLogs("SRCFG02000: Failed to Inject @ConfigProperty for key nonExistantKey into " + beanDir
                                           + ".nonExistantKey1 since the config property could not be found in any config source");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @Test
    public void testNonExistantKeyWithCustomConverter() throws Exception {
        String beanDir = BadConfigPropertyBean.class.getName();
        List<String> errors = server
                        .findStringsInLogs("SRCFG02000: Failed to Inject @ConfigProperty for key nonExistingKeyWithCustomConverter into " + beanDir
                                           + ".nonExistantKey2 since the config property could not be found in any config source");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);

        errors = server.findStringsInLogs(ValidConverter.CHECK_STRING); // Confirm the Converter was never entered
        assertNotNull(errors);
        assertTrue(errors.size() == 0);
    }

    @Test
    public void testConverterMissing() throws Exception {
        String beanDir = BadConfigPropertyBean.class.getName();
        List<String> errors = server.findStringsInLogs("SRCFG02001: Failed to Inject @ConfigProperty for key noConverterKey into " + beanDir + ".noConverterProp"
                                                       + " SRCFG02007: No Converter registered for class " + TypeWithNoConverter.class.getName());
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @Test
    public void testBadConverter() throws Exception {
        String beanDir = BadConfigPropertyBean.class.getName();
        List<String> errors = server
                        .findStringsInLogs("SRCFG02001: Failed to Inject @ConfigProperty for key badConverterKey into " + beanDir + ".badConverterProp"
                                           + " SRCFG00039: The config property badConverterKey with the config value \"aValue\" threw an Exception whilst being converted Converter throwing intentional exception");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @Test
    public void testBadConfigPropertiesInjection() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("SRCFG00039: The config property validPrefix.myString with the config value \"aString\" threw an Exception whilst being converted"
                                           + " SRCFG00029: Expected an integer value, got \"aString\"");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    /**
     * Should be: io.smallrye.config.inject.ConfigException: SRCFG02001: Failed to Inject @ConfigProperty for key keyFromVariableInServerXML into
     * io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.MissingPropertyExpressionBean.nonExistantPropertyExpressionVariable SRCFG00011: Could not expand value
     * nonExistingPropertyForServerXMLVariable in property keyFromVariableInServerXML
     */
    @Test
    public void testNonExistingPropertyExpressionForServerXMLVariable() throws Exception {
        String beanDir = MissingPropertyExpressionBean.class.getName();
        String key = "keyFromVariableInServerXML";
        String field = "nonExistantPropertyExpressionVariable";
        String propertyExpression = "nonExistingPropertyForServerXMLVariable";

        List<String> errors = server
                        .findStringsInLogs("SRCFG02001: Failed to Inject @ConfigProperty for key " + key + " into " + beanDir + "." + field
                                           + " SRCFG00011: Could not expand value " + propertyExpression + " in property " + key);
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    /**
     * Should be: io.smallrye.config.inject.ConfigException: SRCFG02001: Failed to Inject @ConfigProperty for key keyFromAppPropertyInServerXML into
     * io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.MissingPropertyExpressionBean.nonExistantPropertyExpressionAppProperty SRCFG00011: Could not expand
     * value nonExistingPropertyForServerXMLAppProperty in property keyFromAppPropertyInServerXML
     */
    @Test
    public void testNonExistingPropertyExpressionForServerXMLAppProperty() throws Exception {
        String beanDir = MissingPropertyExpressionBean.class.getName();
        String key = "keyFromAppPropertyInServerXML";
        String field = "nonExistantPropertyExpressionAppProperty";
        String propertyExpression = "nonExistingPropertyForServerXMLAppProperty";

        List<String> errors = server
                        .findStringsInLogs("SRCFG02001: Failed to Inject @ConfigProperty for key " + key + " into " + beanDir + "." + field
                                           + " SRCFG00011: Could not expand value " + propertyExpression + " in property " + key);
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKZ0002E");
        //CWWKZ0002E: An exception occurred while starting the application brokenInjectionApp
    }

}
