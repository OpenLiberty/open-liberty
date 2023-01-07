/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package org.test.json.b;

import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.config.PropertyNamingStrategy;
import jakarta.json.bind.spi.JsonbProvider;

/**
 * A fake JSON-B provider that delegates to Yasson.
 */
public class FakeProvider extends JsonbProvider {
    @Override
    public JsonbBuilder create() {
        try {
            JsonbProvider provider = (JsonbProvider) Class.forName("org.eclipse.yasson.JsonBindingProvider")
                            .getConstructor()
                            .newInstance();

            // Change some defaults so that tests can know this is the fake provider:
            JsonbConfig config = new JsonbConfig()
                            .setProperty(JsonbConfig.CREATOR_PARAMETERS_REQUIRED, true)
                            .setProperty(JsonbConfig.PROPERTY_NAMING_STRATEGY, PropertyNamingStrategy.LOWER_CASE_WITH_DASHES);

            JsonbBuilder builder = provider.create().withConfig(config);

            return builder;
        } catch (Exception x) {
            throw new JsonbException(x.getMessage(), x);
        }
    }
}
