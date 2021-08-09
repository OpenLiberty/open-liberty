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
package com.ibm.ws.wsoc.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.ws.wsoc.Constants;
import com.ibm.ws.wsoc.MethodData;
import com.ibm.ws.wsoc.PathParamData;

/**
 * Class which holds data for @OnMessage, @OnError, @OnClose, @OnOpen methods
 */
public class MethodDataImpl implements MethodData, Cloneable {

    private int sessionIndex = -1;
    private int messageIndex = -1;
    private int msgBooleanPairIndex = -1;
    private int closeReasonIndex = -1;
    private int throwableIndex = -1;
    private int endpointConfigIndex = -1;
    private Class<?> msgType;
    private HashMap<Integer, PathParamData> pathParams = new HashMap<Integer, PathParamData>();
    private long maxMessageSize = Constants.ANNOTATED_UNDEFINED_MAX_MSG_SIZE;

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#setSessionIndex(int)
     */
    @Override
    public void setSessionIndex(int index) {
        this.sessionIndex = index;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#getSessionIndex()
     */
    @Override
    public int getSessionIndex() {
        return this.sessionIndex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#setMssageIndex(int)
     */
    @Override
    public void setMessageIndex(int index) {
        this.messageIndex = index;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#getMessageIndex()
     */
    @Override
    public int getMessageIndex() {
        return this.messageIndex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#setMsgBooleanPairIndex(int)
     */
    @Override
    public void setMsgBooleanPairIndex(int index) {
        this.msgBooleanPairIndex = index;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#getMsgBooleanPairIndex()
     */
    @Override
    public int getMsgBooleanPairIndex() {
        return this.msgBooleanPairIndex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#setCloseReasonIndex(int)
     */
    @Override
    public void setCloseReasonIndex(int index) {
        this.closeReasonIndex = index;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#getCloseReasonIndex()
     */
    @Override
    public int getCloseReasonIndex() {
        return this.closeReasonIndex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#setThrowableIndex(int)
     */
    @Override
    public void setThrowableIndex(int index) {
        this.throwableIndex = index;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#getThrowableIndex()
     */
    @Override
    public int getThrowableIndex() {
        return this.throwableIndex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#setMessageType(java.lang.Class)
     */
    @Override
    public void setMessageType(Class<?> type) {
        this.msgType = type;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#getMessageType(java.lang.Class)
     */
    @Override
    public Class<?> getMessageType() {
        return this.msgType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#setEndpointConfigIndex(int)
     */
    @Override
    public void setEndpointConfigIndex(int index) {
        this.endpointConfigIndex = index;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#getEndpointConfigIndex()
     */
    @Override
    public int getEndpointConfigIndex() {
        return this.endpointConfigIndex;
    }

    @Override
    public HashMap<Integer, PathParamData> getPathParams() {
        return this.pathParams;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        MethodDataImpl clone = (MethodDataImpl) super.clone();
        clone.pathParams = new HashMap<Integer, PathParamData>();

        Iterator<Entry<Integer, PathParamData>> iterator = pathParams.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, PathParamData> mapEntry = iterator.next();
            //index of the method argument which has @Pathparam defined
            Integer pathParamIndex = mapEntry.getKey();
            PathParamDataImpl pathParamData = (PathParamDataImpl) ((PathParamDataImpl) mapEntry.getValue()).clone();
            clone.pathParams.put(pathParamIndex, pathParamData);
        }
        return clone;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#setMaxMessageSize(long)
     */
    @Override
    public void setMaxMessageSize(long maxMsgSize) {
        this.maxMessageSize = maxMsgSize;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.MethodData#getMaxMessageSize()
     */
    @Override
    public long getMaxMessageSize() {
        return this.maxMessageSize;
    }
}
