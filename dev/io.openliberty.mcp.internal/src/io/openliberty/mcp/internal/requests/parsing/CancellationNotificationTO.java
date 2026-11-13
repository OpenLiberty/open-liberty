/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests.parsing;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.mcp.internal.requests.RequestId;

/**
 * Transfer object for a {@code notifications/cancelled} message.
 */
@Trivial
public record CancellationNotificationTO(RequestId requestId, String reason) {}
