/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.deploymentErrorApps;

import org.mcpjava.server.Cancellation;
import org.mcpjava.server.tools.Tool;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DuplicateSpecialArgsErrorTest {

    ///////////////////
    //// Duplicate cancellation
    @Tool(name = "duplicateCancellation")
    public void duplicateCancellation(Cancellation cancellation, Cancellation duplicateCancellation) {}

}
