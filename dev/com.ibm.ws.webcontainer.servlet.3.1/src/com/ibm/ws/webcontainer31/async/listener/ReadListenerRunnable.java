/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.async.listener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer31.async.AsyncContext31Impl;
import com.ibm.ws.webcontainer31.async.ThreadContextManager;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.srt.SRTInputStream31;
import com.ibm.wsspi.channelfw.InterChannelCallback;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

/**
 *
 */
public class ReadListenerRunnable implements Runnable {
    
    private final static TraceComponent tc = Tr.register(ReadListenerRunnable.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    private HttpInboundServiceContext _isc = null;
    private InterChannelCallback _callback = null;
    private SRTInputStream31 in;
    private AsyncContext31Impl asyncContext;

    public ReadListenerRunnable(ThreadContextManager tcm, SRTInputStream31 in,AsyncContext31Impl ac) {
        this.in = in;
        _callback = in.getCallback();
        _isc = in.getISC();
        asyncContext = ac;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "run", Thread.currentThread().getName() + " " + this.in.getReadListener());
        }
        
        try {     
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Issuing the async read for the data");
            }
            
            if (_isc==null) {
                
                _callback.complete(null);
            
            } else {
                //Call into the HttpInboundService context for the body data, passing in the callback and forcing
                //the read to go asynchronous
                //If there is data immediately available Channel will call the callback.complete before returning to this thread
                _isc.getRequestBodyBuffer(_callback, true);
            }
        } catch (Exception e){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "An exception occurred during the async read : " + e);
            }
            e.printStackTrace();
            //There was a problem with the read so we should invoke their onError, since technically it's been set now
            if(this.in.getReadListener()!= null)
                this.in.getReadListener().onError(e);
        } finally {
            asyncContext.setReadListenerRunning(false);
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "run", this.in.getReadListener());
        }
    }

}
