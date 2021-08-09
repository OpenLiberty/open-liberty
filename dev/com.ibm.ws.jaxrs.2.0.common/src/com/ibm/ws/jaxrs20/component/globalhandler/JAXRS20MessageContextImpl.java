/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.component.globalhandler;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.message.Message;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

/**
 *
 */
public class JAXRS20MessageContextImpl implements GlobalHandlerMessageContext {

    private final Message message;

    private final MessageContext messageContext;

    public boolean isServerSide = true;

    public boolean isClientSide = false;

    /**
     * @param winkMessageContext
     */
    public JAXRS20MessageContextImpl(Message message) {
        super();
        this.message = message;
        this.messageContext = new MessageContextImpl(message);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.handler.MessageContext#adapt(java.lang.Class)
     */
    @Override
    public <T> T adapt(Class<T> clazz) {
        if (MessageContext.class.isAssignableFrom(clazz)) {
            return clazz.cast(this);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.handler.MessageContext#containsProperty(java.lang.String)
     */
    @Override
    public boolean containsProperty(String name) {
        return message.containsKey(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.handler.MessageContext#getEngineType()
     */
    @Override
    public String getEngineType() {
        return HandlerConstants.ENGINE_TYPE_JAXRS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.handler.MessageContext#getFlowType()
     */
    @Override
    public String getFlowType() {
        return (String) this.getProperty(HandlerConstants.FLOW_TYPE);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.handler.MessageContext#getHttpServletRequest()
     */
    @Override
    public HttpServletRequest getHttpServletRequest() {
        return messageContext.getHttpServletRequest();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.handler.MessageContext#getHttpServletResponse()
     */
    @Override
    public HttpServletResponse getHttpServletResponse() {
        return messageContext.getHttpServletResponse();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.handler.MessageContext#getProperty(java.lang.String, java.lang.Class)
     */
    @Override
    public Object getProperty(String name) {
        return message.get(name);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.handler.MessageContext#getPropertyNames()
     */
    @Override
    public Iterator<String> getPropertyNames() {
        return message.keySet().iterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.handler.MessageContext#isClientSide()
     */
    @Override
    public boolean isClientSide() {
        return isClientSide;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.handler.MessageContext#isServerSide()
     */
    @Override
    public boolean isServerSide() {
        return isServerSide;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.handler.MessageContext#removeProperty(java.lang.String)
     */
    @Override
    public void removeProperty(String name) {
        message.remove(name);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.handler.MessageContext#setProperty(java.lang.String, java.lang.Object)
     */
    @Override
    public void setProperty(String name, Object value) {
        message.put(name, value);
    }

}
