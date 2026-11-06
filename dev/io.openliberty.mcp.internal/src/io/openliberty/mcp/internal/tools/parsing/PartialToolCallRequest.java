/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.tools.parsing;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.mcp.internal.ToolMetadata;

/**
 * Contains partial information about a tool cool, which is needed for metrics and error reporting
 * <p>
 * All fields may be {@code null}.
 *
 * @param toolName the tool name requested, which may not be valid
 * @param tool the tool called, if valid, otherwise {@code null}
 */
@Trivial
public record PartialToolCallRequest(String toolName,
                                     ToolMetadata tool) {}
