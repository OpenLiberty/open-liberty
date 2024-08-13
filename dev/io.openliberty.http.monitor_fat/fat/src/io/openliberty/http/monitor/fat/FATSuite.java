/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(Suite.class)
@MinimumJavaLevel(javaLevel = 11)
@SuiteClasses({
                NoAppTest.class,
                JSPApplicationTest.class,
                RestApplicationTest.class,
                ServletApplicationTest.class,
                ContainerServletApplicationTest.class,
                ContainerJSPApplicationTest.class,
                ContainerRestApplicationTest.class,
                ContainerNoAppTest.class
})

public class FATSuite extends TestContainerSuite {

    public static RepeatTests allMPRepeatsWithMPTel20OrLater(String serverName) {
        return TelemetryActions
                        .repeat(serverName, MicroProfileActions.MP70_EE11, MicroProfileActions.MP70_EE11_APP_MODE, MicroProfileActions.MP70_EE10,
                                TelemetryActions.MP50_MPTEL20, TelemetryActions.MP41_MPTEL20, TelemetryActions.MP14_MPTEL20);
    }

    public static RepeatTests testRepeatMPTMetrics5(String serverName) {
        return RepeatTests.with(FeatureReplacementAction.EE11_FEATURES())
                        .andWith(FeatureReplacementAction.EE10_FEATURES().fullFATOnly());

    }

    //If we're in the app mode repeat this returns an updated Web Archive, otherwise it sets environment properties
    public static WebArchive setTelProperties(WebArchive archive, LibertyServer server) {
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP70_EE11_APP_MODE.getID())) {
            return archive.addAsManifestResource(new File("publish/resources/META-INF/microprofile-config.properties"),
                                                 "microprofile-config.properties");
        } else {
            server.addEnvVar("OTEL_METRICS_EXPORTER", "otlp");
            server.addEnvVar("OTEL_SDK_DISABLED", "false");
            server.addEnvVar("OTEL_TRACES_EXPORTER", "none");
            server.addEnvVar("OTEL_LOGS_EXPORTER", "none");
            server.addEnvVar("OTEL_METRIC_EXPORT_INTERVAL", "200");
            return archive;
        }
    }

    public static String getAppNameOrUnknownService(String appName) {
        if (RepeatTestFilter.isAnyRepeatActionActive(MicroProfileActions.MP70_EE11_APP_MODE.getID())) {
            return appName;
        } else {
            return "unknown_service";
        }
    }
}
