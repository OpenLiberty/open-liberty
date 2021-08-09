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
package com.ibm.ws.cdi.ejb.apps.aroundconstruct;

import static com.ibm.ws.cdi.ejb.apps.aroundconstruct.AroundConstructLogger.ConstructorType.DEFAULT;
import static com.ibm.ws.cdi.ejb.utils.Utils.id;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class AroundConstructLogger {

    public static enum ConstructorType {
        INJECTED, DEFAULT
    }

    private final List<String> constructorInterceptors = new ArrayList<String>();
    private final List<String> interceptedBeans = new ArrayList<String>();
    private ConstructorType beanConstructorUsed = DEFAULT;
    private Constructor<?> constructor = null;
    private Object target = null;

    public void addConstructorInterceptor(final Class<?> interceptor) {
        constructorInterceptors.add(id(interceptor));
    }

    public List<String> getConstructorInterceptors() {
        return constructorInterceptors;
    }

    public List<String> getInterceptedBeans() {
        return interceptedBeans;
    }

    public void setConstructorType(final ConstructorType type) {
        beanConstructorUsed = type;
    }

    public ConstructorType getConstructorType() {
        return beanConstructorUsed;
    }

    public void setConstructor(final Constructor<?> constructor) {
        this.constructor = constructor;
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }

    public void setTarget(final Object target) {
        this.target = target;
    }

    public Object getTarget() {
        return target;
    }
}
