/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.microprofile.telemetry.internal.config_fat.apps.telemetry;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

@SuppressWarnings("serial")
@WebServlet("/testResources")
@ApplicationScoped
public class ResourceServlet extends FATServlet {

    private final Map<String, String> osNameMap = new HashMap<String, String>();

    @Inject
    Tracer tracer;

    @Inject
    OpenTelemetry openTelemetry;

    public ResourceServlet() {

        //Keys come from https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.1.0/instrumentation/resources/library/src/main/java/io/opentelemetry/instrumentation/resources/OsResource.java
        //Values come from https://github.com/open-telemetry/semantic-conventions-java/blob/release/v1.26.0/semconv/src/main/java/io/opentelemetry/semconv/ResourceAttributes.java
        osNameMap.put("windows", "windows");
        osNameMap.put("linux", "linux");
        osNameMap.put("mac", "darwin");
        osNameMap.put("netbsd", "netbsd");
        osNameMap.put("openbsd", "openbsd");
        osNameMap.put("dragonflybsd", "dragonflybsd");
        osNameMap.put("hp-ux", "hpux");
        osNameMap.put("aix", "aix");
        osNameMap.put("solaris", "solaris");
        osNameMap.put("z/os", "z_os");
    }

    @Test
    public void testServiceNameConfig() throws UnknownHostException {
        Tracer tracer = openTelemetry.getTracer("config-test", "1.0.0");
        Span span = tracer.spanBuilder("testSpan").startSpan();
        span.end();

        String output = openTelemetry.toString().toLowerCase();

        assertThat(output, containsString(("host.arch=\"" + System.getProperty("os.arch") + "\"").toLowerCase()));
        assertThat(output, containsString(("host.name=\"" + InetAddress.getLocalHost().getHostName().toString() + "\"").toLowerCase()));

        String osName = System.getProperty("os.name");
        osName = osName != null ? osName.toLowerCase() : "";
        String osVersion = System.getProperty("os.version");
        osVersion = osVersion != null ? osVersion.toLowerCase() : "";
        String osDescription = osVersion != null ? osName + ' ' + osVersion : osName;

        //MicroProfile Telemetry won't output os.type unless its one it knows about.
        if (osNameMap.containsKey(osName)) {
            //os.type is the only value that uses the mapped name;
            assertThat(output, containsString("os.type=\"" + mapOSName(osName) + "\""));
        }
        assertThat(output, containsString("os.description=\"" + osDescription + "\""));

        //Keeping this simple since the reliable way to do this is java 9+
        assertThat(output, containsString("process.command_line"));
        assertThat(output, containsString("ws-server.jar"));
        assertThat(output, containsString("Telemetry10ResourceAttributes".toLowerCase()));

        //This will be too variable to predict a sensible
        assertThat(output, containsString("process.executable.path"));
        //The oTel libraries say their code works for "almost all JDKs. playing it safe with this one"
        assertThat(output, containsString("process.pid"));

        //Locally this failed because it was reading outputs from two different JDKs.
        //While I know its because my environment is in a state from testing various JDKs
        //I'm cutting out the output to stop anyone else hitting similar issues.
        assertThat(output, containsString("process.runtime.name"));
        assertThat(output, containsString("process.runtime.version"));
        assertThat(output, containsString("process.runtime.description"));

        assertThat(output, containsString("telemetry.sdk.language=\"java\""));
        assertThat(output, containsString("telemetry.sdk.name=\"opentelemetry\""));
        //Not testing telemetry.sdk.version for the obvious reason

    }

    /**
     * Maps an OS Name in the same way that io.opentelemetry.instrumentation.resources.OsResource does
     */
    private String mapOSName(String osName) {
        for (String key : osNameMap.keySet()) {
            if (osName.startsWith(key)) {
                return osNameMap.get(key);
            }
        }
        return osName;
    }

}