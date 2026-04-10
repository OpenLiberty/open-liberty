/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import com.ibm.websphere.csi.J2EEName;

import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import jakarta.json.bind.Jsonb;

/**
 * Manages tool registries, maintaining one registry per module.
 */
public class ToolRegistries extends AbstractModuleScopedStore<ToolRegistry> {

    private final SchemaRegistry schemaRegistry;
    private final Jsonb jsonb;

    public ToolRegistries(SchemaRegistry schemaRegistry, Jsonb jsonb) {
        this.schemaRegistry = schemaRegistry;
        this.jsonb = jsonb;
    }

    @Override
    protected ToolRegistry createInstance(J2EEName moduleName) {
        return new ToolRegistry(schemaRegistry, jsonb);
    }
}