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
import com.ibm.ws.zos.core.structures.NativePsa;
import com.ibm.ws.zos.core.utils.DirectBufferHelper;

/**
 * Provides access to a z/OS ASCB.
 */
public class NativeAscbImpl implements NativeAscb {

    /**
     * PSA Object
     */
    private NativePsa nativePsa = null;

    /**
     * Direct Buffer Helper object reference
     */
    private DirectBufferHelper directBufferHelper = null;

    /**
     * Control block constants, taken from the Data Areas books (z/OS 1.12 level if anybody cares)
     */
    protected static final int ASCB_LENGTH = 384;
    protected static final int ASCB_ASCBASSB_OFFSET = 0x150;
    protected static final int ASCB_ASCBJBNS_OFFSET = 0xB0;
    protected static final int ASCB_ASCBJBNI_OFFSET = 0xAC;
    protected static final int ASCB_ASCBASID_OFFSET = 0x24;

    /**
     * Default constructor to enable extension in test and needed for OSGi instantiation
     */
    public NativeAscbImpl() {
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
     * Sets the NativePsa object reference.
     *
     * @param nativePsa The NativePsa reference.
     */
    protected void setNativePsa(NativePsa nativePsa) {
        this.nativePsa = nativePsa;
    }

    /**
     * Unsets the NativePsa object reference.
     *
     * @param nativePsa The NativePsa reference.
     */
    protected void unsetNativePsa(NativePsa nativePsa) {
        if (this.nativePsa == nativePsa) {
            this.nativePsa = null;
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
    public ByteBuffer mapMyAscb() {
        return directBufferHelper.getSlice(nativePsa.getPSAAOLD(), ASCB_LENGTH);
    }

    @Override
    public long getASCBASSB() {
        return directBufferHelper.getInt(nativePsa.getPSAAOLD() + ASCB_ASCBASSB_OFFSET);

    }

    @Override
    public byte[] getASCBJBNS() {
        long ascbjbns = directBufferHelper.getInt(nativePsa.getPSAAOLD() + ASCB_ASCBJBNS_OFFSET);
        byte[] jobname = new byte[8];
        directBufferHelper.get(ascbjbns, jobname);
        return jobname;
    }

    @Override
    public byte[] getASCBJBNI() {
        long ascbjbni = directBufferHelper.getInt(nativePsa.getPSAAOLD() + ASCB_ASCBJBNI_OFFSET);
        byte[] jobname = new byte[8];
        directBufferHelper.get(ascbjbni, jobname);
        return jobname;
    }

    @Override
    public short getASCBASID() {
        return directBufferHelper.getShort(nativePsa.getPSAAOLD() + ASCB_ASCBASID_OFFSET);
    }
}
