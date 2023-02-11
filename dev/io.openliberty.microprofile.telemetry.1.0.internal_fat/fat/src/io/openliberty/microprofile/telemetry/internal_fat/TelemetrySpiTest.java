/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.spi.customizer.CustomizerTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.spi.customizer.TestCustomizer;
import io.openliberty.microprofile.telemetry.internal_fat.apps.spi.exporter.ExporterTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.spi.propagator.PropagatorTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.spi.propagator.TestPropagator;
import io.openliberty.microprofile.telemetry.internal_fat.apps.spi.propagator.TestPropagatorProvider;
import io.openliberty.microprofile.telemetry.internal_fat.apps.spi.resource.ResourceTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.spi.resource.TestResourceProvider;
import io.openliberty.microprofile.telemetry.internal_fat.apps.spi.sampler.SamplerTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.spi.sampler.TestSampler;
import io.openliberty.microprofile.telemetry.internal_fat.apps.spi.sampler.TestSamplerProvider;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;

/**
 * Test use of the Open Telemetry Autoconfigure Trace SPIs: https://www.javadoc.io/doc/io.opentelemetry/opentelemetry-sdk-extension-autoconfigure-spi/latest/index.html
 */
@RunWith(FATRunner.class)
public class TelemetrySpiTest extends FATServletClient {

    public static final String EXPORTER_APP_NAME = "exporterTest";
    public static final String SAMPLER_APP_NAME = "samplerTest";
    public static final String RESOURCE_APP_NAME = "resourceTest";
    public static final String PROPAGATOR_APP_NAME = "propagatorTest";
    public static final String CUSTOMIZER_APP_NAME = "customizerTest";

    @TestServlets({
                    @TestServlet(contextRoot = EXPORTER_APP_NAME, servlet = ExporterTestServlet.class),
                    @TestServlet(contextRoot = SAMPLER_APP_NAME, servlet = SamplerTestServlet.class),
                    @TestServlet(contextRoot = RESOURCE_APP_NAME, servlet = ResourceTestServlet.class),
                    @TestServlet(contextRoot = PROPAGATOR_APP_NAME, servlet = PropagatorTestServlet.class),
                    @TestServlet(contextRoot = CUSTOMIZER_APP_NAME, servlet = CustomizerTestServlet.class),
    })
    @Server("Telemetry10Spi")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Exporter test
        PropertiesAsset exporterConfig = new PropertiesAsset()
                        .addProperty("otel.traces.exporter", "in-memory");
        WebArchive exporterTestWar = ShrinkWrap.create(WebArchive.class, EXPORTER_APP_NAME + ".war")
                        .addClass(ExporterTestServlet.class)
                        .addClasses(InMemorySpanExporter.class, InMemorySpanExporterProvider.class)
                        .addPackage(TestSpans.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(exporterConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, exporterTestWar, SERVER_ONLY);

        // Sampler test
        PropertiesAsset samplerConfig = new PropertiesAsset()
                        .addProperty("otel.traces.sampler", TestSamplerProvider.NAME);
        WebArchive samplerTestWar = ShrinkWrap.create(WebArchive.class, SAMPLER_APP_NAME + ".war")
                        .addClass(SamplerTestServlet.class)
                        .addClasses(TestSampler.class, TestSamplerProvider.class)
                        .addAsServiceProvider(ConfigurableSamplerProvider.class, TestSamplerProvider.class)
                        .addAsResource(samplerConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, samplerTestWar, SERVER_ONLY);

        // Resource test
        // TEST_KEY1 and TEST_KEY2 are expected by TestResourceProvider
        PropertiesAsset resourceConfig = new PropertiesAsset()
                        .addProperty("otel.traces.exporter", "in-memory")
                        .addProperty(TestResourceProvider.TEST_KEY1.getKey(), ResourceTestServlet.TEST_VALUE1);
        server.addEnvVar("OTEL_TEST_KEY2", ResourceTestServlet.TEST_VALUE2);

        WebArchive resourceTestWar = ShrinkWrap.create(WebArchive.class, RESOURCE_APP_NAME + ".war")
                        .addClass(ResourceTestServlet.class)
                        .addClass(TestResourceProvider.class)
                        .addClasses(InMemorySpanExporter.class, InMemorySpanExporterProvider.class)
                        .addPackage(TestSpans.class.getPackage())
                        .addAsServiceProvider(ResourceProvider.class, TestResourceProvider.class)
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(resourceConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, resourceTestWar, SERVER_ONLY);

        // Propagator test
        PropertiesAsset propagatorConfig = new PropertiesAsset()
                        .addProperty("otel.traces.exporter", "in-memory")
                        .addProperty("otel.propagators", TestPropagatorProvider.NAME);

        WebArchive propagatorTestWar = ShrinkWrap.create(WebArchive.class, PROPAGATOR_APP_NAME + ".war")
                        .addClass(PropagatorTestServlet.class)
                        .addClasses(TestPropagator.class, TestPropagatorProvider.class)
                        .addClasses(InMemorySpanExporter.class, InMemorySpanExporterProvider.class)
                        .addPackage(TestSpans.class.getPackage())
                        .addAsServiceProvider(ConfigurablePropagatorProvider.class, TestPropagatorProvider.class)
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(propagatorConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, propagatorTestWar, SERVER_ONLY);

        // CustomizerTest
        PropertiesAsset customizerConfig = new PropertiesAsset()
                        .addProperty("otel.traces.exporter", "in-memory");
        WebArchive customizerTestWar = ShrinkWrap.create(WebArchive.class, CUSTOMIZER_APP_NAME + ".war")
                        .addClass(CustomizerTestServlet.class)
                        .addClass(TestCustomizer.class)
                        .addClasses(InMemorySpanExporter.class, InMemorySpanExporterProvider.class)
                        .addAsServiceProvider(AutoConfigurationCustomizerProvider.class, TestCustomizer.class)
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(customizerConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, customizerTestWar, SERVER_ONLY);

        server.addEnvVar("OTEL_SDK_DISABLED", "false");
        server.addEnvVar("OTEL_BSP_SCHEDULE_DELAY", "100");
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }
}
