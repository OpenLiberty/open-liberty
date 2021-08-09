/*******************************************************************************
 * Copyright (c) 2016,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.srt;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.util.WSThreadLocal;
import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.ws.webcontainer.util.UnsynchronizedStack;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

/**
 *
 */
public class SRTServletRequestThreadData {

    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.srt");
    private static final String CLASS_NAME="com.ibm.ws.webcontainer.srt.SRTServletRequestThreadData";


    private IWebAppDispatcherContext _dispatchContext;
    private String _requestURI = null;
    private String _pathInfo = null;
    private String _queryString = null;
    private Map _parameters = null;
    private LinkedList _queryStringList = null; // 256836
    private boolean _qsSetExplicit = false;


    private UnsynchronizedStack _paramStack = new UnsynchronizedStack();

    private static WSThreadLocal<SRTServletRequestThreadData> instance = new WSThreadLocal<SRTServletRequestThreadData>();

    public static SRTServletRequestThreadData getInstance () {

        SRTServletRequestThreadData tempState = null;
        tempState=(SRTServletRequestThreadData) instance.get();
         
         if (tempState == null) {
                tempState = new SRTServletRequestThreadData();
                instance.set(tempState);
         }
         
         return tempState;
   }


    public SRTServletRequestThreadData() {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"constructor"," " + super.toString());
    }


    public void init(SRTServletRequestThreadData data) {

        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "int ["+super.toString()+"] " , "init data : " + data);

        if (data == null) {
            _dispatchContext = null;
            _requestURI = null;
            _pathInfo = null;
            _queryString=null;
            _parameters=null;
            _queryStringList = null;
            _qsSetExplicit = false;
        } else {
            _dispatchContext = data.getDispatchContext();
            _requestURI = data.getRequestURI();
            _pathInfo = data.getPathInfo();
            _queryString= data.getQueryString();
            _parameters= data.getParameters();
            _queryStringList = data.getQueryStringList();
            _qsSetExplicit = data.isQSSetExplicit();
        }
        if (!_paramStack.isEmpty()) {
            _paramStack.clear();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return  super.toString() + ", [_requestURI=" + _requestURI + ", _pathInfo=" + _pathInfo + ", _queryString=" + _queryString + ", _parameters=" + _parameters + ", _qsSetExplicit=" + _qsSetExplicit + ", _dispatchContext= " + _dispatchContext+ "]";
    }

    /**
     * @return the _requestURI
     */
    public String getRequestURI() {
        return _requestURI;
    }


    /**
     * @param _requestURI the _requestURI to set
     */
    public void setRequestURI(String requestURI) {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"setRequestURI", requestURI);
        this._requestURI = requestURI;
    }


    /**
     * @return the _pathInfo
     */
    public String getPathInfo() {
        return _pathInfo;
    }


    /**
     * @param _pathInfo the _pathInfo to set
     */
    public void setPathInfo(String pathInfo) {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"setPathInfo", pathInfo);
        this._pathInfo = pathInfo;
    }


    /**
     * @return the _queryString
     */
    public String getQueryString() {
        return _queryString;
    }


    /**
     * @param _queryString the _queryString to set
     */
    public void setQueryString(String queryString) {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"setQueryString", queryString);
        this._qsSetExplicit = true;
        this._queryString = queryString;
    }

    /**
     * @return the _qsSetExplicit
     */
    public boolean isQSSetExplicit() {
        return _qsSetExplicit;
    }


    /**
     * @return the _dispatchContext
     */
    public IWebAppDispatcherContext getDispatchContext() {
        return _dispatchContext;
    }



    /**
     * @param _dispatchContext the _dispatchContext to set
     */
    public void setDispatchContext(IWebAppDispatcherContext dispatchContext) {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"setDispatchContext", "dispatchContext = " + dispatchContext);
        this._dispatchContext = dispatchContext;
    }


    /**
     * @return the _parameters
     */
    public Map getParameters() {
        return _parameters;
    }


    /**
     * @param _parameters the _parameters to set
     */
    public void setParameters(Map parameters) {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"setParameters", "Number of parameters = " + (parameters==null? "none": parameters.size()));
        this._parameters = parameters;
    }

    /**
     * Save the state of the parameters before a call to include or forward.
     */
    public void pushParameterStack(Map parameters)
    {
        if (parameters == null)
        {
            _paramStack.push(null);
        } else {
            _paramStack.push(((Hashtable)parameters).clone());
        }
    }

    /**
     * Revert the state of the parameters which was saved before an include call
     * 
     */
    public void popParameterStack()
    {
        try
        {
            _parameters = (Hashtable) _paramStack.pop();
        } catch (java.util.EmptyStackException empty) {
            /// tbd
        }
    }



    /**
     * @return the _queryStringList
     */
    public LinkedList getQueryStringList() {
        return _queryStringList;
    }



    /**
     * @param _queryStringList the _queryStringList to set
     */
    public void setQueryStringList(LinkedList _queryStringList) {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"setQueryStringList", "Number in list = " + (_queryStringList==null? "none": _queryStringList.size()));
        this._queryStringList = _queryStringList;
    }


}
