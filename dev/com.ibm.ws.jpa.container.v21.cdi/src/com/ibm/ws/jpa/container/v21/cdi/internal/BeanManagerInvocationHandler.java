/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.v21.cdi.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.enterprise.inject.spi.BeanManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class BeanManagerInvocationHandler implements InvocationHandler {
    private final static TraceComponent tc = Tr.register(BeanManagerInvocationHandler.class);

    private final CDIService cdiService;
    private Object target; //BeanManager or IBMHibernateExtendedBeanManager. IBMHibernateExtendedBeanManager cannot implement BeanManager without pulling in a dependency on javax.el
    private IBMHibernateExtendedBeanManager extendedBeanManager;

    BeanManagerInvocationHandler(CDIService cdiService) {
        this.cdiService = cdiService;
    }

    BeanManagerInvocationHandler(CDIService cdiService, IBMHibernateExtendedBeanManager extendedBeanManager) {
        this.cdiService = cdiService;
        this.extendedBeanManager = extendedBeanManager;
    }

    @Override
    public String toString() {
        return super.toString() + '[' + target + ']';
    }

    @Override
    @FFDCIgnore(InvocationTargetException.class)
    // proxy is @Sensitive to avoid infinite recursion
    // args is @Sensitive to avoid tracing parameters we don't care about
    public Object invoke(@Sensitive Object proxy, Method method, @Sensitive Object[] args) throws Throwable {
        Object ret;

        String methodName = method.getName();
        if (method.getDeclaringClass() == Object.class) {
            if ("toString".equals(methodName)) {
                return proxy.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(proxy)) + '[' + this + ']';
            }
            if ("equals".equals(methodName)) {
                return proxy == args[0];
            }
            if ("hashCode".equals(methodName)) {
                return getTarget().hashCode();
            }
            throw new UnsupportedOperationException(method.toString());
        }

        try {
            //This method takes an argument of ExtendedBeanManager.LifecycleListener but this class is not available at compile time. 
            if (methodName.equals("registerLifecycleListener")) {
                if (args.length != 1) {
                    throw new IllegalStateException("registerLifecycleListener should have one argument"); //Basic sanity check without doing a classloader lookup.
                } 
                Object listener = args[0]; //only one argument. 
                Method proxyMethod = extendedBeanManager.getClass().getMethod("registerLifecycleListener", Object.class);
                ret = proxyMethod.invoke(extendedBeanManager, listener);
            //All other methods will be going to the bean manager, not the extended bean manager.
            } else {
                ret = method.invoke(getTarget(), args);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invoked BeanManager method", cdiService, method, args, ret);
            }
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        return ret;
    }

    //To avoid depending on the application classloader having access to javax.el we need to avoid direct implimentations of the BeanManager interface
    private synchronized Object getTarget() {
        if (target == null) {
            //We call getCurrentModuleBeanManager instead of the more common getCurrentBeanManager because this class will be in a BDA. We want to find the correct application archive bean manager, not the bean manager for this internal liberty jar.
            BeanManager bm = cdiService.getCurrentModuleBeanManager();
            //As a fallback we can use a stack walk. 
            if (bm == null) {
                bm = cdiService.getCurrentBeanManager();
            }
            if (bm == null) {
                throw new UnsupportedOperationException("No current bean manager found in CDI service");
            }
            target = bm;
        }
        return target;
    }
}
