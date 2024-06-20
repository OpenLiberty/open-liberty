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
 * Provides access to the RCT, pointed to RMCTRCT field of the RMCT data area
 */
public interface NativeRct {

    /**
     * Get a {@code DirectByteBuffer} that maps the z/OS RCT
     */
    public ByteBuffer mapMyRct();

    /**
     * Get the RCTPCPUA field from the RCT
     *
     * @return the RCTPCPUA field
     */
    public int getRCTPCPUA();

}