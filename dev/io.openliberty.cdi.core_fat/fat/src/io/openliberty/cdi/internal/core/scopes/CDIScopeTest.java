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
package io.openliberty.cdi.internal.core.scopes;

import static io.openliberty.cdi.internal.core.Repeats.WITH_BEANS_XML;
import static io.openliberty.cdi.internal.core.Repeats.NO_BEANS_XML;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.util.Objects;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.cdi.internal.core.FATSuite;
import io.openliberty.cdi.internal.core.RepeatRule;
import io.openliberty.cdi.internal.core.Repeats;
import io.openliberty.cdi.internal.core.scopes.app.CDIScopeTestServlet;

@RunWith(FATRunner.class)
public class CDIScopeTest {

    @ClassRule
    public static RepeatRule<Repeats> r = new RepeatRule<>(NO_BEANS_XML, WITH_BEANS_XML);

    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        server = FATSuite.server;

        Package appPackage = CDIScopeTestServlet.class.getPackage();
        WebArchive war = ShrinkWrap.create(WebArchive.class, "cdiScope.war")
                                   .addPackage(appPackage);

        if (r.getRepeat() == WITH_BEANS_XML) {
            war.addAsWebInfResource(appPackage, "beans.xml", "beans.xml");
        }

        FATSuite.deployApp(server, war);
    }

    @AfterClass
    public static void teardown() throws Exception {
        FATSuite.removeApp(server, "cdiScope.war");
    }

    @Test
    public void testAppScoped() throws IOException {
        Pair result1 = getCounterValues("app");
        Pair result2 = getCounterValues("app");

        // Same app scoped should be used in one request, so the counter should increment
        assertThat(result1.a, not(equalTo(result1.b)));
        assertThat(result2.a, not(equalTo(result2.b)));

        // Same app scoped bean should be used across multiple requests, so the counter should increment
        assertThat(result1, not(equalTo(result2)));
    }

    @Test
    public void testRequestScoped() throws IOException {
        Pair result1 = getCounterValues("request");
        Pair result2 = getCounterValues("request");

        // Same request scoped should be used in one request, so the counter should increment
        assertThat(result1.a, not(equalTo(result1.b)));
        assertThat(result2.a, not(equalTo(result2.b)));

        // Different request scoped bean should be used across multiple requests, so the results should be the same
        assertThat(result1, equalTo(result2));
    }

    @Test
    public void testDepedentScoped() throws IOException {
        Pair result = getCounterValues("dependent");

        // New instance expected for each injection point, so counter should not increment
        assertThat(result.a, equalTo(result.b));

        // We don't test dependent scope beans across two requests since they're dependent on the thing they're injected into.
        // If we tested across two requests, we'd really be testing the scope of the servlet itself.
    }

    private Pair getCounterValues(String scope) throws IOException {
        String response = HttpUtils.getHttpResponseAsString(server, "cdiScope/scope?scope=" + scope);
        String[] parts = response.split(",");
        assertThat(parts, arrayWithSize(2));
        return new Pair(Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()));
    }

    private static class Pair {
        public int a, b;

        public Pair(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return "(" + a + ", " + b + ")";
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Pair other = (Pair) obj;
            return a == other.a && b == other.b;
        }
    }

}
