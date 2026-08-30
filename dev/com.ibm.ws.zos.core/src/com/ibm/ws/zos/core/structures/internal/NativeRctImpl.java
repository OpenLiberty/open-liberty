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

import com.ibm.ws.zos.core.structures.NativeRct;
import com.ibm.ws.zos.core.structures.NativeRmct;
import com.ibm.ws.zos.core.utils.DirectBufferHelper;

/**
 * Provides access to the RCT
 */
public class NativeRctImpl implements NativeRct {

    /**
     * CVT Object
     */
    private NativeRmct nativeRmct = null;

    /**
     * Direct Buffer Helper object reference
     */
    private DirectBufferHelper directBufferHelper = null;

    /**
     * Page 1821 - z/OS V2R2 MVS Data Areas Volume 3 (ITK - SCE)
     */
    protected static final int RCT_LENGTH = 272;
    protected static final int RCT_RCTPCPUA_OFFSET = 0xD4;

    /**
     * Default constructor to enable extension in test and needed for OSGi instantiation
     *
     * @return
     */
    public NativeRctImpl() {
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
     * Sets the NativeRmct object reference.
     *
     * @param nativeRmct The NativeRmct reference.
     */
    protected void setNativeRmct(NativeRmct nativeRmct) {
        this.nativeRmct = nativeRmct;
    }

    /**
     * Unsets the NativeRmct object reference.
     *
     * @param nativeRmct The NativeRmct reference.
     */
    protected void unsetNativeRmct(NativeRmct nativeRmct) {
        if (this.nativeRmct == nativeRmct) {
            this.nativeRmct = null;
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
    public ByteBuffer mapMyRct() {
        return directBufferHelper.getSlice(nativeRmct.getRMCTRCT(), RCT_LENGTH);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.core.structures.NativeRct#getRCTPCPUA()
     */
    @Override
    public int getRCTPCPUA() {
        return directBufferHelper.getInt(nativeRmct.getRMCTRCT() + RCT_RCTPCPUA_OFFSET);
    }
}