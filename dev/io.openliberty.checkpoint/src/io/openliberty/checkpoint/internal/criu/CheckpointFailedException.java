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

package io.openliberty.checkpoint.internal.criu;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public class CheckpointFailedException extends Exception {
    private static final long serialVersionUID = -669718085413549145L;

    public enum Type {
        UNSUPPORTED,
        PREPARE_ABORT,
        JVM_CHECKPOINT_FAILED,
        SYSTEM_CHECKPOINT_FAILED,
        JVM_RESTORE_FAILED,
        RESTORE_ABORT,
        UNKNOWN;
    }

    private final int errorCode;
    private final Type type;

    public CheckpointFailedException(Type type, String msg, Exception cause, int errorCode) {
        super(msg, cause);
        this.type = type;
        this.errorCode = errorCode;
    }

    public Type getType() {
        return type;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
