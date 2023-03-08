/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.structures;

import java.nio.ByteBuffer;

/**
 * Provides access to the z/OS Prefix Save Area (PSA). Use NativeUtils.getMyPsa( ) to create one
 */
public interface NativePsa {

    /**
     * Get a {@code DirectByteBuffer} that maps the z/OS Prefix Save Area (PSA)
     */
    public ByteBuffer mapMyPsa();

    /**
     * Get the address of the CVT
     *
     * @return the CVT address
     */
    public long getFLCCVT();

    /**
     * Get the address of the ASCB
     *
     * @return the ASCB Address
     */
    public long getPSAAOLD();

    /**
     * Get the address of the current TCB
     *
     * @return address of the current TCB
     */
    public long getPSATOLD();
}
