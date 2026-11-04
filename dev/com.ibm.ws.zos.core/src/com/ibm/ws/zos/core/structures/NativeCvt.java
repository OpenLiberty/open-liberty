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
 * Provides access to the z/OS Common Vectory Table (CVT)
 */
public interface NativeCvt {

    /**
     * Get a {@code DirectByteBuffer} that maps the z/OS Common Vector Table (CVT)
     */
    public ByteBuffer mapMyCvt();

    /**
     * Get the z/OS System name from the CVT
     *
     * @return The system name, in EBCDIC
     */
    public byte[] getCVTSNAME();

    /**
     * Get the ECVT Pointer
     *
     * @return The ECVT pointer
     */
    public long getCVTECVT();

    /**
     * Get CVTOSLV6 from the cvt
     *
     * @return CVTOSLV6 and the 3 bytes following it.
     */
    public int getCVTOSLV6();

    /**
     * Get the CVTOPCTP pointer from the CVT -- points to RMCT
     *
     * @return The CVTOPCTP pointer
     */
    public long getCVTOPCTP();

    /**
     * Get the CVTRAC pointer from the CVT
     *
     * @return The CVTRAC pointer
     */
    public long getCVTRAC();

    /**
     * Get CVTFLAGS from the cvt
     *
     * @return CVTFLAGS.
     */
    public int getCVTFLAGS();

}
