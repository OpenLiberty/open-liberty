/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
/**
 * Classes for generating schemas for tools and classes.
 * <p>
 * Attempts to generate schemas to match the way that JSON-B will (de-)serialize objects.
 * <p>
 * Main entry point is {@link io.openliberty.mcp.internal.schemas.SchemaRegistry}.
 */
@com.ibm.websphere.ras.annotation.TraceOptions(traceGroup = "MCP")
package io.openliberty.mcp.internal.schemas;