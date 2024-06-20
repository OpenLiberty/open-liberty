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
package com.ibm.ws.zos.channel.wola.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessage;

/**
 * Dispatches a WOLA request into the EJB container and writes the response
 * back to the client.
 */
public class WOLARequestDispatcher implements Runnable {

    /**
     * A reference to the connection from which this request was received.
     */
    private final WolaConnLink wolaConnectionLink;

    /**
     * The WOLA message to dispatch.
     */
    private final WolaMessage wolaMessage;

    /**
     * List of preInvoke/postInvoke interceptors (e.g security)
     */
    private Set<WolaRequestInterceptor> wolaRequestInterceptors = Collections.emptySet();

    /**
     * CTOR.
     *
     * @param wolaConnectionLink
     * @param wolaMessage
     */
    public WOLARequestDispatcher(WolaConnLink wolaConnectionLink, WolaMessage wolaMessage) {
        this.wolaConnectionLink = wolaConnectionLink;
        this.wolaMessage = wolaMessage;
    }

    /**
     * Inject.
     */
    protected WOLARequestDispatcher setWolaRequestInterceptors(Set<WolaRequestInterceptor> wolaRequestInterceptors) {
        this.wolaRequestInterceptors = wolaRequestInterceptors;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {

        // Do the JNDI lookup...
        WOLAInboundTarget ejb = null;
        try {
            WOLAChannelFactoryProvider provider = WOLAChannelFactoryProvider.getInstance();
            if (provider != null) {
                WOLAJNDICache cache = provider.getWolaJndiCache();
                ejb = cache.jndiLookup(wolaMessage.getServiceNameAsEBCDICBytes());
            } else {
                throw new NamingException("An instance of WOLAChannelFactoryProvider could not be found");
            }
        } catch (Exception e) {
            writeResponse(buildJndiExceptionResponse(e));
            return;
        }

        // Run preInvoke interceptors...
        InvokeState invokeState = preInvoke(wolaMessage);

        if (invokeState.responseException == null) {
            // None of the preInvoke interceptors threw an exception,
            // so let's invoke the EJB...
            try {
                invokeState.ejbResponseData = ejb.execute(wolaMessage.getDataArea());
            } catch (Exception e) {
                invokeState.responseException = e;
            }
        }

        // Run postInvoke interceptors...
        postInvoke(invokeState);

        if (invokeState.responseException == null) {
            // Normal response
            writeResponse(buildNormalResponse(invokeState.ejbResponseData));
        } else {
            writeResponse(buildExceptionResponse(invokeState.responseException));
        }

    }

    /**
     * @return a normal (non-exception) WOLA response message.
     */
    private WolaMessage buildNormalResponse(byte[] responseBytes) {
        return wolaMessage.buildResponse().appendDataArea(responseBytes);
    }

    /**
     * Build a JNDI exception response. If the JNDI lookup fails (with the target service name),
     * a RC = 8 RSN = 34 should be returned in the output message (what tWAS did).
     *
     * @return A WOLA JNDI exception response message.
     */
    private WolaMessage buildJndiExceptionResponse(Exception e) {
        return wolaMessage.buildResponse().setReturnCodeReasonCode(8, 34);
    }

    /**
     * Build an exception response message (most likely the application threw an exception,
     * although it's possible that some other error occurred during the invoke).
     *
     * Return RC = 8 RSN = 44, which is what tWAS used when an exception was encountered
     * in the EJB.
     *
     * @return A WOLA exception response message.
     */
    private WolaMessage buildExceptionResponse(Exception e) {
        return wolaMessage.buildResponse().setReturnCodeReasonCode(8, 44);
    }

    /**
     *
     * preInvoke/invoke/postInvoke scenarios
     * 1. An interceptor in preInvoke throws an exception?
     * - must run postInvoke for all interceptors that were preInvoked.
     * - return preInvoke exception as response
     * 2. An interceptor in postInvoke throws an exception?
     * - FFDC & ignore it. continue to run remaining interceptors.
     * - return normal response
     * 3. invoke throws an exception
     * - must run postInvoke for all interceptors
     * - return exception response
     */
    protected InvokeState preInvoke(WolaMessage wolaMessage) {
        InvokeState retMe = new InvokeState();

        try {
            for (WolaRequestInterceptor interceptor : wolaRequestInterceptors) {
                retMe.interceptorTokenMap.put(interceptor, interceptor.preInvoke(wolaMessage));
            }
        } catch (Exception e) {
            // If any interceptor throws an exception, remember it, stop calling
            // other interceptors, and return.  postInvoke will take care of calling
            // postInvoke on only the interceptors that were preInvoked (i.e. only those
            // interceptors in the tokenMap).
            retMe.responseException = e;
        }

        return retMe;
    }

    /**
     * Run postInvoke on all interceptors in the InvokeState interceptorTokenMap.
     */
    protected void postInvoke(InvokeState invokeState) {

        for (Map.Entry<WolaRequestInterceptor, Object> entry : invokeState.interceptorTokenMap.entrySet()) {
            try {
                entry.getKey().postInvoke(entry.getValue(), invokeState.responseException);
            } catch (Exception e) {
                // No exceptions are allowed to percolate from postInvoke (except Errors)
                // Let FFDC log the ex and continue.
            }
        }
    }

    /**
     * Write the given wolaResponseMessage to the pipe, er, downstream channel.
     */
    private void writeResponse(WolaMessage wolaResponseMessage) {
        try {
            wolaConnectionLink.getDeviceLinkChannelAccessor().syncWrite(wolaResponseMessage.toByteBuffer());
        } catch (IOException ioe) {
            // TODO: close the connection?
        }
    }

    /**
     * Helper object maintains state across preInvoke/invoke/postInvoke.
     */
    protected static class InvokeState {

        /**
         * This could be an exception thrown by the EJB or thrown by one of the
         * preInvoke interceptors. As soon as a preInvoke interceptor throws an
         * exception, request processing stops, postInvoke is called on only those
         * interceptors for which preInvoke was called, and the exception is
         * sent back to the client.
         */
        Exception responseException;

        /**
         * This token map serves 2 purposes:
         *
         * 1. It keeps track of each interceptor's token object (returned by preInvoke,
         * passed back on postInvoke)
         *
         * 2. It keeps track of which interceptors have actually been preInvoked. Only
         * those interceptors that were preInvoked will be called for postInvoke.
         */
        Map<WolaRequestInterceptor, Object> interceptorTokenMap = new HashMap<WolaRequestInterceptor, Object>();

        /**
         * The response data returned by the EJB, to be sent back on the response.
         */
        byte[] ejbResponseData;
    }

}
