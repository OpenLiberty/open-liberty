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
 * Provides access to the z/OS Address Space Control Block (ASCB)
 */
public interface NativeAscb {

    /**
     * Get a {@code DirectByteBuffer} that maps the z/OS Address Space Control Block (ASCB)
     */
    public ByteBuffer mapMyAscb();

    /**
     * Get the ASSB pointer from the ASCB
     *
     * @return ASSBASCB value
     */
    public long getASCBASSB();

    /**
     * Get the started task job name from the ASCB
     *
     * @return The started task job name (in EBCDIC)
     */
    public byte[] getASCBJBNS();

    /**
     * Get the jobname from ASCBJBNI
     *
     * @return The job name (in EBCDIC)
     */
    public byte[] getASCBJBNI();

    /**
     * Get the ASID
     *
     * @return The ASID
     */
    public short getASCBASID();

}
