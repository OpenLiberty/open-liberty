/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.queuing.internal;

import java.io.IOException;

/**
 * Thrown when a caller tries to obtain access to a client's shared memory
 * area but is denied.
 */
public class SharedMemoryAccessException extends IOException {

    public SharedMemoryAccessException(String msg) {
        super(msg);
    }
}
