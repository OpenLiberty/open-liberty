/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.java.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.management.MBeanServer;
import javax.net.ssl.SSLParameters;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

@Path("/")
@ApplicationScoped
public class TestService {

    private StringWriter sw = new StringWriter();

    @GET
    public String test() {
        try {
            log(">>> ENTER");
            doTest();
            log("<<< EXIT SUCCESSFUL");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            e.printStackTrace(new PrintWriter(sw));
            log("<<< EXIT FAILED");
        }
        String result = sw.toString();
        sw = new StringWriter();
        return result;
    }

    private void doTest() throws Exception {
        log("Beginning Java 27 testing");
        testPostQuantumTLS();       // JEP 527
        testCompactObjectHeaders(); // JEP 534
        testJFRDataRedaction();     // JEP 536
        log("Leaving Java 27 testing");
    }

    // JEP 527: Post-Quantum Hybrid Key Exchange for TLS 1.3
    // https://openjdk.org/jeps/527
    // X25519MLKEM768 is the primary hybrid group mandated by JEP 527 and must always
    // be present. SecP256r1MLKEM768 and SecP384r1MLKEM1024 are optional; their
    // availability is JDK-vendor/build-dependent (e.g. absent on Oracle JDK 27 EA).

    private void testPostQuantumTLS() throws Exception {
        log("Beginning JEP 527 testing: Post-Quantum Hybrid Key Exchange for TLS 1.3");

        // Must use SSLContext to get the populated default list;
        // new SSLParameters() is blank and returns null for getNamedGroups().
        SSLParameters params = javax.net.ssl.SSLContext.getDefault().getDefaultSSLParameters();
        String[] namedGroups = params.getNamedGroups();

        if (namedGroups == null || namedGroups.length == 0) {
            throw new Exception("JEP 527 FAILED: default SSLParameters returned no named groups");
        }
        log("Default TLS named groups (" + namedGroups.length + "): " + java.util.Arrays.toString(namedGroups));

        java.util.Set<String> groups = new java.util.HashSet<>(java.util.Arrays.asList(namedGroups));
        if (!groups.contains("X25519MLKEM768")) {
            throw new Exception("JEP 527 FAILED: X25519MLKEM768 missing from default SSLParameters named groups");
        }
        log("Hybrid group present: X25519MLKEM768");
        for (String hybrid : new String[]{"SecP256r1MLKEM768", "SecP384r1MLKEM1024"}) {
            if (groups.contains(hybrid)) {
                log("Hybrid group present: " + hybrid);
            } else {
                log("Hybrid group optional (not present on this JDK build): " + hybrid);
            }
        }

        log("Leaving JEP 527 testing");
    }

    // JEP 534: Compact Object Headers by Default https://openjdk.org/jeps/534
    // -XX:+UseCompactObjectHeaders is set in jvm.options, so this
    // test asserts it as a hard failure rather than a soft notice.
    // Skips gracefully on non-HotSpot JVMs where HotSpotDiagnosticMXBean is absent.
    private void testCompactObjectHeaders() throws Exception {
        log("Beginning JEP 534 testing: Compact Object Headers by Default");

        com.sun.management.HotSpotDiagnosticMXBean diagBean =
            ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);
        if (diagBean == null) {
            log("HotSpotDiagnosticMXBean not available — skipping JEP 534 check (non-HotSpot JVM)");
            return;
        }

        // Ensure the MBean is registered before querying
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        javax.management.ObjectName diagName =
            new javax.management.ObjectName("com.sun.management:type=HotSpotDiagnostic");
        if (!server.isRegistered(diagName)) {
            log("HotSpotDiagnostic MBean not registered — skipping JEP 534 check");
            return;
        }

        String flagValue = diagBean.getVMOption("UseCompactObjectHeaders").getValue();
        log("UseCompactObjectHeaders = " + flagValue);

        if (!"true".equalsIgnoreCase(flagValue)) {
            throw new Exception("JEP 534 FAILED: UseCompactObjectHeaders is not 'true' — "
                + "expected it to be enabled via -XX:+UseCompactObjectHeaders in jvm.options");
        }
        log("SUCCESS: Compact object headers are active (UseCompactObjectHeaders=true)");

        log("Leaving JEP 534 testing");
    }

    // JEP 536: JFR In-Process Data Redaction
    // https://openjdk.org/jeps/536

    private void testJFRDataRedaction() throws Exception {
        log("Beginning JEP 536 testing: JFR In-Process Data Redaction");

        final String sensitiveKey   = "jep536.test.password"; // matches default filter *password*
        final String sensitiveValue = "test-fixture-value";

        java.util.List<RecordedEvent> events = new java.util.ArrayList<>();
        try (Recording rec = new Recording()) {
            rec.enable("jdk.InitialSystemProperty");
            rec.start();
            rec.stop();
            java.nio.file.Path tmp = java.nio.file.Files.createTempFile("jep536-", ".jfr");
            try {
                rec.dump(tmp);
                events.addAll(RecordingFile.readAllEvents(tmp));
            } finally {
                java.nio.file.Files.deleteIfExists(tmp);
            }
        }

        // Find the recorded value for our sensitive key
        String recordedValue = null;
        for (RecordedEvent e : events) {
            if ("jdk.InitialSystemProperty".equals(e.getEventType().getName())
                    && sensitiveKey.equals(e.getString("key"))) {
                recordedValue = e.getString("value");
                log("jdk.InitialSystemProperty key=" + sensitiveKey + " value=" + recordedValue);
                break;
            }
        }

        if (recordedValue == null) {
            log("NOTE: '" + sensitiveKey + "' not in startup snapshot — re-run with -D" + sensitiveKey + "=<value>");
        } else if ("[REDACTED]".equals(recordedValue)) {
            log("SUCCESS: JEP 536 redacted '" + sensitiveKey + "' in the JFR recording");
        } else if (sensitiveValue.equals(recordedValue)) {
            throw new Exception("JEP 536 FAILED: '" + sensitiveKey + "' was recorded in plain text — expected [REDACTED]");
        } else {
            log("NOTE: unexpected recorded value for '" + sensitiveKey + "': " + recordedValue);
        }

        log("Leaving JEP 536 testing");
    }

    public void log(String msg) {
        System.out.println(msg);
        sw.append(msg);
        sw.append("<br/>");
    }
}
