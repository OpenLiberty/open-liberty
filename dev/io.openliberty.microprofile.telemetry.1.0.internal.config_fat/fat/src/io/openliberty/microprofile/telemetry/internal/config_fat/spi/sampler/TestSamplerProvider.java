/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.config_fat.spi.sampler;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;

public class TestSamplerProvider implements ConfigurableSamplerProvider {

    public static final String NAME = "test-sampler";

    /** {@inheritDoc} */
    @Override
    public Sampler createSampler(ConfigProperties config) {
        return new TestSampler();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return NAME;
    }

}
