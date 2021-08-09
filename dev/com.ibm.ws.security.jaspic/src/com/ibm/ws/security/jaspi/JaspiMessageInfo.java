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
package com.ibm.ws.security.jaspi;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author IBM Corp.
 * 
 */
public class JaspiMessageInfo implements MessageInfo {

    private Map<?, ?> map;
    private HttpServletRequest req;
    private HttpServletResponse rsp;

    /**
	 * 
	 */
    @SuppressWarnings("unchecked")
    public JaspiMessageInfo() {
        this(new HashMap());
    }

    /**
     * @param map
     */
    public JaspiMessageInfo(Map<?, ?> map) {
        super();
        this.map = map;
    }

    /**
     * @param req
     * @param rsp
     */
    public JaspiMessageInfo(HttpServletRequest req, HttpServletResponse rsp) {
        this();
        this.req = req;
        this.rsp = rsp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.security.auth.message.MessageInfo#getMap()
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map getMap() {
        if (map == null) {
            map = new HashMap();
        }
        return map;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.security.auth.message.MessageInfo#getRequestMessage()
     */
    @Override
    public Object getRequestMessage() {
        return req;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.security.auth.message.MessageInfo#getResponseMessage()
     */
    @Override
    public Object getResponseMessage() {
        return rsp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.security.auth.message.MessageInfo#setRequestMessage(java.lang.Object)
     */
    @Override
    public void setRequestMessage(Object req) {
        this.req = (HttpServletRequest) req;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.security.auth.message.MessageInfo#setResponseMessage(java.lang.Object)
     */
    @Override
    public void setResponseMessage(Object rsp) {
        // TODO behavior change here due to findbugs, it was rsp=rsp, so if something breaks...
        this.rsp = (HttpServletResponse) rsp;
    }

    @Override
    public String toString() {
        return super.toString() + "[" + req + ", " + rsp + ", map=" + map + "]";
    }

}
