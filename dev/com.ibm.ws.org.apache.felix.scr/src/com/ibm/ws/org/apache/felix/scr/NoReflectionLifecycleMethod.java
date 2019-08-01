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

import org.apache.felix.scr.impl.inject.LifecycleMethod;
import org.apache.felix.scr.impl.inject.MethodResult;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.osgi.service.log.LogService;

public class NoReflectionLifecycleMethod implements LifecycleMethod {

    public enum LifeCycleMethodType {
        ACTIVATE,
        DEACTIVATE,
        MODIFIED
    }
    
    private final StaticComponentManager m_componentLifecycleManager;

    private final LifeCycleMethodType methodType;

    public NoReflectionLifecycleMethod(StaticComponentManager componentLifecycleManager, LifeCycleMethodType methodType) {
        m_componentLifecycleManager = componentLifecycleManager;
        this.methodType = methodType;
	}

	@Override
    public MethodResult invoke(Object componentInstance, ComponentContextImpl<?> componentContext, int reason,
            MethodResult methodCallFailureResult) {
        try {
            ReturnValue val;
            if (methodType == LifeCycleMethodType.ACTIVATE) {
                val = m_componentLifecycleManager.activate(componentInstance, componentContext);
            } else if (methodType == LifeCycleMethodType.DEACTIVATE) {
                val = m_componentLifecycleManager.deactivate(componentInstance, componentContext, reason);
            } else {
                val = m_componentLifecycleManager.modified(componentInstance, componentContext);
            }
            return val == ReturnValue.VOID ? MethodResult.VOID : new MethodResult(val.isVoid(), val.getReturnValue());
        } catch (Exception e) {
            componentContext.getLogger().log( LogService.LOG_ERROR, "The {0} method has thrown an exception", e,
                    methodType );
            if ( methodCallFailureResult != null && methodCallFailureResult.getResult() != null )
            {
                methodCallFailureResult.getResult().put("exception", e);
            }
        }
        return methodCallFailureResult;
	}

}