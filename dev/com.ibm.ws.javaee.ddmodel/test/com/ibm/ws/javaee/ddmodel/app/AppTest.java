/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.app;

import java.util.List;

import org.junit.Test;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.common.ContextService;
import com.ibm.ws.javaee.dd.common.ManagedExecutor;
import com.ibm.ws.javaee.dd.common.ManagedScheduledExecutor;
import com.ibm.ws.javaee.dd.common.ManagedThreadFactory;
import com.ibm.ws.javaee.ddmodel.DDJakarta10Elements;
import com.ibm.ws.javaee.ddmodel.DDJakarta11Elements;

public class AppTest extends AppTestBase {
    @Test
    public void testApp() throws Exception {
        for (int schemaVersion : Application.VERSIONS) {
            for (int maxSchemaVersion : Application.VERSIONS) {
                // Open liberty will always parse JavaEE6 and earlier
                // schema versions.
                int effectiveMax;
                if (maxSchemaVersion < VERSION_6_0_INT) {
                    effectiveMax = VERSION_6_0_INT;
                } else {
                    effectiveMax = maxSchemaVersion;
                }

                String altMessage;
                String[] messages;
                if (schemaVersion > effectiveMax) {
                    altMessage = UNPROVISIONED_DESCRIPTOR_VERSION_ALT_MESSAGE;
                    messages = UNPROVISIONED_DESCRIPTOR_VERSION_MESSAGES;
                } else {
                    altMessage = null;
                    messages = null;
                }

                parseApp(app(schemaVersion, appBody), maxSchemaVersion, altMessage, messages);
            }
        }
    }

    // Verify new elements to EE 10 cannot be used with EE 9 schema

    @Test
    public void testEE10ContextServiceApp90() throws Exception {
        parseApp(app(Application.VERSION_9, DDJakarta10Elements.CONTEXT_SERVICE_XML),
                 Application.VERSION_9,
                 "unexpected.child.element",
                 "CWWKC2259E", "context-service", "myEAR.ear : META-INF/application.xml");
    }

    @Test
    public void testEE10ManagedExecutorApp90() throws Exception {
        parseApp(app(Application.VERSION_9, DDJakarta10Elements.MANAGED_EXECUTOR_XML),
                 Application.VERSION_9,
                 "unexpected.child.element",
                 "CWWKC2259E", "managed-executor", "myEAR.ear : META-INF/application.xml");
    }

    @Test
    public void testEE10ManagedScheduledExecutorApp90() throws Exception {
        parseApp(app(Application.VERSION_9, DDJakarta10Elements.MANAGED_SCHEDULED_EXECUTOR_XML),
                 Application.VERSION_9,
                 "unexpected.child.element",
                 "CWWKC2259E", "managed-scheduled-executor", "myEAR.ear : META-INF/application.xml");
    }

    @Test
    public void testEE10ManagedThreadFactoryApp90() throws Exception {
        parseApp(app(Application.VERSION_9, DDJakarta10Elements.MANAGED_THREAD_FACTORY_XML),
                 Application.VERSION_9,
                 "unexpected.child.element",
                 "CWWKC2259E", "managed-thread-factory", "myEAR.ear : META-INF/application.xml");
    }

    //  Verify new elements to EE 10 are parsed correctly

    @Test
    public void testEE10ContextServiceApp100() throws Exception {
        Application app = parseApp(
                                   app(Application.VERSION_10, DDJakarta10Elements.CONTEXT_SERVICE_XML),
                                   Application.VERSION_10);

        List<String> names = DDJakarta10Elements.names("Application", "contextServices");

        List<ContextService> services = app.getContextServices();
        DDJakarta10Elements.verifySize(names, 1, services);
        DDJakarta10Elements.verify(names, services.get(0));
    }

    @Test
    public void testEE10ManagedExecutorApp100() throws Exception {
        Application app = parseApp(
                                   app(Application.VERSION_10, DDJakarta10Elements.MANAGED_EXECUTOR_XML),
                                   Application.VERSION_10);

        List<String> names = DDJakarta10Elements.names("Application", "managedExecutors");

        List<ManagedExecutor> executors = app.getManagedExecutors();
        DDJakarta10Elements.verifySize(names, 1, executors);
        DDJakarta10Elements.verify(names, executors.get(0));
    }

    @Test
    public void testEE10ManagedScheduledExecutorApp100() throws Exception {
        Application app = parseApp(
                                   app(Application.VERSION_10, DDJakarta10Elements.MANAGED_SCHEDULED_EXECUTOR_XML),
                                   Application.VERSION_10);

        List<String> names = DDJakarta10Elements.names("Application", "managedScheduledExecutors");

        List<ManagedScheduledExecutor> executors = app.getManagedScheduledExecutors();
        DDJakarta10Elements.verifySize(names, 1, executors);
        DDJakarta10Elements.verify(names, executors.get(0));
    }

    @Test
    public void testEE10ManagedThreadFactoryApp100() throws Exception {
        Application app = parseApp(
                                   app(Application.VERSION_10, DDJakarta10Elements.MANAGED_THREAD_FACTORY_XML),
                                   Application.VERSION_10);

        List<String> names = DDJakarta10Elements.names("Application", "managedThreadFactories");

        List<ManagedThreadFactory> factories = app.getManagedThreadFactories();
        DDJakarta10Elements.verifySize(names, 1, factories);
        DDJakarta10Elements.verify(names, factories.get(0));
    }

    // Verify new elements to EE 11 are parsed correctly

    @Test
    public void testEE11ContextServiceApp110() throws Exception {
        Application app = parseApp(
                                   app(Application.VERSION_11, DDJakarta11Elements.CONTEXT_SERVICE_XML),
                                   Application.VERSION_11);

        List<String> names = DDJakarta10Elements.names("Application", "contextServices");

        List<ContextService> services = app.getContextServices();
        DDJakarta10Elements.verifySize(names, 1, services);
        DDJakarta10Elements.verify(names, services.get(0));
    }

    @Test
    public void testEE11ManagedExecutorApp110() throws Exception {
        Application app = parseApp(
                                   app(Application.VERSION_11, DDJakarta11Elements.MANAGED_EXECUTOR_XML),
                                   Application.VERSION_11);

        List<String> names = DDJakarta10Elements.names("Application", "managedExecutors");

        List<ManagedExecutor> executors = app.getManagedExecutors();
        DDJakarta10Elements.verifySize(names, 1, executors);
        DDJakarta10Elements.verify(names, executors.get(0));
    }

    @Test
    public void testEE11ManagedScheduledExecutorApp110() throws Exception {
        Application app = parseApp(
                                   app(Application.VERSION_11, DDJakarta11Elements.MANAGED_SCHEDULED_EXECUTOR_XML),
                                   Application.VERSION_11);

        List<String> names = DDJakarta10Elements.names("Application", "managedScheduledExecutors");

        List<ManagedScheduledExecutor> executors = app.getManagedScheduledExecutors();
        DDJakarta10Elements.verifySize(names, 1, executors);
        DDJakarta10Elements.verify(names, executors.get(0));
    }

    @Test
    public void testEE11ManagedThreadFactoryApp110() throws Exception {
        Application app = parseApp(
                                   app(Application.VERSION_11, DDJakarta11Elements.MANAGED_THREAD_FACTORY_XML),
                                   Application.VERSION_11);

        List<String> names = DDJakarta10Elements.names("Application", "managedThreadFactories");

        List<ManagedThreadFactory> factories = app.getManagedThreadFactories();
        DDJakarta10Elements.verifySize(names, 1, factories);
        DDJakarta10Elements.verify(names, factories.get(0));
    }
}
