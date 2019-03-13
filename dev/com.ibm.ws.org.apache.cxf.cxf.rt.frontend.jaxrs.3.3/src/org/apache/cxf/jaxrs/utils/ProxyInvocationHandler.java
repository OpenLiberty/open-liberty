/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.apache.cxf.jaxrs.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This class is used to wrap message context created in service bundle
 * as the application provided message context interface.
 * 
 * @author Grant
 * 
 */
public class ProxyInvocationHandler implements InvocationHandler {

    private final Object target;

    public ProxyInvocationHandler(Object target) {
        this.target = target;
    }

    private boolean matchMethod(Method left, Method right) {
        if ((left.getName().equals(right.getName())) &&
            (left.getParameterTypes().length == right.getParameterTypes().length)) {

            Set<Class<?>> parametersLeft = new HashSet<Class<?>>();
            parametersLeft.addAll(Arrays.asList(left.getParameterTypes()));

            Set<Class<?>> parametersRight = new HashSet<Class<?>>();
            parametersRight.addAll(Arrays.asList(right.getParameterTypes()));

            return parametersLeft.equals(parametersRight);
        }
        return false;
    }

    private Method findTargetMethod(Method method) {
        Method[] methods = target.getClass().getMethods();
        for (Method currentMethod : methods) {
            if (matchMethod(currentMethod, method))
                return currentMethod;
            else
                continue;
        }
        return null;
    }

    @Override
    @Trivial
    public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {

        Method targetMethod = findTargetMethod(method);
        if (targetMethod != null) {
            return targetMethod.invoke(target, args);
        } else
            throw new IllegalArgumentException("no matched method found:" + method.toString());
    }
}
//Liberty Change for CXF End