/*
 *******************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.isolation;

import javax.enterprise.inject.spi.CDI;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;

public class PullExporterAutoConfigurationCustomizerProvider implements AutoConfigurationCustomizerProvider {

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
        autoConfiguration.addMeterProviderCustomizer(this::registerMeterProvider);
    }

    private SdkMeterProviderBuilder registerMeterProvider(SdkMeterProviderBuilder builder,
                                                          ConfigProperties properties) {
        InMemoryMetricReader exporter = CDI.current().select(InMemoryMetricReader.class).get();
        builder.registerMetricReader(exporter);
        return builder;
    }

}