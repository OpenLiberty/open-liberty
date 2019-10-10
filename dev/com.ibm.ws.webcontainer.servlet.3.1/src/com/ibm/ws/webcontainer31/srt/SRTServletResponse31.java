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
package com.ibm.ws.webcontainer31.srt;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet31.response.IResponse31;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.osgi.response.IResponse31Impl;
import com.ibm.ws.webcontainer31.osgi.srt.SRTConnectionContext31;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

/**
 * The Servlet Runtime Response object
 * 
 * The SRTServletResponse class handles response object functions that involve the input and output streams. This class
 * contains no WebApp level information, and should not be hacked to include any. A
 * WebAppDispatcherResponse object will proxy this response and handle method calls that need 
 * path or webapp information. 
 * 
 * @author The Unknown Programmer
 * 
 */
public class SRTServletResponse31 extends com.ibm.ws.webcontainer.srt.SRTServletResponse implements HttpServletResponse{ 

    private static final TraceComponent tc = Tr.register(SRTServletResponse31.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);
    private static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.srt");
    private static final String CLASS_NAME="com.ibm.ws.webcontainer.srt.SRTServletResponse31";    
    

    public SRTServletResponse31(SRTConnectionContext31 context) {
        super(context);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#setContentLengthLong(long)
     */
    @Override
    public void setContentLengthLong (long len) {
        String methodName = "setContentLengthLong";
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) { 
            logger.entering(CLASS_NAME, methodName, "length --> " + String.valueOf(len) + " ["+this+"]");
        }

        // check the include flag
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), methodName + " length --> " + String.valueOf(len));  //311717
            }
        }
        else
        {
            if (_response!=null) {
                if (!_response.isCommitted()) {
                    ((IResponse31)_response).setContentLengthLong(len);
                }
            }
            _contentLength = len;
            
            _bufferedWriter.setLimitLong(_contentLength);
            //still want to try to set the header even if the response is committed to throw the warning
            setHeader(HEADER_CONTENT_LENGTH, Long.toString(_contentLength)); // TODO SERVLET 3.1 - Need "long" implementation??
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME, methodName);
        }
       
    }

    /*
     * Return the default "X-Powered-By" header value for Servlet 3.1
     */
    protected String getXPoweredbyHeader() {
       return WebContainerConstants.X_POWERED_BY_DEFAULT_VALUE31;
    }
    
    //for upgrade request
    /**
     * @throws IOException
     */
    public void finishKeepConnection() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {  
            Tr.entry(tc, "finishKeepConnection", " ["+this+"]"); 
        }           

        if (!isCommitted()) {
            commit();
            // need to flush data, so code outside WebContainer that are upgraded can immediately send data once the response is finished
            flushData();

        }
        super.cleanupFromFinish();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {  
            Tr.exit(tc, "finishKeepConnection", " ["+this+"]"); 
        }
    }


    /**
     * @throws IOException
     */
    protected void flushData() throws IOException {    
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {  
            Tr.entry(tc, "flushData", " we've written output so flush" +" ["+this+"]");
        }

        if (_pwriter != null && writerClosed == false) {
            _pwriter.flush();
        }
        if (_gotOutputStream)
        {              
            _response.flushBuffer();                                    
        }
        // if resource did not had printWriter or output stream but still needs to flush, then flush headers
        // check if this will effect Websocket, may be not as it always get it for Transportconnection ?
        if (_pwriter == null && !_gotOutputStream){   

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {  
                Tr.debug(tc, "only flush headers as output or writer are not created" +" ["+this+"]");   
            }                      
            ((IResponse31Impl)_response).flushHeaders();  
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {  
            Tr.exit(tc, "flushData", " we've flushed" +" ["+this+"]");
        }  
    }

}
