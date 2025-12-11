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
package io.openliberty.microprofile.telemetry.internal.config_fat.spi.resource;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

public class TestResourceProvider implements ResourceProvider {

    public static final AttributeKey<String> TEST_KEY1 = AttributeKey.stringKey("otel.test.key1");
    public static final AttributeKey<String> TEST_KEY2 = AttributeKey.stringKey("otel.test.key2");

    /** {@inheritDoc} */
    @Override
    public Resource createResource(ConfigProperties config) {
        System.out.println("createResource called with config: " + config);
        // Read two test values from config and add them
        return Resource.builder()
                        .put(TEST_KEY1, config.getString(TEST_KEY1.getKey()))
                        .put(TEST_KEY2, config.getString(TEST_KEY2.getKey()))
                        .build();
    }

}
