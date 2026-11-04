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
 * Provides access to the z/OS Extended Common Vectory Table (ECVT)
 */
public interface NativeEcvt {

    /**
     * Get a {@code DirectByteBuffer} that maps the z/OS Extended Common Vector Table (ECVT)
     */
    public ByteBuffer mapMyEcvt();

    /**
     * Get the z/OS Syplex name from the CVT
     *
     * @return The sysplex name, in EBCDIC
     */
    public byte[] getECVTSPLX();

}
