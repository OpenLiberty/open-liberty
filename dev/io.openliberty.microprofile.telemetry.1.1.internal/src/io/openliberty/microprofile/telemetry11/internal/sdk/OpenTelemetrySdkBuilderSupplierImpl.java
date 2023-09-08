/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry11.internal.sdk;

import org.osgi.service.component.annotations.Component;

import io.openliberty.microprofile.telemetry.internal.common.OpenTelemetryInfoFactory;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;

/**
 * This class contains version specific API calls to AutoConfiguredOpenTelemetrySdk.builder()
 */
@Component
public class OpenTelemetrySdkBuilderSupplierImpl implements OpenTelemetryInfoFactory.OpenTelemetrySdkBuilderSupplier {

    @Override
    public AutoConfiguredOpenTelemetrySdkBuilder getPartiallyConfiguredOpenTelemetrySDKBuilder() {
        return AutoConfiguredOpenTelemetrySdk.builder()
                        .disableShutdownHook();
    }
}
