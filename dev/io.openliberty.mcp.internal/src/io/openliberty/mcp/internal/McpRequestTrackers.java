/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import com.ibm.websphere.csi.J2EEName;

import io.openliberty.mcp.internal.config.McpConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Manages request trackers, maintaining one tracker per module.
 */
@ApplicationScoped
public class McpRequestTrackers extends AbstractModuleScopedStore<McpRequestTracker> {

    @Inject
    McpConfig mcpConfig;

    @Override
    protected McpRequestTracker createInstance(J2EEName moduleName) {
        return new McpRequestTracker(mcpConfig);
    }
}