/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.srt;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.servlet.IServletResponse;

public class SRTConnectionContext implements Cloneable
{
    //objects requiring cloning
    //==========================
    protected IExtendedRequest _request;
    protected WebAppDispatcherContext _dispatchContext = null;
    //==========================
    protected IExtendedResponse _response;

    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.srt");
    protected static final String CLASS_NAME="com.ibm.ws.webcontainer.srt.SRTConnectionContext";

    public SRTConnectionContext() 
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"SRTConnectionContext", "Constructor");
        }
        _request = newSRTServletRequest();
        _response = newSRTServletResponse();
        
        //TODO: why is this necessary?  this is basically setting the dispatcher context to null...
        //  it is duplicated by the subclass's constructor
        _request.setWebAppDispatcherContext(_dispatchContext);
    }

    protected SRTServletRequest newSRTServletRequest() {
        return new SRTServletRequest(this);
    }
    
    protected SRTServletResponse newSRTServletResponse() {
        return new SRTServletResponse(this);
    }
    
    public void prepareForNextConnection(IRequest req, IResponse res)
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.entering(CLASS_NAME,"prepareForNextConnection");
            logger.logp(Level.FINE, CLASS_NAME, "prepareForNextConnection", "channel req->"+req
                        +", channel res->"+res
                        +", IExtendedRequest->"+_request+", IExtendedResponse"+_response);
        }
        _request.initForNextRequest(req);
        _response.initForNextResponse(res);
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.exiting(CLASS_NAME,"prepareForNextConnection");
        }
    }

    public void start(){
        this._response.start();
        this._request.start();
    }

    public void finishConnection()
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.entering(CLASS_NAME,"finishConnection");
            logger.logp(Level.FINE, CLASS_NAME, "finishConnection",
                        "IExtendedRequest->"+_request+", IExtendedResponse"+_response);
        }
        try
        {
            try
            {
                _response.finish();
            }
            catch (Throwable th)
            {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.srt.SRTConnectionContext.finishConnection", "64", this);
            }		

            try
            {
                _request.finish();
            }
            catch (Throwable th)
            {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.srt.SRTConnectionContext.finishConnection", "74", this);
                WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext)_request.getWebAppDispatcherContext();
                dispatchContext.getWebApp().logError("Error while finishing the connection", th);
            }



            dispatchContextFinish();
        }
        finally
        {
            _request.initForNextRequest(null);
            _response.initForNextResponse(null);
        }
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.exiting(CLASS_NAME,"finishConnection");
        }
    }

    protected void dispatchContextFinish(){
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.entering(CLASS_NAME,"dispatchContextFinish");
            logger.logp(Level.FINE, CLASS_NAME, "dispatchContextFinish",
                        "IExtendedRequest->"+_request+", IExtendedResponse"+_response);
        }
        try
        {
            this._dispatchContext.finish();
        }
        catch (Throwable th)
        {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.srt.SRTConnectionContext.dispatchContextFinish", "84", this);
        }
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.exiting(CLASS_NAME,"dispatchContextFinish");
        }

    }

    public IExtendedRequest getRequest()
    {
        return _request;
    }

    public IExtendedResponse getResponse()
    {
        return _response;
    }
    protected Object clone(SRTServletRequest clonedRequest,WebAppDispatcherContext clonedDispatchContext) throws CloneNotSupportedException
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"clone", " entry");
        }

        SRTConnectionContext clonedConnContext= (SRTConnectionContext)super.clone();
        clonedConnContext._request=clonedRequest;
        clonedConnContext._dispatchContext=clonedDispatchContext;
        if (_response instanceof IServletResponse){
            clonedConnContext._response = (IExtendedResponse)((IServletResponse)_response).clone();
        }


        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"clone", " exit original -->" + this + " cloned -->" + clonedConnContext);
        }

        return clonedConnContext;
    }

    public void destroy(){
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"destroy", " entry");
        }
        _request.destroy();
        _response.destroy();
        _dispatchContext = null;
        _request = null;
        _response = null;

        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"destroy", " exit");
        }

    }

    public void setResponse(IExtendedResponse extResp) {
        _response = extResp;
    }


}
