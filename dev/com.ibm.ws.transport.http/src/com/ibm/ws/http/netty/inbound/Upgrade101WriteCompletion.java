/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.inbound;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.pipeline.inbound.HttpDispatcherHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AsciiString;
import io.netty.util.AttributeKey;

/** Observes the actual 101 write before notifying the upgrade pipeline. */
public final class Upgrade101WriteCompletion {
    private static final AttributeKey<Boolean> UPGRADE_COMMIT_EVENT_FIRED = AttributeKey.valueOf("upgradeCommitFired");

    private Upgrade101WriteCompletion() {
    }

    public static boolean isUpgrade101(Object message) {
        if (!(message instanceof HttpResponse)) {
            return false;
        }
        HttpResponse response = (HttpResponse) message;
        if (!response.status().equals(HttpResponseStatus.SWITCHING_PROTOCOLS)) {
            return false;
        }
        CharSequence connection = response.headers().get(HttpHeaderNames.CONNECTION);
        CharSequence upgrade = response.headers().get(HttpHeaderNames.UPGRADE);
        return connection != null && upgrade != null && upgrade.length() > 0
                        && AsciiString.containsIgnoreCase(connection, "upgrade");
    }

    public static void ensureReadinessPromise(Channel channel) {
        if (channel.attr(NettyHttpConstants.UPGRADE_READY_PROMISE).get() == null) {
            channel.attr(NettyHttpConstants.UPGRADE_READY_PROMISE).set(new CompletableFuture<>());
        }
    }

    public static void observe(Channel channel, ChannelFuture future) {
        future.addListener((ChannelFutureListener) completed -> {
            if (completed.isSuccess()) {
                if (!Boolean.TRUE.equals(channel.attr(UPGRADE_COMMIT_EVENT_FIRED).get())) {
                    channel.attr(UPGRADE_COMMIT_EVENT_FIRED).set(Boolean.TRUE);
                    channel.pipeline().fireUserEventTriggered(HttpDispatcherHandler.UPGRADE_101_COMMITTED_EVENT);
                }
            } else {
                CompletableFuture<Void> promise = channel.attr(NettyHttpConstants.UPGRADE_READY_PROMISE).get();
                if (promise != null) {
                    promise.completeExceptionally(completed.cause() != null ? completed.cause() : new IOException("101 response write failed"));
                }
            }
        });
    }
}
