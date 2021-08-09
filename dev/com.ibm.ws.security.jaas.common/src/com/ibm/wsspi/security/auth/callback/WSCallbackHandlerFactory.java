/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.auth.callback;

import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A factory for CallbackHandler in WAS security to enable ISV and security provider
 * to extend WAS default CallbackHandler.
 * 
 * @ibm-spi
 */
@SuppressWarnings("unchecked")
public abstract class WSCallbackHandlerFactory {

    /*
     * The constructor of a WSCallbackhandlerFactory should take a java.util.Map parameter,
     * which is used by security runtime to pass parameters to initialize the WSCallbackHandlerFactory.
     * The parameters would include a reference of the TrustAssociationManager and
     * the Authentication Cache.
     */

    private final static TraceComponent tc = Tr.register(WSCallbackHandlerFactory.class);
    private static WSCallbackHandlerFactory factory = null;

    public static WSCallbackHandlerFactory getInstance(String cbFactory)
                    throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (factory == null) {
            if ((cbFactory != null) && (cbFactory.equals("com.ibm.ws.security.jaas.common.internal.callback.WSCallbackHandlerFactoryImpl"))) {
                factory = new com.ibm.ws.security.jaas.common.internal.callback.WSCallbackHandlerFactoryImpl();
            } else {
                // TODO: We might have to look into shared libraries
                factory = (WSCallbackHandlerFactory) Class.forName(cbFactory).newInstance();
            }
        }
        if (factory == null) {
            throw new NullPointerException("WSCallbackHandlerFactory not initialized");
        }
        return factory;
    }

    public static WSCallbackHandlerFactory getInstance() {
        if (factory == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "WSCallbackHandlerFactory() initialized to com.ibm.ws.security.jaas.common.internal.callback.WSCallbackHandlerFactoryImpl");
            }
            factory = new com.ibm.ws.security.jaas.common.internal.callback.WSCallbackHandlerFactoryImpl();
        }
        if (factory == null) {
            throw new NullPointerException("WSCallbackHandlerFactory not initialized");
        }
        return factory;
    }

    public abstract CallbackHandler getCallbackHandler(String userName, String password);

    public abstract CallbackHandler getCallbackHandler(String userName, String realmName, String password);

    public abstract CallbackHandler getCallbackHandler(String userName, String realmName, String ccacheFile, String defaultCcache);

    public abstract CallbackHandler getCallbackHandler(String userName, String realmName, String password, java.util.List tokenHolderList);

    public abstract CallbackHandler getCallbackHandler(String userName, String realmName, String password, HttpServletRequest req, HttpServletResponse resp, Map appContext);

    public abstract CallbackHandler getCallbackHandler(String userName, String realmName, java.util.List tokenHolderList);

    public abstract CallbackHandler getCallbackHandler(String userName, String realmName, java.util.List tokenHolderList, Map appContext); // 685272 

    public abstract CallbackHandler getCallbackHandler(String realmName, java.security.cert.X509Certificate[] certChain);

    public abstract CallbackHandler getCallbackHandler(String realmName, java.security.cert.X509Certificate[] certChain, java.util.List tokenHolderList);

    public abstract CallbackHandler getCallbackHandler(String realmName, java.security.cert.X509Certificate[] certChain, HttpServletRequest req, HttpServletResponse resp,
                                                       Map appContext);

    public abstract CallbackHandler getCallbackHandler(byte[] credToken);

    public abstract CallbackHandler getCallbackHandler(byte[] credToken, String authMechOid);

    // for security attribute propagation
    public abstract CallbackHandler getCallbackHandler(byte[] credToken, java.util.List tokenHolderList);

    public abstract CallbackHandler getCallbackHandler(byte[] credToken, java.util.List tokenHolderList, String authMechOid);

    public abstract CallbackHandler getCallbackHandler(byte[] credToken, HttpServletRequest req, HttpServletResponse resp, java.util.List tokenHolderList, Map appContext,
                                                       String authMechOid);

    public abstract CallbackHandler getCallbackHandler(byte[] credToken, HttpServletRequest req, HttpServletResponse resp, Map appContext);

    public abstract CallbackHandler getCallbackHandler(byte[] credToken, HttpServletRequest req, HttpServletResponse resp, Map appContext, java.util.List tokenHolderList);

    // for use by the protocol for mapping or security attribute propagation
    public abstract CallbackHandler getCallbackHandler(java.lang.Object protocolPolicy);
}
