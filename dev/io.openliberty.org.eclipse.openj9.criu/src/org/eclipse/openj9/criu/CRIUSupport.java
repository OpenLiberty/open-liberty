/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.openj9.criu;

import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.nio.file.Files;
import java.util.Objects;
import java.io.File;

public final class CRIUSupport {

	public static enum CRIUResultType {
		SUCCESS,
		UNSUPPORTED_OPERATION,
		INVALID_ARGUMENTS,
		SYSTEM_CHECKPOINT_FAILURE,
		JVM_CHECKPOINT_FAILURE,
		JVM_RESTORE_FAILURE;
	}

	public final static class CRIUResult {
		public CRIUResultType getType() {
			return null;
		}

		public Throwable getThrowable() {
			return null;
		}
	}

	public CRIUSupport(Path imageDir) {
	}

	public static boolean isCRIUSupportEnabled() {
		return false;
	}

	public CRIUSupport setImageDir(Path imageDir) {
		return this;
	}

	public CRIUSupport setLeaveRunning(boolean leaveRunning) {
		return this;
	}

	public CRIUSupport setShellJob(boolean shellJob) {
		return this;
	}

	public CRIUSupport setExtUnixSupport(boolean extUnixSupport) {
		return this;
	}

	public CRIUSupport setLogLevel(int logLevel) {
		return this;
	}

	public CRIUSupport setLogFile(String logFile) {
		return this;
	}

	public CRIUSupport setFileLocks(boolean fileLocks) {
		return this;
	}

	public CRIUSupport setWorkDir(Path workDir) {
		return this;
	}

	public CRIUResult checkpointJVM() {
		return null;
	}
}
