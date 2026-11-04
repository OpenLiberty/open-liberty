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
import com.ibm.ws.zos.core.structures.NativeRmct;
import com.ibm.ws.zos.core.utils.DirectBufferHelper;

/**
 * Provides access to the System Resources Manager Control Table (RMCT)
 */
public class NativeRmctImpl implements NativeRmct {

    /**
     * CVT Object
     */
    private NativeCvt nativeCvt = null;

    /**
     * Direct Buffer Helper object reference
     */
    private DirectBufferHelper directBufferHelper = null;

    /**
     * Page 1904 - z/OS V2R2 MVS Data Areas Volume 3 (ITK - SCE)
     */
    protected static final int RMCT_LENGTH = 1024;
    protected static final int RMCT_RMCTADJC_OFFSET = 0x40;
    protected static final int RMCT_RMCTRCT_OFFSET = 0xE4;

    /**
     * Default constructor to enable extension in test and needed for OSGi instantiation
     */
    public NativeRmctImpl() {
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
    public ByteBuffer mapMyRmct() {
        return directBufferHelper.getSlice(nativeCvt.getCVTOPCTP(), RMCT_LENGTH);
    }

    @Override
    // doc says RMCTADJC type is "signed" and length is 4 bytes, use getInt?
    public int getRMCTADJC() {
        return directBufferHelper.getInt(nativeCvt.getCVTOPCTP() + RMCT_RMCTADJC_OFFSET);
    }

    @Override
    public long getRMCTRCT() {
        return directBufferHelper.getInt(nativeCvt.getCVTOPCTP() + RMCT_RMCTRCT_OFFSET);
    }
}
