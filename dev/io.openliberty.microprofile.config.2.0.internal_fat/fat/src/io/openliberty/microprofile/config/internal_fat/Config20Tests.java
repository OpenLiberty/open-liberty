/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfigActions;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfigActions.Version;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.config.internal_fat.apps.TestUtils;
import io.openliberty.microprofile.config.internal_fat.apps.badObserver.TestObserver;
import io.openliberty.microprofile.config.internal_fat.apps.classLoader.ClassLoadersTestServlet;
import io.openliberty.microprofile.config.internal_fat.apps.converter.ConvertersTestServlet;
import io.openliberty.microprofile.config.internal_fat.apps.defaultSources.DefaultSourcesTestServlet;

@RunWith(FATRunner.class)
public class Config20Tests extends FATServletClient {

    public static final String BAD_OBSERVER_APP_NAME = "badObserverApp";
    public static final String DEFAULT_SOURCES_APP_NAME = "defaultSourcesApp";
    public static final String BROKEN_INJECTION_APP_NAME = "brokenInjectionApp";
    public static final String CLASS_LOADER_APP_NAME = "classLoadersApp";
    public static final String CONVERTER_LOADER_APP_NAME = "convertersApp";

    public static final String SERVER_NAME = "Config20Server";

    @ClassRule
    public static RepeatTests r = RepeatConfigActions.repeat(SERVER_NAME, Version.LATEST);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = DefaultSourcesTestServlet.class, contextRoot = DEFAULT_SOURCES_APP_NAME),
                    @TestServlet(servlet = ClassLoadersTestServlet.class, contextRoot = CLASS_LOADER_APP_NAME),
                    @TestServlet(servlet = ConvertersTestServlet.class, contextRoot = CONVERTER_LOADER_APP_NAME)

    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive badObserverWar = ShrinkWrap.create(WebArchive.class, BAD_OBSERVER_APP_NAME + ".war")
                        .addPackages(true, TestObserver.class.getPackage());

        WebArchive defaultSourcesWar = ShrinkWrap.create(WebArchive.class, DEFAULT_SOURCES_APP_NAME + ".war")
                        .addPackages(true, DefaultSourcesTestServlet.class.getPackage());

        /*
         * Currently, when an exception is thrown from `ConfigProducerUtil.getConfigKey(injectionPoint, configProperty);` in SmallRye,
         * (at io.smallrye.config.inject.ConfigExtension.validate(ConfigExtension.java:144)),
         * The exception is not caught and the remaining InjectionPoints are not validated.
         * Hence, when testMethodUnnamed() or testConstructorUnnamed() throw their exceptions, the remaining Injections aren't validated -> aren't thrown
         * So currently, when the 4 tests run from brokenInjectionWar at least one of them will fail.
         *
         * TODO: The Filter needs removing when the SmallRye code allows for multiple failed Injection points.
         */
        WebArchive brokenInjectionWar = ShrinkWrap.create(WebArchive.class, BROKEN_INJECTION_APP_NAME + ".war")
                        .addPackages(true, Filters.exclude(".*ConfigUnnamed.*InjectionBean.*"), "io.openliberty.microprofile.config.internal_fat.apps.brokenInjection") //Filters.exclude(".*ConfigUnnamed.*InjectionBean.*")
                        .addAsManifestResource(new File("publish/servers/Config20Server/org.eclipse.microprofile.config.spi.Converter"),
                                               "services/org.eclipse.microprofile.config.spi.Converter");

        WebArchive classLoadersWar = ShrinkWrap.create(WebArchive.class, CLASS_LOADER_APP_NAME + ".war")
                        .addPackages(true, ClassLoadersTestServlet.class.getPackage())
                        .addClass(TestUtils.class);

        WebArchive convertersWar = ShrinkWrap.create(WebArchive.class, CONVERTER_LOADER_APP_NAME + ".war")
                        .addPackages(true, ConvertersTestServlet.class.getPackage())
                        .addClass(TestUtils.class);

        ShrinkHelper.exportDropinAppToServer(server, badObserverWar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, defaultSourcesWar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, brokenInjectionWar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, classLoadersWar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, convertersWar, DeployOptions.SERVER_ONLY);

        server.startServerAndValidate(true, true, false);

    }

    @Test
    public void testBadObserver() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("SRCFG02000: No Config Value exists for required property DOESNOTEXIST");
        assertNotNull("error not found", errors);
        assertTrue("error not found: " + errors.size(), errors.size() > 0);
    }

    /*
     * Old output: ConfigUnnamedMethodInjectionBean.*setSimpleKey6.*The property name must be specified for Constructor and Method configuration property injection
     *
     * New output: java.lang.IllegalStateException: SRCFG02002: Could not find default name for @ConfigProperty InjectionPoint [BackedAnnotatedParameter] Parameter 1 of
     * [BackedAnnotatedMethod] @Inject public io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.ConfigUnnamedMethodInjectionBean.setSimpleKey6(@ConfigProperty
     * String)
     */
    //TODO: See line 74 for why this test is disabled
    @Test
    public void testMethodUnnamed() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("SRCFG02002: Could not find default name for .*io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.ConfigUnnamedMethodInjectionBean.*setSimpleKey6");
        assertNotNull("error not found", errors);
        assertTrue("error not found: " + errors.size(), errors.size() > 0);
    }

    /*
     * Old output: ConfigUnnamedConstructorInjectionBean.*The property name must be specified for Constructor and Method configuration property injection
     *
     * New output: java.lang.IllegalStateException: SRCFG02002: Could not find default name for @ConfigProperty InjectionPoint [BackedAnnotatedParameter] Parameter 1 of
     * [BackedAnnotatedConstructor] @Inject public io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.ConfigUnnamedConstructorInjectionBean(@ConfigProperty
     * String, Config)
     */
    //TODO: See line 74 for why this test is disabled
    @Test
    public void testConstructorUnnamed() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("SRCFG02002: Could not find default name for .*io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.ConfigUnnamedConstructorInjectionBean");
        assertNotNull("error not found", errors);
        assertTrue("error not found: " + errors.size(), errors.size() > 0);
    }

    /*
     * Old output: CWMCG5003E.*nonExistantKey.*CWMCG0015E: The property com.ibm.ws.microprofile.appConfig.cdi.broken.beans.MissingConfigPropertyBean.nonExistantKey was not found in
     * the configuration
     *
     * Initial SmallRye output: java.util.NoSuchElementException: SRCFG02004: Required property
     * io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.MissingConfigPropertyBean.nonExistantKey not found
     *
     * New SmallRye output: io.smallrye.config.inject.ConfigException: SRCFG02000: No Config Value exists for required property
     * io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.MissingConfigPropertyBean.nonExistantKey
     */
    @Test
    public void testNonExistantKey() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("SRCFG02000: No Config Value exists for required property io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.MissingConfigPropertyBean.nonExistantKey");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    /*
     * Initial SmallRye output: java.util.NoSuchElementException: SRCFG02004: Required property
     * io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.MissingConfigPropertyBean.undefinedKeyWithConverter not found
     *
     * New SmallRye output: io.smallrye.config.inject.ConfigException: SRCFG02000: No Config Value exists for required property
     * io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.MissingConfigPropertyBean.undefinedKeyWithConverter
     */
    @Test //This prevents testNonExistantKey() messages being returned. (think this may be fixed with validate() fixes)
    public void testNonExistantKeyWithCustomConverter() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("SRCFG02000: No Config Value exists for required property io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.MissingConfigPropertyBean.undefinedKeyWithConverter");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    /*
     * Old output: CWMCG5003E.*noConverterProp.*CWMCG0014E: A Converter could not be found for type com.ibm.ws.microprofile.appConfig.cdi.broken.test.NoConverter
     *
     * Initial SmallRye output: java.lang.IllegalArgumentException: No Converter registered for class
     * io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.NoConverter
     *
     * New SmallRye output: java.lang.IllegalArgumentException: SRCFG02006: The property noConverterKey cannot be converted to class
     * io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.TypeWithNoConverter
     */
    @Test
    public void testConverterMissing() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("SRCFG02006: The property noConverterKey cannot be converted to class io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.TypeWithNoConverter");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKZ0002E", "CWMCG5003E", "CWMCG5005E");
        //CWWKZ0002E: An exception occurred while starting the application badObserverApp
        //CWMCG5003E: The {0} InjectionPoint dependency was not resolved. Error: {1}
        //CWMCG5005E: The InjectionPoint dependency was not resolved for the Observer method: private static final void com.ibm.ws.microprofile.config14.test.apps.badobserver.TestObserver.observerMethod(java.lang.Object,java.lang.String).
    }

}
