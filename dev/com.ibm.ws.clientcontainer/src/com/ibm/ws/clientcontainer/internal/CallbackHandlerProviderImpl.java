/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.security.auth.callback.CallbackHandler;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.clientcontainer.metadata.CallbackHandlerProvider;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;

/**
 * An implementation of CallbackHandlerProvider.
 */
public class CallbackHandlerProviderImpl implements CallbackHandlerProvider {
    private static final TraceComponent tc = Tr.register(CallbackHandlerProviderImpl.class, "clientContainer", "com.ibm.ws.clientcontainer.resources.Messages");
    final private CallbackHandler callbackHandler;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public CallbackHandlerProviderImpl(ModuleInfo moduleInfo, String callbackHandlerName) {
        if (callbackHandlerName == null) {
            callbackHandler = null;
            return;
        }
        Class<?> callbackHandlerClass = null;
        ClassLoader cl = moduleInfo.getClassLoader();
        try {
            callbackHandlerClass = cl.loadClass(callbackHandlerName);
            final java.lang.reflect.Constructor c = callbackHandlerClass.getDeclaredConstructor((Class<?>[]) null);
            AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                    c.setAccessible(true);
                    return c;
                }
            });
            callbackHandler = (CallbackHandler) c.newInstance((Object[]) null);
        } catch (NoSuchMethodException nme) {
            Tr.error(tc, "MISSING_NOARGS_CONSTRUCTOR_CWWKC2451E");
            throw new IllegalArgumentException(nme);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }
}
