/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.classloading;

import static com.ibm.ws.classloading.TestUtils.buildAndExportBellLibrary;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests for {@link LibraryServiceExporter}.
 */
public class BellPropertiesTest {

    static final LibertyServer server = LibertyServerFactory.getLibertyServer("bell_props_server");

    private static final String USER_BUNDLE_NAME = "exporting.metainf.services";
    private static final String USER_FEATURE_NAME = "user.feature.meta.inf.services-1.0";

    @BeforeClass
    public static void setup() throws Throwable {
        buildAndExportBellLibrary(server, "testMultipleValidServices.jar", "MultipleValidServices1", "MultipleValidServices2");
        buildAndExportBellLibrary(server, "testProperties.jar", "EmptyProperties", "PropertiesResolveLibertyVars");
        buildAndExportBellLibrary(server, "testImplClassNotConstructible.jar", "ImplClassNotConstructible");

        server.installUserBundle(USER_BUNDLE_NAME);
        server.installUserFeature(USER_FEATURE_NAME);
    }

    @AfterClass
    public static void tearDown() throws Throwable {
        stopServer();
        try {
            server.uninstallUserFeature(USER_FEATURE_NAME);
            server.uninstallUserBundle(USER_BUNDLE_NAME);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void afterTest() {
        stopServer();
    };

    static void stopServer() {
        if (server.isStarted()) {
            try {
                server.stopServer();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    };

    // These four exceptions will all appear as FFDCs at some point. They may be issued before
    // or after the server start message, so they could potentially appear as a new FFDC during
    // any of these tests. There isn't really a way to reliably wait for all of the FFDCs to be
    // issued, so every test should include these as allowed FFDCs.
    static final String
        INSTANTIATION_EXCEPTION = "java.lang.InstantiationException",
        CLASS_NOT_FOUND_EXCEPTION = "java.lang.ClassNotFoundException",
        NO_CLASSDEF_FOUND_EXCEPTION = "java.lang.NoClassDefFoundError",
        EXCEPTION = "java.lang.Exception";

    enum Inject_Op {
        SingleArgCtor(" "),
        UpdateBellMethod(" updated ");

        private final String logStr;
        Inject_Op(String str) { this.logStr = str; }
        String getLogStr() { return this.logStr; }
    }

    static final int
        TimeOut = 3000; // ms


    /**
     * Verify a service impl logged a message indicating the expected injection operation
     * and property set.
     * @serviceName The simple name of a service impl class.
     * @injectOp The expected method used to inject properties into the service impl.
     * @expectedProps The expected set of properties injected into the service impl.
     */
    void assertBellProps(String serviceName, Inject_Op injectOp, Set<String> expectedProps)
    {
        // Msg format: <serviceName> has (e | updated) properties (null | {} | {<name>=<val>, <name>=<val>, ...})
        String servicePropsMsg = ".*" + serviceName + " has" + injectOp.getLogStr() + "properties ";

        if (expectedProps == null) {
            // no injection ==> no <properties/> declaration
            servicePropsMsg += "null";
            assertNotNull("The " + serviceName + " does not have \"null\" properties as expected.",
                    server.waitForStringInLog(servicePropsMsg));
        }
        else if (expectedProps.isEmpty()) {
            // zero properties injected ==> empty <properties/> declaration
            servicePropsMsg += "\\{\\}";
            assertNotNull("The " + serviceName + " does not have \"{}\" properties as expected.",
                    server.waitForStringInLog(servicePropsMsg));
        }
        else {
            // props names are unique per bell
            String propPrefix = expectedProps.iterator().next();
            propPrefix = propPrefix.substring(0, propPrefix.indexOf("_"));
            servicePropsMsg += "\\{" + propPrefix + ".*";

            // find the msg with props unique to the bell
            String logEntry =  server.waitForStringInLog(servicePropsMsg);
            assertNotNull("The " + serviceName + " does not have properties " + expectedProps + " as expected.",
                    logEntry);

            // verify the exact set of bell properties were injected
            String propsString = logEntry.substring(logEntry.indexOf("{")+1, logEntry.indexOf("}"));
            String[] propsSplit = propsString.split("\\s*,\\s*");  // trim spaces on elements split over ","
            Set<String> loggedProps = Stream.of( propsSplit ).collect(Collectors.toCollection(HashSet::new));

            assertEquals("The " + serviceName + " does not have properties " + expectedProps + " as expected, but instead has " + loggedProps + ", order not important.",
                    expectedProps, loggedProps);
        }
    }

    /**
     * Verify the server injects the exact bell properties using the expected method
     * when creating a BELL service implementation.
     */
    @Test
    @AllowedFFDC({
        INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION
    })
    public void testProperties() throws Exception
    {
        server.startServer();

        assertNotNull("The server did not register service testPropertiesLib/MultipleValidServices1, but should have.",
                server.waitForStringInLog(".*CWWKL0050I: .*testPropertiesLib.*MultipleValidServices1"));

        Set<String> expectedProps = Stream.of("TP_P0=TP_P0_VAL", "TP_P1=TP_P1_VAL", "TP_P2=TP_P2_VAL", "TP_P3=TP_P3_VAL")
                .collect(Collectors.toCollection(HashSet::new));

        // Properties inject using single-arg ctor
        assertBellProps("MultipleValidServices1", Inject_Op.SingleArgCtor, expectedProps);

        assertNotNull("The server did not register service testPropertiesLib/MultipleValidServices2, but should have.",
                server.waitForStringInLog(".*CWWKL0050I: .*testPropertiesLib.*MultipleValidServices2"));

        // Missing single-arg ctor; properties inject using updateBell()
        assertBellProps("MultipleValidServices2", Inject_Op.UpdateBellMethod, expectedProps);
    }

    /**
     * Verify the server injects an empty property set.
     */
    @Test
    @AllowedFFDC({
        INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION
    })
    public void testPropertiesEmpty() throws Exception
    {
        server.startServer();

        assertNotNull("The server did not register service testEmptyPropertiesLib/EmptyProperties, but should have.",
                server.waitForStringInLog(".*CWWKL0050I: .*testEmptyPropertiesLib.*EmptyProperties"));

        Set<String> expectedProps = Collections.emptySet();

        assertBellProps("EmptyProperties", Inject_Op.SingleArgCtor, expectedProps);
    }

    /**
     * Verify the server warns that a BELL service implementation lacks the single-arg ctor
     * and updateBell methods required for property injection.
     */
    @Test
    @AllowedFFDC({
        INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION
    })
    public void testPropertiesMissingInjectionMethods() throws Exception
    {
        server.startServer();

        assertNotNull("The server did not register service testMissingInjectionMethods/testImplClassNotConstructible, but should have.",
                server.waitForStringInLog(".*CWWKL0050I: .*testMissingInjectionMethods.*testImplClassNotConstructible"));

        // Server attempts to inject using single-arg ctor and updateBell() methods
        assertNotNull("The server did not warn that class ImplClassNotConstructible is missing methods that support property injection, but should have.",
                server.waitForStringInLog(".*CWWKL0062W: .*ImplClassNotConstructible"));

        // Server must attempt to create impl with the zero-arg ctor
        assertNotNull("The server did not warn that class ImplClassNotConstructible failed to instantiate, but should have.",
                server.waitForStringInLog(".*CWWKL0053W: .*ImplClassNotConstructible"));
    }

    /**
     * Verify the server emits a warning for invalid BELL property declarations and does not
     * inject BELL properties.
     *
     *  Duplicate property names and multiple property elements are valid configurations.
     *  See {@link #TestProperties}.
     */
    @Test
    @AllowedFFDC({
        INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION
    })
    public void testPropertiesInvalid() throws Exception
    {
        final LibertyServer propsInvalidServer = LibertyServerFactory.getLibertyServer("bell_props_invalid_server");
        try {
            propsInvalidServer.startServerAndValidate(
                    true,   // preClean
                    true,   // cleanStart
                    false,  // validateApps
                    false,  // expectStartFailure
                    false); // validateTimedExit

            // During startup the server logs CWWKG0014E when it discovers a syntax error in a property
            // declaration, such as 'TP1=', '="P1-VAL"', 'P1=P1-VAL"', etc. The server will also log CWWKF0009W
            // indicating no features were installed. During BELL update the server logs CWWKG0014E and
            // otherwise ignores the updated configuration. In either case the server cannot process BELLs
            // configured with malformed properties.

            assertNull("The server should not register service testInvalidPropertiesLib/MultipleValidServices1, but did",
                       propsInvalidServer.waitForStringInLog(".*CWWKL0050I: .*testInvalidPropertiesLib.*MultipleValidServices1", TimeOut));
        }
        finally {
            if (propsInvalidServer.isStarted()) {
                propsInvalidServer.stopServer(
                        "CWWKG0014E: .*TIP_P0", // xml parse error on malformed property TIP_P0
                        "CWWKF0009W");          // no features installed
            }
        }
    }

    /**
     * Verify the server resolves Liberty Variables in BELL property values.
     */
    @Test
    @AllowedFFDC({
        INSTANTIATION_EXCEPTION, CLASS_NOT_FOUND_EXCEPTION, NO_CLASSDEF_FOUND_EXCEPTION, EXCEPTION
    })
    public void testPropertiesResolveLibertyVars() throws Exception
    {
        server.startServer();

        assertNotNull("The server did not register service testPropertiesResolveLibertyVarsLib/PropertiesResolveLibertyVars, but should have.",
                server.waitForStringInLog(".*CWWKL0050I: .*testPropertiesResolveLibertyVarsLib.*PropertiesResolveLibertyVars"));

        Set<String> expectedProps = Stream.of("TPCLV_P0=LV_0_VAL", "TPCLV_P1=TPCLV_P1_LV_1_VAL", "TPCLV_P2=TPCLV_P2_VAL")
                                            .collect(Collectors.toCollection(HashSet::new));

        assertBellProps("PropertiesResolveLibertyVars", Inject_Op.SingleArgCtor, expectedProps);
    }

}