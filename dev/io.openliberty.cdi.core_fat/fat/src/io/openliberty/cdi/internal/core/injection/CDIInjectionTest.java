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
package io.openliberty.cdi.internal.core.injection;

import static io.openliberty.cdi.internal.core.Repeats.WITH_BEANS_XML;
import static io.openliberty.cdi.internal.core.Repeats.NO_BEANS_XML;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.cdi.internal.core.FATSuite;
import io.openliberty.cdi.internal.core.RepeatRule;
import io.openliberty.cdi.internal.core.Repeats;
import io.openliberty.cdi.internal.core.injection.app.CDIInjectionTestServlet;

@RunWith(FATRunner.class)
public class CDIInjectionTest {

    @ClassRule
    public static RepeatRule<Repeats> r = new RepeatRule<>(NO_BEANS_XML, WITH_BEANS_XML);

    @TestServlet(contextRoot = "cdiInjection", servlet = CDIInjectionTestServlet.class)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        server = FATSuite.server;

        Package appPackage = CDIInjectionTestServlet.class.getPackage();
        WebArchive war = ShrinkWrap.create(WebArchive.class, "cdiInjection.war")
                                   .addPackage(appPackage);

        if (r.getRepeat() == WITH_BEANS_XML) {
            war.addAsWebInfResource(appPackage, "beans.xml", "beans.xml");
        }

        FATSuite.deployApp(server, war);
    }

    @AfterClass
    public static void teardown() throws Exception {
        FATSuite.removeApp(server, "cdiInjection.war");
    }

}
