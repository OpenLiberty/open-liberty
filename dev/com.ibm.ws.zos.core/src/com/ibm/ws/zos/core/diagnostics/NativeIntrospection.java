/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.diagnostics;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

import com.ibm.ws.zos.core.utils.DirectBufferHelper;
import com.ibm.ws.zos.core.utils.DoubleGutter;
import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * z/OS Native Diagnostic Introspection
 */
public class NativeIntrospection implements com.ibm.wsspi.logging.IntrospectableService {

    protected NativeMethodManager nativeMethodManager = null;

    /**
     * The instance of the buffer management code used to read main memory.
     */
    private DirectBufferHelper bufferHelper;

    /**
     * Helper to format byte[] with Gutters
     */
    private DoubleGutter doubleGutter;

    /**
     * DS method to activate this component.
     *
     * @param nativeMethodManager reference to a NativeMethodManager
     *
     * @throws Exception
     */
    protected void activate(Map<String, Object> properties) throws Exception {
        nativeMethodManager.registerNatives(NativeIntrospection.class);
    }

    /**
     * DS method to deactivate this component.
     *
     * @param reason
     *                   int representation of reason the component is stopping
     */
    protected void deactivate(int reason) {

    }

    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
    }

    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        if (this.nativeMethodManager == nativeMethodManager) {
            this.nativeMethodManager = null;
        }
    }

    protected void setDoubleGutter(DoubleGutter doubleGutter) {
        this.doubleGutter = doubleGutter;
    }

    protected void unsetDoubleGutter(DoubleGutter doubleGutter) {
        if (this.doubleGutter == doubleGutter) {
            this.doubleGutter = null;
        }
    }

    /**
     * Sets the NativeUtils object reference.
     *
     * @param nativeUtils The NativeUtils reference.
     */
    protected void setBufferHelper(DirectBufferHelper bufferHelper) {
        this.bufferHelper = bufferHelper;
    }

    /**
     * Unsets the NativeUtils object reference.
     *
     * @param nativeUtils The NativeUtils reference.
     */
    protected void unsetBufferHelper(DirectBufferHelper bufferHelper) {
        if (this.bufferHelper == bufferHelper) {
            this.bufferHelper = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "z/OS Native Constructs";
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "zosNativeIntrospection";
    }

    /** {@inheritDoc} */
    @Override
    public void introspect(OutputStream out) throws IOException {
        PrintWriter writer = new PrintWriter(out, true);

        // Put out a header before the information
        writer.println("Native Process-level Information");
        writer.println("--------------------------------");

        writer.print("\n");

        // Format Native PGOO (server_process_data)
        formatPGOO(writer);

        writer.flush();
    }

    /**
     * Format the "server_process_data"
     */
    void formatPGOO(PrintWriter writer) {
        final int PGOO_VERSION1_LENGTH = 512;
        long nativePGOO_Address = 0;
        // int nativePGOO_Length = 0;

        writer.println("server_process_data (PGOO)");
        writer.println("--------------------------");
        nativePGOO_Address = ntv_getPGOO();
        if (nativePGOO_Address != 0) {
            byte[] rawData = new byte[PGOO_VERSION1_LENGTH];
            bufferHelper.get(nativePGOO_Address, rawData);

            writer.println(doubleGutter.asDoubleGutter(nativePGOO_Address, rawData));

        } else {
            writer.println("*** Address returned from Native call was zero ***");
        }

    }

    // Native Services
    // ---------------
    protected native long ntv_getPGOO();

}
