/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import java.io.IOException;
import java.util.concurrent.Executor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

public class WsocWriteCallback extends BaseCallback implements TCPWriteCompletedCallback {

    private static final TraceComponent tc = Tr.register(WsocWriteCallback.class);

    @Override
    public void complete(VirtualConnection vc, final TCPWriteRequestContext wsc) {

        if (connLink != null) {
            // Run under Classify Executor
            ParametersOfInterest things = connLink.getParametersOfInterest();
            Executor executor = things.getExecutor();

            if (executor != null) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        completeLogic(wsc);
                    }
                });
            } else {
                completeLogic(wsc);
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No connection link found");
            }
        }
    }

    protected void completeLogic(TCPWriteRequestContext wsc) {
        // setup the classloader and Component Metadata for the app to use during async read processing (or onError or onClose). reset when done.
        ClassLoader originalCL = pushContexts();

        try {
            connLink.processWrite(wsc);
        } finally {
            popContexts(originalCL);
        }
    }

    @Override
    public void error(VirtualConnection vc, final TCPWriteRequestContext wsc,
                      final IOException ioe) {

        if (connLink != null) {
            // Run under Classify Executor
            ParametersOfInterest things = connLink.getParametersOfInterest();
            Executor executor = things.getExecutor();

            if (executor != null) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        errorLogic(wsc, ioe);
                    }
                });
            } else {
                errorLogic(wsc, ioe);
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No connection link found");
            }
        }
    }

    protected void errorLogic(TCPWriteRequestContext wsc, IOException ioe) {
        // setup the classloader and Component Metadata for the app to use during async write processing (or onError or onClose). reset when done.
        ClassLoader originalCL = pushContexts();

        try {
            connLink.processWriteError(wsc, ioe);
        } finally {
            popContexts(originalCL);
        }
    }

    @Override
    protected ClassLoader pushContexts() {
        connLink.waitWritePush();
        return super.pushContexts();
    }

    @Override
    protected void popContexts(ClassLoader originalCL) {
        super.popContexts(originalCL);
        connLink.notifyWritePush();
    }

}
