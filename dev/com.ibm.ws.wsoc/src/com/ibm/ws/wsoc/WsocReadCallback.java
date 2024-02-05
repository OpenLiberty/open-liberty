/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Executor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

public class WsocReadCallback extends BaseCallback implements TCPReadCompletedCallback {

    private static final TraceComponent tc = Tr.register(WsocReadCallback.class);

    @Override
    public void complete(VirtualConnection vc, final TCPReadRequestContext rrc) {

        if (connLink != null) {
            connLink.signalReadComplete();

            // Run under Classify Executor if present, otherwise drive the rest of the logic here.
            ParametersOfInterest things = connLink.getParametersOfInterest();
            Executor executor = things.getExecutor();

            if (executor != null) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        completeLogic(rrc);
                    }
                });
            } else {
                completeLogic(rrc);
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No connection link found");
            }
        }
    }

    protected void completeLogic(TCPReadRequestContext rrc) {
        // setup the classloader and Component Metadata for the app to use during onMessage (or onError or onClose). reset when done.
        ClassLoader originalCL = pushContexts();

        try {
            //popContexts(originalCL);
            connLink.processRead(rrc);
        } finally {
            popContexts(originalCL);
        }
    }

    @Override
    public void error(VirtualConnection vc, final TCPReadRequestContext rrc, final IOException ioe) {

        if (connLink != null) {
            // Run under Classify Executor if present, otherwise drive the rest of the logic here.
            ParametersOfInterest things = connLink.getParametersOfInterest();
            Executor executor = things.getExecutor();

            if (executor != null) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        errorLogic(rrc, ioe);
                    }
                });
            } else {
                errorLogic(rrc, ioe);
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No connection link found");
            }
        }
    }

    protected void errorLogic(TCPReadRequestContext rrc, IOException ioe) {
        // setup the classloader and Component Metadata for the app to use during onMessage (or onError or onClose). reset when done.
        // Not sure this this hits on liberty - but outbound session (idle) close read cancel could hit this with user code stack...
        ClassLoader originalCL = AccessController.doPrivileged(
                                                               new PrivilegedAction<ClassLoader>() {
                                                                   @Override
                                                                   public ClassLoader run() {
                                                                       return pushContexts();
                                                                   }
                                                               });
        boolean startAsyncRead = false;

        try {
            startAsyncRead = connLink.processReadErrorComplete(ioe);
        } finally {
            final ClassLoader origCL = originalCL;
            AccessController.doPrivileged(
                                          new PrivilegedAction<Void>() {
                                              @Override
                                              public Void run() {
                                                  popContexts(origCL);
                                                  return null;
                                              }
                                          });

            WsByteBuffer buf = rrc.getBuffer();

            if (buf != null) {
                buf.release();
            }
        }

        if (startAsyncRead) {
            connLink.startAsyncRead(rrc);
        }
    }

    @Override
    protected ClassLoader pushContexts() {
        connLink.waitReadPush();
        return super.pushContexts();
    }

    @Override
    protected void popContexts(ClassLoader originalCL) {
        super.popContexts(originalCL);
        connLink.notifyReadPush();
    }

}
