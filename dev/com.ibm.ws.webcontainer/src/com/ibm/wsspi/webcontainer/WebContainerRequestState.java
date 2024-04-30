/*******************************************************************************
 * Copyright (c) 1997, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.wsspi.webcontainer;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.util.WSThreadLocal;
import com.ibm.wsspi.webcontainer.servlet.AsyncContext;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;

/**
*
* 
* WebContainerRequestState is the thread local used to store per request info that can't be passed because of method
* signature requirements
* @ibm-private-in-use
*/
@SuppressWarnings("unchecked")
public class WebContainerRequestState {
    static protected Logger logger= Logger.getLogger("com.ibm.wsspi.webcontainer");
    private static final String CLASS_NAME="com.ibm.wsspi.webcontainer.WebContainerRequestState";
    private Map _attributes = null;
    private boolean _invokedFilters;
    private boolean completed;
    private boolean ardRequest;
    private boolean startAsync;
    private IExtendedRequest currentThreadsIExtendedRequest;
    private IExtendedResponse currentThreadsIExtendedResponse;
    // Each cookie has a Map of attributes
    private Map<String,HashMap<String,String>> cookieAttributesMap = null;

    public IExtendedResponse getCurrentThreadsIExtendedResponse() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getCurrentThreadsIExtendedResponse", " currentThreadsIExtendedResponse --> " + currentThreadsIExtendedResponse);
        }
        return currentThreadsIExtendedResponse;
    }

    private AsyncContext asyncContext;

    public void setCurrentThreadsIExtendedRequest(IExtendedRequest currentThreadsIExtendedRequest) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setCurrentThreadsIExtendedRequest", " new currentThreadsIExtendedRequest --> " + currentThreadsIExtendedRequest);
        }
        this.currentThreadsIExtendedRequest = currentThreadsIExtendedRequest;
    }

    public boolean isAsyncMode() {
        return startAsync;
    }

    private static WSThreadLocal instance = new WSThreadLocal();
    
    public static WebContainerRequestState getInstance (boolean create) {
    	
        WebContainerRequestState tempState = null;
        tempState=(WebContainerRequestState) instance.get();
       	 
         if (tempState == null && create) {
       	  	tempState = createInstance();
         }
         
         if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST) ){
             logger.logp(Level.FINEST,CLASS_NAME,"getInstance","created->"+create+ ", request state->"+tempState);        //PM50111
         }
         return (tempState);
    }

    public static WebContainerRequestState createInstance() {
        WebContainerRequestState newState = new WebContainerRequestState();
        instance.set(newState);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST) ){
            logger.logp(Level.FINEST,CLASS_NAME,"createInstance","created requestState -->"+ newState.toString());        
        }

        return newState;
    }
	
    public void init(){
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST) ){
            logger.logp(Level.FINEST,CLASS_NAME,"init","init for request State");        //PM50111 
	}
        if (_attributes!=null) {
            _attributes.clear();
        }
        if (cookieAttributesMap!=null) {
            cookieAttributesMap.clear();
        }
        _invokedFilters=false;
        completed=false;
        ardRequest = false;
        this.currentThreadsIExtendedRequest = null;
        startAsync=false;
        this.asyncContext = null;
    }

    public void setAttribute(String string, Object obj) {
        if (_attributes==null) {
            _attributes=new HashMap();
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"setAttribute", " name --> " + string);
        } 
        _attributes.put(string, obj);
    }
	
    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
     */
    public Object getAttribute(String arg0) {
        //321485
    	if (_attributes==null) {
    		return null;
    	}
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"getAttribute", " name --> " + arg0);
        }
    	return _attributes.get(arg0);
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String arg0) {
        //321485
    	if (_attributes==null)
    		return;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"removeAttribute", " name --> " + arg0);
        }
        _attributes.remove(arg0);
    }

    public boolean isInvokedFilters() {
	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
	    logger.logp(Level.FINE, CLASS_NAME,"isInvokedFilters", " _invokedFilters --> " + _invokedFilters);
	}
	return _invokedFilters;
    }

    public void setInvokedFilters(boolean filters) {
	_invokedFilters = filters;
	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"setInvokedFilters", " invokedFilters --> " + _invokedFilters);
       }
    }

    public boolean isCompleted() {
	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"isCompleted", " completed --> " + completed);
	}
	return completed;
    }
	
    public void setCompleted(boolean completed){
	this.completed = completed;
	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"setCompleted", " completed --> " + completed);
	}
    }
	
    public boolean isArdRequest(){
        return ardRequest;
    }
	
    public void setArdRequest(boolean ardRequest){
	this.ardRequest = ardRequest;
}

    public void setAsyncMode(boolean b) {
       this.startAsync = b;
    }

    public IExtendedRequest getCurrentThreadsIExtendedRequest() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getCurrentThreadsIExtendedRequest", " currentThreadsIExtendedRequest --> " + this.currentThreadsIExtendedRequest);
        }
        return this.currentThreadsIExtendedRequest;
    }

    public AsyncContext getAsyncContext() {
        return this.asyncContext;
    }

    public void setAsyncContext(AsyncContext asyncContext2) {
        this.asyncContext = asyncContext2;
    }

    public void setCurrentThreadsIExtendedResponse(IExtendedResponse hres) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setCurrentThreadsIExtendedResponse", " new currentThreadsIExtendedResponse --> " + hres);
        }
        this.currentThreadsIExtendedResponse = hres;
    }
    
    /**
     * Allows Cookie attributes to be set on a particular Cookie specified by the
     * cookieName parameter.
     *
     * Once the Cookie has been added to the Response the removeCookieAttribute
     * method is called for the same cookieName.
     *
     * Currently the only Cookie attribute that is supported by the runtime here
     * is the SameSite Cookie attribute.  All other existing Cookie attributes must 
     * be added via the Cookie API. Using this API to add anything but the SameSite attribute
     * will be ignored.
     *
     * The cookieAttribute should be in the form: attributeName=attributeValue.
     *
     * @param cookieName - The Cookie name to add the attribute to.
     * @param cookieAttributes - The Cookie attributes to be added in  the form: attributeName = attributeValue.  Currently, only SameSite=Lax|None|Strict is supported.
     */
    public void setCookieAttributes(String cookieName, String cookieAttributes) {
        String methodName = "setCookieAttributes";

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, " cookieName --> " + cookieName + " cookieAttribute --> " + cookieAttributes);
        }

        String[] attribute = cookieAttributes.split("=");
        /*
         * Beta: This method will start accepting "Parititioned" as a new attribute. It will need to be in the form "partitioned=true/false".
         */
        if (ProductInfo.getBetaEdition()) { 
            if (!(attribute[0].equals("SameSite") || attribute[0].equals("Partitioned"))) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, " Only SameSite attribute and Partitioned attribute are supported at this time.");
                return;
            }
        } else {
            if (!attribute[0].equals("SameSite")) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, " Only SameSite attribute is supported at this time.");
                return;
            }
        }

        logger.logp(Level.FINE, CLASS_NAME, methodName, "Num Attr values: " + attribute.length) ;
        String tempFixedValue = null ; 
       if (attribute.length > 1) {
           tempFixedValue = attribute[1] ; 
        }
        String fixedValue = tempFixedValue ; 

        if (cookieAttributesMap == null || cookieAttributesMap.isEmpty()) {
            cookieAttributesMap = new HashMap<String,HashMap<String,String>>();
            cookieAttributesMap.put(cookieName, new HashMap<String,String>() {{
                put(attribute[0], fixedValue);
            }});
        } else {
            HashMap<String,String> existingAttributesMap = cookieAttributesMap.get(cookieName);
            if(existingAttributesMap == null) { // avoid unit test NPEs
                existingAttributesMap = new HashMap<String,String>();
            }
            existingAttributesMap.put(attribute[0], fixedValue);
            cookieAttributesMap.put(cookieName, existingAttributesMap);
        }

        HashMap<String,String> existingAttributesMap = cookieAttributesMap.get(cookieName);
        if(existingAttributesMap == null) {
            existingAttributesMap = new HashMap<String,String>();
        }
        existingAttributesMap.put(attribute[0], attribute[1]);
        cookieAttributesMap.put(cookieName, existingAttributesMap);
    }
    
    /**
     * Return the Cookie attributes associated with the provided cookieName that were
     * added via the setCookieAttributes()
     *
     * @param cookieName - The name of the Cookie the attributes were set for.
     * @return - The Cookie attributes associated with the specified Cookie name.
     */
    public String getCookieAttributes(String cookieName) {
        String methodName = "getCookieAttributes";

        if (cookieAttributesMap == null || cookieAttributesMap.get(cookieName) == null) {
                return null;
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, " cookieName --> " + cookieName);
        }

        /*
        * If multiple attributes exist for a cookie, then they will be returned semi-colon delimited key value pairs.
        * For example: attributeName1=attributeValue1;attributeName2=attributeValue2
        * 
        * If no cookie attributes exist, null is returned;
        */
        String cookieAttributes = cookieAttributesMap.get(cookieName).entrySet()
                                                     .stream().map(e->e.getKey() + "=" + e.getValue())
                                                     .collect(java.util.stream.Collectors.joining(";"));

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, " cookieAttribute --> " + cookieAttributes);
        }

        return cookieAttributes;
    } 
    
    /**
     * Removes the Cookie attributes associated with the cookieName that were added via 
     * setCookieAttributes().  It does not remove any other attributes which were added via 
     * the Cookie API.
     *
     * @param cookieName - The name of the Cookie the attributes were set for.
     */
    public void removeCookieAttributes(String cookieName) {
        String methodName = "removeCookieAttributes";

        if (cookieAttributesMap == null) {
                return;
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, " cookieName --> " + cookieName);
        }

        HashMap<String,String> removedAttributes = cookieAttributesMap.remove(cookieName);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, "removedAttribute --> " + removedAttributes);
        }
    }
}
