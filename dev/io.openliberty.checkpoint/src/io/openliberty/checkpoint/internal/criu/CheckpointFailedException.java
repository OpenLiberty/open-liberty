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
public class CheckpointFailedException extends RuntimeException {
    private static final long serialVersionUID = -669718085413549145L;
    private static String CHECKPOINT_FAILED_KEY = "CHECKPOINT_FAILED_CWWKC0453E";
    private static String RESTORE_FAILED_KEY = "RESTORE_FAILED_CWWKC0454E";

    public enum Type {
        /**
         * CRIU not supported. The JVM we are running does not offer the org.eclipse.openj9.criu package.
         */
        UNSUPPORTED_IN_JVM(70, CHECKPOINT_FAILED_KEY),

        /**
         * CRIU not supported. We are running a JVM with support but the VM was not launched with the option--XX:+EnableCRIUSupport.
         */
        UNSUPPORTED_DISABLED_IN_JVM(71, CHECKPOINT_FAILED_KEY),

        LIBERTY_PREPARE_FAILED(72, CHECKPOINT_FAILED_KEY),
        JVM_CHECKPOINT_FAILED(73, CHECKPOINT_FAILED_KEY),
        SYSTEM_CHECKPOINT_FAILED(74, CHECKPOINT_FAILED_KEY),
        JVM_RESTORE_FAILED(75, RESTORE_FAILED_KEY),
        LIBERTY_RESTORE_FAILED(76, RESTORE_FAILED_KEY),
        // unknown could be a checkpoint or restore error, but most likely checkpoint
        // may need a separate message just for unknown
        UNKNOWN(77, CHECKPOINT_FAILED_KEY);

        final int errorCode;
        final String errorMsgKey;

        private Type(int errorCode, String errorMsgKey) {
            this.errorCode = errorCode;
            this.errorMsgKey = errorMsgKey;
        }
    }

    private final int errorCode;
    private final Type type;

    public CheckpointFailedException(Type type, String msg, Throwable cause) {
        this(type, msg, cause, type.errorCode);
    }

    public CheckpointFailedException(Type type, String msg, Throwable cause, int errorCode) {
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

    public String getErrorMsgKey() {
        return type.errorMsgKey;
    }
}
