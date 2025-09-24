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

import java.util.List;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

/**
 * A test sampler which looks for the "test.sample.me" attribute to decide whether a span should be sampled
 */
public class TestSampler implements Sampler {

    public static final AttributeKey<Boolean> SAMPLE_ME = AttributeKey.booleanKey("test.sample.me");

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Test sampler, samples if test.sample.me is true";
    }

    /** {@inheritDoc} */
    @Override
    public SamplingResult shouldSample(Context context, String traceId, String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {

        if (attributes.get(SAMPLE_ME) == Boolean.TRUE) {
            return SamplingResult.recordAndSample();
        } else {
            return SamplingResult.drop();
        }
    }

}
