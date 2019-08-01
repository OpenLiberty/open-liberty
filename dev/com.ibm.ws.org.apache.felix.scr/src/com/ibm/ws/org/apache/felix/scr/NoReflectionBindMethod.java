/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.org.apache.felix.scr;

import org.apache.felix.scr.impl.inject.BindParameters;
import org.apache.felix.scr.impl.inject.InitReferenceMethod;
import org.apache.felix.scr.impl.inject.LifecycleMethod;
import org.apache.felix.scr.impl.inject.MethodResult;
import org.apache.felix.scr.impl.inject.ReferenceMethod;
import org.apache.felix.scr.impl.logger.ComponentLogger;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class NoReflectionBindMethod implements ReferenceMethod, InitReferenceMethod {

    public enum BindMethodType {
        BIND,
        UNBIND,
        UPDATED,
        INIT
    }
    
    private final StaticComponentManager componentManager;
    private final String name;
    private final BindMethodType methodType;

    public NoReflectionBindMethod(StaticComponentManager componentManager, String name, BindMethodType methodType) {
        this.componentManager = componentManager;
        this.name = name;
        this.methodType = methodType;
	}

    @Override
    public <S, T> boolean getServiceObject(BindParameters parameters, BundleContext context) {
        if ( parameters.getServiceObject() == null) {
            return parameters.getServiceObject(context);
        }
        return true;
    }

    @Override
    public <S, T> MethodResult invoke(Object componentInstance, BindParameters parameter, MethodResult methodCallFailureResult) {
        try {
            ReturnValue val;
            if (methodType == BindMethodType.BIND) {
                val = componentManager.bind(componentInstance, name, new Parameters(parameter));
            } else if (methodType == BindMethodType.UNBIND) {
                val = componentManager.unbind(componentInstance, name, new Parameters(parameter));
            } else {
                val = componentManager.updated(componentInstance, name, new Parameters(parameter));
            }
            return val == ReturnValue.VOID ? MethodResult.VOID : new MethodResult(val.isVoid(), val.getReturnValue());
        } catch (Exception e) {
            parameter.getComponentContext().getLogger().log( LogService.LOG_ERROR, "The {0} method has thrown an exception", e,
                    methodType );
            if ( methodCallFailureResult != null && methodCallFailureResult.getResult() != null )
            {
                methodCallFailureResult.getResult().put("exception", e);
            }
        }
        return methodCallFailureResult;
    }

    @Override
    public boolean init(Object instance, ComponentLogger logger) {
        return componentManager.init(instance, name);
    }

}