/*******************************************************************************n * Copyright (c) 2019 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License v1.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-v10.htmln *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package com.ibm.ws.threading.internal;

import java.util.Map;
import java.util.concurrent.ThreadFactory;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.org.apache.felix.scr.Parameters;
import com.ibm.ws.org.apache.felix.scr.ReturnValue;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;
import com.ibm.wsspi.threading.ExecutorServiceTaskInterceptor;

/**
 *
 */
public class ExecutorServiceComponentManager implements StaticComponentManager {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        ((ExecutorServiceImpl) instance).activate((Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        ((ExecutorServiceImpl) instance).deactivate(reason);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        ((ExecutorServiceImpl) instance).modified((Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setInterceptor".equals(name)) {
            Object[] params = parameters.getParameters(ExecutorServiceTaskInterceptor.class);
            ((ExecutorServiceImpl) instance).setInterceptor((ExecutorServiceTaskInterceptor) params[0]);
        } else if ("setThreadFactory".equals(name)) {
            Object[] params = parameters.getParameters(ThreadFactory.class);
            ((ExecutorServiceImpl) instance).setThreadFactory((ThreadFactory) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetInterceptor".equals(name)) {
            Object[] params = parameters.getParameters(ExecutorServiceTaskInterceptor.class);
            ((ExecutorServiceImpl) instance).unsetInterceptor((ExecutorServiceTaskInterceptor) params[0]);
        } else if ("unsetThreadFactory".equals(name)) {
            Object[] params = parameters.getParameters(ThreadFactory.class);
            ((ExecutorServiceImpl) instance).unsetThreadFactory((ThreadFactory) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue updated(Object instance, String name, Parameters parameters) {
        return ReturnValue.VOID;
    }

    @Override
    public boolean init(Object instance, String name) {
        return true;
    }

}
