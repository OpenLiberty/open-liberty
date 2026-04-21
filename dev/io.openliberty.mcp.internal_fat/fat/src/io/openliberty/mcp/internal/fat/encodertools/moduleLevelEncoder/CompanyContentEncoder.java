/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.encodertools.moduleLevelEncoder;

import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.content.TextContent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * Encoder for Company objects.
 * This encoder is ONLY available in war1WithInitializedEvent module.
 */
@ApplicationScoped
public class CompanyContentEncoder implements ContentEncoder<Company> {

    private static final Jsonb jsonb = JsonbBuilder.create();

    @Override
    public boolean supports(Class<?> runtimeType) {
        return Company.class.isAssignableFrom(runtimeType);
    }

    @Override
    public Content encode(Company company) {
        Company encoded = new Company(
                                      company.employees(),
                                      company.industry(),
                                      company.name() + " (encoded by War1)");
        return new TextContent(jsonb.toJson(encoded));
    }
}
