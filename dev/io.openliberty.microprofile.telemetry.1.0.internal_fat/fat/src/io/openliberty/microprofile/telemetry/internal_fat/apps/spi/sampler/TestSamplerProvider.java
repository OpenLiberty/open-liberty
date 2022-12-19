/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.spi.sampler;

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
