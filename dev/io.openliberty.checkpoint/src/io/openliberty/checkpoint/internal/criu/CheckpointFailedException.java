/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
        UNSUPPORTED_IN_JVM(70, false),

        /**
         * CRIU not supported. We are running a JVM with support but the VM was not launched with the option--XX:+EnableCRIUSupport.
         */
        UNSUPPORTED_DISABLED_IN_JVM(71, false),

        LIBERTY_PREPARE_FAILED(72, false),
        JVM_CHECKPOINT_FAILED(73, false),
        SYSTEM_CHECKPOINT_FAILED(74, false),
        UNKNOWN_CHECKPOINT(75, false),

        SYSTEM_RESTORE_FAILED(80, true),
        JVM_RESTORE_FAILED(81, true),
        LIBERTY_RESTORE_FAILED(82, true),
        UNKNOWN_RESTORE(83, true);

        final int errorCode;
        final boolean isRestore;

        private Type(int errorCode, boolean isRestore) {
            this.errorCode = errorCode;
            this.isRestore = isRestore;
        }
    }

    private final Type type;

    public CheckpointFailedException(Type type, String msg, Throwable cause) {
        super(msg, cause);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public int getErrorCode() {
        return type.errorCode;
    }

    public String getErrorMsgKey() {
        return type.isRestore ? RESTORE_FAILED_KEY : CHECKPOINT_FAILED_KEY;
    }

    public boolean isRestore() {
        return type.isRestore;
    }

}
