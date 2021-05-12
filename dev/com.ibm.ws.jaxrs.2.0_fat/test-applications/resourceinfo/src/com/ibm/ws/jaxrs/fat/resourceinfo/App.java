/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.resourceinfo;


import java.lang.reflect.Method;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

@ApplicationPath("/app")
public class App extends Application {
    static ThreadLocal<Class<?>> classFromRequestFilter = new ThreadLocal<>();
    static ThreadLocal<Method> methodFromRequestFilter = new ThreadLocal<>();
    static ThreadLocal<Class<?>> classFromPreMatchRequestFilter = new ThreadLocal<>();
    static ThreadLocal<Method> methodFromPreMatchRequestFilter = new ThreadLocal<>();
    static ThreadLocal<Class<?>> classFromResponseFilter = new ThreadLocal<>();
    static ThreadLocal<Method> methodFromResponseFilter = new ThreadLocal<>();

    static Response process(Class<?> c, Method m) {
        ResponseBuilder rb = Response.ok();
        rb = rb.header("ClassFromResource", getClassString(c));
        rb = rb.header("MethodFromResource", getMethodString(m));

        rb = rb.header("ClassFromRequestFilter", getClassString(classFromRequestFilter.get()));
        rb = rb.header("MethodFromRequestFilter", getMethodString(methodFromRequestFilter.get()));

        rb = rb.header("ClassFromPreMatchRequestFilter", getClassString(classFromPreMatchRequestFilter.get()));
        rb = rb.header("MethodFromPreMatchRequestFilter", getMethodString(methodFromPreMatchRequestFilter.get()));

        return rb.build();
    }

    static String getClassString(Class<?> c) {
        return c == null ? "NULL" : c.getName();
    }

    static String getMethodString(Method m) {
        return m == null ? "NULL" : m.getDeclaringClass().getName() + " " + m.getName();
    }
}
