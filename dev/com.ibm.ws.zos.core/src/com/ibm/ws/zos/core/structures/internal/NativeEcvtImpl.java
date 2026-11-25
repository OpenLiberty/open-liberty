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
package com.ibm.ws.zos.core.structures.internal;

import java.nio.ByteBuffer;
import java.util.Map;

import com.ibm.ws.zos.core.structures.NativeCvt;
import com.ibm.ws.zos.core.structures.NativeEcvt;
import com.ibm.ws.zos.core.utils.DirectBufferHelper;

/**
 * * Provides access to the z/OS ECVT.
 */
public class NativeEcvtImpl implements NativeEcvt {

    /**
     * Provides access to the z/OS CVT.
     */

    /**
     * CVT Object
     */
    private NativeCvt nativeCvt = null;

    /**
     * Direct Buffer Helper object reference
     */
    private DirectBufferHelper directBufferHelper = null;

    /**
     * Control block constants, taken from the Data Areas books (z/OS 1.12 level if anybody cares)
     */
    protected static final int ECVT_LENGTH = 920;
    protected static final int ECVT_ECVTSPLX_OFFSET = 0x8;
    protected static final int ECVT_ECVTSPLX_LENGTH = 8;

    /**
     * Default constructor to enable extension in test and needed for OSGi instantiation
     */
    public NativeEcvtImpl() {
    }

    /**
     * DS method to activate this component.
     *
     * @param properties
     *
     * @throws Exception
     */
    protected void activate(Map<String, Object> properties) throws Exception {
    }

    /**
     * DS method to deactivate this component.
     *
     * @param reason The representation of reason the component is stopping
     */
    protected void deactivate() {
    }

    /**
     * Sets the NativeCvt object reference.
     *
     * @param nativeCvt The NativeCvt reference.
     */
    protected void setNativeCvt(NativeCvt nativeCvt) {
        this.nativeCvt = nativeCvt;
    }

    /**
     * Unsets the NativeCvt object reference.
     *
     * @param nativeCvt The NativeCvt reference.
     */
    protected void unsetNativeCvt(NativeCvt nativeCvt) {
        if (this.nativeCvt == nativeCvt) {
            this.nativeCvt = null;
        }
    }

    /**
     * Sets the DirectBufferHelper object reference
     *
     * @param directBufferHelper The DirectBufferHelper reference
     */
    protected void setDirectBufferHelper(DirectBufferHelper directBufferHelper) {
        this.directBufferHelper = directBufferHelper;
    }

    protected void unsetDirectBufferHelper(DirectBufferHelper directBufferHelper) {
        if (this.directBufferHelper == directBufferHelper) {
            this.directBufferHelper = null;
        }
    }

    @Override
    public ByteBuffer mapMyEcvt() {
        return directBufferHelper.getSlice(nativeCvt.getCVTECVT(), ECVT_LENGTH);
    }

    @Override
    public byte[] getECVTSPLX() {
        byte[] ecvtsplx = new byte[ECVT_ECVTSPLX_LENGTH];
        directBufferHelper.get(nativeCvt.getCVTECVT() + ECVT_ECVTSPLX_OFFSET, ecvtsplx);
        return ecvtsplx;
    }
}
