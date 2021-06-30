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

	public static boolean isCRIUSupportEnabled() {
		return false;
	}

	public static CRIUResult checkPointJVM(Path checkPointDir) {
		return null;
	}
}
