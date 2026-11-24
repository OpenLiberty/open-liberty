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
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Ordinary HTTP/2 spool budget for one active owner. Created before channel startup;
 * no directory or executor creation is allowed on a channel event loop.
 */
public final class H2SpoolProvider {
    // Ordinary requests are published after END_STREAM, so a finite aggregate
    // stored-byte limit can reject a request that MessageSizeLimit permits.
    // Keep the finite constructor budget for callers that explicitly need it.
    public static final long DEFAULT_MAX_STORED_BYTES = Long.MAX_VALUE;
    public static final long MAX_PENDING_BYTES = 64L * 1024 * 1024;
    public static final int MAX_OPEN_BODIES = Integer.MAX_VALUE;
    private static final int MAX_CLOSE_ATTEMPTS = 4;
    private static final long CLOSE_RETRY_MILLIS = 250;
    private static final String DIRECTORY_PREFIX = "liberty-h2-body-v1-";
    private static final String LOCK_SUFFIX = ".lock";
    private static final String PENDING_SUFFIX = ".pending";
    // Stable across bundle classloaders in this JVM; a collision only skips cleanup.
    private static final String PROCESS_PREFIX = DIRECTORY_PREFIX
                    + Long.toHexString(ManagementFactory.getRuntimeMXBean().getStartTime()) + "-"
                    + Integer.toHexString(ManagementFactory.getRuntimeMXBean().getName().hashCode()) + "-";
    private static final Pattern OWNER_FILE_NAME = Pattern.compile(
                    "liberty-h2-body-v1-[0-9a-f]{1,16}-[0-9a-f]{1,8}-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.lock");

    private static final Set<PosixFilePermission> PRIVATE_DIRECTORY = PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> PRIVATE_FILE = PosixFilePermissions.fromString("rw-------");
    // A second channel to a locked file can release this JVM's lock when closed.
    private static final Object OWNER_REGISTRY_LOCK = new Object();
    private static final Set<Path> OPEN_OWNER_FILES = new HashSet<>();

    private final Path directory;
    private final Path ownerFile;
    private final FileChannel ownerChannel;
    private final FileLock ownerLock;
    private final ExecutorService writer;
    private final ExecutorService cleanup;
    private final H2SpoolResources resources;
    private CompletableFuture<Void> closing;

    public H2SpoolProvider(long maxStoredBytes) throws IOException {
        if (maxStoredBytes <= 0) {
            throw new IllegalArgumentException("HTTP/2 body spool storage limit must be positive");
        }
        Path tempRoot = Paths.get(System.getProperty("java.io.tmpdir")).toRealPath();
        String name = PROCESS_PREFIX + UUID.randomUUID();
        Path privateDirectory = tempRoot.resolve(name);
        Path privateOwnerFile = tempRoot.resolve(name + LOCK_SUFFIX);
        Path pendingOwnerFile = tempRoot.resolve(name + PENDING_SUFFIX);
        FileChannel privateOwnerChannel = null;
        FileLock privateOwnerLock = null;
        boolean pendingFileCreated = false;
        boolean ownerFilePublished = false;
        boolean directoryCreated = false;
        ExecutorService writerExecutor = null;
        ExecutorService cleanupExecutor = null;
        try {
            // Publish the marker only after the inode is locked. The pending
            // name is deliberately outside the stale-directory scan pattern.
            privateOwnerChannel = FileChannel.open(pendingOwnerFile,
                            EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE),
                            PosixFilePermissions.asFileAttribute(PRIVATE_FILE));
            pendingFileCreated = true;
            privateOwnerLock = privateOwnerChannel.tryLock();
            if (privateOwnerLock == null) {
                throw new IOException("Cannot lock HTTP/2 body spool owner file");
            }
            synchronized (OWNER_REGISTRY_LOCK) {
                if (OPEN_OWNER_FILES.contains(privateOwnerFile)) {
                    throw new IOException("HTTP/2 body spool owner file already exists");
                }
                OPEN_OWNER_FILES.add(privateOwnerFile);
                try {
                    // A hard link publishes the already-locked inode atomically
                    // and fails rather than replacing an existing owner file.
                    Files.createLink(privateOwnerFile, pendingOwnerFile);
                    ownerFilePublished = true;
                    Files.delete(pendingOwnerFile);
                } catch (IOException | RuntimeException | Error failure) {
                    if (!ownerFilePublished) {
                        OPEN_OWNER_FILES.remove(privateOwnerFile);
                    }
                    throw failure;
                }
            }
            Files.createDirectory(privateDirectory, PosixFilePermissions.asFileAttribute(PRIVATE_DIRECTORY));
            directoryCreated = true;
            UserPrincipal owner = Files.getOwner(privateOwnerFile, LinkOption.NOFOLLOW_LINKS);
            if (!Files.getPosixFilePermissions(privateOwnerFile, LinkOption.NOFOLLOW_LINKS).equals(PRIVATE_FILE)
                            || !Files.getOwner(privateDirectory, LinkOption.NOFOLLOW_LINKS).equals(owner)
                            || !Files.getPosixFilePermissions(privateDirectory).equals(PRIVATE_DIRECTORY)
                            || !Files.isWritable(privateDirectory)
                            || Files.getFileStore(privateDirectory).getUsableSpace() == 0) {
                throw new IOException("HTTP/2 body spool directory is not private and writable");
            }
            probe(privateDirectory);
            writerExecutor = Executors.newFixedThreadPool(4, namedThreads("liberty-h2-body-writer-"));
            cleanupExecutor = Executors.newSingleThreadExecutor(namedThreads("liberty-h2-body-cleanup-"));
            this.resources = new H2SpoolResources(privateDirectory, maxStoredBytes, MAX_PENDING_BYTES,
                            MAX_OPEN_BODIES, writerExecutor, cleanupExecutor);
            // Recovery may scan many old bodies; it must not starve live lease cleanup.
            namedThreads("liberty-h2-body-recovery-").newThread(() -> cleanStaleDirectories(tempRoot, owner)).start();
        } catch (IOException | RuntimeException | Error failure) {
            if (writerExecutor != null) {
                writerExecutor.shutdownNow();
            }
            if (cleanupExecutor != null) {
                cleanupExecutor.shutdownNow();
            }
            boolean directoryRemoved = !directoryCreated;
            if (directoryCreated) {
                try {
                    Files.deleteIfExists(privateDirectory);
                    directoryRemoved = true;
                } catch (IOException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            synchronized (OWNER_REGISTRY_LOCK) {
                try {
                    if (privateOwnerLock != null && privateOwnerLock.isValid()) {
                        privateOwnerLock.release();
                    }
                } catch (IOException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
                try {
                    if (privateOwnerChannel != null) {
                        privateOwnerChannel.close();
                    }
                } catch (IOException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
                if (privateOwnerChannel == null || !privateOwnerChannel.isOpen()) {
                    OPEN_OWNER_FILES.remove(privateOwnerFile);
                }
                if (directoryRemoved) {
                    try {
                        if (ownerFilePublished) {
                            Files.deleteIfExists(privateOwnerFile);
                        }
                    } catch (IOException cleanupFailure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                }
                if (pendingFileCreated) {
                    try {
                        Files.deleteIfExists(pendingOwnerFile);
                    } catch (IOException cleanupFailure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                }
            }
            throw failure;
        }
        this.directory = privateDirectory;
        this.ownerFile = privateOwnerFile;
        this.ownerChannel = privateOwnerChannel;
        this.ownerLock = privateOwnerLock;
        this.writer = writerExecutor;
        this.cleanup = cleanupExecutor;
    }

    /** Reclaim only directories whose cooperative owner is no longer alive. */
    private static void cleanStaleDirectories(Path tempRoot, UserPrincipal owner) {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(tempRoot, DIRECTORY_PREFIX + "*" + LOCK_SUFFIX)) {
            for (Path candidate : files) {
                String candidateName = candidate.getFileName().toString();
                if (!OWNER_FILE_NAME.matcher(candidateName).matches() || candidateName.startsWith(PROCESS_PREFIX)) {
                    continue;
                }
                try {
                    cleanStaleDirectory(tempRoot, candidate, owner);
                } catch (IOException | RuntimeException failure) {
                    FFDCFilter.processException(failure, H2SpoolProvider.class.getName() + ".cleanStaleDirectory", "1");
                }
            }
        } catch (IOException | RuntimeException failure) {
            FFDCFilter.processException(failure, H2SpoolProvider.class.getName() + ".cleanStaleDirectories", "1");
        }
    }

    private static void cleanStaleDirectory(Path tempRoot, Path lockFile, UserPrincipal owner) throws IOException {
        if (!isPrivateFile(lockFile, owner)) {
            return;
        }
        FileChannel lockChannel;
        FileLock lock = null;
        synchronized (OWNER_REGISTRY_LOCK) {
            if (OPEN_OWNER_FILES.contains(lockFile)) {
                return;
            }
            lockChannel = FileChannel.open(lockFile, StandardOpenOption.READ,
                            StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            try {
                try {
                    lock = lockChannel.tryLock();
                } catch (OverlappingFileLockException liveOwner) {
                    return;
                }
                if (lock == null) {
                    return;
                }
                OPEN_OWNER_FILES.add(lockFile);
            } finally {
                if (lock == null) {
                    lockChannel.close();
                }
            }
        }
        boolean removeLockFile = false;
        try {
            if (!isPrivateFile(lockFile, owner)) {
                return;
            }
            String lockName = lockFile.getFileName().toString();
            Path staleDirectory = tempRoot.resolve(lockName.substring(0, lockName.length() - LOCK_SUFFIX.length()));
            try {
                if (!Files.readAttributes(staleDirectory, java.nio.file.attribute.BasicFileAttributes.class,
                                LinkOption.NOFOLLOW_LINKS).isDirectory()) {
                    return;
                }
            } catch (NoSuchFileException alreadyRemoved) {
                removeLockFile = true;
            }
            if (!removeLockFile) {
                if (!Files.getOwner(staleDirectory, LinkOption.NOFOLLOW_LINKS).equals(owner)
                                || !Files.getPosixFilePermissions(staleDirectory, LinkOption.NOFOLLOW_LINKS).equals(PRIVATE_DIRECTORY)) {
                    return;
                }
                try (DirectoryStream<Path> contents = Files.newDirectoryStream(staleDirectory)) {
                    for (Path file : contents) {
                        String fileName = file.getFileName().toString();
                        if (!(fileName.startsWith("h2-body-") || fileName.startsWith("probe-"))
                                        || !fileName.endsWith(".tmp") || !isPrivateFile(file, owner)) {
                            return; // unknown content is not ours to delete
                        }
                        Files.deleteIfExists(file);
                    }
                }
                Files.deleteIfExists(staleDirectory);
                removeLockFile = true;
            }
            if (removeLockFile) {
                // Unlink under the lock so another scan cannot act on a newly
                // created marker with the same name after this lock is released.
                Files.deleteIfExists(lockFile);
            }
        } finally {
            synchronized (OWNER_REGISTRY_LOCK) {
                try {
                    lock.release();
                } finally {
                    try {
                        lockChannel.close();
                    } finally {
                        if (!lockChannel.isOpen()) {
                            OPEN_OWNER_FILES.remove(lockFile);
                        }
                    }
                }
            }
        }
    }

    private static boolean isPrivateFile(Path file, UserPrincipal owner) throws IOException {
        return Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                        && Files.getOwner(file, LinkOption.NOFOLLOW_LINKS).equals(owner)
                        && Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS).equals(PRIVATE_FILE);
    }

    private static void probe(Path directory) throws IOException {
        Path file = Files.createTempFile(directory, "probe-", ".tmp",
                        PosixFilePermissions.asFileAttribute(PRIVATE_FILE));
        try {
            if (!Files.getPosixFilePermissions(file).equals(PRIVATE_FILE)) {
                throw new IOException("HTTP/2 body spool file is not private");
            }
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
                ByteBuffer byteToWrite = ByteBuffer.wrap(new byte[] { 0 });
                while (byteToWrite.hasRemaining()) {
                    channel.write(byteToWrite);
                }
                channel.force(false);
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static ThreadFactory namedThreads(String prefix) {
        AtomicInteger next = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, prefix + next.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    public H2SpoolResources.ConnectionScope openConnection() {
        return resources.openConnection();
    }

    /** Retries a failed close on the same closed owner, without admitting new leases. */
    public synchronized CompletionStage<Void> closeAsync() {
        if (closing != null && !closing.isCompletedExceptionally()) {
            return closing;
        }
        CompletableFuture<Void> result = new CompletableFuture<>();
        closing = result;
        ScheduledExecutorService retries;
        try {
            retries = Executors.newSingleThreadScheduledExecutor(namedThreads("liberty-h2-body-retry-"));
        } catch (RuntimeException | Error failure) {
            // Close admissions even if the retry timer cannot be created.
            // Lease cleanup remains scheduled on the existing off-loop executors.
            try {
                resources.close();
            } catch (RuntimeException | Error closeFailure) {
                if (closeFailure != failure) {
                    failure.addSuppressed(closeFailure);
                }
            }
            result.completeExceptionally(failure);
            return result;
        }
        result.whenComplete((ignored, failure) -> retries.shutdown());
        attemptClose(result, retries, 1);
        return result;
    }

    private void attemptClose(CompletableFuture<Void> result, ScheduledExecutorService retries, int attempt) {
        CompletionStage<Void> resourcesClosed;
        try {
            resourcesClosed = resources.close();
        } catch (RuntimeException | Error failure) {
            retryClose(result, retries, attempt, failure);
            return;
        }
        resourcesClosed.whenComplete((ignored, failure) -> {
            if (failure != null) {
                // A lease may still own a file. Keep both executors accepting cleanup work.
                retryClose(result, retries, attempt, failure);
                return;
            }
            try {
                cleanup.execute(() -> {
                    try {
                        deleteOwnedDirectory();
                    } catch (IOException | RuntimeException | Error cleanupFailure) {
                        retryClose(result, retries, attempt, cleanupFailure);
                        return;
                    }
                    try {
                        writer.shutdown();
                        cleanup.shutdown();
                        result.complete(null);
                    } catch (RuntimeException | Error shutdownFailure) {
                        retryClose(result, retries, attempt, shutdownFailure);
                    }
                });
            } catch (RuntimeException | Error rejected) {
                // The caller may be an event loop; file cleanup cannot move inline.
                retryClose(result, retries, attempt, rejected);
            }
        });
    }

    /** Called only on the cleanup worker after every body lease has retired. */
    private void deleteOwnedDirectory() throws IOException {
        Files.deleteIfExists(directory);
        synchronized (OWNER_REGISTRY_LOCK) {
            try {
                Files.deleteIfExists(ownerFile);
                if (ownerLock.isValid()) {
                    ownerLock.release();
                }
                if (ownerChannel.isOpen()) {
                    ownerChannel.close();
                }
            } finally {
                if (!ownerChannel.isOpen()) {
                    OPEN_OWNER_FILES.remove(ownerFile);
                }
            }
        }
    }

    private void retryClose(CompletableFuture<Void> result, ScheduledExecutorService retries, int attempt, Throwable failure) {
        if (attempt >= MAX_CLOSE_ATTEMPTS) {
            result.completeExceptionally(failure);
            return;
        }
        try {
            retries.schedule(() -> attemptClose(result, retries, attempt + 1),
                            CLOSE_RETRY_MILLIS << (attempt - 1), TimeUnit.MILLISECONDS);
        } catch (RuntimeException | Error rejected) {
            failure.addSuppressed(rejected);
            result.completeExceptionally(failure);
        }
    }
}
