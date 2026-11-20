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

import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.json.JsonObject;
import jakarta.json.bind.annotation.JsonbProperty;

@Trivial
public record ToolCallRequestTO(String name,
                                JsonObject arguments,
                                @JsonbProperty("_meta") Map<String, Object> metadata) {

}
