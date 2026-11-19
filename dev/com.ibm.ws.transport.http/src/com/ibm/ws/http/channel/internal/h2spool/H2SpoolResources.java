/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.h2spool;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Aggregate ordinary-H2 spool budget shared by endpoint connections.
 * Directory and executor lifecycle belong to the caller.
 */
public final class H2SpoolResources {
    // A body retains its slot through file deletion until its reader
    // retires.
    private final Path directory;
    private final long maxStoredBytes;
    private final long maxPendingBytes;
    private final int maxOpenBodies;
    private final Executor writer;
    private final Executor cleanupExecutor;
    private final Set<Lease> active = new HashSet<>();

    private long reservedStoredBytes;
    private long reservedPendingBytes;
    private boolean closed;

    /**
     * No filesystem operation occurs here. The caller establishes a private
     * directory and keeps the off-loop cleanup executor accepting until
     * {@link #close()} settles.
     */
    public H2SpoolResources(Path directory, long maxStoredBytes, long maxPendingBytes, int maxOpenBodies,
                            Executor writer, Executor cleanupExecutor) {
        if (directory == null || !directory.isAbsolute() || writer == null || cleanupExecutor == null || writer == cleanupExecutor
                        || maxStoredBytes < 0 || maxPendingBytes <= 0 || maxOpenBodies <= 0) {
            throw new IllegalArgumentException("Invalid HTTP/2 spool resource budget");
        }
        this.directory = directory;
        this.maxStoredBytes = maxStoredBytes;
        this.maxPendingBytes = maxPendingBytes;
        this.maxOpenBodies = maxOpenBodies;
        this.writer = writer;
        this.cleanupExecutor = cleanupExecutor;
    }

    /** Opens a channel-local lifetime within the shared endpoint budget. */
    public synchronized ConnectionScope openConnection() {
        if (closed) {
            throw new IllegalStateException("HTTP/2 spool resources are closed");
        }
        return new ConnectionScope();
    }

    public final class ConnectionScope {
        private final Set<Lease> leases = new HashSet<>();
        private boolean connectionClosed;

        /** Reserves one body slot without filesystem work; null if the endpoint owner has closed. */
        public H2BodySpool tryOpenStream(long streamStoredLimit, int streamPendingBytes, int maxDataFrameBytes,
                                         BiConsumer<Integer, Throwable> onChunkSettled) throws CapacityException {
            if (streamStoredLimit < 0 || streamPendingBytes < maxDataFrameBytes || maxDataFrameBytes <= 0 || onChunkSettled == null) {
                throw new IllegalArgumentException("Invalid HTTP/2 stream spool capacity");
            }
            synchronized (H2SpoolResources.this) {
                if (connectionClosed) {
                    throw new CapacityException("HTTP/2 connection spool is closed");
                }
                if (closed) {
                    return null; // no lease or file ownership transferred
                }
                if (maxPendingBytes < maxDataFrameBytes) {
                    throw new CapacityException("HTTP/2 endpoint pending quota cannot hold one DATA frame");
                }
                if (active.size() >= maxOpenBodies) {
                    throw new CapacityException("HTTP/2 body slot quota exceeded");
                }
                Lease lease = new Lease(this);
                H2BodySpool spool = new H2BodySpool(directory, streamStoredLimit, streamPendingBytes, maxDataFrameBytes,
                                writer, cleanupExecutor, lease, onChunkSettled);
                lease.spool = spool;
                active.add(lease);
                leases.add(lease);
                return spool;
            }
        }

        /** Reports this cleanup attempt; a later call retries retained leases after failure. */
        public CompletionStage<Void> close() {
            List<Lease> snapshot;
            synchronized (H2SpoolResources.this) {
                connectionClosed = true;
                snapshot = new ArrayList<>(leases);
            }
            return terminate(snapshot);
        }
    }

    private CompletionStage<Void> terminate(List<Lease> snapshot) {
        if (snapshot.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        // Report a failed deletion without waiting for an unrelated application
        // reader; the provider can then retry the retained failed lease.
        CompletableFuture<Void> result = new CompletableFuture<>();
        AtomicInteger remaining = new AtomicInteger(snapshot.size());
        for (Lease lease : snapshot) {
            lease.terminate().whenComplete((ignored, failure) -> {
                if (failure != null) {
                    result.completeExceptionally(failure);
                } else if (remaining.decrementAndGet() == 0) {
                    result.complete(null);
                }
            });
        }
        return result;
    }

    /** Stops admissions and unclaimed bodies; waits for application-owned readers to close. */
    public CompletionStage<Void> close() {
        List<Lease> snapshot;
        synchronized (this) {
            closed = true;
            snapshot = new ArrayList<>(active);
        }
        return terminate(snapshot);
    }

    /** A stream's exact aggregate reservation, retained through file deletion. */
    final class Lease {
        private final ConnectionScope scope;
        private long storedBytes;
        private long pendingBytes;
        private boolean released;
        private boolean fileRetired;
        private boolean readLifetimeActive;
        private boolean applicationOwned;
        private boolean closeRequested;
        private H2BodySpool spool;
        private H2SealedBody body;
        private final CompletableFuture<H2SealedBody> handoff = new CompletableFuture<>();
        private final CompletableFuture<Void> applicationCloseRequested = new CompletableFuture<>();
        private final CompletableFuture<Void> fullyRetired = new CompletableFuture<>();

        private Lease(ConnectionScope scope) {
            this.scope = scope;
        }

        boolean reserve(long storage, int pending) {
            synchronized (H2SpoolResources.this) {
                if (closed || scope.connectionClosed || released || fileRetired || storage < 0 || pending < 0 ||
                    storage > maxStoredBytes - reservedStoredBytes || pending > maxPendingBytes - reservedPendingBytes) {
                    return false;
                }
                reservedStoredBytes += storage;
                reservedPendingBytes += pending;
                storedBytes += storage;
                pendingBytes += pending;
                return true;
            }
        }

        void releasePending(long count) {
            synchronized (H2SpoolResources.this) {
                if (count < 0 || count > pendingBytes) {
                    throw new IllegalStateException("Invalid HTTP/2 spool pending reservation release");
                }
                pendingBytes -= count;
                reservedPendingBytes -= count;
            }
        }

        void releaseOffer(long storage, long pending) {
            synchronized (H2SpoolResources.this) {
                if (storage < 0 || storage > storedBytes || pending < 0 || pending > pendingBytes) {
                    throw new IllegalStateException("Invalid HTTP/2 spool reservation rollback");
                }
                storedBytes -= storage;
                reservedStoredBytes -= storage;
                pendingBytes -= pending;
                reservedPendingBytes -= pending;
            }
        }

        /** False means the connection closed before the handle was published. */
        boolean onSealed(H2SealedBody sealedBody) {
            boolean connectionOpen;
            synchronized (H2SpoolResources.this) {
                body = sealedBody;
                spool = null;
                connectionOpen = !closed && !scope.connectionClosed;
            }
            handoff.complete(sealedBody);
            return connectionOpen;
        }

        boolean claimForApplication(H2SealedBody sealedBody) {
            synchronized (H2SpoolResources.this) {
                if (closed || scope.connectionClosed || released || closeRequested || applicationOwned || body != sealedBody) {
                    return false;
                }
                applicationOwned = true;
                scope.leases.remove(this);
                return true;
            }
        }

        void onCloseRequested() {
            synchronized (H2SpoolResources.this) {
                closeRequested = true;
            }
            applicationCloseRequested.complete(null);
        }

        boolean beginReadLifetime(H2SealedBody sealedBody) {
            synchronized (H2SpoolResources.this) {
                if (released || fileRetired || closeRequested || body != sealedBody || readLifetimeActive) {
                    return false;
                }
                readLifetimeActive = true;
                return true;
            }
        }

        void retireReadLifetime() {
            boolean retired;
            synchronized (H2SpoolResources.this) {
                if (!readLifetimeActive) {
                    return;
                }
                readLifetimeActive = false;
                retired = retireIfDone();
            }
            if (retired) {
                fullyRetired.complete(null);
            }
        }

        void releaseAfterDelete() {
            boolean retired;
            synchronized (H2SpoolResources.this) {
                if (fileRetired) {
                    return;
                }
                fileRetired = true;
                reservedStoredBytes -= storedBytes;
                reservedPendingBytes -= pendingBytes;
                storedBytes = 0;
                pendingBytes = 0;
                spool = null;
                body = null;
                retired = retireIfDone();
            }
            if (retired) {
                fullyRetired.complete(null);
            }
        }

        /** Called only while holding the aggregate owner's monitor. */
        private boolean retireIfDone() {
            if (!released && fileRetired && !readLifetimeActive) {
                released = true;
                active.remove(this);
                scope.leases.remove(this);
                return true;
            }
            return false;
        }

        private CompletionStage<Void> terminate() {
            H2BodySpool currentSpool;
            H2SealedBody currentBody;
            boolean ownedByApplication;
            boolean deleted;
            synchronized (H2SpoolResources.this) {
                if (released) {
                    return CompletableFuture.completedFuture(null);
                }
                currentSpool = spool;
                currentBody = body;
                ownedByApplication = applicationOwned;
                deleted = fileRetired;
            }
            if (ownedByApplication) {
                // Application EOF/close/cancel owns the first close. A later root
                // close retries deletion only after that close has been requested.
                return applicationCloseRequested.thenCompose(ignored -> {
                    H2SealedBody closingBody;
                    synchronized (H2SpoolResources.this) {
                        if (released) {
                            return CompletableFuture.completedFuture(null);
                        }
                        closingBody = body;
                    }
                    return closingBody == null ? fullyRetired : closingBody.closeAsync().thenCompose(done -> fullyRetired);
                });
            }
            if (currentBody != null) {
                return currentBody.closeAsync().thenCompose(done -> fullyRetired);
            }
            if (currentSpool == null) {
                return deleted ? fullyRetired : handoff.thenCompose(H2SealedBody::closeAsync).thenCompose(done -> fullyRetired);
            }
            return currentSpool.abort(new IOException("HTTP/2 connection closed")).thenCompose(ignored -> {
                H2SealedBody sealedBody;
                synchronized (H2SpoolResources.this) {
                    if (released) {
                        return CompletableFuture.completedFuture(null);
                    }
                    sealedBody = body;
                }
                if (sealedBody != null) {
                    return sealedBody.closeAsync().thenCompose(done -> fullyRetired);
                }
                // abort raced with seal's ownership transfer; wait for onSealed.
                return handoff.thenCompose(H2SealedBody::closeAsync).thenCompose(done -> fullyRetired);
            });
        }
    }

    public static final class CapacityException extends Exception {
        private static final long serialVersionUID = 1L;

        CapacityException(String message) {
            super(message);
        }
    }
}
