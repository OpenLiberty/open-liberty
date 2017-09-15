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
package com.ibm.ws.wsoc;

import java.net.URI;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.MessageHandler.Partial;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpointConfig;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.transport.access.TransportConnectionAccess;
import com.ibm.ws.wsoc.WsocConnLink.LINK_STATUS;
import com.ibm.ws.wsoc.WsocConnLink.READ_LINK_STATUS;
import com.ibm.ws.wsoc.external.RemoteEndpointAsyncExt;
import com.ibm.ws.wsoc.external.RemoteEndpointBasicExt;
import com.ibm.ws.wsoc.external.SessionExt;
import com.ibm.ws.wsoc.external.WebSocketContainerExt;
import com.ibm.ws.wsoc.injection.InjectionProvider;
import com.ibm.ws.wsoc.injection.InjectionProvider12;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

public class SessionImpl {

    private static final TraceComponent tc = Tr.register(SessionImpl.class);

    WsocConnLink connLink = null;
    TCPConnectionContext tcpConnectionContext = null;
    VirtualConnection vc = null;
    SessionExt sessionExt = null;
    Endpoint endpoint = null;
    EndpointConfig endpointConfig = null;
    ParametersOfInterest things = null;

    RemoteEndpointAsyncImpl remoteEndpointAsyncImpl = null;
    RemoteEndpointAsyncExt remoteEndpointAsyncExt = null;

    RemoteEndpointBasicImpl remoteEndpointBasicImpl = null;
    RemoteEndpointBasicExt remoteEndpointBasicExt = null;

    WebSocketContainerManager manager = null;
    WebSocketContainerExt container = null;

    // these values are to be set during the initialize, from the container object
    long maxIdleTimeout = 0;
    int maxBinaryMessageBufferSize = 0;
    int maxTextMessageBufferSize = 0;

    HashMap<String, Object> userProperties = null;

    String sessionID = null;

    boolean clientSide = false;
    HashMap<String, String> pathParameters = new HashMap<String, String>();
    SessionIdleTimeout sessionIdleTimeout = null;

    public SessionImpl() {

    }

    public void initialize(Endpoint ep, EndpointConfig epc, TransportConnectionAccess access, SessionExt sesExt, WebSocketContainerExt wsce) {
        this.initialize(ep, epc, access, sesExt, wsce, false);
    }

    public void initialize(Endpoint ep, EndpointConfig epc, TransportConnectionAccess access, SessionExt sesExt, WebSocketContainerExt wsce, boolean clientSide) {

        endpoint = ep;
        endpointConfig = epc;
        sessionExt = sesExt;
        this.clientSide = clientSide;

        connLink = new WsocConnLink();
        connLink.initialize(endpoint, endpointConfig, sessionExt, access, clientSide);

        manager = WebSocketContainerManager.getRef();
        container = wsce;

        maxIdleTimeout = container.getDefaultMaxSessionIdleTimeout();
        maxBinaryMessageBufferSize = container.getDefaultMaxBinaryMessageBufferSize();
        maxTextMessageBufferSize = container.getDefaultMaxTextMessageBufferSize();

        sessionID = manager.generateNewId();
        sessionIdleTimeout = new SessionIdleTimeout(sessionID, maxIdleTimeout, connLink);
    }

    public void setParametersOfInterest(ParametersOfInterest value) {
        things = value;
        connLink.setParametersOfInterest(value);
    }

    public void signalAppOnOpen() {
        signalAppOnOpen(null, false);
    }

    public void signalAppOnOpen(WsByteBuffer remainingBuf, boolean runWithDoPriv) {

        // doPriv is needed if this method  is called from client/User code, since the user code on the stack will not have the right privileges.
        // if called form server code, then all modules on the stack should have the right privileges, and no use to slow down the code with doPriv blocks        
        ClassLoader originalCL = null;

        ComponentMetaDataAccessorImpl cmdai = null;
        boolean appActivateResult = false;
        InjectionProvider ip = null;
        InjectionProvider12 ip12 = null;

        connLink.setLinkStatusesToOK();

        connLink.setEndpointManager(things.getEndpointManager());

        // Not sure if we should do this right before or right after onOpen, spec is ambigioius, seems like right before is better.
        userProperties = new HashMap<String, Object>();
        things.setUserProperties(userProperties);

        things.setSessionID(sessionID);

        // add session to the list of open endpoints
        if (!clientSide) {
            EndpointManager epm = things.getEndpointManager();
            epm.addSession(endpoint, sessionExt);
        }

        // setup the context class loader and Component Metadata for the app to use during onOpen
        // also need to store and restore the current context classloader and not leak out the Component Metadata 
        if (runWithDoPriv) {
            final Thread t = Thread.currentThread();
            originalCL = AccessController.doPrivileged(
                            new PrivilegedAction<ClassLoader>() {
                                @Override
                                public ClassLoader run() {
                                    ClassLoader cl = t.getContextClassLoader();
                                    Thread.currentThread().setContextClassLoader(things.getTccl());
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, "classloader is: " + Thread.currentThread().getContextClassLoader());
                                    }
                                    return cl;
                                }
                            }
                            );
        } else {
            originalCL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(things.getTccl());
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "classloader is: " + Thread.currentThread().getContextClassLoader());
            }
        }

        ComponentMetaData cmd = things.getCmd();
        if (cmd != null) {
            cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
            cmdai.beginContext(cmd);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "componentMetaData is: " + cmd);
            Tr.debug(tc, "classloader is: " + Thread.currentThread().getContextClassLoader());
            Tr.debug(tc, "endpoint URI is: " + things.getURI());
        }

        // if CDI 1.2 is loaded, don't need to do anything here
        ip12 = ServiceManager.getInjectionProvider12();
        if (ip12 == null) {
            // only try OWB CDI 1.0 if 1.2 is not loaded
            // process possible CDI session scope injection objects
            ip = ServiceManager.getInjectionProvider();
            if (ip != null) {
                HttpSession httpSession = things.getHttpSession();

                // Activate Application Scope
                appActivateResult = ip.activateAppContext(cmd);

                // Session Scope needs started every time because thread could have an older deactivated Session scope on it
                if (httpSession != null) {
                    ip.startSesContext(httpSession);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "thread ID: " + Thread.currentThread().getId() + "Session ID: " + httpSession.getId());
                    }
                } else if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Attempted to use sessions scope when there was no valid HttpSession, guess the HttpSession expired?");
                }
            }
        }
        else if (tc.isDebugEnabled()) {
            Tr.debug(tc, "InjectionProvider was null");
        }

        //now session is open, start the idle timeout for this session
        sessionIdleTimeout.restartIdleTimeout(this.getMaxIdleTimeout());

        connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
        endpoint.onOpen(sessionExt, endpointConfig);

        // if CDI 1.2 is loaded, don't need to do anything here
        if (ip12 == null) {
            if (ip != null) {
                // if cdi injection is being used, then de-activate the application scopes only if our code did the activate
                if (appActivateResult == true) {
                    ip.deActivateAppContext();
                }

                // attempt to deactivate the session scope always
                ip.deActivateSesContext();
            }
        }

        // remove access to the Component Metadata and put back the original classloader 
        if (cmdai != null) {
            cmdai.endContext();
        }
        Thread.currentThread().setContextClassLoader(originalCL);

        // Once we do this, we could return on another thread right away with read data.  So be careful about putting any session logic after
        // this start reading call.
        if (remainingBuf != null) {
            connLink.processDataThenStartRead(remainingBuf);
        } else {
            connLink.startReading();
        }

    }

    /* ***** methods in support of Session and SessionExt ***** */

    public void addMessageHandler(MessageHandler handler) throws IllegalStateException {
        connLink.addMessageHandler(handler);
    }

    public synchronized RemoteEndpoint.Async getAsyncRemote() {

        if (remoteEndpointAsyncImpl == null) {
            if (connLink != null) {
                remoteEndpointAsyncImpl = new RemoteEndpointAsyncImpl();
                remoteEndpointAsyncImpl.initialize(connLink);
                remoteEndpointBasicImpl = new RemoteEndpointBasicImpl();
                remoteEndpointBasicImpl.initialize(connLink);
            }
        }

        if (remoteEndpointAsyncImpl != null) {
            remoteEndpointAsyncExt = new RemoteEndpointAsyncExt(remoteEndpointAsyncImpl, remoteEndpointBasicImpl);
            return remoteEndpointAsyncExt;
        }

        // should never fall through to here,  so only debug.
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "unexpectedly not able to create RemoteEndpoint.Async object ");
        }
        return null;
    }

    public synchronized RemoteEndpoint.Basic getBasicRemote() {
        if (remoteEndpointBasicImpl == null) {
            if (connLink != null) {
                remoteEndpointBasicImpl = new RemoteEndpointBasicImpl();
                remoteEndpointBasicImpl.initialize(connLink);
            }
        }

        if (remoteEndpointBasicImpl != null) {
            remoteEndpointBasicExt = new RemoteEndpointBasicExt(remoteEndpointBasicImpl);
            return remoteEndpointBasicExt;
        }

        // should never fall through to here,  so only debug.
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "unexpectedly not able to create RemoteEndpoint.Basic object ");
        }
        return null;
    }

    public Set<MessageHandler> getMessageHandlers() {
        if (connLink != null) {
            return connLink.getMessageHandlers();
        }
        return null;
    }

    public void removeMessageHandler(MessageHandler handler) {
        if (connLink != null) {
            connLink.removeMessageHandler(handler);
        }
    }

    public long getMaxIdleTimeout() {
        return maxIdleTimeout;
    }

    public void setMaxIdleTimeout(long timeoutMillis) {
        maxIdleTimeout = timeoutMillis;
        //user has changed the timeout setting. Re-start idle timeout
        sessionIdleTimeout.restartIdleTimeout(maxIdleTimeout);
    }

    public long getDefaultAsyncSendTimeout() {
        return container.getDefaultAsyncSendTimeout();
    }

    public int getMaxBinaryMessageBufferSize() {
        return maxBinaryMessageBufferSize;
    }

    public void setMaxBinaryMessageBufferSize(int size) {
        maxBinaryMessageBufferSize = size;
    }

    public int getMaxTextMessageBufferSize() {
        return maxTextMessageBufferSize;
    }

    public void setMaxTextMessageBufferSize(int size) {
        maxTextMessageBufferSize = size;
    }

    public WebSocketContainer getContainerExt() {
        return container;
    }

    public URI getRequestURI() {
        return things.getURI();
    }

    public Map<String, List<String>> getRequestParameterMap() {
        return things.getParameterMap();
    }

    public String getQueryString() {
        return things.getQueryString();
    }

    public Principal getUserPrincipal() {
        return things.getUserPrincipal();
    }

    public String getId() {
        return sessionID;
    }

    public String getProtocolVersion() {
        return things.getWsocProtocolVersion();
    }

    public Map<String, Object> getUserProperties() {
        return things.getUserProperties();
    }

    public String getNegotiatedSubprotocol() {
        return things.getAgreedSubProtocol();
    }

    public List<Extension> getNegotiatedExtensions() {
        return things.getNegotiatedExtensions();
    }

    public Set<Session> getOpenSessions() {

        EndpointManager epm = things.getEndpointManager();
        if (epm == null) {
            Set<Session> empty = new HashSet<Session>();
            return empty;
        }
        return epm.getOpenSessions(endpoint);
    }

    public boolean isOpen() {
        if (connLink.getLinkStatus() == LINK_STATUS.IO_OK) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * We have a lot of different ways to close now. I don't think we should read for the return close frame and closing the connection now
     * * when the application is stopping makes sense. best way to do that (for now) is to use this close..
     */
    public void closeBecauseAppStopping(CloseReason cr) {
        if (connLink.getLinkStatus() == LINK_STATUS.IO_OK) {
            try {
                connLink.close(cr, true, true);
            } catch (RuntimeException e) {
                if (connLink.getLinkStatus() == LINK_STATUS.IO_OK) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Runtime exception during application stop close.   IO status is ok so throwing exception.");
                    }
                    throw e;
                }
                else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc,
                                 "Runtime exception during application stop close.   IO status is not ok so likely an exception during server shutdown, not throwing exception.");
                    }
                }

            }
        }
        else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc,
                         "Application stopped and tried to close connection, but IO status is not OK which is indicative of server shutting down, not attempting connection close.");
            }

        }
    }

    public void close(CloseReason cr, boolean outgoing) {
        connLink.finishReadBeforeClose(sessionIdleTimeout);
        Object key = null;

        if (outgoing) {
            connLink.outgoingCloseConnection(cr);
        }

        if (endpoint instanceof AnnotatedEndpoint) {
            key = ((AnnotatedEndpoint) endpoint).getAppInstance();
        } else {
            key = endpoint;
        }
        if (key != null) {
            WebSocketContainerManager.getRef().releaseCC(key);
        }
    }

    public void internalDestory() {

        things.setUserProperties(null);

        if (connLink != null) {
            connLink.destroy(null);
        }

    }

    protected void setPathParameters() {
        String[] endpointURIParts = null;
        //endpoint path. e.g /basic/bookings/{guest-id}
        if (endpoint instanceof AnnotatedEndpoint) { //annotated
            endpointURIParts = ((AnnotatedEndpoint) endpoint).getEndpointPath().split("/");
        } else { //programmatic
            endpointURIParts = ((ServerEndpointConfig) endpointConfig).getPath().split("/");
        }
        //incoming request uri path. e.g /bookings/JohnDoe
        String[] requestURIParts = getRequestURI().getPath().split("/");
        int i = endpointURIParts.length - 1;
        int j = requestURIParts.length - 1;
        //start to compare the endpoint path and request uri path, starting from the last segment because
        //request uri at this point of execution does not have context root of the webapp, which in this case is '/basic' 
        //and endpoint uri on the other hand does have the context root of the webapp at this point of execution.
        while (i > 1) { //skipping the first part because first part of split("/") for /../../.. is always an empty string
            if (endpointURIParts[i].startsWith("{") && endpointURIParts[i].endsWith("}")) {
                String endpointPart = endpointURIParts[i].substring(1, endpointURIParts[i].length() - 1); //guest-id
                pathParameters.put(endpointPart, requestURIParts[j]); //Map will have pathName, pathvalue --> guest-id,JohnDoe
            }
            i--;
            j--;
        }
    }

    public Map<String, String> getPathParameters() {
        return this.pathParameters;
    }

    public boolean isSecure() {
        return things.isSecure();
    }

    public String getHttpSessionID() {
        return things.getHttpSessionID();
    }

    public void markHttpSessionInvalid() {
        things.setHttpSession(null);
    }

    //websocket 1.1 methods
    public <T> void addMessageHandler(Class<T> clazz, Whole<T> handler) {
        connLink.addMessageHandler(clazz, handler);
    }

    public <T> void addMessageHandler(Class<T> clazz, Partial<T> handler) {
        connLink.addMessageHandler(clazz, handler);
    }
}
