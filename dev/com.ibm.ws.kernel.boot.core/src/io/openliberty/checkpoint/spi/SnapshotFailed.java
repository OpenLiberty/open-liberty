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
public class SnapshotFailed extends Exception {
    private static final long serialVersionUID = -669718085413549145L;

    public enum Type {
        UNSUPPORTED,
        PREPARE_ABORT,
        SNAPSHOT_FAILED,
        RESTORE_ABORT;
    }

    private final Type type;

    public SnapshotFailed(Type type, String msg, Exception cause) {
        super(msg, cause);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
