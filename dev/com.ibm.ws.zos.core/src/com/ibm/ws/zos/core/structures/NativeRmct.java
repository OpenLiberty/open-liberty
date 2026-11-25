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
 * Provides access to the System Resources Manager Control Table (RMCT)
 */
public interface NativeRmct {

    /**
     * Get a {@code DirectByteBuffer} that maps the z/OS RMCT
     */
    public ByteBuffer mapMyRmct();

    /**
     * Get the RMCTADJC field of the RMCT
     *
     * @return The RMCTADJC field
     */
    public int getRMCTADJC();

    /**
     * Get the RMCTRCT Pointer from RMCT, points to RCT
     *
     * @return The RCT pointer
     */
    public long getRMCTRCT();

}
