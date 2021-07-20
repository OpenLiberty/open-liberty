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

package io.openliberty.checkpoint.spi;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public class SnapshotResult {

    public enum SnapshotResultType {
        SUCCESS(1),
        UNSUPPORTED_OPERATION(-1),
        INVALID_ARGUMENTS(-2),
        SYSTEM_CHECKPOINT_FAILURE(-3),
        JVM_CHECKPOINT_FAILURE(-4),
        JVM_RESTORE_FAILURE(-5),
        UNSUPPORTED(-6),
        PREPARE_ABORT(-7),
        SNAPSHOT_FAILED(-8),
        RESTORE_ABORT(-9);

        private int code;

        private SnapshotResultType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    private final SnapshotResultType type;
    private final String msg;
    private final Exception cause;

    public SnapshotResult(SnapshotResultType type, String msg, Exception cause) {
        this.msg = msg;
        this.cause = cause;
        this.type = type;
    }

    public SnapshotResultType getType() {
        return type;
    }

    public String getMsg() {
        return msg;
    }

    public Exception getCause() {
        return cause;
    }
}
