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
package com.ibm.ws.webcontainer31.upgrade;

import javax.servlet.WriteListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer31.async.ThreadContextManager;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.srt.SRTUpgradeOutputStream31;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;


/**
 * 
 * This runnable will be invoked first time setWriteListener API is called on WCCUpgradeOutputStream by the application.
 * This will then call onWritePossible API on the WriteListener passed by the application.
 * 
 * @author anupag
 *
 */
public class UpgradeWriteListenerRunnable implements Runnable {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(UpgradeWriteListenerRunnable.class, 
                                                         WebContainerConstants.TR_GROUP, 
                                                         WebContainerConstants.NLS_PROPS );
    private WriteListener _listener = null;
    private ThreadContextManager _tcm = null;
    private UpgradeAsyncWriteCallback _cb = null;
    private SRTUpgradeOutputStream31 _out = null;

    /**
     * @param _listener
     * @param srtUpgradeOutputStream31
     * @param tcm
     * @param callback
     */
    public UpgradeWriteListenerRunnable(WriteListener _listener, SRTUpgradeOutputStream31 srtUpgradeOutputStream31, ThreadContextManager tcm, UpgradeAsyncWriteCallback _callback) {
        this._listener = _listener;
        _tcm = tcm;
        _cb = _callback;
        _out = srtUpgradeOutputStream31;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                Tr.debug(tc, "Run WriteListenerRunnable start , current thread -->" + Thread.currentThread().getName());
            }
            // clean up everything on this thread
            WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
            if (reqState != null) {
                reqState.init();
            }
            // Push the original thread's context onto the current thread, also
            // save off the current thread's context
            _tcm.pushContextData();

            // call onWritePossible
            synchronized (this._out) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                    Tr.debug(tc, "Invoking the onWritePossible first time");
                }
                WebContainerRequestState.getInstance(true).setAttribute("com.ibm.ws.webcontainer.upgrade.WriteAllowedonThisThread", true);
                this._listener.onWritePossible();

            }
        } catch (Exception e) {
            // If any other exception is thrown then call error which will set
            // exception from error which will allow application to write error
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                Tr.debug(tc, "An exception occurred during the onWritePossible : " + e);
            }
            _cb.error(_out.getBufferHelper().get_vc(), e);

        } finally {
            // Revert back to the thread's current context
            _tcm.popContextData();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                Tr.debug(tc, "Run WriteListenerRunnable done");
            }
        }
    }
}
