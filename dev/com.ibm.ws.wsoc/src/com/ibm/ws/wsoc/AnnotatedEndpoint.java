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

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.websocket.CloseReason;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.wsoc.impl.MethodDataImpl;
import com.ibm.ws.wsoc.util.Utils;

/**
 *
 */
public class AnnotatedEndpoint extends Endpoint implements Cloneable {

    private static final TraceComponent tc = Tr.register(AnnotatedEndpoint.class);

    private Class<?> sepClass = null;
    private Object appInstance = null;

    private EndpointMethodHelper onError = null;
    private EndpointMethodHelper onClose = null;
    private EndpointMethodHelper onOpen = null;
    private EndpointMethodHelper onMessageText = null;
    private EndpointMethodHelper onMessageBinary = null;
    private EndpointMethodHelper onMessagePong = null;
    private String endpointPath = null;
    private String requestPath = null;
    private boolean shouldProcessPath = true;

    private static final Class<?>[] messageTextTypesAllowed = { String.class, Reader.class, char.class, Character.class, byte.class, Byte.class, short.class, Short.class,
                                                                int.class, Integer.class,
                                                                long.class, Long.class, float.class, Float.class, double.class, Double.class, boolean.class, Boolean.class };
    private static List<Class<?>> messageTextTypesList = Arrays.asList(messageTextTypesAllowed);

    private static final Class<?>[] messageBinaryTypesAllowed = { ByteBuffer.class, InputStream.class, byte[].class };
    private static List<Class<?>> messageBinaryTypesList = Arrays.asList(messageBinaryTypesAllowed);

    private static final Class<?>[] messagePongTypesAllowed = { PongMessage.class };
    private static List<Class<?>> messagePongTypesList = Arrays.asList(messagePongTypesAllowed);

    public AnnotatedEndpoint() {}

    public void initialize(Class<?> clazz, EndpointConfig endpointConfig) throws DeploymentException {
        this.initialize(clazz, endpointConfig, true);
    }

    public void initialize(Class<?> clazz, EndpointConfig endpointConfig, boolean isServer) throws DeploymentException {
        if (!isValid(clazz, isServer)) {
            String msg = Tr.formatMessage(tc,
                                          "invalid.endpointclass",
                                          clazz.getName());

            Tr.error(tc,
                     "invalid.endpointclass",
                     clazz.getName());

            throw new DeploymentException(msg);
        }
        sepClass = clazz;
        this.shouldProcessPath = isServer;
        if (shouldProcessPath) {
            this.endpointPath = ((ServerEndpointConfig) endpointConfig).getPath();
        }

        processOnMessage(clazz, endpointConfig);

        //process @OnOpen,@OnClose,@OnError
        processOpenCloseError(sepClass);

        //Error case: Endpoint is unusable (and invalid) if both @onOpen and @onMessage is missing. However Endpoint is still usable if @onMessage is missing and @onOpen is present
        //for the use-case where user might just want server endpoint to send messages to the client and not receive any message from the client. In this case, user
        //will still need onOpen(..) in order to get hold of session object.

        if (onOpen == null && onMessageText == null && onMessageBinary == null && onMessagePong == null) {
            String msg = Tr.formatMessage(tc,
                                          "missing.annotation",
                                          clazz.getName());
            Tr.error(tc,
                     "missing.annotation",
                     clazz.getName());
            throw new DeploymentException(msg);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Annotated Endpoint: " + clazz + " initialized with endpoint : " + this.endpointPath + " onMessageText :" + onMessageText + " onMessageBinary :"
                         + onMessageBinary + " onMessagePong :" + onMessagePong + " onOpen :" + onOpen + " onError :" + onError + " onClose :" + onClose);
        }

    }

    public void setAppInstance(Object instance) {
        appInstance = instance;
    }

    public boolean isValid(Class<?> clazz, boolean needsNoArgConstructor) {
        boolean valid = false;
        int modifiers = clazz.getModifiers();
        if (Modifier.isPublic(modifiers) && !Modifier.isAbstract(modifiers)) {
            if (needsNoArgConstructor) {
                Constructor<?>[] constructors = clazz.getConstructors();
                for (Constructor<?> constructor : constructors) {
                    if (constructor.getParameterTypes().length == 0 && Modifier.isPublic(constructor.getModifiers())) {
                        valid = true;
                        break;
                    }
                }
            } else {
                valid = true;
            }
        }
        return valid;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.websocket.Endpoint#onOpen(javax.websocket.Session, javax.websocket.EndpointConfig)
     */
    @Override
    public void onOpen(Session session, EndpointConfig config) {

        if (onOpen != null) {
            try {
                Object args[] = new Object[onOpen.getMethod().getParameterTypes().length];
                MethodData methodData = onOpen.getMethodData();
                //check if method has optional Session parameter
                int sessionIndex = methodData.getSessionIndex();
                if (sessionIndex >= 0) {
                    args[sessionIndex] = session;
                }
                int configIndex = methodData.getEndpointConfigIndex();
                if (configIndex >= 0) {
                    args[configIndex] = config;
                }
                if (shouldProcessPath) {
                    try {
                        //substitute values for @PathParam variables for this method.
                        onOpen.processPathParameters(this, args);
                    } catch (DecodeException e) {
                        /*
                         * If onOpen itself is not successful, onError() should not be invoked as there is no session at this point.
                         */
                        return;
                    }
                }
                onOpen.getMethod().invoke(appInstance, args);

            } catch (IllegalAccessException e) {
                // allow instrumented FFDC to be used here
            } catch (IllegalArgumentException e) {
                // allow instrumented FFDC to be used here
            } catch (InvocationTargetException e) {
                // allow instrumented FFDC to be used here
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.websocket.Endpoint#onOpen(javax.websocket.Session, javax.websocket.EndpointConfig)
     */
    @Override
    public void onClose(Session session, CloseReason closeReason) {
        if (onClose != null) {
            Object args[] = new Object[onClose.getMethod().getParameterTypes().length];
            MethodData methodData = onClose.getMethodData();
            //check if method has optional Session parameter
            int sessionIndex = methodData.getSessionIndex();
            if (sessionIndex >= 0) {
                args[sessionIndex] = session;
            }
            //check if method has optional closeReason parameter
            int reasonIndex = methodData.getCloseReasonIndex();
            if (reasonIndex >= 0) {
                args[reasonIndex] = closeReason;
            }
            if (shouldProcessPath) {
                try {
                    //substitute values for @PathParam values for this method.
                    onClose.processPathParameters(this, args);
                } catch (DecodeException e) {
                    /*
                     * JSR 356 @PathParam section
                     * "If the container cannot decode the path segment appropriately to the annotated path parameter, then the container must raise an DecodeException to the error
                     * handling method of the websocket containing the path segment. [WSC-4.3-6]"
                     * Section
                     * 4.
                     * 5:"If the method itself throws an error, the implementation must pass this error to the onError() method of the endpoint together with the session [WSC-4.5-3]"
                     */
                    this.onError(session, e);
                    return;
                }
            }

            try {
                if (appInstance == null) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "application instance is null in onClose");
                    }
                    return;
                }

                onClose.getMethod().invoke(appInstance, args);

            } catch (IllegalAccessException e) {
                //JSR 356 Section 4.5:  "If the method itself throws an error, the implementation
                //must pass this error to the onError() method of the endpoint together with the session [WSC-4.5-3]"
                this.onError(session, e);
            } catch (IllegalArgumentException e) {
                this.onError(session, e);
            } catch (InvocationTargetException e) {
                this.onError(session, e);
            }
        } else { //onClose method is not defined, hence log the problem in trace
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "onClose() is called on Endpoint class: " + sepClass + " which does not have onClose method defined");
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.websocket.Endpoint#onOpen(javax.websocket.Session, javax.websocket.EndpointConfig)
     */
    @Override
    public void onError(Session session, Throwable thr) {
        if (onError != null) {
            Object args[] = new Object[onError.getMethod().getParameterTypes().length];
            MethodData methodData = onError.getMethodData();
            //check if method has optional Session parameter
            int sessionIndex = methodData.getSessionIndex();
            if (sessionIndex >= 0) {
                args[sessionIndex] = session;
            }
            int throableIndx = methodData.getThrowableIndex();
            if (throableIndx >= 0) {
                args[throableIndx] = thr;
            }
            if (shouldProcessPath) {
                try {
                    //substitute values for @PathParam values for this method.
                    onError.processPathParameters(this, args);
                } catch (DecodeException e) {
                    //We can not call onError() again for this since that will result in infinite loop. This is a double error situation,
                    //1) First exception is original exception 'thr' which onError method is called for
                    //2) Secondly, while processing @OnError method's pathparam, there is an exception. Spec is not clear on how to report both exceptions to the user.
                    //We report DecodeException (@PathParam processing issue) first, so that user can correct their @Pathparam parameter in their app
                    //For e.g onError() method, if user has declared parameter as 'int vipLevel'and passed value for vipLevel as 'xyz' by mistake, DecodeException
                    //will occur here and they need to correct it first anyway. Then, for the next invocation user's onError(..) is called with the exception why the
                    //onError(..) is invoked for.
                    thr = e;
                }
            }

            try {

                if (appInstance == null) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "application instance is null in onError");
                    }
                    return;
                }

                onError.getMethod().invoke(appInstance, args);

            } catch (IllegalAccessException e) {
                // allow instrumented FFDC to be used here
            } catch (IllegalArgumentException e) {
                // allow instrumented FFDC to be used here
            } catch (InvocationTargetException e) {
                // allow instrumented FFDC to be used here
            }
        } else { //onError() is not defined
            //JSR 356 5.2.2 -->"If the developer has not provided an error handling method on an endpoint that is generating errors, this
            //indicates to the implementation that the developer does not wish to handle such errors. In these cases, the
            //container must make this information available for later analysis, for example by logging it. [WSC-5.2.2-3]"
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Trying to report error: '" + thr.getMessage() + "' calling onError(). However onError() is not defined on the endpoint: " + sepClass.getName());
            }
            //log in FFDC since we could not report it back to the user
            FFDCFilter.processException(thr, getClass().getName(), "288", this);
        }
    }

    public EndpointMethodHelper getOnMessageTextMethod() {
        return onMessageText;
    }

    public EndpointMethodHelper getOnMessageBinaryMethod() {
        return onMessageBinary;
    }

    public EndpointMethodHelper getOnMessagePongMethod() {
        return onMessagePong;
    }

    public Object getAppInstance() {
        return appInstance;
    }

    private void processOnMessage(Class<?> clazz, EndpointConfig endpointConfig) throws DeploymentException {
        // get a list of the annotated methods. go through each method until the first match is found
        boolean onMsgBinaryProcessed = false;
        boolean onMsgPongProcessed = false;
        boolean onMsgTextProcessed = false;
        Method[] methods = getDeclaredMethodsPrivileged(clazz);
        EndpointMethodHelper onMessageTextLocal = null;
        EndpointMethodHelper onMessageBinaryLocal = null;
        EndpointMethodHelper onMessagePongLocal = null;

        for (Method method : methods) {
            if (method.isAnnotationPresent(OnMessage.class)) {

                //find pong method
                onMessagePongLocal = discoverOnMessagePongMethodAnnotation(clazz, method);
                if (onMessagePongLocal.getMethodData().getMessageType() == null) {
                    //pong method does not exist. Now find binary message
                    onMessageBinaryLocal = discoverOnMessageBinaryMethodAnnotation(clazz, endpointConfig, method);
                } else {
                    if (onMsgPongProcessed) { //found two onMessage methods with Pong message
                        String msg = Tr.formatMessage(tc,
                                                      "morethanonepong.annotation",
                                                      clazz.getName());
                        Tr.error(tc,
                                 "morethanonepong.annotation",
                                 clazz.getName());
                        throw new DeploymentException(msg);
                    }
                    onMessagePong = onMessagePongLocal;
                    onMsgPongProcessed = true;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc,
                                 "Pong @OnMessage found: Path: " + endpointPath + "Endpoint Class: " + clazz.getName() + " Method name: " + onMessagePong.method.getName()
                                     + " Message Type: "
                                     + onMessagePongLocal.getMethodData().getMessageType()
                                     + " Message index: "
                                     + onMessagePongLocal.getMethodData().getMessageIndex()
                                     + " Session index: " + onMessagePongLocal.getMethodData().getSessionIndex());
                    }
                    continue;
                }

                if (onMessageBinaryLocal.getMethodData().getMessageType() == null) {
                    //binary method does not exist. Now find text message
                    onMessageTextLocal = discoverOnMessageTextMethodAnnotation(clazz, endpointConfig, method);
                } else {
                    if (onMsgBinaryProcessed) { //found two onMessage methods with Binary message
                        String msg = Tr.formatMessage(tc,
                                                      "morethanonebinary.annotation",
                                                      clazz.getName());
                        Tr.error(tc,
                                 "morethanonebinary.annotation",
                                 clazz.getName());
                        throw new DeploymentException(msg);
                    }
                    onMessageBinary = onMessageBinaryLocal;
                    onMsgBinaryProcessed = true;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc,
                                 "Binary @OnMessage found: Path: " + endpointPath + "Endpoint Class: " + clazz.getName() + " Method name: " + onMessageBinary.method.getName()
                                     + " Message Type: "
                                     + onMessageBinary.getMethodData().getMessageType()
                                     + " Message index: "
                                     + onMessageBinary.getMethodData().getMessageIndex()
                                     + " Session index: " + onMessageBinary.getMethodData().getSessionIndex());
                    }
                    //sets maxMessageSize if user has specified in @OnMessage annotation. This is applicable only for whole binary and text messages
                    setMaxMessageSize(method, onMessageBinaryLocal);
                    continue;
                }
                //@OnMessage desn't have pong, binary or text message type, which is invalid
                if (onMessageTextLocal.getMethodData().getMessageType() == null) {
                    String msg = Tr.formatMessage(tc,
                                                  "missing.msgtype.param",
                                                  clazz.getName());
                    Tr.error(tc,
                             "missing.msgtype.param",
                             clazz.getName());
                    throw new DeploymentException(msg);
                } else {
                    if (onMsgTextProcessed) { //found two onMessage methods with Text message
                        String msg = Tr.formatMessage(tc,
                                                      "morethanonetext.annotation",
                                                      clazz.getName());
                        Tr.error(tc,
                                 "morethanonetext.annotation",
                                 clazz.getName());
                        throw new DeploymentException(msg);
                    }
                    onMessageText = onMessageTextLocal;
                    onMsgTextProcessed = true;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc,
                                 "Text @OnMessage found: Path: " + endpointPath + "Endpoint Class: " + clazz.getName() + " Method name: " + onMessageText.method.getName()
                                     + " Message Type: "
                                     + onMessageText.getMethodData().getMessageType()
                                     + " Message index: "
                                     + onMessageText.getMethodData().getMessageIndex()
                                     + " Session index: " + onMessageText.getMethodData().getSessionIndex());
                    }
                    //sets maxMessageSize if user has specified in @OnMessage annotation. This is applicable only for whole binary and text messages
                    setMaxMessageSize(method, onMessageTextLocal);
                    continue;
                }
            }
        }
    }

    private void processOpenCloseError(Class<?> c) throws DeploymentException {
        Method[] methods = getDeclaredMethodsPrivileged(c);
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnOpen.class)) {
                processOnOpen(c, method);
            }
            if (method.isAnnotationPresent(OnClose.class)) {
                processOnClose(c, method);
            }
            if (method.isAnnotationPresent(OnError.class)) {
                processOnError(c, method);
            }
        }
    }

    private void processOnOpen(Class<?> c, Method method) throws DeploymentException {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OnOpen method found in ServerEndpoint class: " + c.getName() + " Method name: " + method.getName());
        }

        if (onOpen != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Two @OnOpen methods on the Annotated endpoint found " + "method 1: " + onOpen.getMethod() + "  method 2: " + method);
            }
            String msg = Tr.formatMessage(tc,
                                          "morethanoneonopen.annotation",
                                          onOpen.getMethod().getName(), method.getName());
            Tr.error(tc,
                     "morethanoneonopen.annotation",
                     onOpen.getMethod().getName(), method.getName());
            throw new DeploymentException(msg);
        }
        onOpen = initializeEpMethodHelper(method);
        MethodData methodData = onOpen.getMethodData();
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            Class<?> param = params[i];
            //check if this param is already processed as @PathParam. If yes, skip to the next param
            if (methodData.getPathParams() != null && methodData.getPathParams().containsKey(i)) {
                continue;
            } else if (param.equals(javax.websocket.Session.class)) { //check if this is a optional session parameter
                methodData.setSessionIndex(i);
            } else if (param.equals(javax.websocket.EndpointConfig.class)) { //check if this is a optional EndpointConfig parameter
                methodData.setEndpointConfigIndex(i);
            } else { //invalid type
                String msg = Tr.formatMessage(tc,
                                              "invalid.onopen.annotation",
                                              onOpen.getMethod().getName(), c.getName());
                Tr.error(tc,
                         "invalid.onopen.annotation",
                         onOpen.getMethod().getName(), c.getName());
                onOpen = null;
                throw new DeploymentException(msg);
            }
        }

    }

    private void processOnClose(Class<?> c, Method method) throws DeploymentException {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OnClose method found in ServerEndpoint class: " + c.getName() + " Method name: " + method.getName());
        }
        if (onClose != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Two @OnClose methods on the same Annotated endpoint " + "method 1: " + onClose.getMethod().getName() + "  method 2: " + method.getName());
            }
            String msg = Tr.formatMessage(tc,
                                          "morethanoneclose.annotation",
                                          c.getName(), onClose.getMethod().getName(), method.getName());
            Tr.error(tc,
                     "morethanoneclose.annotation",
                     c.getName(), onClose.getMethod().getName(), method.getName());
            onClose = null;
            throw new DeploymentException(msg);
        }
        onClose = initializeEpMethodHelper(method);
        MethodData methodData = onClose.getMethodData();
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            Class<?> param = params[i];
            //check if this param is already processed as @PathParam. If yes, skip to the next param
            if (methodData.getPathParams() != null && methodData.getPathParams().containsKey(i)) {
                continue;
            } else if (param.equals(javax.websocket.Session.class)) { //check if this is a optional session parameter
                methodData.setSessionIndex(i);
            } else if (param.equals(javax.websocket.CloseReason.class)) { //check if this is a optional CloseReason parameter
                methodData.setCloseReasonIndex(i);
            } else { //invalid type
                onClose = null;
                String msg = Tr.formatMessage(tc,
                                              "invalid.onclose.annotation",
                                              method.getName(), c.getName());
                Tr.error(tc,
                         "invalid.onclose.annotation",
                         method.getName(), c.getName());
                throw new DeploymentException(msg);
            }
        }
    }

    private void processOnError(Class<?> c, Method method) throws DeploymentException {
        boolean onErrorProcessed = false;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OnError method found in ServerEndpoint class: " + c.getName() + " Method name: " + method.getName());
        }
        if (onError != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Two @OnError methods on the same Annotated endpoint " + c.getName() + " method 1: " + onError.getMethod() + "  method 2: " + method);
            }
            String msg = Tr.formatMessage(tc,
                                          "morethanoneerror.annotation",
                                          c.getName(), onError.getMethod().getName(), method.getName());
            Tr.error(tc,
                     "morethanoneerror.annotation",
                     c.getName(), onError.getMethod().getName(), method.getName());
            onError = null;
            throw new DeploymentException(msg);
        }
        onError = initializeEpMethodHelper(method);
        MethodData methodData = onError.getMethodData();
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            Class<?> param = params[i];
            //check if this param is already processed as @PathParam. If yes, skip to the next param
            if (methodData.getPathParams() != null && methodData.getPathParams().containsKey(i)) {
                continue;
            } else if (param.equals(javax.websocket.Session.class)) { //check if this is a optional session parameter
                methodData.setSessionIndex(i);
            } else if (param.equals(java.lang.Throwable.class)) { //check if mandatory parameter Throwable exists
                methodData.setThrowableIndex(i);
                onErrorProcessed = true;
            } else { //invalid type
                onError = null;
                String msg = Tr.formatMessage(tc,
                                              "invalid.onerror.annotation",
                                              method.getName(), c.getName());
                Tr.error(tc,
                         "invalid.onerror.annotation",
                         method.getName(), c.getName());
                throw new DeploymentException(msg);
            }
        }
        //OnError method is valid only if mandatory parameter 'Throwable' exists. For e.g this declaration is invalid -> onError()
        if (!onErrorProcessed) {
            onError = null;
            String msg = Tr.formatMessage(tc,
                                          "missing.throwable",
                                          method.getName(), c.getName());
            Tr.error(tc,
                     "missing.throwable",
                     method.getName(), c.getName());
            throw new DeploymentException(msg);
        }
    }

    private EndpointMethodHelper initializeEpMethodHelper(Method method) throws DeploymentException {
        EndpointMethodHelper epMethodHelper = new EndpointMethodHelper(method);
        MethodData methodData = new MethodDataImpl();
        epMethodHelper.setMethodData(methodData);
        if (shouldProcessPath) {
            epMethodHelper.setEndpointPath(endpointPath);
            epMethodHelper.introspectPathParams();
        }
        return epMethodHelper;
    }

    private EndpointMethodHelper discoverOnMessagePongMethodAnnotation(Class<?> c, Method method) throws DeploymentException {
        MethodData methodData = null;
        EndpointMethodHelper methodHelper = null;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "@OnMessage method found in ServerEndpoint class: " + c.getName() + " Method name: " + method.getName());
        }
        methodHelper = new EndpointMethodHelper(method);
        methodData = new MethodDataImpl();
        methodHelper.setMethodData(methodData);

        if (shouldProcessPath) {
            methodHelper.setEndpointPath(endpointPath);
            methodHelper.introspectPathParams();
        }

        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            Class<?> param = params[i];
            //check if this param is already processed as @PathParam. If yes, skip to the next param
            if (methodData.getPathParams() != null && methodData.getPathParams().containsKey(i)) {
                continue;
            } else if (param.equals(javax.websocket.Session.class)) { //check if this is a session parameter
                methodData.setSessionIndex(i);
            } else if (messagePongTypesList.contains(param)) {
                if (methodData.getMessageType() == null) {
                    methodData.setMessageType(param);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc,
                                 "In discoverOnMessagePongMethodAnnotation: Path: " + endpointPath + "Endpoint Class: " + c.getName() + " Method name: " + method.getName()
                                     + " MethodHelper instance: "
                                     + methodHelper
                                     + " Setting MessageType in methodData: "
                                     + methodData
                                     + " param: "
                                     + param);
                    }
                    methodData.setMessageIndex(i);
                }
            } else { //not an expected parameter for this method
                if (methodData.getMessageType() != null) { //check if we already found pong msg type parameter
                    //invalid param type found in a pong msg type method.
                    String msg = Tr.formatMessage(tc,
                                                  "invalid.pong.annotation",
                                                  method.getName(), c.getName(), param.getName());
                    Tr.error(tc,
                             "invalid.pong.annotation",
                             method.getName(), c.getName(), param.getName());
                    throw new DeploymentException(msg);
                }
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Class name: " + c.getName() + " Method name: " + methodHelper.method.getName() + " Message Type: " + methodData.getMessageType() + " Message index: "
                         + methodData.getMessageIndex()
                         + " Session index: " + methodData.getSessionIndex());
        }
        return methodHelper;
    }

    private EndpointMethodHelper discoverOnMessageBinaryMethodAnnotation(Class<?> c, EndpointConfig endpointConfig, Method method) throws DeploymentException {
        List<Class<? extends Decoder>> decoders = endpointConfig.getDecoders();
        if (decoders == null) {
            decoders = Collections.emptyList();
        }
        EndpointMethodHelper methodHelper = null;

        methodHelper = new EndpointMethodHelper(method);
        MethodData methodData = new MethodDataImpl();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc,
                     "In discoverOnMessageBinaryMethodAnnotation: Path: " + endpointPath + "Endpoint Class: " + c.getName() + " Method name: " + method.getName()
                         + " New MethodHelper instance: "
                         + methodHelper
                         + " New methodData instance: "
                         + methodData);
        }
        methodHelper.setMethodData(methodData);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc,
                     "In discoverOnMessageBinaryMethodAnnotation: Path: " + endpointPath + "Endpoint Class: " + c.getName() + " Method name: " + method.getName()
                         + " MethodHelper instance: "
                         + methodHelper
                         + " Setting methodData instance to method helper: "
                         + methodData);
        }
        if (shouldProcessPath) {
            methodHelper.setEndpointPath(endpointPath);
            methodHelper.introspectPathParams();
        }

        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            Class<?> param = params[i];
            //check if this param is already processed as @PathParam. If yes, skip to the next param
            if (methodData.getPathParams() != null && methodData.getPathParams().containsKey(i)) {
                continue;
            } else if (param.equals(javax.websocket.Session.class)) {//check if this is a session parameter
                methodData.setSessionIndex(i);
            }
            //Spec: OnMessage method can have byte[] and boolean pair (optional), or ByteBuffer and boolean pair(optional) to receive the message in parts
            //check if this is expected binary types or it's boolean pair
            else if (messageBinaryTypesList.contains(param) || param.equals(boolean.class)) {
                if (methodData.getMessageType() == null) {
                    //set the boolean pair param index
                    if (param.equals(boolean.class)) {
                        methodData.setMsgBooleanPairIndex(i);
                    } else {
                        //found binary msg type. set it.
                        methodData.setMessageType(param);
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc,
                                     "In discoverOnMessageBinaryMethodAnnotation: Path: " + endpointPath + "Endpoint Class: " + c.getName() + " Method name: " + method.getName()
                                         + " MethodHelper instance: "
                                         + methodHelper
                                         + " Setting MessageType in methodData: "
                                         + methodData
                                         + " param: "
                                         + param);
                        }
                        methodData.setMessageIndex(i);
                    }
                } else { //found binary type param already, check if this a boolean pair for the binary type
                    if (param.equals(boolean.class)) {
                        if (methodData.getMessageType().equals(byte[].class) || methodData.getMessageType().equals(java.nio.ByteBuffer.class)) {
                            methodData.setMsgBooleanPairIndex(i);
                        } else {
                            //invalid param type found in a pong msg type method.
                            String msg = Tr.formatMessage(tc,
                                                          "invalid.binary.param",
                                                          method.getName(), c.getName(), param.getClass().getName());
                            Tr.error(tc,
                                     "invalid.binary.param",
                                     method.getName(), c.getName(), param.getClass().getName());
                            throw new DeploymentException(msg);

                        }
                    } else {
                        //found another binary type param. This is a user error as there can be only one binary type param type in a method
                        String msg = Tr.formatMessage(tc,
                                                      "morethanone.binary.param",
                                                      method.getName(), c.getName(), param.getClass().getName());
                        Tr.error(tc,
                                 "morethanone.binary.param",
                                 method.getName(), c.getName(), param.getClass().getName());
                        throw new DeploymentException(msg);
                    }
                }
            } else if (decoders.size() > 0) {
                // no match found yet, so look for a match with a Decoder type
                for (Class<? extends Decoder> decoder : decoders) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "looking at decoder: " + decoder);
                    }
                    //Section 4.1.3 -"These classes must implement some form of the Decoder interface, and have public no-arg constructors ...."
                    if (!isValid(decoder, true)) {
                        String msg = Tr.formatMessage(tc,
                                                      "invalid.decoderclass",
                                                      decoder.getName(), param.getClass().getName(), c.getName());
                        Tr.error(tc,
                                 "invalid.decoderclass",
                                 decoder.getName(), param.getClass().getName(), c.getName());
                        throw new DeploymentException(msg);
                    }
                    ArrayList<Type> interfaces = new ArrayList<Type>();
                    Utils.getAllInterfaces(decoder, interfaces);
                    Object ta[] = interfaces.toArray();
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "ta[]: " + Arrays.toString(ta));
                    }
                    for (Object t : ta) {
                        if (((Type) t) instanceof ParameterizedType) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, t + " is instanceof ParameterizedType");
                            }
                            ParameterizedType pt = (ParameterizedType) t;
                            Type rawType = pt.getRawType();
                            if (rawType instanceof Class) {
                                Class<?> clazz = (Class<?>) rawType;
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "rawType: " + decoder);
                                }
                                // make sure we are looking at the desired interface
                                if ((clazz.equals(Decoder.Binary.class))) {
                                    Type ta2[] = pt.getActualTypeArguments();
                                    // should only be one actual type
                                    if (ta2.length == 1) {
                                        Type t2 = ta2[0];
                                        Class<?> clazz2 = Utils.getClassByType(t2);
                                        if (clazz2 != null) {
                                            if (param.equals(clazz2)) {
                                                if (tc.isDebugEnabled()) {
                                                    Tr.debug(tc, "Found match: " + clazz2);
                                                }

                                                if (methodData.getMessageType() == null) {
                                                    methodData.setMessageType(param);
                                                    if (tc.isDebugEnabled()) {
                                                        Tr.debug(tc,
                                                                 "In discoverOnMessageBinaryMethodAnnotation: Path: " + endpointPath + "Endpoint Class: " + c.getName()
                                                                     + " Method name: " + method.getName()
                                                                     + " MethodHelper instance: "
                                                                     + methodHelper
                                                                     + " Setting MessageType in methodData: "
                                                                     + methodData
                                                                     + " param: "
                                                                     + param);
                                                    }
                                                    methodData.setMessageIndex(i);
                                                } else {
                                                    if (methodData.getMessageType() != param) { //skip if multiple decoders with same interface are defined for same parameter
                                                        //Found 2nd msg type param with decoder, which is a error case since onMessage can not have more than one msg type param
                                                        String msg = Tr.formatMessage(tc,
                                                                                      "morethanone.message.param",
                                                                                      method.getName(), c.getName(), param.getName());
                                                        Tr.error(tc,
                                                                 "morethanone.message.param",
                                                                 method.getName(), c.getName(), param.getName());
                                                        throw new DeploymentException(msg);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if ((clazz.equals(Decoder.BinaryStream.class))) {
                                    Type ta2[] = pt.getActualTypeArguments();
                                    // should only be one actual type
                                    if (ta2.length == 1) {
                                        Type t2 = ta2[0];

                                        Class<?> clazz2 = Utils.getClassByType(t2);
                                        if (clazz2 != null) {
                                            if (param.equals(clazz2)) {
                                                if (tc.isDebugEnabled()) {
                                                    Tr.debug(tc, "Found match: " + clazz2);
                                                }
                                                if (methodData.getMessageType() == null) {
                                                    methodData.setMessageType(param);
                                                    if (tc.isDebugEnabled()) {
                                                        Tr.debug(tc,
                                                                 "In discoverOnMessageBinaryMethodAnnotation: Path: " + endpointPath + "Endpoint Class: " + c.getName()
                                                                     + " Method name: " + method.getName()
                                                                     + " MethodHelper instance: "
                                                                     + methodHelper
                                                                     + " Setting MessageType in methodData: "
                                                                     + methodData
                                                                     + " param: "
                                                                     + param);
                                                    }
                                                    methodData.setMessageIndex(i);
                                                } else {
                                                    if (methodData.getMessageType() != param) { //skip if multiple decoders with same interface are defined for same parameter
                                                        //Found 2nd msg type param with decoder, which is a error case since onMessage can not have more than one msg type param
                                                        String msg = Tr.formatMessage(tc,
                                                                                      "morethanone.message.param",
                                                                                      method.getName(), c.getName(), param.getName());
                                                        Tr.error(tc,
                                                                 "morethanone.message.param",
                                                                 method.getName(), c.getName(), param.getName());
                                                        throw new DeploymentException(msg);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else { //not an expected parameter for this method
                if (methodData.getMessageType() != null) { //check if we already found pong msg type parameter
                    //invalid param type found in a pong msg type method.
                    String msg = Tr.formatMessage(tc,
                                                  "invalid.binary.param",
                                                  method.getName(), c.getName(), param.getClass().getName());
                    Tr.error(tc,
                             "invalid.binary.param",
                             method.getName(), c.getName(), param.getClass().getName());
                    throw new DeploymentException(msg);
                }
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Class name: " + c.getName() + " Method name: " + methodHelper.method.getName() + " Message Type: " + methodData.getMessageType() + " Message index: "
                         + methodData.getMessageIndex()
                         + " Boolean Pair index: "
                         + methodData.getMsgBooleanPairIndex() + " Session index: " + methodData.getSessionIndex());
        }
        return methodHelper;
    }

    private EndpointMethodHelper discoverOnMessageTextMethodAnnotation(Class<?> c, EndpointConfig endpointConfig, Method method) throws IllegalStateException, DeploymentException {
        List<Class<? extends Decoder>> decoders = endpointConfig.getDecoders();
        if (decoders == null) {
            decoders = Collections.emptyList();
        }
        // get a list of the apps annotated methods. go through each method until the first match is found meeting the OnMessage text rules
        EndpointMethodHelper methodHelper = null;
        MethodData methodData = null;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "@OnMessage method found in ServerEndpoint class: " + c.getName() + " Method name: " + method.getName());
        }
        methodHelper = new EndpointMethodHelper(method);
        methodData = new MethodDataImpl();
        methodHelper.setMethodData(methodData);

        if (shouldProcessPath) {
            methodHelper.setEndpointPath(endpointPath);
            methodHelper.introspectPathParams();
        }

        Class<?>[] params = method.getParameterTypes();
        int booleanIndex = -1;
        for (int i = 0; i < params.length; i++) {
            Class<?> param = params[i];

            //check if this param is already processed as @PathParam. If yes, skip to the next param
            if (methodData.getPathParams() != null && methodData.getPathParams().containsKey(i)) {
                continue;
            }

            //check if this is a session parameter
            else if (param.equals(javax.websocket.Session.class)) {
                methodData.setSessionIndex(i);
            }
            //check if this is a text types or it's boolean pair for string type message
            else if (messageTextTypesList.contains(param)) {
                if (methodData.getMessageType() == null) {
                    //this could be the boolean pair, set it. we are not sure if this is a boolean pair or boolean msg at this point. It will get used in else { }
                    if (param.equals(boolean.class)) {
                        booleanIndex = i;
                    }
                    methodData.setMessageType(param);
                    methodData.setMessageIndex(i);
                } else {
                    //Now we are certain this is a boolean pair, not a boolean message
                    if (param.equals(boolean.class)) {
                        if (methodData.getMessageType().equals(String.class)) {
                            methodData.setMsgBooleanPairIndex(i);
                        } else {
                            String msg = Tr.formatMessage(tc,
                                                          "invalid.text.param",
                                                          method.getName(), c.getName(), param.getClass().getName());
                            Tr.error(tc,
                                     "invalid.text.param",
                                     method.getName(), c.getName(), param.getClass().getName());
                            throw new DeploymentException(msg);

                        }
                    } else if (param.equals(String.class) && !methodData.getMessageType().equals(String.class)) { //check if 2nd param is also string. 2 string msg params are not allowed
                        methodData.setMessageType(param); //found a string param, which means in if {} block was indeed a boolean pair. Hence override the msg type.
                        methodData.setMessageIndex(i);
                        // it's certain what was found in previous loop in if {} was a boolean pair
                        if (booleanIndex != -1) {
                            methodData.setMsgBooleanPairIndex(i);
                        }
                    } else {
                        //found another text type param. This is a user error as there can be only one text type in a method
                        String msg = Tr.formatMessage(tc,
                                                      "morethanone.text.param",
                                                      method.getName(), c.getName(), param.getName());
                        Tr.error(tc,
                                 "morethanone.text.param",
                                 method.getName(), c.getName(), param.getName());
                        throw new DeploymentException(msg);
                    }
                }
            } else if (decoders.size() > 0) {
                // no match found yet, so look for a match with a Decoder type
                for (Class<? extends Decoder> decoder : decoders) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "looking at decoder: " + decoder);
                    }
                    //Section 4.1.3 -"These classes must implement some form of the Decoder interface, and have public no-arg constructors ...."
                    if (!isValid(decoder, true)) {
                        String msg = Tr.formatMessage(tc,
                                                      "invalid.decoderclass",
                                                      decoder.getName(), method.getName(), c.getName());
                        Tr.error(tc,
                                 "invalid.decoderclass",
                                 decoder.getName(), method.getName(), c.getName());
                        throw new DeploymentException(msg);
                    }
                    ArrayList<Type> interfaces = new ArrayList<Type>();
                    Utils.getAllInterfaces(decoder, interfaces);
                    Object ta[] = interfaces.toArray();
                    for (Object t : ta) {
                        if (((Type) t) instanceof ParameterizedType) {
                            ParameterizedType pt = (ParameterizedType) t;
                            Type rawType = pt.getRawType();
                            if (rawType instanceof Class) {
                                Class<?> clazz = (Class<?>) rawType;
                                // make sure we are looking at the desired interface
                                if ((clazz.equals(Decoder.Text.class))) {
                                    Type ta2[] = pt.getActualTypeArguments();
                                    // should only be one actual type
                                    if (ta2.length == 1) {
                                        Type t2 = ta2[0];
                                        Class<?> clazz2 = Utils.getClassByType(t2);
                                        if (clazz2 != null) {
                                            if (param.equals(clazz2)) {
                                                if (tc.isDebugEnabled()) {
                                                    Tr.debug(tc, "Found decoder match: " + clazz2);
                                                }
                                                if (methodData.getMessageType() == null) {
                                                    methodData.setMessageType(param);
                                                    methodData.setMessageIndex(i);
                                                } else {
                                                    if (methodData.getMessageType() != param) { //skip if multiple decoders with same interface are defined for same parameter
                                                        //Found 2nd msg type param with decoder, which is a error case since onMessage can not have more than one msg type param
                                                        String msg = Tr.formatMessage(tc,
                                                                                      "morethanone.message.param",
                                                                                      method.getName(), c.getName(), param.getName());
                                                        Tr.error(tc,
                                                                 "morethanone.message.param",
                                                                 method.getName(), c.getName(), param.getName());
                                                        throw new DeploymentException(msg);
                                                    }
                                                }
                                            }
                                        }
                                    }

                                }
                                if ((clazz.equals(Decoder.TextStream.class))) {
                                    Type ta2[] = pt.getActualTypeArguments();
                                    if (ta2.length == 1) {
                                        Type t2 = ta2[0];
                                        Class<?> clazz2 = Utils.getClassByType(t2);
                                        if (clazz2 != null) {
                                            if (param.equals(clazz2)) {
                                                if (tc.isDebugEnabled()) {
                                                    Tr.debug(tc, "Found decoder match: " + clazz2);
                                                }
                                                if (methodData.getMessageType() == null) {
                                                    methodData.setMessageType(param);
                                                    methodData.setMessageIndex(i);
                                                } else {
                                                    if (methodData.getMessageType() != param) { //skip if multiple decoders with same interface are defined for same parameter
                                                        //Found 2nd msg type param with decoder, which is a error case since onMessage can not have more than one msg type param
                                                        String msg = Tr.formatMessage(tc,
                                                                                      "morethanone.message.param",
                                                                                      method.getName(), c.getName(), param.getName());
                                                        Tr.error(tc,
                                                                 "morethanone.message.param",
                                                                 method.getName(), c.getName(), param.getName());
                                                        throw new DeploymentException(msg);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                //not an expected parameter for this method
                if (methodData.getMessageType() != null) { //check if we already found text msg type parameter
                    //invalid param type found in a pong msg type method.
                    String msg = Tr.formatMessage(tc,
                                                  "invalid.text.param",
                                                  method.getName(), c.getName(), param.getClass().getName());
                    Tr.error(tc,
                             "invalid.text.param",
                             method.getName(), c.getName(), param.getClass().getName());
                    throw new DeploymentException(msg);
                }
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Class name: " + c.getName() + " Method name: " + methodHelper.method.getName() + " Message Type: " + methodData.getMessageType() + " Message index: "
                         + methodData.getMessageIndex() + " Boolean Pair index: "
                         + methodData.getMsgBooleanPairIndex() + " Session index: " + methodData.getSessionIndex());
        }
        return methodHelper;
    }

    /**
     * Returns endpoint Path. For e.g endpoint path is /bookings/{guest-id} for endpoint class with @ServerEndpoint("/bookings/{guest-id}")
     *
     * @return endpoint path
     */
    public String getEndpointPath() {
        return this.endpointPath;
    }

    /**
     * Returns request path for the runtime request.
     * endpoint class with @ServerEndpoint("/bookings/{guest-id}") the request path for this endpoint couldbe be /bookings/JohnSmith
     *
     * @return endpoint path
     */
    public String getRequestPath() {
        return this.requestPath;
    }

    /**
     * endpoint class with @ServerEndpoint("/bookings/{guest-id}") the request path for this endpoint could be /bookings/JohnSmith
     *
     * @param requestPath the requestPath
     *
     */
    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        AnnotatedEndpoint clone = (AnnotatedEndpoint) super.clone();
        if (this.onError != null) {
            clone.onError = (EndpointMethodHelper) this.onError.clone();
        } else {
            clone.onError = null;
        }
        if (this.onClose != null) {
            clone.onClose = (EndpointMethodHelper) this.onClose.clone();
        } else {
            clone.onClose = null;
        }
        if (this.onMessageText != null) {
            clone.onMessageText = (EndpointMethodHelper) this.onMessageText.clone();
        } else {
            clone.onMessageText = null;
        }
        if (this.onMessageBinary != null) {
            clone.onMessageBinary = (EndpointMethodHelper) this.onMessageBinary.clone();
        } else {
            clone.onMessageBinary = null;
        }
        if (this.onMessagePong != null) {
            clone.onMessagePong = (EndpointMethodHelper) this.onMessagePong.clone();
        } else {
            clone.onMessagePong = null;
        }

        clone.endpointPath = String.copyValueOf(endpointPath.toCharArray());

        //no need to clone appInstance/requestPath as they are set at runtime, on a cloned instance.

        return clone;
    }

    public Class<?> getServerEndpointClass() {
        return this.sepClass;
    }

    /*
     * maxMessageSize attribute in @OnMessage: Specifies the maximum size of message in bytes that the method this annotates will be able to process, or -1 to indicate that there
     * is no maximum defined, and therefore it is undefined which means unlimited.
     */
    private void setMaxMessageSize(Method method, EndpointMethodHelper endpointMethodHelper) {
        OnMessage onMsgAnnotation = method.getAnnotation(OnMessage.class);
        Long maxMessageSize = onMsgAnnotation.maxMessageSize();
        // maxMessageSize is -1 if it is not defined.
        if (maxMessageSize < -1) {
            // user has put in an invalid value, so change it to undefined.
            maxMessageSize = Constants.ANNOTATED_UNDEFINED_MAX_MSG_SIZE;
        }

        endpointMethodHelper.getMethodData().setMaxMessageSize(maxMessageSize);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setMaxMessageSize: maxMessageSize from annotation: " + maxMessageSize);
        }
    }

    private Method[] getDeclaredMethodsPrivileged(final Class<?> clazz) {

        return AccessController.doPrivileged(
                                             new PrivilegedAction<Method[]>() {
                                                 @Override
                                                 public Method[] run() {
                                                     return clazz.getDeclaredMethods();
                                                 }
                                             });
    }
}
