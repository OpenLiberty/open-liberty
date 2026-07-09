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

import java.util.Optional;

import org.mcpjava.server.progress.Progress;
import org.mcpjava.server.progress.ProgressNotification.Builder;
import org.mcpjava.server.progress.ProgressToken;

/**
 * Minimal implementation of {@link Progress} for cases where no progress token
 * is expected to be specified by the MCP client. {@link #token()} always returns
 * an empty {@link java.util.Optional}, and the builder methods will throw
 * {@link IllegalStateException} if invoked.
 */
public class ProgressImpl implements Progress {

    @Override
    public Optional<ProgressToken> token() {
        return Optional.empty();
    }

    @Override
    public Builder notificationBuilder() {
        throw new IllegalStateException("No progress token present");
    }

    @Override
    public org.mcpjava.server.progress.ProgressTracker.Builder trackerBuilder() {
        throw new IllegalStateException("No progress token present");
    }
}
