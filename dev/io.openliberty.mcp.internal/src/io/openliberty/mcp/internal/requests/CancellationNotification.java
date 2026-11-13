/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

/**
 * A request to cancel a call
 *
 * @param request the underlying request
 * @param idToCancel the ID of the request to cancel
 * @param reason the reason for cancellation, may be {@code null} if the client did not send a reason
 */
public record CancellationNotification(McpOperationRequest request, RequestId idToCancel, String reason) {

}
