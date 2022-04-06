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
package org.test.json.b;

import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
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

            JsonbBuilder builder = provider.create();

            return builder;
        } catch (Exception x) {
            throw new JsonbException(x.getMessage(), x);
        }
    }
}
