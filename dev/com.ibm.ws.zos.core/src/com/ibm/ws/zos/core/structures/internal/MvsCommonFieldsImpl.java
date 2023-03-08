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

import java.util.Map;

import com.ibm.ws.zos.core.structures.MvsCommonFields;
import com.ibm.ws.zos.core.structures.NativeAscb;
import com.ibm.ws.zos.core.structures.NativeAssb;
import com.ibm.ws.zos.core.structures.NativeCvt;
import com.ibm.ws.zos.core.structures.NativeEcvt;
import com.ibm.ws.zos.core.structures.NativeJsab;
import com.ibm.ws.zos.core.structures.NativePsa;
import com.ibm.ws.zos.core.structures.NativeRct;
import com.ibm.ws.zos.core.structures.NativeRmct;

/**
 *
 */
public class MvsCommonFieldsImpl implements MvsCommonFields {

    /**
     * Object references
     */
    private NativePsa nativePsa = null;
    private NativeCvt nativeCvt = null;
    private NativeRmct nativeRmct = null;
    private NativeRct nativeRct = null;
    private NativeEcvt nativeEcvt = null;
    private NativeAscb nativeAscb = null;
    private NativeAssb nativeAssb = null;
    private NativeJsab nativeJsab = null;

    /**
     * Default constructor to enable extension in test and needed for OSGi instantiation
     */
    public MvsCommonFieldsImpl() {
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
     * Sets the NativeRmct object reference.
     *
     * @param nativePsa The NativeRmct reference.
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

    protected void setNativeRct(NativeRct nativeRct) {
        this.nativeRct = nativeRct;
    }

    protected void unsetNativeRct(NativeRct nativeRct) {
        if (this.nativeRct == nativeRct) {
            this.nativeRct = null;
        }
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
     * Sets the NativeEcvt object reference.
     *
     * @param nativeEcvt The NativeEcvt reference.
     */
    protected void setNativeEcvt(NativeEcvt nativeEcvt) {
        this.nativeEcvt = nativeEcvt;
    }

    /**
     * Unsets the NativeEcvt object reference.
     *
     * @param nativeEcvt The NativeEcvt reference.
     */
    protected void unsetNativeEcvt(NativeEcvt nativeEcvt) {
        if (this.nativeEcvt == nativeEcvt) {
            this.nativeEcvt = null;
        }
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
     * @param nativeAssb The NativeAssb reference.
     */
    protected void unsetNativeAssb(NativeAssb nativeAssb) {
        if (this.nativeAssb == nativeAssb) {
            this.nativeAssb = null;
        }
    }

    /**
     * Sets the NativeJsab object reference.
     *
     * @param nativeJsab The NativeJsab reference.
     */
    protected void setNativeJsab(NativeJsab nativeJsab) {
        this.nativeJsab = nativeJsab;
    }

    /**
     * Unsets the NativeJsab object reference.
     *
     * @param nativeJsab The NativeJsab reference.
     */
    protected void unsetNativeJsab(NativeJsab nativeJsab) {
        if (this.nativeJsab == nativeJsab) {
            this.nativeJsab = null;
        }
    }

    @Override
    public int getRMCTADJC() {
        return nativeRmct.getRMCTADJC();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.core.structures.MvsCommonFields#getCVTSNAME()
     */
    @Override
    public byte[] getCVTSNAME() {
        return nativeCvt.getCVTSNAME();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.core.structures.MvsCommonFields#getECVTSPLX()
     */
    @Override
    public byte[] getECVTSPLX() {
        return nativeEcvt.getECVTSPLX();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.core.structures.MvsCommonFields#getASCBJBNS()
     */
    @Override
    public byte[] getASCBJBNS() {
        return nativeAscb.getASCBJBNS();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.core.structures.MvsCommonFields#getASCBJBNI()
     */
    @Override
    public byte[] getASCBJBNI() {
        return nativeAscb.getASCBJBNI();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.core.structures.MvsCommonFields#getASCBASID()
     */
    @Override
    public short getASCBASID() {
        return nativeAscb.getASCBASID();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.core.structures.MvsCommonFields#getASSBSTKN()
     */
    @Override
    public byte[] getASSBSTKN() {
        return nativeAssb.getASSBSTKN();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.core.structures.MvsCommonFields#getJSABJBNM()
     */
    @Override
    public byte[] getJSABJBNM() {
        return nativeJsab.getJSABJBNM();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.core.structures.MvsCommonFields#getJSABJBID()
     */
    @Override
    public byte[] getJSABJBID() {
        return nativeJsab.getJSABJBID();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.core.structures.MvsCommonFields#getPSATOLD()
     */
    @Override
    public long getPSATOLD() {
        return nativePsa.getPSATOLD();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.core.structures.MvsCommonFields#getRCTPCPUA()
     */
    @Override
    public int getRCTPCPUA() {
        return nativeRct.getRCTPCPUA();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.core.structures.MvsCommonFields#getCVTFLAGS()
     */
    @Override
    public int getCVTFLAGS() {
        return nativeCvt.getCVTFLAGS();
    }
}