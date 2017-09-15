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
package com.ibm.ws.wsoc;

import java.util.HashMap;

/**
 * data for @OnMessage, @OnError, @OnClose, @OnOpen methods
 */
public interface MethodData extends Cloneable {
    /**
     * Sets the Session parameter index
     * 
     * @param Session
     */
    public void setSessionIndex(int index);

    /**
     * Returns index of the session parameter in the method. First parameter has index 0
     * 
     * @return int index of the session parameter in the method
     */
    public int getSessionIndex();

    /**
     * Sets message type
     * 
     * @param Class<?> message type
     */
    public void setMessageType(Class<?> type);

    /**
     * Returns Message type
     * 
     * @return Class<?> message type
     */
    public Class<?> getMessageType();

    /**
     * Sets Message parameter index in OnMessage method
     * 
     * @param int index
     */
    public void setMessageIndex(int index);

    /**
     * Returns index of the Message parameter in @OnMessage method
     * 
     * @return int index of the Message parameter in @OnMessage method
     */
    public int getMessageIndex();

    /**
     * Sets index of the boolean pair parameter for String message type in @OnMessage method
     * 
     * @param index
     */
    public void setMsgBooleanPairIndex(int index);

    /**
     * Returns index of the boolean pair parameter for String message type in @OnMessage method
     * 
     * @return int index of the session parameter in the method
     */
    public int getMsgBooleanPairIndex();

    /**
     * Sets index of the CloseReason parameter in @OnClose method
     * 
     * @param int index of the CloseReason parameter in @OnClose method
     */
    public void setCloseReasonIndex(int index);

    /**
     * Returns index of the CloseReason parameter in @OnClose method
     * 
     * @return int index of the CloseReason in @OnClose method
     */
    public int getCloseReasonIndex();

    /**
     * Sets index of the Throwable parameter in @OnError method
     * 
     * @param int index of the Throwable parameter in @OnError method
     */
    public void setThrowableIndex(int index);

    /**
     * Returns index of the Throwable parameter in @OnError method
     * 
     * @return index of the Throwable parameter in @OnError method
     */
    public int getThrowableIndex();

    /**
     * Sets index of the EndpointConfig parameter in @OnOpen method
     * 
     * @param int index of the EndpointConfig parameter in @OnOpen method
     */
    public void setEndpointConfigIndex(int index);

    /**
     * Returns index of the EndpointConfig parameter in @OnOpen method
     * 
     * @return index of the EndpointConfig parameter in @OnOpen method
     */
    public int getEndpointConfigIndex();

    public HashMap<Integer, PathParamData> getPathParams();

    /*
     * API doc: "Specifies the maximum size of message in bytes that the method this annotates will be able to process, or -1 to indicate that there is no maximum. The default
     * is -1. This attribute only applies when the annotation is used to process whole messages, not to those methods that process messages in parts or use a stream or reader
     * parameter to handle the incoming message. If the incoming whole message exceeds this limit, then the implementation generates an error and closes the connection using the
     * reason that the message was too big."
     */
    public void setMaxMessageSize(long maxMsgSize);

    public long getMaxMessageSize();
}
