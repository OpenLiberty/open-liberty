/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.openj9.criu;

import java.nio.file.Path;

/**
 * CRIU Support API
 */
public final class CRIUSupport {

	/**
	 * Constructs a new {@code CRIUSupport}.
	 *
	 * The default CRIU dump options are:
	 * <p>
	 * {@code imageDir} = imageDir, the directory where the images are to be
	 * created.
	 * <p>
	 * {@code leaveRunning} = false
	 * <p>
	 * {@code shellJob} = false
	 * <p>
	 * {@code extUnixSupport} = false
	 * <p>
	 * {@code logLevel} = 2
	 * <p>
	 * {@code logFile} = criu.log
	 * <p>
	 * {@code fileLocks} = false
	 * <p>
	 * {@code workDir} = imageDir, the directory where the images are to be created.
	 *
	 * @param imageDir the directory that will hold the dump files as a
	 *                 java.nio.file.Path
	 * @throws NullPointerException     if imageDir is null
	 * @throws SecurityException        if no permission to access imageDir or no
	 *                                  CRIU_DUMP_PERMISSION
	 * @throws IllegalArgumentException if imageDir is not a valid directory
	 */
	public CRIUSupport(Path imageDir) {
	}

	/**
	 * Queries if CRIU support is enabled and j9criu29 library has been loaded.
	 *
	 * @return TRUE if support is enabled and the library is loaded, FALSE otherwise
	 */
	public static boolean isCRIUSupportEnabled() {
		return true;
	}

	/**
	 * Queries if CRIU Checkpoint is allowed.
	 *
	 * @return true if Checkpoint is allowed, otherwise false
	 */
	public static boolean isCheckpointAllowed() {
		return true;
	}

	/**
	 * Returns an error message describing why isCRIUSupportEnabled()
	 * returns false, and what can be done to remediate the issue.
	 *
	 * @return NULL if isCRIUSupportEnabled() returns true and nativeLoaded is true as well, otherwise the error message.
	 */
	public static String getErrorMessage() {
		return null;
	}
	/**
	 * Returns the singleton CRIUSupport object.
	 *
	 * Most methods of class {@code CRIUSupport} are instance methods and must be
	 * invoked via this object.
	 *
	 * @return the singleton {@code CRIUSupport} object
	 */
	public static CRIUSupport getCRIUSupport() {
		return new CRIUSupport(null);
	}

	/**
	 * Sets the directory that will hold the images upon checkpoint. This must be
	 * set before calling {@link #checkpointJVM()}.
	 *
	 * @param imageDir the directory as a java.nio.file.Path
	 * @return this
	 * @throws NullPointerException     if imageDir is null
	 * @throws SecurityException        if no permission to access imageDir
	 * @throws IllegalArgumentException if imageDir is not a valid directory
	 */
	public CRIUSupport setImageDir(Path imageDir) {
		return this;
	}

	/**
	 * Controls whether process trees are left running after checkpoint.
	 * <p>
	 * Default: false
	 *
	 * @param leaveRunning
	 * @return this
	 */
	public CRIUSupport setLeaveRunning(boolean leaveRunning) {
		return this;
	}

	/**
	 * Controls ability to dump shell jobs.
	 * <p>
	 * Default: false
	 *
	 * @param shellJob
	 * @return this
	 */
	public CRIUSupport setShellJob(boolean shellJob) {
		return this;
	}

	/**
	 * Controls whether to dump only one end of a unix socket pair.
	 * <p>
	 * Default: false
	 *
	 * @param extUnixSupport
	 * @return this
	 */
	public CRIUSupport setExtUnixSupport(boolean extUnixSupport) {
		return this;
	}

	/**
	 * Sets the verbosity of log output. Available levels:
	 * <ol>
	 * <li>Only errors
	 * <li>Errors and warnings
	 * <li>Above + information messages and timestamps
	 * <li>Above + debug
	 * </ol>
	 * <p>
	 * Default: 2
	 *
	 * @param logLevel verbosity from 1 to 4 inclusive
	 * @return this
	 * @throws IllegalArgumentException if logLevel is not valid
	 */
	public CRIUSupport setLogLevel(int logLevel) {
		return this;
	}

	/**
	 * Write log output to logFile.
	 * <p>
	 * Default: criu.log
	 *
	 * @param logFile name of the file to write log output to. The path to the file
	 *                can be set with {@link #setWorkDir(Path)}.
	 * @return this
	 * @throws IllegalArgumentException if logFile is null or a path
	 */
	public CRIUSupport setLogFile(String logFile) {
		return this;
	}

	/**
	 * Controls whether to dump file locks.
	 * <p>
	 * Default: false
	 *
	 * @param fileLocks
	 * @return this
	 */
	public CRIUSupport setFileLocks(boolean fileLocks) {
		return this;
	}

	/**
	 * Controls whether to re-establish TCP connects.
	 * <p>
	 * Default: false
	 *
	 * @param tcpEstablished
	 * @return this
	 */
	public CRIUSupport setTCPEstablished(boolean tcpEstablished) {
		return this;
	}

	/**
	 * Controls whether auto dedup of memory pages is enabled.
	 * <p>
	 * Default: false
	 *
	 * @param autoDedup
	 * @return this
	 */
	public CRIUSupport setAutoDedup(boolean autoDedup) {
		return this;
	}

	/**
	 * Controls whether memory tracking is enabled.
	 * <p>
	 * Default: false
	 *
	 * @param trackMemory
	 * @return this
	 */
	public CRIUSupport setTrackMemory(boolean trackMemory) {
		return this;
	}

	/**
	 * Sets the directory where non-image files are stored (e.g. logs).
	 * <p>
	 * Default: same as path set by {@link #setImageDir(Path)}.
	 *
	 * @param workDir the directory as a java.nio.file.Path
	 * @return this
	 * @throws NullPointerException     if workDir is null
	 * @throws SecurityException        if no permission to access workDir
	 * @throws IllegalArgumentException if workDir is not a valid directory
	 */
	public CRIUSupport setWorkDir(Path workDir) {
		return this;
	}

	/**
	 * Controls whether CRIU will be invoked in privileged or unprivileged mode.
	 * <p>
	 * Default: false
	 *
	 * @param unprivileged
	 * @return this
	 */
	public CRIUSupport setUnprivileged(boolean unprivileged) {
		return this;
	}

	/**
	 * Append new environment variables to the set returned by ProcessEnvironment.getenv(...) upon
	 * restore. All pre-existing (environment variables from checkpoint run) env
	 * vars are retained. All environment variables specified in the envFile are
	 * added as long as they do not modifiy pre-existeing environment variables.
	 *
	 * Format for envFile is the following: ENV_VAR_NAME1=ENV_VAR_VALUE1 ...
	 * ENV_VAR_NAMEN=ENV_VAR_VALUEN
	 *
	 * @param envFile The file that contains the new environment variables to be
	 *                added
	 * @return this
	 */
	public CRIUSupport registerRestoreEnvFile(Path envFile) {
		return this;
	}

	/**
	 * User hook that is run after restoring a checkpoint image.
	 *
	 * Hooks will be run in single threaded mode, no other application threads
	 * will be active. Users should avoid synchronization of objects that are not owned
	 * by the thread, terminally blocking operations and launching new threads in the hook.
	 *
	 * @param hook user hook
	 *
	 * @return this
	 *
	 * TODO: Additional JVM capabilities will be added to prevent certain deadlock scenarios
	 */
	public CRIUSupport registerPostRestoreHook(Runnable hook) {
		return this;
	}

	/**
	 * User hook that is run before checkpointing the JVM.
	 *
	 * Hooks will be run in single threaded mode, no other application threads
	 * will be active. Users should avoid synchronization of objects that are not owned
	 * by the thread, terminally blocking operations and launching new threads in the hook.
	 *
	 * @param hook user hook
	 *
	 * @return this
	 *
	 * TODO: Additional JVM capabilities will be added to prevent certain deadlock scenarios
	 */
	public CRIUSupport registerPreCheckpointHook(Runnable hook) {
		return this;
	}

	/**
	 * Checkpoint the JVM. This operation will use the CRIU options set by the
	 * options setters.
	 *
	 * @throws UnsupportedOperationException if CRIU is not supported
	 *  or running in non-portable mode (only one checkpoint is allowed),
	 *  and we have already checkpointed once.
	 * @throws JVMCheckpointException        if a JVM error occurred before
	 *                                       checkpoint
	 * @throws SystemCheckpointException     if a CRIU operation failed
	 * @throws JVMRestoreException           if an error occurred during or after
	 *                                       restore
	 */
	public synchronized void checkpointJVM() {
	}
}
