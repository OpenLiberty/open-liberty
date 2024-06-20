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

import com.ibm.ws.zos.core.structures.NativeAscb;
import com.ibm.ws.zos.core.structures.NativeAssb;
import com.ibm.ws.zos.core.utils.DirectBufferHelper;

/**
 * Provides access to a z/OS ASSB.
 */
public class NativeAssbImpl implements NativeAssb {

    /**
     * ASCB Object
     */
    private NativeAscb nativeAscb = null;

    /**
     * Direct Buffer Helper object reference
     */
    private DirectBufferHelper directBufferHelper = null;

    /**
     * Control block constants, taken from the Data Areas books (z/OS 1.12 level if anybody cares)
     */
    protected static final int ASSB_LENGTH = 2304;
    protected static final int ASSB_ASSBSTKN_OFFSET = 0x30;
    protected static final int ASSB_ASSBJSAB_OFFSET = 0xA8;

    /**
     * Default constructor to enable extension in test and needed for OSGi instantiation
     */
    public NativeAssbImpl() {
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
     * Sets the NativeAscb object reference.
     *
     * @param nativeAscb The NativeAscb reference.
     */
    protected void setNativeAscb(NativeAscb nativeAscb) {
        this.nativeAscb = nativeAscb;
    }

    /**
     * Unsets the NativeAscb object reference.
     *
     * @param nativeAscb The NativeAscb reference.
     */
    protected void unsetNativeAscb(NativeAscb nativeAscb) {
        if (this.nativeAscb == nativeAscb) {
            this.nativeAscb = null;
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
    public ByteBuffer mapMyAssb() {
        return directBufferHelper.getSlice(nativeAscb.getASCBASSB(), ASSB_LENGTH);
    }

    @Override
    public byte[] getASSBSTKN() {
        byte[] stoken = new byte[8];
        directBufferHelper.get(nativeAscb.getASCBASSB() + ASSB_ASSBSTKN_OFFSET, stoken);
        return stoken;
    }

    @Override
    public long getASSBJSAB() {
        return directBufferHelper.getInt(nativeAscb.getASCBASSB() + ASSB_ASSBJSAB_OFFSET);
    }
}
