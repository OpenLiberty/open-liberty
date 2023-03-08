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

import com.ibm.ws.zos.core.structures.NativeAssb;
import com.ibm.ws.zos.core.structures.NativeJsab;
import com.ibm.ws.zos.core.utils.DirectBufferHelper;

/**
 * Provides access to thenative JSAB
 */
public class NativeJsabImpl implements NativeJsab {
    /**
     * ASCB Object
     */
    private NativeAssb nativeAssb = null;

    /**
     * Direct Buffer Helper object reference
     */
    private DirectBufferHelper directBufferHelper = null;

    /**
     * Control block constants, taken from the Data Areas books (z/OS 1.12 level if anybody cares)
     */
    protected static final int JSAB_LENGTH = 128;
    protected static final int JSAB_JSABJBID_OFFSET = 0x14;
    protected static final int JSAB_JSABJBNM_OFFSET = 0x1C;

    /**
     * Default constructor to enable extension in test and needed for OSGi instantiation
     */
    public NativeJsabImpl() {
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
     * Sets the NativeAssb object reference.
     *
     * @param nativeAssb The NativeAssb reference.
     */
    protected void setNativeAssb(NativeAssb nativeAssb) {
        this.nativeAssb = nativeAssb;
    }

    /**
     * Unsets the NativeAssb object reference.
     *
     * @param nativeAssb The NativeAsscb reference.
     */
    protected void unsetNativeAssb(NativeAssb nativeAssb) {
        if (this.nativeAssb == nativeAssb) {
            this.nativeAssb = null;
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
    public ByteBuffer mapMyJsab() {
        return directBufferHelper.getSlice(nativeAssb.getASSBJSAB(), JSAB_LENGTH);
    }

    @Override
    public byte[] getJSABJBNM() {
        byte[] jobname = new byte[8];
        directBufferHelper.get(nativeAssb.getASSBJSAB() + JSAB_JSABJBNM_OFFSET, jobname);
        return jobname;
    }

    @Override
    public byte[] getJSABJBID() {
        byte[] jobid = new byte[8];
        directBufferHelper.get(nativeAssb.getASSBJSAB() + JSAB_JSABJBID_OFFSET, jobid);
        return jobid;
    }
}
