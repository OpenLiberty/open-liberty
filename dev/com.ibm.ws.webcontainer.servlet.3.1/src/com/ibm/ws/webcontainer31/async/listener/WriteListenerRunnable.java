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
package com.ibm.ws.webcontainer31.async.listener;

import java.io.IOException;

import javax.servlet.WriteListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.srt.SRTServletRequestThreadData;
import com.ibm.ws.webcontainer31.async.AsyncWriteCallback;
import com.ibm.ws.webcontainer31.async.ThreadContextManager;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.http.ee7.HttpOutputStreamEE7;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;


/**
 * This runnable will be invoked by container when first time setWriteListener API on outputStream 
 * will be called by the JEE application.
 */
public class WriteListenerRunnable implements Runnable{

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(WriteListenerRunnable.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS ); 

    private WriteListener _listener = null;
    private ThreadContextManager _tcm = null;
    private HttpOutputStreamEE7 _hout = null;
    private AsyncWriteCallback _cb = null;
    private SRTServletRequestThreadData _requestDataWriteListenerThread;

    public WriteListenerRunnable(WriteListener listener, HttpOutputStreamEE7 hout, AsyncWriteCallback callback, ThreadContextManager tcm, SRTServletRequestThreadData threadData ) {
        this._listener = listener;
        this._tcm = tcm;
        this._hout = hout;
        this._cb = callback;
        _requestDataWriteListenerThread = threadData;
    }

    /**
     * @param listener
     * @param tcm
     */
    public WriteListenerRunnable(WriteListener listener, ThreadContextManager tcm) {
        this._listener = listener;
        this._tcm = tcm;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    @FFDCIgnore({ IOException.class })
    public void run() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "Run WriteListenerRunnable start, WriteListener enabled: " + this._listener +" , current thread -->"+ Thread.currentThread().getName()); 
        }
        //clean up everything on this thread
        WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
        if (reqState!=null){
            reqState.init();
        }
        
        SRTServletRequestThreadData.getInstance().init(_requestDataWriteListenerThread);
        
        //Push the original thread's context onto the current thread, also save off the current thread's context
        _tcm.pushContextData();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "Invoking the onWritePossible first time");
        }
        WebContainerRequestState.getInstance(true).setAttribute("com.ibm.ws.webcontainer.WriteAllowedonThisThread", true); 
        if(_hout != null){
            synchronized(_hout) {
                try {     
                    //call onWritePossible
                    this._listener.onWritePossible();
                    
                } catch (Exception e){
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                        Tr.debug(tc, "An exception occurred during the onWritePossible : " + e); 
                    }
                    _hout.setExceptionDuringOnWP(true);
                    _cb.error(_hout.getVc(), e );
                }
                finally{
                    //Revert back to the thread's current context
                    _tcm.popContextData();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                        Tr.debug(tc, "Run WriteListenerRunnable done");
                    }
                }
            }
        }
        else{
            try {     
                //call onWritePossible
                this._listener.onWritePossible();

            } catch (IOException ioe){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                    Tr.debug(tc, "An exception occurred during the onWritePossible : " + ioe); 
                }
                this._listener.onError(ioe);
            }
            finally{
                //Revert back to the thread's current context
                _tcm.popContextData();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                    Tr.debug(tc, "Run WriteListenerRunnable done");
                }
            }
        }
    }
}
