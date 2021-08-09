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

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Test callback handler factory class used to validate that the WSCallbackHandlerFactory
 * can load classes using Class.forName(factoryClassName);
 */
public class TestCallbackHandlerFactory extends WSCallbackHandlerFactory {

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(String userName, String password) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(String userName, String realmName, String password) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(String userName, String realmName, String ccacheFile, String defaultCcache) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(String userName, String realmName, String password, List tokenHolderList) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(String userName, String realmName, String password, HttpServletRequest req, HttpServletResponse resp, Map appContext) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(String userName, String realmName, List tokenHolderList) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(String userName, String realmName, List tokenHolderList, Map appContext) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(String realmName, X509Certificate[] certChain) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(String realmName, X509Certificate[] certChain, List tokenHolderList) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(String realmName, X509Certificate[] certChain, HttpServletRequest req, HttpServletResponse resp, Map appContext) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(byte[] credToken) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(byte[] credToken, String authMechOid) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(byte[] credToken, List tokenHolderList) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(byte[] credToken, List tokenHolderList, String authMechOid) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(byte[] credToken, HttpServletRequest req, HttpServletResponse resp, List tokenHolderList, Map appContext, String authMechOid) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(byte[] credToken, HttpServletRequest req, HttpServletResponse resp, Map appContext) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(byte[] credToken, HttpServletRequest req, HttpServletResponse resp, Map appContext, List tokenHolderList) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackHandler getCallbackHandler(Object protocolPolicy) {
        // TODO Auto-generated method stub
        return null;
    }

}
