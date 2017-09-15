/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.external;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.MessageHandler.Partial;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.wsoc.SessionImpl;

public class SessionExt implements Session {

    SessionImpl impl = null;

    public SessionExt() {

    }

    public void initialize(SessionImpl _impl) {
        impl = _impl;
    }

    @Override
    public void addMessageHandler(MessageHandler handler) throws IllegalStateException {
        impl.addMessageHandler(handler);
    }

    @Override
    public RemoteEndpoint.Async getAsyncRemote() {
        return impl.getAsyncRemote();
    }

    @Override
    public RemoteEndpoint.Basic getBasicRemote() {
        return impl.getBasicRemote();
    }

    @Override
    public WebSocketContainer getContainer() {
        return impl.getContainerExt();
    }

    @Override
    public String getId() {
        return impl.getId();
    }

    @Override
    public int getMaxBinaryMessageBufferSize() {
        return impl.getMaxBinaryMessageBufferSize();
    }

    @Override
    public long getMaxIdleTimeout() {
        return impl.getMaxIdleTimeout();
    }

    @Override
    public int getMaxTextMessageBufferSize() {
        return impl.getMaxTextMessageBufferSize();
    }

    @Override
    public Set<MessageHandler> getMessageHandlers() {
        return impl.getMessageHandlers();
    }

    @Override
    public String getProtocolVersion() {
        return impl.getProtocolVersion();
    }

    @Override
    public String getQueryString() {
        return impl.getQueryString();
    }

    @Override
    public Map<String, List<String>> getRequestParameterMap() {
        return impl.getRequestParameterMap();
    }

    @Override
    public URI getRequestURI() {
        return impl.getRequestURI();
    }

    @Override
    @Sensitive
    public Principal getUserPrincipal() {
        return impl.getUserPrincipal();
    }

    @Override
    @Sensitive
    public Map<String, Object> getUserProperties() {
        return impl.getUserProperties();
    }

    @Override
    public void removeMessageHandler(MessageHandler handler) {
        impl.removeMessageHandler(handler);
    }

    @Override
    public void setMaxBinaryMessageBufferSize(int length) {
        impl.setMaxBinaryMessageBufferSize(length);
    }

    @Override
    public void setMaxIdleTimeout(long milliseconds) {
        impl.setMaxIdleTimeout(milliseconds);
    }

    @Override
    public void setMaxTextMessageBufferSize(int length) {
        impl.setMaxTextMessageBufferSize(length);
    }

    @Override
    public boolean isOpen() {
        return impl.isOpen();
    }

    @Override
    public String getNegotiatedSubprotocol() {
        return impl.getNegotiatedSubprotocol();
    }

    @Override
    public Map<String, String> getPathParameters() {
        return impl.getPathParameters();
    }

    @Override
    public List<Extension> getNegotiatedExtensions() {
        return impl.getNegotiatedExtensions();
    }

    @Override
    /**
     * Return a copy of the Set of all the open web socket sessions that represent connections to the same endpoint to
     * which this session represents a connection. The Set includes the session this method is called on. These sessions
     * may not still be open at any point after the return of this method. For example, iterating over the set at a
     * later time may yield one or more closed sessions. Developers should use session.isOpen() to check.
     * 
     * @return the set of sessions, open at the time of return.
     */
    public Set<Session> getOpenSessions() {
        return impl.getOpenSessions();
    }

    @Override
    public boolean isSecure() {
        return impl.isSecure();
    }

    @Override
    public void close() {
        impl.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, ""), true);
    }

    @Override
    public void close(CloseReason closeReason) {
        impl.close(closeReason, true);

    }

    public SessionImpl getSessionImpl() {
        return impl;
    }

    //websocket 1.1 methods. 
    //[rashmi] TODO revisit if we can get away from declaring this since 1.0 doesn't depend on javax.websocket 1.1 API 
    public <T> void addMessageHandler(Class<T> clazz, Whole<T> handler) {
        throw new RuntimeException(); //TODO come up with WebSocket specific MethodNotFound Runtime Exception. 
    }

    public <T> void addMessageHandler(Class<T> clazz, Partial<T> handler) {
        throw new RuntimeException();
    }
    //end of websocket 1.1 methods
}
